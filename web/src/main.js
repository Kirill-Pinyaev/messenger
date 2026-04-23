import "./styles.css";

import { Code, ConnectError } from "@connectrpc/connect";
import { Empty } from "@bufbuild/protobuf";

import { createMessengerClients } from "./lib/api.js";
import { loginFeedback } from "./lib/auth-ui.js";
import { addDraftMember, filterSelectableUsers, removeDraftMember } from "./lib/group-editor.js";
import { canManageGroupMembers, canRemoveGroupMember, canTransferAdmin, currentUserRole } from "./lib/group-permissions.js";
import {
  conversationLabel,
  conversationMetaLine,
  displayName as resolveDisplayName,
  ensureConversationForMessage as computeConversationsAfterMessage,
  findMessageById as lookupMessageById,
  isGroupMessage,
  makeConversationId,
  normalizeAvatarHex,
  peerFromConversationId,
  removeConversationById,
  upsertConversation,
  removeMessageCollection,
  upsertMessageCollection,
} from "./lib/chat-state.js";

const app = document.getElementById("app");

// ── Design helpers ──────────────────────────────────────────────────────────

const AVATAR_COLORS = ['#5b8dee','#ee5b8d','#5beeca','#eec15b','#c45bee','#7c6fff','#ef4444','#10b981'];

function avatarColorFor(username) {
  let h = 0;
  for (let i = 0; i < username.length; i++) h = (h * 31 + username.charCodeAt(i)) | 0;
  return AVATAR_COLORS[Math.abs(h) % AVATAR_COLORS.length];
}

function getAvatarColor(username) {
  const p = state.profiles.get(username);
  if (p?.avatarHex) return `#${p.avatarHex}`;
  return avatarColorFor(username);
}

function initials(name) {
  return (name || '?').split(' ').map(w => w[0] || '').join('').toUpperCase().slice(0, 2) || '?';
}

function avatarHtml(name, color, size = 38, online = false, borderColor = 'var(--sidebar)') {
  const dot = Math.round(size * 0.28);
  return `<div class="avatar" style="width:${size}px;height:${size}px;background:${escapeHtml(color)};font-size:${Math.round(size*0.36)}px;">` +
    escapeHtml(initials(name)) +
    (online ? `<div class="avatar-dot" style="width:${dot}px;height:${dot}px;border-color:${borderColor};"></div>` : '') +
    `</div>`;
}

const IC = {
  search:   "M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z",
  send:     "M22 2L11 13M22 2L15 22l-4-9-9-4 20-7z",
  logout:   "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9",
  plus:     "M12 5v14M5 12h14",
  settings: "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z",
  user:     "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
  trash:    "M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6",
  close:    "M18 6L6 18M6 6l12 12",
  check:    "M20 6L9 17l-5-5",
};

function ic(name, size = 18, color = 'currentColor') {
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="${IC[name]}"/></svg>`;
}

function shortTime(timestamp) {
  if (!timestamp) return '';
  let d;
  if (typeof timestamp.toDate === 'function') d = timestamp.toDate();
  else if (typeof timestamp.seconds === 'bigint') d = new Date(Number(timestamp.seconds) * 1000);
  else if (typeof timestamp.seconds === 'number') d = new Date(timestamp.seconds * 1000);
  else return '';
  return d.toLocaleTimeString('ru', { hour: '2-digit', minute: '2-digit' });
}

function getLastMessage(conversationId) {
  const msgs = state.messages.get(conversationId) || [];
  return msgs[msgs.length - 1] || null;
}

const state = {
  token: localStorage.getItem("token") || "",
  username: localStorage.getItem("username") || "",
  profile: null,
  conversations: [],
  profiles: new Map(),
  messages: new Map(),
  activeConversationId: "",
  activePeer: "",
  userSearchQuery: "",
  userSearchResults: [],
  messageSearchQuery: "",
  messageSearchResults: [],
  selectedMessageId: 0,
  groupEditorOpen: false,
  groupEditorMode: "create",
  groupEditorConversationId: "",
  groupTitleDraft: "",
  groupMemberQuery: "",
  groupSearchResults: [],
  groupSelectedMembers: [],
  streamAbort: null,
  streamRetryTimer: null,
  onlineUsers: new Set(),
  status: "disconnected",
  authMode: "login",
  authMessage: "",
  authError: false,
  showRegisterPrompt: false,
  showProfileEditor: false,
  profileDraft: {
    firstName: "",
    lastName: "",
    avatarHex: "",
    avatarData: "",
  },
};

const authInterceptor = (next) => async (req) => {
  if (state.token) {
    req.header.set("Authorization", `Bearer ${state.token}`);
  }
  return next(req);
};

const { authClient, userClient, messageClient } = createMessengerClients(window.location.origin, authInterceptor);

bootstrap().catch((err) => {
  console.error(err);
  setAuthMessage(readError(err), true);
  render();
});

function render() {
  if (!state.token) {
    renderAuth();
    bindAuthEvents();
    return;
  }

  renderChat();
  bindChatEvents();
}

function renderAuth() {
  const isLogin = state.authMode === "login";
  app.innerHTML = `
    <div class="auth-shell">
      <div class="auth-card">
        <div class="auth-logo">
          <div class="auth-logo-icon">✦</div>
          <h1>Messenger</h1>
          <p>gRPC-Web клиент</p>
        </div>

        <div class="auth-tabs">
          <button id="auth-login-tab" class="${isLogin ? "active" : ""}" type="button">Вход</button>
          <button id="auth-register-tab" class="${!isLogin ? "active" : ""}" type="button">Регистрация</button>
        </div>

        <form id="auth-form" class="auth-form">
          ${!isLogin ? `
            <div class="split">
              <div class="field"><input id="first-name" placeholder="Имя" /></div>
              <div class="field"><input id="last-name" placeholder="Фамилия" /></div>
            </div>
          ` : ""}
          <div class="field">
            <div class="field-icon">${ic("user", 16)}</div>
            <input id="username" class="has-icon" placeholder="Никнейм" value="${escapeHtml(state.username)}" />
          </div>
          <div class="field">
            <input id="password" type="password" placeholder="Пароль" />
          </div>
          ${!isLogin ? `<div class="field"><input id="password-repeat" type="password" placeholder="Повторите пароль" /></div>` : ""}
          ${state.authMessage ? `<div class="auth-error">${escapeHtml(state.authMessage)}</div>` : ""}
          <button type="submit" class="btn btn-primary btn-full">${isLogin ? "Войти" : "Создать аккаунт"}</button>
          ${isLogin && state.showRegisterPrompt ? `<button id="suggest-register" class="btn btn-ghost btn-full" type="button">Перейти к регистрации</button>` : ""}
        </form>
      </div>
    </div>
  `;
}

function renderChat() {
  const activeMessages = getActiveMessages();
  const profile = state.profile || {};
  const activeConversation = state.conversations.find((item) => item.conversationId === state.activeConversationId) || null;
  const selectedMessage = findMessageById(state.selectedMessageId);
  const selectableGroupUsers = filterSelectableUsers(state.groupSearchResults, state.groupSelectedMembers, state.username);
  const activeGroupCanManage = canManageGroupMembers(activeConversation, state.username);

  const myName = displayName(state.username);
  const myColor = getAvatarColor(state.username);

  // ── Sidebar ──────────────────────────────────────────────────────────────
  const sidebarHtml = `
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-header-top">
          <div class="sidebar-logo"><span class="logo-star">✦</span> Messenger</div>
          <div style="display:flex;gap:4px;align-items:center;">
            <div class="status-pill ${state.status === "connected" ? "connected" : ""}">
              <div class="status-dot"></div>${escapeHtml(state.status)}
            </div>
          </div>
        </div>
        <div class="search-wrap">
          <div class="search-icon">${ic("search", 16)}</div>
          <input id="user-search" placeholder="Найти пользователя..." value="${escapeHtml(state.userSearchQuery)}" />
        </div>
      </div>

      ${state.userSearchResults.length > 0 ? `
        <div class="search-results-panel">
          <div class="search-results-label">Найдено</div>
          ${state.userSearchResults.map((u) => `
            <button class="user-search-item" data-user-open="${escapeHtml(u.username)}" type="button">
              ${avatarHtml(displayName(u.username), getAvatarColor(u.username), 34, state.onlineUsers.has(u.username))}
              <div>
                <div style="font-size:13px;font-weight:500;color:var(--text);">${escapeHtml(displayName(u.username))}</div>
                <div style="font-size:11px;color:var(--text-muted);">@${escapeHtml(u.username)}</div>
              </div>
            </button>
          `).join("")}
        </div>
      ` : ""}

      <div class="convs-scroll">
        <div class="convs-header">
          <div class="convs-label">Диалоги</div>
          <button id="open-create-group" class="btn-subtle" type="button" style="display:flex;align-items:center;gap:4px;">
            ${ic("plus", 13)} Группа
          </button>
        </div>
        ${state.conversations.map((conv) => {
          const isGroup = conv.kind === 2;
          const label = conversationLabel(state.profiles, state.profile, conv);
          const peer = conv.peerUsername || conv.peerProfile?.username || "";
          const online = !isGroup && state.onlineUsers.has(peer);
          const color = isGroup ? "#5b8dee" : getAvatarColor(peer || label);
          const lastMsg = getLastMessage(conv.conversationId);
          const lastText = lastMsg ? escapeHtml(String(lastMsg.text || "").slice(0, 40)) : "";
          const lastTime = lastMsg ? escapeHtml(shortTime(lastMsg.createdAt)) : "";
          const active = conv.conversationId === state.activeConversationId ? "active" : "";
          return `
            <button class="conv-item ${active}" data-conversation-open="${escapeHtml(conv.conversationId)}" type="button">
              ${avatarHtml(label, color, 44, online)}
              <div class="conv-body">
                <div class="conv-top">
                  <div class="conv-name">${escapeHtml(label)}</div>
                  ${lastTime ? `<div class="conv-time">${lastTime}</div>` : ""}
                </div>
                <div class="conv-bottom">
                  <div class="conv-last">${lastText || (isGroup ? `${conv.memberUsernames?.length || 0} участников` : (online ? "в сети" : ""))}</div>
                </div>
              </div>
            </button>
          `;
        }).join("") || `<div class="convs-empty">Диалогов пока нет.</div>`}
      </div>

      <div class="sidebar-footer">
        <div class="profile-row">
          <div class="profile-clickable" id="toggle-profile">
            ${avatarHtml(myName, myColor, 38, true)}
            <div style="min-width:0;">
              <div class="profile-name">${escapeHtml(myName)}</div>
              <div class="profile-username">@${escapeHtml(state.username)}</div>
            </div>
          </div>
          <button class="logout-btn" id="logout" title="Выйти">${ic("logout", 16)}</button>
        </div>
        ${state.showProfileEditor ? `
          <div class="profile-editor">
            <input id="profile-first-name" placeholder="Имя" value="${escapeHtml(profile.firstName || "")}" />
            <input id="profile-last-name" placeholder="Фамилия" value="${escapeHtml(profile.lastName || "")}" />
            <input id="profile-avatar-hex" placeholder="Цвет аватара (hex), например 7c6fff" value="${escapeHtml((profile.avatarHex || "").replace("#", ""))}" />
            <div class="profile-editor-actions">
              <button id="save-profile" class="btn btn-primary btn-sm" type="button">Сохранить</button>
              <button id="cancel-profile" class="btn btn-ghost btn-sm" type="button">Отмена</button>
              <button id="delete-account" class="btn btn-danger btn-sm" type="button">Удалить аккаунт</button>
            </div>
          </div>
        ` : ""}
      </div>
    </div>
  `;

  // ── Group editor ─────────────────────────────────────────────────────────
  const groupEditorHtml = `
    <div class="group-editor">
      <div class="group-editor-hd">
        <div>
          <div class="group-editor-title">${state.groupEditorMode === "create" ? "Создание группы" : "Управление участниками"}</div>
          <div class="group-editor-sub">${state.groupEditorMode === "create" ? "Задайте название и выберите участников через поиск." : "Добавляйте или исключайте участников."}</div>
        </div>
        <button id="close-group-editor" class="btn btn-ghost btn-sm" type="button">${ic("close", 16)} Закрыть</button>
      </div>

      <div>
        <div class="section-label">Название группы</div>
        <input id="group-title" placeholder="Название группы" value="${escapeHtml(state.groupTitleDraft)}" ${state.groupEditorMode === "edit" ? "disabled" : ""} />
      </div>

      <div>
        <div class="section-label">Поиск участников</div>
        <input id="group-user-search" placeholder="Начните вводить ник..." value="${escapeHtml(state.groupMemberQuery)}" />
        <div class="ge-list" style="margin-top:8px;">
          ${selectableGroupUsers.map((u) => `
            <button class="ge-search-item" data-group-add="${escapeHtml(u.username)}" type="button">
              ${avatarHtml(displayName(u.username), getAvatarColor(u.username), 32, false, 'var(--chat-bg)')}
              <div>
                <div style="font-size:13px;font-weight:500;">${escapeHtml(displayName(u.username))}</div>
                <div style="font-size:11px;color:var(--text-muted);">@${escapeHtml(u.username)}</div>
              </div>
            </button>
          `).join("") || `<div style="color:var(--text-muted);font-size:13px;padding:8px 0;">Введите ник для поиска.</div>`}
        </div>
      </div>

      <div>
        <div class="section-label">Участники (${state.groupSelectedMembers.length})</div>
        <div class="ge-list">
          ${state.groupSelectedMembers.map((u) => `
            <div class="ge-item">
              ${avatarHtml(displayName(u.username), getAvatarColor(u.username), 32, false, 'var(--panel)')}
              <div class="ge-item-info">
                <div class="ge-item-name">${escapeHtml(displayName(u.username))}</div>
                <div class="ge-item-sub">${u.role === 1 ? "Администратор" : (u.addedBy ? `Добавил: @${escapeHtml(u.addedBy)}` : "@" + escapeHtml(u.username))}</div>
              </div>
              <div class="ge-item-actions">
                ${state.groupEditorMode === "edit" && isExistingGroupMember(u.username) && canTransferAdmin(activeConversation, state.username) && u.username !== state.username
                  ? `<button class="btn btn-subtle btn-sm" data-group-transfer-admin="${escapeHtml(u.username)}" type="button">Сделать админом</button>` : ""}
                ${(state.groupEditorMode === "create" || canRemoveGroupMember(activeConversation, state.username, u.username) || !isExistingGroupMember(u.username))
                  ? `<button class="btn btn-danger btn-sm" data-group-remove="${escapeHtml(u.username)}" type="button">${state.groupEditorMode === "edit" && isExistingGroupMember(u.username) ? "Исключить" : ic("close", 14)}</button>` : ""}
              </div>
            </div>
          `).join("") || `<div style="color:var(--text-muted);font-size:13px;padding:8px 0;">Участники пока не выбраны.</div>`}
        </div>
      </div>

      <div class="ge-actions">
        ${state.groupEditorMode === "create"
          ? `<button id="submit-create-group" class="btn btn-primary" type="button">Создать группу</button>`
          : (activeGroupCanManage ? `<button id="submit-add-group-members" class="btn btn-primary" type="button">Добавить выбранных</button>` : "")}
      </div>
    </div>
  `;

  // ── Chat area ────────────────────────────────────────────────────────────
  let chatAreaHtml;
  if (state.groupEditorOpen) {
    chatAreaHtml = `<div class="chat-main">${groupEditorHtml}</div>`;
  } else if (!activeConversation) {
    chatAreaHtml = `
      <div class="chat-main empty-state">
        <div class="empty-star">✦</div>
        <div class="empty-title">Выберите диалог</div>
        <div class="empty-sub">или найдите пользователя для начала переписки</div>
      </div>
    `;
  } else {
    const isGroup = activeConversation.kind === 2;
    const chatLabel = conversationLabel(state.profiles, state.profile, activeConversation);
    const chatPeer = activeConversation.peerUsername || activeConversation.peerProfile?.username || "";
    const chatOnline = !isGroup && state.onlineUsers.has(chatPeer);
    const chatColor = isGroup ? "#5b8dee" : getAvatarColor(chatPeer || chatLabel);
    const chatSub = isGroup
      ? `${activeConversation.memberUsernames?.length || 0} участников`
      : (chatOnline ? "в сети" : "не в сети");

    chatAreaHtml = `
      <div class="chat-main">
        <div class="chat-header">
          ${avatarHtml(chatLabel, chatColor, 40, chatOnline, 'var(--panel)')}
          <div class="chat-header-info">
            <div class="chat-header-name">${escapeHtml(chatLabel)}</div>
            <div class="chat-header-sub ${chatOnline ? "online-sub" : ""}">
              ${chatOnline ? `<div class="online-pulse"></div>` : ""}
              ${escapeHtml(chatSub)}
            </div>
          </div>
          <div class="chat-header-actions">
            ${isGroup ? `<button id="open-edit-group" class="btn btn-subtle btn-sm" type="button">Участники</button>` : ""}
            ${isGroup ? `<button id="leave-group" class="btn btn-danger btn-sm" type="button">Выйти</button>` : ""}
            <input id="message-search" class="msg-search-input" placeholder="Поиск..." value="${escapeHtml(state.messageSearchQuery)}" />
            <button id="search-messages" class="btn btn-subtle btn-sm" type="button">${ic("search", 14)}</button>
          </div>
        </div>

        <div class="messages-scroll" id="messages">
          ${renderMessages(activeMessages)}
        </div>

        ${state.messageSearchResults.length > 0 ? `
          <div class="msg-search-results">
            <div class="msg-search-label">Найдено</div>
            ${state.messageSearchResults.map((msg) => `
              <button class="search-hit-btn" data-search-open="${msg.messageId}" type="button">
                ${escapeHtml(String(msg.text || "").slice(0, 80))}
                <small>@${escapeHtml(msg.from)} · ${escapeHtml(formatTimestamp(msg.createdAt))}</small>
              </button>
            `).join("")}
          </div>
        ` : ""}

        <div class="composer-wrap">
          ${selectedMessage ? `
            <div class="selected-bar">
              <span>Сообщение #${selectedMessage.messageId} выбрано</span>
              ${selectedMessage.from === state.username ? `<button id="delete-message" class="btn btn-danger btn-sm" type="button">${ic("trash", 14)} Удалить</button>` : ""}
            </div>
          ` : ""}
          <div class="composer-inner">
            <textarea id="message-text" placeholder="Написать сообщение..." rows="1"></textarea>
            <button id="send-message" class="send-btn" type="button">${ic("send", 16)}</button>
          </div>
        </div>
      </div>
    `;
  }

  app.innerHTML = `<div class="app-layout">${sidebarHtml}${chatAreaHtml}</div>`;

  // Update send button state reactively via input event
  const textarea = document.getElementById("message-text");
  const sendBtn = document.getElementById("send-message");
  if (textarea && sendBtn) {
    const updateSendBtn = () => {
      sendBtn.classList.toggle("ready", textarea.value.trim().length > 0);
    };
    textarea.addEventListener("input", updateSendBtn);
  }

  if (activeConversation) {
    document.title = `${conversationLabel(state.profiles, state.profile, activeConversation)} · Messenger`;
  } else {
    document.title = "Messenger";
  }
}

function renderMessages(messages) {
  if (messages.length === 0) {
    return `<div style="margin:auto;color:var(--text-muted);font-size:14px;text-align:center;">История пока пустая. Отправьте первое сообщение.</div>`;
  }

  const isGroup = state.conversations.find((c) => c.conversationId === state.activeConversationId)?.kind === 2;
  let lastDay = "";
  return messages.map((msg, i) => {
    const own = msg.from === state.username;
    const selected = state.selectedMessageId === msg.messageId ? " selected-msg" : "";
    const senderName = own ? "Вы" : displayName(msg.from);
    const senderColor = own ? "var(--accent)" : getAvatarColor(msg.from);
    const time = shortTime(msg.createdAt);

    let day = "";
    if (msg.createdAt) {
      let d;
      if (typeof msg.createdAt.toDate === "function") d = msg.createdAt.toDate();
      else if (typeof msg.createdAt.seconds === "bigint") d = new Date(Number(msg.createdAt.seconds) * 1000);
      else if (typeof msg.createdAt.seconds === "number") d = new Date(msg.createdAt.seconds * 1000);
      if (d) day = d.toLocaleDateString("ru", { day: "numeric", month: "long" });
    }
    const showDay = day && day !== lastDay;
    if (showDay) lastDay = day;

    const showSender = isGroup && !own && (i === 0 || messages[i - 1].from !== msg.from);

    const avatarEl = !own
      ? avatarHtml(senderName, senderColor, 28, false, 'var(--chat-bg)')
      : "";

    return `
      ${showDay ? `<div class="date-chip"><span>${escapeHtml(day)}</span></div>` : ""}
      <div class="msg-row ${own ? "msg-me" : ""}">
        ${!own ? avatarEl : ""}
        <div style="max-width:65%;">
          ${showSender ? `<div class="bubble-sender" style="color:${senderColor};">${escapeHtml(senderName)}</div>` : ""}
          <div class="bubble ${own ? "bubble-me" : "bubble-other"}${selected}" data-message-select="${msg.messageId}" style="cursor:pointer;">
            ${escapeHtml(msg.text || "")}
            <div class="bubble-foot">
              ${escapeHtml(time)}
            </div>
          </div>
        </div>
      </div>
    `;
  }).join("");
}

function bindAuthEvents() {
  document.getElementById("auth-login-tab")?.addEventListener("click", () => {
    state.authMode = "login";
    setAuthMessage("");
    render();
  });

  document.getElementById("auth-register-tab")?.addEventListener("click", () => {
    state.authMode = "register";
    setAuthMessage("");
    render();
  });

  document.getElementById("auth-form")?.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const username = String(form.get("username") || document.getElementById("username")?.value || "").trim();
    const password = String(form.get("password") || document.getElementById("password")?.value || "");

    if (!username || !password) {
      setAuthMessage("Нужны username и password.", true);
      render();
      return;
    }

    try {
      if (state.authMode === "register") {
        const passwordRepeat = String(document.getElementById("password-repeat")?.value || "");
        if (password !== passwordRepeat) {
          throw new Error("Пароли не совпадают.");
        }

        await authClient.register({
          username,
          password,
          firstName: String(document.getElementById("first-name")?.value || "").trim(),
          lastName: String(document.getElementById("last-name")?.value || "").trim(),
        });
      }

      const response = await authClient.login({ username, password });
      state.token = response.token;
      state.username = username;
      localStorage.setItem("token", response.token);
      localStorage.setItem("username", username);
      setAuthMessage("");
      await initializeSession();
    } catch (err) {
      if (state.authMode === "login") {
        const feedback = loginFeedback(err);
        setAuthMessage(feedback.message, true, feedback.showRegisterPrompt);
      } else {
        setAuthMessage(readError(err), true);
      }
      render();
    }
  });

  document.getElementById("suggest-register")?.addEventListener("click", () => {
    state.authMode = "register";
    setAuthMessage("");
    render();
  });
}

function bindChatEvents() {
  document.getElementById("open-create-group")?.addEventListener("click", () => {
    state.groupEditorOpen = true;
    state.groupEditorMode = "create";
    state.groupEditorConversationId = "";
    state.groupTitleDraft = "";
    state.groupMemberQuery = "";
    state.groupSearchResults = [];
    state.groupSelectedMembers = [];
    render();
  });

  document.getElementById("open-edit-group")?.addEventListener("click", () => {
    const conversation = state.conversations.find((item) => item.conversationId === state.activeConversationId);
    if (!conversation || conversation.kind !== 2) {
      return;
    }
    openGroupEditorForConversation(conversation);
    render();
  });

  document.getElementById("close-group-editor")?.addEventListener("click", () => {
    state.groupEditorOpen = false;
    state.groupMemberQuery = "";
    state.groupSearchResults = [];
    render();
  });

  document.getElementById("logout")?.addEventListener("click", () => {
    resetSession();
    render();
  });

  document.getElementById("delete-account")?.addEventListener("click", async () => {
    if (!confirm("Удалить аккаунт и сообщения?")) {
      return;
    }
    try {
      await authClient.deleteAccount(new Empty());
      resetSession();
      render();
    } catch (err) {
      alert(readError(err));
    }
  });

  document.getElementById("leave-group")?.addEventListener("click", async () => {
    if (!state.activeConversationId || !confirm("Выйти из этой группы?")) {
      return;
    }
    try {
      await userClient.leaveGroupConversation({ conversationId: state.activeConversationId });
    } catch (err) {
      alert(readError(err));
    }
  });

  document.querySelectorAll("[data-conversation-open]").forEach((element) => {
    element.addEventListener("click", async () => {
      const conversationId = element.getAttribute("data-conversation-open");
      if (!conversationId) {
        return;
      }
      await openConversation(conversationId);
      render();
      scrollMessagesToBottom();
    });
  });

  document.getElementById("send-message")?.addEventListener("click", async () => {
    const textarea = document.getElementById("message-text");
    const text = String(textarea?.value || "").trim();
    const activeConversation = state.conversations.find((item) => item.conversationId === state.activeConversationId);
    if (!activeConversation || !text) {
      return;
    }

    try {
      const payload = activeConversation.kind === 2
        ? { conversationId: activeConversation.conversationId, text }
        : { to: state.activePeer, text };
      const message = await messageClient.sendMessage(payload);
      upsertMessage(message);
      textarea.value = "";
      state.selectedMessageId = 0;
      render();
      scrollMessagesToBottom();
    } catch (err) {
      alert(readError(err));
    }
  });

  document.querySelectorAll("[data-message-select]").forEach((element) => {
    element.addEventListener("click", () => {
      const messageId = Number(element.getAttribute("data-message-select"));
      state.selectedMessageId = state.selectedMessageId === messageId ? 0 : messageId;
      render();
    });
  });

  document.getElementById("delete-message")?.addEventListener("click", async () => {
    if (!state.selectedMessageId) {
      return;
    }
    try {
      await messageClient.deleteMessage({ messageId: state.selectedMessageId });
      removeMessage(state.selectedMessageId);
      state.selectedMessageId = 0;
      render();
    } catch (err) {
      alert(readError(err));
    }
  });

  document.getElementById("toggle-profile")?.addEventListener("click", () => {
    state.showProfileEditor = !state.showProfileEditor;
    render();
  });

  document.getElementById("cancel-profile")?.addEventListener("click", () => {
    state.showProfileEditor = false;
    render();
  });

  document.getElementById("save-profile")?.addEventListener("click", async () => {
    try {
      const profile = await userClient.updateProfile({
        firstName: String(document.getElementById("profile-first-name")?.value || "").trim(),
        lastName: String(document.getElementById("profile-last-name")?.value || "").trim(),
        avatarHex: normalizeAvatarHex(String(document.getElementById("profile-avatar-hex")?.value || "")),
      });
      applyProfile(profile);
      state.showProfileEditor = false;
      render();
    } catch (err) {
      alert(readError(err));
    }
  });

  document.getElementById("user-search")?.addEventListener("input", async (event) => {
    state.userSearchQuery = event.target.value.trim();
    if (!state.userSearchQuery) {
      state.userSearchResults = [];
      render();
      return;
    }

    try {
      const response = await userClient.searchUsers({ query: state.userSearchQuery, limit: 12 });
      state.userSearchResults = response.items.filter((item) => item.username !== state.username);
      state.userSearchResults.forEach(applyProfile);
      render();
    } catch (err) {
      console.error(err);
    }
  });

  document.querySelectorAll("[data-user-open]").forEach((element) => {
    element.addEventListener("click", async () => {
      const username = element.getAttribute("data-user-open");
      if (!username) {
        return;
      }

      const conversationId = makeConversationId(state.username, username);
      state.activePeer = username;
      state.activeConversationId = conversationId;
      if (!state.messages.has(conversationId)) {
        state.messages.set(conversationId, []);
      }
      if (!state.conversations.some((item) => item.conversationId === conversationId)) {
        state.conversations.unshift({
          conversationId,
          peerUsername: username,
          peerProfile: state.profiles.get(username) || null,
        });
      }
      await loadMessages({ withUsername: username });
      render();
      scrollMessagesToBottom();
    });
  });

  document.getElementById("group-user-search")?.addEventListener("input", async (event) => {
    state.groupMemberQuery = event.target.value.trim();
    if (!state.groupMemberQuery) {
      state.groupSearchResults = [];
      render();
      return;
    }
    try {
      const response = await userClient.searchUsers({ query: state.groupMemberQuery, limit: 12 });
      state.groupSearchResults = response.items;
      response.items.forEach(applyProfile);
      render();
    } catch (err) {
      console.error(err);
    }
  });

  document.querySelectorAll("[data-group-add]").forEach((element) => {
    element.addEventListener("click", () => {
      const username = element.getAttribute("data-group-add");
      const user = state.groupSearchResults.find((item) => item.username === username);
      if (!user) {
        return;
      }
      state.groupSelectedMembers = addDraftMember(state.groupSelectedMembers, user);
      state.groupMemberQuery = "";
      state.groupSearchResults = [];
      render();
    });
  });

  document.querySelectorAll("[data-group-remove]").forEach((element) => {
    element.addEventListener("click", async () => {
      const username = element.getAttribute("data-group-remove");
      if (!username) {
        return;
      }
      if (state.groupEditorMode === "edit") {
        if (!isExistingGroupMember(username)) {
          state.groupSelectedMembers = removeDraftMember(state.groupSelectedMembers, username);
          render();
          return;
        }
        try {
          const conversation = await userClient.removeGroupMember({
            conversationId: state.groupEditorConversationId,
            username,
          });
          replaceConversation(conversation);
          state.groupSelectedMembers = groupMembersForEditor(conversation);
          if (state.activeConversationId === conversation.conversationId) {
            openGroupEditorForConversation(conversation);
          }
          render();
        } catch (err) {
          alert(readError(err));
        }
        return;
      }
      state.groupSelectedMembers = removeDraftMember(state.groupSelectedMembers, username);
      render();
    });
  });

  document.querySelectorAll("[data-group-transfer-admin]").forEach((element) => {
    element.addEventListener("click", async () => {
      const username = element.getAttribute("data-group-transfer-admin");
      if (!username) {
        return;
      }
      try {
        const conversation = await userClient.transferGroupAdmin({
          conversationId: state.groupEditorConversationId,
          username,
        });
        replaceConversation(conversation);
        openGroupEditorForConversation(conversation);
        render();
      } catch (err) {
        alert(readError(err));
      }
    });
  });

  document.getElementById("submit-create-group")?.addEventListener("click", async () => {
    const title = String(document.getElementById("group-title")?.value || "").trim();
    if (!title || state.groupSelectedMembers.length === 0) {
      alert("Нужны название группы и хотя бы один участник.");
      return;
    }
    try {
      const conversation = await userClient.createGroupConversation({
        title,
        memberUsernames: state.groupSelectedMembers.map((item) => item.username),
      });
      replaceConversation(conversation);
      state.activeConversationId = conversation.conversationId;
      state.activePeer = "";
      state.groupEditorOpen = false;
      state.groupTitleDraft = "";
      state.groupMemberQuery = "";
      state.groupSearchResults = [];
      state.groupSelectedMembers = [];
      if (!state.messages.has(conversation.conversationId)) {
        state.messages.set(conversation.conversationId, []);
      }
      render();
    } catch (err) {
      alert(readError(err));
    }
  });

  document.getElementById("submit-add-group-members")?.addEventListener("click", async () => {
    const currentConversation = state.conversations.find((item) => item.conversationId === state.groupEditorConversationId);
    const existingMembers = new Set(currentConversation?.memberUsernames || []);
    const additions = state.groupSelectedMembers.map((item) => item.username).filter((username) => !existingMembers.has(username));
    if (additions.length === 0) {
      alert("Выберите новых участников для добавления.");
      return;
    }
    try {
      const conversation = await userClient.addGroupMembers({
        conversationId: state.groupEditorConversationId,
        memberUsernames: additions,
      });
      replaceConversation(conversation);
      openGroupEditorForConversation(conversation);
      render();
    } catch (err) {
      alert(readError(err));
    }
  });

  document.getElementById("search-messages")?.addEventListener("click", async () => {
    const query = String(document.getElementById("message-search")?.value || "").trim();
    state.messageSearchQuery = query;
    if (!query) {
      state.messageSearchResults = [];
      render();
      return;
    }
    try {
      const response = await messageClient.searchMessages({ query, limit: 20 });
      state.messageSearchResults = response.items;
      response.items.forEach(upsertMessage);
      render();
    } catch (err) {
      alert(readError(err));
    }
  });

  document.querySelectorAll("[data-search-open]").forEach((element) => {
    element.addEventListener("click", async () => {
      const messageId = Number(element.getAttribute("data-search-open"));
      const message = findMessageById(messageId);
      if (!message) {
        return;
      }
      await openConversation(message.conversationId);
      state.selectedMessageId = messageId;
      render();
      scrollMessagesToBottom();
    });
  });
}

async function bootstrap() {
  render();
  if (!state.token) {
    return;
  }
  await initializeSession();
}

async function initializeSession() {
  await Promise.all([loadProfile(), loadConversations()]);
  render();
  openEventStream();
  if (state.activeConversationId) {
    scrollMessagesToBottom();
  }
}

async function loadProfile() {
  const profile = await userClient.getProfile({ username: "" });
  applyProfile(profile);
}

async function loadConversations() {
  const response = await userClient.listConversations(new Empty());
  state.conversations = response.items || [];

  for (const conversation of state.conversations) {
    if (conversation.peerProfile) {
      applyProfile(conversation.peerProfile);
    }
  }

  if (state.activeConversationId && state.conversations.some((item) => item.conversationId === state.activeConversationId)) {
    await openConversation(state.activeConversationId);
    return;
  }

  if (state.conversations.length > 0) {
    await openConversation(state.conversations[0].conversationId);
  } else {
    state.activeConversationId = "";
    state.activePeer = "";
  }
}

async function openConversation(conversationId) {
  const conversation = state.conversations.find((item) => item.conversationId === conversationId);
  state.activeConversationId = conversationId;
  if (conversation?.kind === 2) {
    state.activePeer = "";
  } else {
    state.activePeer = conversation?.peerUsername || conversation?.peerProfile?.username || peerFromConversationId(conversationId, state.username);
  }
  await loadMessages({ conversationId });
}

async function loadMessages({ conversationId = "", withUsername = "" }) {
  const response = await messageClient.getMessages({
    conversationId,
    withUsername,
    limit: 100,
  });

  const targetId = conversationId || makeConversationId(state.username, withUsername);
  state.messages.set(targetId, response.items || []);
}

function openEventStream() {
  clearTimeout(state.streamRetryTimer);
  state.streamAbort?.abort();

  const abort = new AbortController();
  state.streamAbort = abort;
  state.status = "connecting";
  render();

  (async () => {
    try {
      for await (const event of messageClient.streamEvents({}, { signal: abort.signal })) {
        handleServerEvent(event);
      }
    } catch (err) {
      if (abort.signal.aborted) {
        return;
      }
      console.error(err);
      if (isUnauthenticated(err)) {
        resetSession();
        render();
        return;
      }
    }

    if (!abort.signal.aborted) {
      state.status = "disconnected";
      render();
      state.streamRetryTimer = window.setTimeout(() => {
        openEventStream();
      }, 1500);
    }
  })();
}

function handleServerEvent(event) {
  state.status = "connected";
  if (!event?.payload?.case) {
    render();
    return;
  }

  switch (event.payload.case) {
    case "presence":
      state.onlineUsers = new Set(event.payload.value.onlineUsers || []);
      break;
    case "message":
      upsertMessage(event.payload.value);
      if (isGroupMessage(event.payload.value) && !state.conversations.some((item) => item.conversationId === event.payload.value.conversationId)) {
        void loadConversations().then(() => {
          render();
          scrollMessagesToBottom();
        });
      }
      ensureConversationForMessage(event.payload.value);
      break;
    case "profile":
      if (event.payload.value?.profile) {
        applyProfile(event.payload.value.profile);
      }
      break;
    case "conversation":
      replaceConversation(event.payload.value);
      if (state.groupEditorOpen && state.groupEditorConversationId === event.payload.value.conversationId) {
        openGroupEditorForConversation(event.payload.value);
      }
      break;
    case "conversationRemoved":
      handleConversationRemoved(event.payload.value.conversationId);
      break;
    case "messageDeleted":
      removeMessage(event.payload.value.messageId);
      break;
    default:
      break;
  }

  render();
  if (event.payload.case === "message" && event.payload.value.conversationId === state.activeConversationId) {
    scrollMessagesToBottom();
  }
}

function ensureConversationForMessage(message) {
  const conversationId = message.conversationId;
  if (!conversationId) {
    return;
  }

  state.conversations = computeConversationsAfterMessage(state.conversations, state.profiles, state.username, message);
}

function applyProfile(profile) {
  if (!profile?.username) {
    return;
  }
  state.profiles.set(profile.username, profile);
  if (profile.username === state.username) {
    state.profile = profile;
  }
  state.conversations = state.conversations.map((conversation) => {
    if (conversation.peerUsername === profile.username || conversation.peerProfile?.username === profile.username) {
      return {
        ...conversation,
        peerUsername: profile.username,
        peerProfile: profile,
      };
    }
    return conversation;
  });
}

function replaceConversation(conversation) {
  state.conversations = upsertConversation(state.conversations, conversation);
}

function groupMembersForEditor(conversation) {
  if (conversation?.members?.length) {
    return conversation.members
      .filter((member) => member.username !== state.username)
      .map((member) => ({
        ...(state.profiles.get(member.username) || { username: member.username }),
        role: member.role,
        addedBy: member.addedBy,
      }));
  }
  return (conversation?.memberUsernames || [])
    .filter((username) => username !== state.username)
    .map((username) => state.profiles.get(username) || { username });
}

function openGroupEditorForConversation(conversation) {
  state.groupEditorOpen = true;
  state.groupEditorMode = "edit";
  state.groupEditorConversationId = conversation.conversationId;
  state.groupTitleDraft = conversation.title || "";
  state.groupMemberQuery = "";
  state.groupSearchResults = [];
  state.groupSelectedMembers = groupMembersForEditor(conversation);
}

function upsertMessage(message) {
  state.messages = upsertMessageCollection(state.messages, message);
}

function isExistingGroupMember(username) {
  if (state.groupEditorMode !== "edit") {
    return false;
  }
  const conversation = state.conversations.find((item) => item.conversationId === state.groupEditorConversationId);
  return Boolean(conversation?.memberUsernames?.includes(username));
}

function handleConversationRemoved(conversationId) {
  state.conversations = removeConversationById(state.conversations, conversationId);
  state.messages.delete(conversationId);

  if (state.groupEditorOpen && state.groupEditorConversationId === conversationId) {
    state.groupEditorOpen = false;
    state.groupEditorConversationId = "";
    state.groupSelectedMembers = [];
  }

  if (state.activeConversationId !== conversationId) {
    return;
  }

  if (state.conversations.length === 0) {
    state.activeConversationId = "";
    state.activePeer = "";
    return;
  }

  const nextConversationId = state.conversations[0].conversationId;
  void openConversation(nextConversationId).then(() => {
    render();
    scrollMessagesToBottom();
  });
}

function removeMessage(messageId) {
  state.messages = removeMessageCollection(state.messages, messageId);
}

function getActiveMessages() {
  return state.messages.get(state.activeConversationId) || [];
}

function findMessageById(messageId) {
  return lookupMessageById(state.messages, messageId);
}

function setAuthMessage(message, isError = false, showRegisterPrompt = false) {
  state.authMessage = message;
  state.authError = isError;
  state.showRegisterPrompt = showRegisterPrompt;
}

function resetSession() {
  state.streamAbort?.abort();
  clearTimeout(state.streamRetryTimer);

  state.token = "";
  state.username = "";
  state.profile = null;
  state.conversations = [];
  state.messages = new Map();
  state.profiles = new Map();
  state.activeConversationId = "";
  state.activePeer = "";
  state.userSearchQuery = "";
  state.userSearchResults = [];
  state.messageSearchQuery = "";
  state.messageSearchResults = [];
  state.selectedMessageId = 0;
  state.onlineUsers = new Set();
  state.status = "disconnected";
  state.showProfileEditor = false;
  setAuthMessage("");

  localStorage.removeItem("token");
  localStorage.removeItem("username");
}

function displayName(username) {
  return resolveDisplayName(state.profiles, state.profile, username);
}

function formatTimestamp(timestamp) {
  if (!timestamp) {
    return "";
  }
  if (typeof timestamp.toDate === "function") {
    return timestamp.toDate().toLocaleString("ru-RU");
  }
  if (typeof timestamp.seconds === "bigint") {
    return new Date(Number(timestamp.seconds) * 1000).toLocaleString("ru-RU");
  }
  if (typeof timestamp.seconds === "number") {
    return new Date(timestamp.seconds * 1000).toLocaleString("ru-RU");
  }
  return "";
}

function readError(err) {
  if (err instanceof ConnectError) {
    return err.rawMessage || err.message;
  }
  if (err instanceof Error) {
    return err.message;
  }
  return String(err || "unknown error");
}

function isUnauthenticated(err) {
  return err instanceof ConnectError && err.code === Code.Unauthenticated;
}

function scrollMessagesToBottom() {
  requestAnimationFrame(() => {
    const messages = document.getElementById("messages");
    if (messages) {
      messages.scrollTop = messages.scrollHeight;
    }
  });
}

function escapeHtml(value) {
  return String(value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
