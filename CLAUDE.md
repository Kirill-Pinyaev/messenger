# Messenger — контекст проекта для ИИ-агентов

_Последнее обновление: 2026-04-24 (добавлены настройки)_

## Общее

Дипломный проект: мессенджер с gRPC-Web фронтендом и Go бэкендом.

- **Бэкенд**: Go, connectrpc, PostgreSQL
- **Фронтенд**: Vanilla JS (ES modules), Vite, connectrpc/gRPC-Web, bufbuild protobuf
- **Нет фреймворков** (ни React, ни Vue): весь UI — innerHTML-шаблоны + ручная привязка событий

---

## Структура проекта

```
messenger/
├── api/proto/messenger/v1/messenger.proto  — gRPC схема
├── cmd/server/main.go                      — точка входа сервера
├── internal/
│   ├── auth/         — JWT + bcrypt
│   ├── grpcapi/      — gRPC-сервер (server.go)
│   └── store/        — репозитории (memory + postgres)
├── web/
│   ├── index.html
│   └── src/
│       ├── main.js       — весь UI (1700 строк)
│       ├── styles.css    — весь CSS (~525 строк)
│       └── lib/
│           ├── api.js                — createMessengerClients()
│           ├── auth-ui.js            — loginFeedback()
│           ├── chat-state.js         — чистые функции над state
│           ├── group-editor.js       — addDraftMember / removeDraftMember
│           └── group-permissions.js  — canManageGroupMembers и др.
└── CLAUDE.md
```

---

## Фронтенд: архитектура

### Рендер

- Единый объект `state` (см. ниже).
- `render()` → `renderChat()` + `bindChatEvents()` (или `renderAuth()` + `bindAuthEvents()`).
- Каждый `render()` полностью заменяет `app.innerHTML` → все DOM-узлы пересоздаются → event listeners навешиваются заново в bind-функциях.
- **Исключения без полного render()** (патч DOM напрямую, чтобы не было мерцания):
  - `patchGroupUserList()` — список пользователей в модале создания группы при поиске.
  - `bindGroupToggleHandlers()` — чекбоксы участников группы.
  - `patchSearchUI()` — блок сообщений и счётчик навигации при поиске сообщений.

### state (main.js:106)

```js
{
  token, username, profile,
  conversations,          // Array — список диалогов
  profiles,               // Map<username, profile>
  messages,               // Map<conversationId, message[]>
  activeConversationId,
  activePeer,
  userSearchQuery, userSearchResults,
  messageSearchOpen,      // bool — показать ли поле поиска в шапке
  messageSearchQuery,     // raw (не trimmed) — .trim() при поиске
  messageSearchResults, messageSearchIndex,
  selectedMessageId,
  localUnread,            // Map<conversationId, number> — локальный счётчик непрочитанных
  groupEditorOpen, groupEditorMode,   // "create" | "edit"
  groupEditorConversationId,
  groupTitleDraft, groupMemberQuery,
  groupSearchResults, groupSelectedMembers,
  streamAbort, streamRetryTimer,
  onlineUsers,            // Set<username>
  status,                 // "connected" | "disconnected" | "reconnecting"
  authMode,               // "login" | "register"
  authMessage, authError, showRegisterPrompt,
  showProfileEditor,
  showSelfProfile,        // модал своего профиля
  showConvProfile,        // модал профиля собеседника / группы
  profileDraft,
}
```

### Ключевые функции

| Функция | Назначение |
|---|---|
| `render()` | Точка входа рендера. Ветвится по `state.token`. |
| `renderChat()` | Строит весь `app.innerHTML` для чата. |
| `bindChatEvents()` | Навешивает все listeners после render. |
| `patchSearchUI()` | Патч только `#messages` + `#search-nav-area` при поиске. |
| `patchGroupUserList()` | Патч только `#group-user-list` при вводе в поиск группы. |
| `bindGroupToggleHandlers()` | Listeners на `[data-group-toggle]` в модале группы. |
| `renderMessages(msgs, searchQuery, searchCurrentMsgId)` | HTML сообщений с подсветкой поиска. |
| `openConversation(id)` | Открывает диалог, сбрасывает search/unread. |
| `handleServerEvent(event)` | Обработчик Server-Sent Events (message, profile, conversation, online, deleted). |
| `resetSession()` | Полный сброс state при logout. |
| `openEventStream()` | Запускает SSE поток, авто-реконнект. |

### UI-хелперы (main.js:32–103)

```js
ic(name, size, color)          // SVG-иконка из словаря IC
avatarHtml(name, color, size, online, borderColor)  // div.avatar
shortTime(timestamp)           // "HH:MM" из protobuf Timestamp
doubleCheck(read)              // SVG двойная галочка ✓✓
highlightText(text, query)     // escapeHtml + <mark class="search-hl">
getLastMessage(conversationId) // последнее сообщение из state.messages
avatarColorFor(username)       // детерминированный цвет по хешу username
getAvatarColor(username)       // avatarHex из профиля или avatarColorFor
initials(name)                 // первые 2 буквы из слов имени
displayName(username)          // обёртка над resolveDisplayName(state.profiles, state.profile, username)
escapeHtml(value)              // XSS-защита для всех innerHTML вставок
```

### Модалы

Модалы рендерятся как `position:fixed; inset:0` поверх `app-layout`.
HTML: `app.innerHTML = "<div class='app-layout'>...</div>${groupEditorModal}${selfProfileModal}${convProfileModal}"`.

- `selfProfileModal` — показывается при `state.showSelfProfile`.
- `convProfileModal` — показывается при `state.showConvProfile && activeConversation`:
  - для DM: профиль собеседника + кнопка «Написать».
  - для группы: информация + список участников (кликабельны, кроме себя → открывают DM).
- `groupEditorModal` — создание/редактирование группы. Поиск патчит DOM напрямую через `patchGroupUserList()`, без `render()`.

### Поиск сообщений

- Кнопка-лупа в шапке → `state.messageSearchOpen = true` → в шапке появляется `input#message-search`.
- Ввод текста → `patchSearchUI()` (НЕ `render()`) — сохраняет позицию курсора.
- `patchSearchUI()` обновляет: `#messages` (innerHTML через `renderMessages`) + `#search-nav-area` (счётчик + кнопки ↑↓).
- Кнопка ✕ → `state.messageSearchOpen = false` → полный `render()`.
- `state.messageSearchQuery` хранит raw значение (с пробелами), `.trim()` используется при API-запросе и рендере.

### CSS (styles.css)

CSS-переменные (`:root`):
- `--accent: #7c6fff` — акцент/фиолетовый
- `--bubble-me: #7c6fff` — пузырёк своих сообщений
- `--online: #3be8a0` — зелёный онлайн
- `--danger: #ff5b5b` — красный
- `--bg: #0b0b10`, `--sidebar: #0f0f18`, `--chat-bg: #0d0d16`

Подсветка поиска:
- `mark.search-hl` — янтарно-жёлтый фон `rgba(255,190,0,0.38)`
- `.bubble.search-match` — тонкий жёлтый контур
- `.bubble.search-current` — жирный жёлтый контур + glow

Кликабельные элементы шапки:
- `.chat-header-clickable` — обёртка вокруг аватара + имени → открывает `convProfileModal`
- `.group-member-clickable` — участник группы в модале → открывает DM

### Анти-мерцание паттерн

Если действие меняет только часть модала (чекбокс, список поиска, счётчик) — **не вызывать `render()`**, а патчить DOM напрямую:

```js
// ✓ Правильно — без мерцания
const cb = element.querySelector(".checkbox");
cb.className = `checkbox${checked ? " checked" : ""}`;
patchGroupUserList();   // только список
patchSearchUI();        // только сообщения + счётчик

// ✗ Неправильно при каждом нажатии клавиши
render(); // пересоздаёт всё, теряет фокус и позицию курсора
```

### Непрочитанные сообщения

- `state.localUnread: Map<conversationId, number>` — локальный счётчик.
- Инкрементируется в `handleServerEvent` при входящем сообщении в неактивный диалог.
- Сбрасывается в `openConversation()`.
- Значок `div.unread-badge` отображается в списке диалогов.

### Двойные галочки

- `doubleCheck(read)` — возвращает SVG двойной галочки.
- `read=true` → белые (прочитано), `read=false` → полупрозрачные (отправлено).
- Отображаются только на своих сообщениях, в `bubble-foot`.
- Последнее сообщение — серые (отправлено), остальные — белые (прочитано). Это упрощение: реального ACK нет.

---

## Бэкенд (Go)

- `internal/grpcapi/server.go` — реализация всех gRPC методов.
- `internal/store/` — интерфейсы + реализации (memory для тестов, postgres для prod).
- JWT хранится в `localStorage` на фронтенде, передаётся в `Authorization: Bearer` заголовке.
- SSE-стрим: `SubscribeEvents` в proto, реализован как Server-Sent Events поверх HTTP.

---

## Сборка и запуск

```bash
# Фронтенд (dev)
cd web && npm run dev

# Фронтенд (prod build) — protoc нужен в /tmp/protoc29/
cd web && npx vite build   # если protoc не установлен — только vite build

# Бэкенд
go run ./cmd/server

# Docker
docker-compose up
```

---

## Настройки (Settings)

- `state.showSettings` — открыт ли модал настроек.
- `state.settings` — объект `{ theme, accent, font, pushNotifications, messageSounds, compactMode }`, персистится в `localStorage("messenger-settings")`.
- `applySettings()` — применяет CSS-переменные через `document.documentElement.style.setProperty()`. Вызывается при старте и при каждом изменении настройки.
- `ACCENT_COLORS` — массив `[value, hoverValue]` для 6 цветов акцента.
- Тема «Светлая» переопределяет переменные CSS через `style.setProperty()`, тёмная — сбрасывает их (`removeProperty()`), возвращая значения из `:root`.
- Шрифт меняется через CSS-переменную `--font` (используется в `html, body, #app { font-family: var(--font, ...) }`).
- Компактный режим добавляет класс `compact` на `document.body`.
- Кнопка открытия: `id="open-settings"`, рядом с лого в шапке сайдбара (класс `.logo-settings-btn`).
- Модал: `buildSettingsModal()`, включён в `app.innerHTML` как последний модал.

## Известные упрощения

- Двойные галочки — визуальная имитация. Реальный read-receipt требует поддержки в proto/бэкенде.
- `state.localUnread` персистится в `localStorage("messenger-unread")` и восстанавливается при reload. Сохраняется через `saveUnreadCounts()`, загружается через `loadUnreadCounts()` в `initializeSession()`. При logout очищается.
- Поиск сообщений и пользователей в группе — debounce 300ms, API не вызывается на каждый символ.
