# Messenger — контекст проекта для ИИ-агентов

_Последнее обновление: 2026-04-24 (добавлены prekey bundle для direct E2EE и атомарная server-side ротация group keys)_

## Общее

Дипломный проект: мессенджер с Go-бэкендом, gRPC/gRPC-Web API, Web-клиентом на Vanilla JS и подготовленным контрактом для второй платформы.

Текущий приоритет разработки:
- сначала доводится `server + web + E2EE`;
- Android ещё не реализован в рабочем дереве;
- при значимых изменениях `proto`, state-модели, key lifecycle и архитектуры нужно обновлять этот файл.

## Актуальная структура

```text
messenger/
├── api/proto/messenger/v1/messenger.proto
├── cmd/server/main.go
├── internal/
│   ├── auth/                    — регистрация, логин, токены
│   ├── grpcapi/server.go        — gRPC API
│   └── store/
│       ├── users.go             — user store interface + memory
│       ├── postgres_users.go    — postgres user store
│       ├── messages.go          — message store interface + memory
│       ├── postgres.go          — postgres message store
│       ├── conversations.go     — группы, роли, membership rules
│       ├── postgres_conversations.go
│       ├── keys.go              — identity keys + group key packages (memory)
│       └── postgres_keys.go     — identity keys + group key packages (postgres)
├── gen/messenger/v1/            — Go-код, сгенерированный из proto
├── scripts/protoc.sh            — скачивает/кеширует protoc локально
├── web/
│   ├── package.json
│   ├── src/main.js              — основной UI и orchestration
│   └── src/lib/
│       ├── api.js
│       ├── auth-ui.js
│       ├── chat-state.js
│       ├── e2ee.js              — WebCrypto helper-ы
│       ├── group-editor.js
│       └── group-permissions.js
└── CLAUDE.md
```

## Proto и публичные контракты

`messenger.proto` уже включает:
- обычные auth/user/message RPC;
- группы и роли (`CreateGroupConversation`, `AddGroupMembers`, `RemoveGroupMember`, `LeaveGroupConversation`, `TransferGroupAdmin`);
- события `conversation_removed`;
- E2EE-сущности:
  - `IdentityKey`
  - `PublishIdentityKey`
  - `PublishPrekeyBundle`
  - `GetIdentityKey`
  - `GetIdentityKeys`
  - `AcquirePrekeyBundle`
  - `SignedPrekey`
  - `OneTimePrekey`
  - `PrekeyBundle`
  - `ConversationKeyEnvelope`
  - `ConversationKey`
  - `UpsertConversationKey`
  - `GetConversationKey`
  - `GroupKeyUpdate`
- encrypted message fields в `Message` / `SendMessageRequest`:
  - `ciphertext`
  - `nonce`
  - `sender_key_id`
  - `conversation_key_version`
  - `encrypted`
  - `recipient_signed_prekey_id`
  - `recipient_signed_prekey_public`
  - `recipient_one_time_prekey_id`
  - `recipient_one_time_prekey_public`

Замечание:
- поле `text` в `Message` пока сохранено для совместимости, но в E2EE flow серверный Web-путь использует ciphertext;
- `GetConversationKey(version=0)` трактуется как запрос последней версии группового ключа.

## Бэкенд

### Что реализовано

- `internal/grpcapi/server.go` обслуживает:
  - профили и поиск пользователей;
  - личные и групповые диалоги;
  - роли в группе;
  - identity key publication/fetch;
  - signed prekeys + one-time prekeys;
  - atomic prekey bundle acquisition;
  - group conversation key packages;
  - server-side atomic membership + group key rotation, если клиент передаёт `initial_key` / `next_key`;
  - encrypted message delivery.
- Сервер хранит:
  - пользователей;
  - сообщения;
  - группы и membership rules;
  - identity public keys;
  - версии групповых ключей и envelopes.

### Message model

`store.Message` теперь поддерживает оба режима:

```go
type Message struct {
    ID             int64
    ConversationID string
    From           string
    To             string
    Text           string
    Ciphertext     []byte
    Nonce          []byte
    SenderKeyID    string
    KeyVersion     int32
    Encrypted      bool
    RecipientSignedPrekeyID     string
    RecipientSignedPrekeyPublic []byte
    RecipientOneTimePrekeyID    string
    RecipientOneTimePrekeyPublic []byte
    TS             time.Time
}
```

Поведение:
- plaintext-путь всё ещё не удалён полностью;
- encrypted message требует `ciphertext + nonce + sender_key_id`;
- PostgreSQL search ищет только по `encrypted = FALSE`, потому что сервер не умеет искать по plaintext внутри E2EE.

### Key store

Есть отдельный `KeyStore`:
- `UpsertIdentityKey`
- `GetIdentityKey`
- `GetIdentityKeys`
- `UpsertSignedPrekey`
- `PutOneTimePrekeys`
- `AcquirePrekeyBundle`
- `UpsertConversationKey`
- `GetConversationKey`
- `DeleteUser`

Отдельно есть `GroupStateStore`, который поверх conversation/key storage выполняет:
- atomic `CreateGroupConversation + initial key`;
- atomic `AddGroupMembers + next key`;
- atomic `RemoveGroupMember + next key`;
- atomic `LeaveGroupConversation + next key`, если после выхода группа остаётся непустой.

## Фронтенд

### Технологии

- Vanilla JS
- Vite
- `@connectrpc/connect-web`
- `@bufbuild/protobuf`
- WebCrypto API

### Основная архитектура UI

- один глобальный `state`;
- `render()` полностью пересобирает `app.innerHTML`;
- часть UI патчится точечно без полного render:
  - `patchGroupUserList()`
  - `bindGroupToggleHandlers()`
  - `patchSearchUI()`

### Важные state-поля

Помимо обычных `token / conversations / messages / profile / unread`, сейчас есть E2EE-состояние:

```js
{
  identity,        // локальная identity key pair + signed prekey + one-time prekeys текущего пользователя
  identityKeys,    // Map<username, IdentityKey proto>
  groupKeys,       // Map<conversationId:version, Uint8Array>
  encryptionPrefs, // Map<conversationId, boolean>, локальный флаг E2EE on/off для конкретного чата
  e2eeReady,       // опубликован ли identity key и загружено ли локальное состояние
}
```

### E2EE на Web

`web/src/lib/e2ee.js` реализует:
- генерацию identity key pair;
- генерацию signed prekey и one-time prekeys;
- экспорт/импорт identity state;
- direct encryption/decryption через recipient prekey bundle;
- group key package generation;
- envelope decryption;
- group message encryption/decryption;
- сериализацию group key в `localStorage`.

Текущая схема:
- direct chats:
  - Web публикует identity public key через `PublishIdentityKey`;
  - Web публикует signed prekey + one-time prekeys через `PublishPrekeyBundle`;
  - отправитель получает bundle получателя через `AcquirePrekeyBundle`;
  - plaintext шифруется на клиенте на базе recipient signed/one-time prekeys;
  - сервер получает только ciphertext.
- group chats:
  - клиент-инициатор генерирует симметричный group key;
  - для каждого участника создаётся envelope;
  - при create/add/remove/leave новый `GroupKeyUpdate` передаётся прямо в membership RPC;
  - сервер фиксирует membership change и новую версию group key в одном действии;
  - сообщения в группе шифруются group key и несут `conversation_key_version`.

### Поиск сообщений

После E2EE поиск на Web работает локально по уже расшифрованным сообщениям из `state.messages`.

Важно:
- серверный `SearchMessages` больше не годится как основной механизм поиска для encrypted history;
- текущий Web-поиск не тянет всю историю со всех диалогов автоматически, а ищет по уже загруженным сообщениям.

### UX-поведение для E2EE

- в пузырьках сообщений есть бейдж `E2EE`;
- при ошибке дешифрования текст заменяется на `[Не удалось расшифровать]`;
- `handleServerEvent()` для входящих сообщений сначала пытается расшифровать payload, потом обновляет `state.messages`.
- если у собеседника ещё нет опубликованного `identity key`, прямой чат всё равно открывается в UI, но отправка сообщения останавливается с явным сообщением, что пользователь ещё не входил в зашифрованную версию и должен сначала опубликовать ключ.
- в шапке чата есть локальный переключатель `E2EE`; если он выключен для конкретного `conversationId`, Web отправляет plaintext через тот же серверный контракт.

## Группы и права

Поддерживаются два уровня прав:
- `admin`
  - добавляет и удаляет любых участников;
  - передаёт права другому;
- `member`
  - может добавлять участников;
  - может удалять только тех, кого сам добавил.

При выходе администратора:
- сервер передаёт роль следующему участнику.

## Сборка и запуск

### Proto

Не нужно вручную подготавливать `/tmp/protoc29`.

Используется wrapper:
- `scripts/protoc.sh`
- он скачивает `protoc` в `.tools/protoc-29.3/` при первом запуске.

### Команды

```bash
make proto
make test
make test-integration
make test-web

cd web && npm run build
docker compose up -d --build
```

## Тесты

Есть следующие уровни тестов:
- `internal/auth` — unit tests;
- `internal/grpcapi` — bufconn gRPC tests;
- `internal/store` — memory tests;
- `internal/store` с тегом `integration` — PostgreSQL + testcontainers;
- `web/src/lib/*.test.js` — unit tests для чистых helper-модулей, включая `e2ee.js`.

Критическое правило проекта:
- для нового функционального кода сначала пишутся тесты, потом реализация.

## Текущие ограничения

- E2EE реализован как учебная модель, а не production-grade Signal-протокол.
- Direct E2EE уже использует identity key + signed prekey + one-time prekeys, но без полноценного double ratchet и без device-to-device multi-session model.
- Server-side search по encrypted сообщениям не поддерживается.
- Android-клиент ещё не начат и должен строиться поверх уже существующего `proto` и E2EE-контракта.
