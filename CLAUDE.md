# Messenger — контекст проекта для ИИ-агентов

_Последнее обновление: 2026-04-25 (E2EE-вложения для Web и Android добавлены, Этап 6 в процессе)_

## Общее

Дипломный проект: мессенджер с Go-бэкендом, gRPC/gRPC-Web API, Web-клиентом на Vanilla JS и Android-клиентом на Kotlin/Compose.

Текущий статус разработки:
- **Этап 5 завершён**: `server + web + E2EE` — полностью реализованы и работают.
- **Этап 6 в процессе**: Android-клиент написан, собирается, работает flow `login → conversations → chat с E2EE`, включая E2EE-вложения.
- при значимых изменениях `proto`, state-модели, key lifecycle и архитектуры нужно обновлять этот файл.

## Актуальная структура

```text
messenger/                          ← Go-бэкенд + Web (этот репозиторий)
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
│       ├── media-e2ee.js        — AES-GCM для media blobs + descriptor format
│       ├── group-editor.js
│       └── group-permissions.js
└── CLAUDE.md

~/AndroidStudioProjects/messenger/ ← Android-клиент (отдельный путь)
├── app/src/main/java/com/example/messenger/
│   ├── App.kt                   — Application, lazy-init grpc/stores/eventService
│   ├── MainActivity.kt
│   ├── crypto/
│   │   ├── E2EE.kt              — P-256 ECDH, HKDF-SHA256, AES-GCM
│   │   └── IdentityStore.kt     — DataStore-персистентность ключей
│   ├── data/
│   │   ├── GrpcManager.kt       — ManagedChannel, stub factories
│   │   ├── AuthInterceptor.kt   — Bearer token interceptor
│   │   ├── TokenStore.kt        — DataStore<Preferences>
│   │   ├── AuthRepository.kt    — login / register
│   │   ├── MessengerRepository.kt — все gRPC-вызовы
│   │   └── EventService.kt      — фоновый stream событий с auto-reconnect
│   └── ui/
│       ├── AppNav.kt            — NavHost: LOGIN → CONVERSATIONS → CHAT
│       ├── auth/                — LoginScreen, RegisterScreen, AuthViewModel
│       ├── conversations/       — ConversationListScreen + ViewModel
│       ├── chat/                — ChatScreen + ChatViewModel (E2EE decrypt + attachments)
│       └── theme/               — Material 3, violet palette
├── app/src/main/java/com/example/messenger/proto/  ← сгенерированные gRPC-stubs
│   └── *.java, *.kt             — proto-классы + Kotlin DSL + gRPC Kotlin coroutine stubs
└── app/src/main/proto/          ← исходный .proto (копия серверного)
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
- media/attachment RPC и модели:
  - `PrepareMediaUpload`
  - `UploadMedia`
  - `GetMedia`
  - `AttachmentKind`
  - `AttachmentPreview`
  - `Attachment`

Замечание:
- поле `text` в `Message` пока сохранено для совместимости, но в E2EE flow серверный Web-путь использует ciphertext;
- `GetConversationKey(version=0)` трактуется как запрос последней версии группового ключа.

### Proto-генерация для Android

Proto-стабы для Android генерируются вручную (protobuf-gradle-plugin несовместим с AGP 9.x):

```bash
/home/kot/stydi/diplom/messenger/scripts/protoc.sh \
  -I /home/kot/stydi/diplom/messenger/.tools/protoc-29.3/include \
  -I app/src/main/proto \
  --plugin=protoc-gen-grpc-java=/home/kot/AndroidStudioProjects/messenger/.tools/protoc-gen-grpc-java \
  --plugin=protoc-gen-grpc-kotlin=/home/kot/AndroidStudioProjects/messenger/.tools/protoc-gen-grpc-kotlin \
  --java_out=lite:app/src/main/java \
  --kotlin_out=lite:app/src/main/java \
  --grpc-java_out=lite:app/src/main/java \
  --grpc-kotlin_out=lite:app/src/main/java \
  app/src/main/proto/messenger/v1/messenger.proto
```

Флаг `lite` для `--grpc-java_out` обязателен: без него генерируется код с `io.grpc.protobuf.ProtoUtils`, которого нет в `grpc-protobuf-lite`.

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
  - encrypted message delivery;
  - media upload/download для E2EE-вложений.
- Сервер хранит:
  - пользователей;
  - сообщения;
  - `message_attachments`;
  - `media_objects`;
  - группы и membership rules;
  - identity public keys;
  - версии групповых ключей и envelopes;
  - encrypted media blobs на локальном диске (`MEDIA_DIR`, по умолчанию `./data/media`).

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
    Attachments    []Attachment
    TS             time.Time
}
```

Поведение:
- plaintext-путь всё ещё не удалён полностью;
- encrypted message требует `ciphertext + nonce + sender_key_id`;
- attachment-only message допустим даже без `text`;
- PostgreSQL search ищет только по `encrypted = FALSE`, потому что сервер не умеет искать по plaintext внутри E2EE.

### Media model

Для вложений используется отдельный media path:
- клиент сначала вызывает `PrepareMediaUpload`;
- потом загружает уже **зашифрованный** blob через `UploadMedia`;
- потом отправляет `SendMessage` с `attachments`, где лежит не plaintext descriptor, а `encrypted_descriptor + descriptor_nonce`.

`media_objects` хранит:
- `media_id`
- `owner_username`
- `storage_key`
- `size_bytes`
- `ciphertext_size`
- `mime_type`
- `filename`
- `kind`
- `nonce`
- `sha256`
- `uploaded`

`message_attachments` хранит:
- `message_id`
- `attachment_id`
- `media_id`
- `kind`
- `filename`
- `mime_type`
- `size_bytes`
- `encrypted_descriptor`
- `descriptor_nonce`
- `sha256`
- `ciphertext_size`

Правила:
- лимит объекта: `25 МБ`;
- сервер никогда не видит plaintext файла и не знает `media_key`;
- `GetMedia` разрешается только если пользователь имеет доступ к сообщению/диалогу, к которому привязан `media_id`;
- при `DeleteAccount` удаляются media metadata пользователя и соответствующие encrypted blobs с диска.

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

## Фронтенд (Web)

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

### E2EE-вложения на Web

- blob каждого вложения шифруется отдельным случайным `media key` через AES-GCM;
- descriptor (`media_key`, hash, mime, filename, size) сериализуется в JSON и отдельно шифруется ключом чата:
  - direct: direct prekey flow;
  - group: текущий group key;
- изображения рендерятся inline через `objectURL`;
- видео и прочие файлы показываются как download-card;
- расшифрованные blobs кэшируются только в памяти (`state.mediaCache`).

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

## Android-клиент

### Технологии

- Kotlin + Jetpack Compose + Material 3
- AGP 9.1.1, Gradle 9.3.1, Kotlin 2.0.21, compileSdk 36
- `grpc-okhttp` + `grpc-kotlin-stub` + `grpc-protobuf-lite`
- `protobuf-kotlin-lite` (lite runtime для Android)
- DataStore<Preferences> для хранения токена и identity state (JSON)
- Navigation Compose

### UI и дизайн

- Android UI приведён к мобильному макету из `Messenger/MessengerMobile.html`
- основной стиль:
  - тёмный фон `#0B0B10 / #0D0D16`
  - панели `#13131E / #181828`
  - акцент `#7C6FFF`
  - pill/glass action buttons
  - крупный заголовок списка чатов
  - отдельная строка поиска
  - карточный контейнер списка диалогов
  - переработанный composer и header экрана чата
- ключевые Compose-файлы по новому стилю:
  - `ui/theme/Color.kt`
  - `ui/theme/Theme.kt`
  - `ui/components/MobileChrome.kt`
  - `ui/conversations/ConversationListScreen.kt`
  - `ui/chat/ChatScreen.kt`
- поиск по списку диалогов вынесен в helper `ui/conversations/ConversationSearch.kt` с unit-тестом
- в Android список диалогов больше не использует ручную кнопку `Refresh`:
  - обновление приходит через `ServerEvent`
  - дополнительно выполняется reload списка на `ON_RESUME`
- если Android получает `message` event для direct conversation, которой ещё нет в локальном списке, `ConversationListViewModel` делает автоматический reload, чтобы новый чат появился сразу
- строка поиска на Android теперь делает реальный `SearchUsers` через gRPC и позволяет открыть direct chat с найденным пользователем, даже если диалог ещё не был создан ранее

### E2EE на Android

`crypto/E2EE.kt` реализует на Android JCE:
- `generateKeyPair()` — P-256 через `KeyPairGenerator("EC")`
- `exportPublicKey()` — 65-байтовый raw uncompressed point (0x04 || X || Y)
- `importPublicKey()` — добавляет X.509 SubjectPublicKeyInfo header для P-256
- `exportPrivateKey()` / `importPrivateKey()` — PKCS8 encoding
- `ecdh()` — **ВАЖНО**: output всегда дополняется до 32 байт (left-pad), потому что WebCrypto `deriveBits(..., 256)` всегда возвращает ровно 32 байта, а JVM `generateSecret()` может вернуть меньше при ведущих нулях x-координаты
- `hkdf()` — RFC 5869 HKDF-SHA256, salt = 32 нулевых байта (совпадает с Web)
- `aesGcmEncrypt()` / `aesGcmDecrypt()` — AES-256-GCM, 12-байтовый nonce, 128-битный тег
- `encryptMedia()` / `decryptMedia()` — AES-GCM для вложений + hash verification по descriptor JSON

`crypto/IdentityStore.kt`:
- хранит identity state в DataStore как JSON (Base64-encoded key bytes)
- 1 identity key pair, 1 signed prekey, 10 one-time prekeys

Ключевое правило совместимости Web ↔ Android:
- Все public key bytes — 65-байтовый raw uncompressed P-256 point
- HKDF salt = 32 нулевых байта, info = `"messenger-direct-prekey-v1"` или `"messenger-group-envelope-v1"`
- ECDH output всегда 32 байта (дополняем left-pad если нужно)

### Подключение к серверу

- `GrpcManager.SERVER_HOST = "10.0.2.2"` — адрес хост-машины из эмулятора
- `SERVER_PORT = 9090` — plaintext gRPC (usePlaintext)
- Для физического устройства нужно изменить HOST на IP хост-машины в локальной сети
- `network_security_config.xml` разрешает cleartext для `10.0.2.2` и `localhost`

### AuthViewModel flow

При логине:
1. gRPC `Login` → получить token
2. `TokenStore.save(token, username)`
3. `ensureIdentityPublished()`:
   - если identity не найдена → `generate()` (1 identity + 1 SPK + 10 OTPs)
   - если `published = false` → `PublishIdentityKey` + `PublishPrekeyBundle`
   - сохранить identity с `published = true`
4. `EventService.start(token)` — запустить фоновый stream

### ChatViewModel

- `init()` → `loadMessages()` + `collectEvents()`
- `decodeMessage()` — suspend, inline в `list.map { }` (работает т.к. `map` inline)
- `decryptGroupMsg()` — group key кэшируется в `groupKeys` Map по `"convId:version"`
- `decryptDirectMsg()` — sender identity key кэшируется в `senderPubCache`
- тип дешифровки на Android должен определяться **контекстом открытого чата** (`isGroup`), а не эвристикой по полям `Message`; иначе direct-сообщение можно ошибочно отправить в `GetConversationKey`
- `collectEvents()` в Android должен фильтровать новые сообщения по `message.conversationId == activeConversationId`
- для direct `SendMessage` Android **не должен** передавать `conversationId`; сервер трактует непустой `conversationId` как group conversation. Для личного чата нужно передавать только `to`, а server сам соберёт deterministic direct conversation id
- при публикации нового `signed prekey` сервер сбрасывает старую очередь `one-time prekeys` для пользователя; это защищает Android/Web от рассинхрона после регенерации локальной identity
- Android при логине должен проверять, что локальная `identity` принадлежит текущему `username`; если нет, генерируется и публикуется новый набор ключей
- если Android получает `UNAUTHENTICATED/unauthorized` на post-login экранах, приложение очищает локальную сессию и возвращает пользователя на экран логина
- ошибки дешифрования логируются через `Log.e("E2EE", ...)` для диагностики
- Android `ChatViewModel` materialize-ит attachments:
  - `image` → inline bitmap;
  - `video/file` → временный файл в `cacheDir` + открытие через `FileProvider`.

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

### Бэкенд и Web

```bash
make proto
make test
make test-integration
make test-web

cd web && npm run build
docker compose up -d --build
```

### Proto-инструменты

Используется wrapper `scripts/protoc.sh` — скачивает `protoc` в `.tools/protoc-29.3/` при первом запуске.

### Android

Открыть `~/AndroidStudioProjects/messenger` в Android Studio.

Если proto-файлы изменились, регенерировать стабы командой (см. раздел «Proto-генерация для Android»), затем скопировать вывод в `app/src/main/java/`.

Gradle sync и сборка через Android Studio (compileSdk 36, minSdk 24).

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
- Direct E2EE использует identity key + signed prekey + one-time prekeys, но без полноценного double ratchet и без device-to-device multi-session model.
- Server-side search по encrypted сообщениям не поддерживается.
- Android UI пока не даёт полноценного группового менеджмента как на Web, но текущий `ChatScreen` уже умеет отправлять E2EE group messages и group attachments при наличии conversation.
- После logout Android генерирует новые ключи; старые OTP на сервере становятся недостижимыми (сервер их хранит, но private-части уже нет).
- Android attachment picker сейчас однофайловый за сообщение; Web уже умеет несколько вложений в одном `SendMessage`.
- На Android inline preview есть только для изображений; видео пока открывается внешним viewer как файл.
