import "./styles.css";

import { Code, ConnectError } from "@connectrpc/connect";
import { Empty } from "@bufbuild/protobuf";

import { createMessengerClients } from "./lib/api.js";
import { loginFeedback } from "./lib/auth-ui.js";
import { avatarView } from "./lib/avatar.js";
import {
  buildPublishPrekeyBundle,
  createIdentity,
  decryptDirectMessageForRecipient,
  decryptDirectMessageForSender,
  decryptGroupKeyEnvelope,
  decryptGroupMessage,
  deserializeGroupKey,
  encryptDirectMessage,
  encryptGroupMessage,
  exportIdentityState,
  importIdentityState,
  markPrekeysAsPublished,
  serializeGroupKey,
  topUpOneTimePrekeys,
  createGroupKeyPackage,
} from "./lib/e2ee.js";
import {
  createArchiveIdentity,
  decryptArchivePayload,
  encryptArchivePayload,
  exportArchiveIdentityState,
  importArchiveIdentityState,
  restoreArchiveIdentityFromServer,
  wrapArchiveIdentityForServer,
} from "./lib/archive-e2ee.js";
import {
  decryptMediaBytes,
  deserializeMediaDescriptor,
  encryptMediaBytes,
  serializeMediaDescriptor,
} from "./lib/media-e2ee.js";
import { addDraftMember, filterSelectableUsers, removeDraftMember } from "./lib/group-editor.js";
import { canManageGroupMembers, canRemoveGroupMember, canTransferAdmin, currentUserRole } from "./lib/group-permissions.js";
import {
  isConversationEncryptionEnabled,
  loadEncryptionPrefs,
  saveEncryptionPrefs,
  setConversationEncryption,
} from "./lib/chat-encryption.js";
import {
  conversationLabel,
  conversationMetaLine,
  displayName as resolveDisplayName,
  ensureConversationForMessage as computeConversationsAfterMessage,
  findMessageById as lookupMessageById,
  isGroupMessage,
  makeConversationId,
  peerFromConversationId,
  removeConversationById,
  upsertConversation,
  removeMessageCollection,
  upsertMessageCollection,
} from "./lib/chat-state.js";
import {
  currentDevicePrekeyBundle,
  directIdentityErrorMessage,
  hasDirectBundleMaterial,
  prepareDirectConversation,
  shouldDecryptDirectAsSender,
} from "./lib/direct-chat.js";
import { hasUsableDirectIdentityMaterial, identityStorageKey } from "./lib/identity-state.js";

const app = document.getElementById("app");
const GROUP_KEYS_STORAGE_PREFIX = "messenger-e2ee-groupkeys:";
const DEVICE_ID_STORAGE_KEY = "messenger-device-id";
const ARCHIVE_IDENTITY_STORAGE_PREFIX = "messenger-archive-identity:";

function randomId() {
  if (globalThis.crypto?.randomUUID) {
    return globalThis.crypto.randomUUID();
  }
  const bytes = new Uint8Array(16);
  if (globalThis.crypto?.getRandomValues) {
    globalThis.crypto.getRandomValues(bytes);
  } else {
    for (let i = 0; i < bytes.length; i++) {
      bytes[i] = Math.floor(Math.random() * 256);
    }
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((value) => value.toString(16).padStart(2, "0"));
  return `${hex.slice(0, 4).join("")}-${hex.slice(4, 6).join("")}-${hex.slice(6, 8).join("")}-${hex.slice(8, 10).join("")}-${hex.slice(10, 16).join("")}`;
}

// ── Design helpers ──────────────────────────────────────────────────────────

const AVATAR_COLORS = ['#5b8dee','#ee5b8d','#5beeca','#eec15b','#c45bee','#7c6fff','#ef4444','#10b981'];

const ACCENT_COLORS = [
  ["#7c6fff", "#9380ff"],
  ["#4d8fff", "#70aaff"],
  ["#10b981", "#34d399"],
  ["#f59e0b", "#fbbf24"],
  ["#ef4444", "#f87171"],
  ["#ec4899", "#f472b6"],
];

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

function avatarHtml(name, profile, fallbackColor, size = 38, online = false, borderColor = 'var(--sidebar)', extraClass = "") {
  const view = avatarView(profile, fallbackColor, name);
  const dot = Math.round(size * 0.28);
  return `<div class="avatar ${extraClass}" style="width:${size}px;height:${size}px;${view.kind === "initials" ? `background:${escapeHtml(view.color)};` : ""}font-size:${Math.round(size*0.36)}px;">` +
    (view.kind === "image"
      ? `<img src="${escapeHtml(view.src)}" alt="${escapeHtml(view.alt)}" class="avatar-image" />`
      : escapeHtml(view.text)) +
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
  close:      "M18 6L6 18M6 6l12 12",
  check:      "M20 6L9 17l-5-5",
  edit:       "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z",
  chevronUp:  "M18 15l-6-6-6 6",
  chevronDown:"M6 9l6 6 6-6",
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

function readAvatarFile(file) {
  return new Promise((resolve, reject) => {
    if (!file) {
      resolve("");
      return;
    }
    if (!file.type.startsWith("image/")) {
      reject(new Error("Можно загрузить только изображение."));
      return;
    }
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result || ""));
    reader.onerror = () => reject(new Error("Не удалось прочитать изображение."));
    reader.readAsDataURL(file);
  });
}

function doubleCheck(read = true) {
  const c = read ? 'rgba(255,255,255,0.92)' : 'rgba(255,255,255,0.45)';
  return `<svg width="20" height="12" viewBox="0 0 20 12" fill="none" style="flex-shrink:0;vertical-align:middle;">
    <polyline points="1,6 5,10 12,2" stroke="${c}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
    <polyline points="8,10 15,2" stroke="${c}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`;
}

function highlightText(text, query) {
  if (!query) return escapeHtml(text);
  const escaped = escapeHtml(text);
  const escapedQuery = escapeHtml(query);
  const regex = new RegExp(`(${escapedQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
  return escaped.replace(regex, '<mark class="search-hl">$1</mark>');
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
  messageSearchOpen: false,
  messageSearchQuery: "",
  messageSearchResults: [],
  messageSearchIndex: 0,
  selectedMessageId: 0,
  localUnread: new Map(),
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
  showSelfProfile: false,
  showConvProfile: false,
  showSettings: false,
  settings: (() => {
    const defaults = { theme: "dark", accent: "#7c6fff", font: "Inter", pushNotifications: true, messageSounds: false, compactMode: false };
    try { return { ...defaults, ...JSON.parse(localStorage.getItem("messenger-settings") || "{}") }; } catch { return defaults; }
  })(),
  identity: null,
  archiveIdentity: null,
  identityKeys: new Map(),
  groupKeys: new Map(),
  mediaCache: new Map(),
  encryptionPrefs: new Map(),
  e2eeReady: false,
  archiveSequence: 0,
  pendingAttachments: [],
  imagePreview: null,
  deviceId: loadOrCreateDeviceId(),
  profileDraft: {
    firstName: "",
    lastName: "",
    avatarData: "",
  },
};

function applySettings() {
  const s = state.settings;
  const root = document.documentElement;

  const pair = ACCENT_COLORS.find(([v]) => v === s.accent) || ACCENT_COLORS[0];
  root.style.setProperty("--accent", pair[0]);
  root.style.setProperty("--accent-hover", pair[1]);
  root.style.setProperty("--bubble-me", pair[0]);

  const fontMap = { "Inter": "'Inter',sans-serif", "IBM Plex": "'IBM Plex Mono',monospace", "Space G.": "'Space Grotesk',sans-serif" };
  root.style.setProperty("--font", fontMap[s.font] || fontMap["Inter"]);

  const lightVars = {
    "--bg":"#f2f2fa","--sidebar":"#e8e8f4","--sidebar-hover":"#dcdcea",
    "--panel":"#f4f4fc","--panel-border":"#d0d0e8","--chat-bg":"#ededf7",
    "--bubble":"#e0e0f0","--input-bg":"#eaeaf4","--input-border":"#c4c4dc",
    "--text":"#1a1a30","--text-muted":"#6060a0","--text-soft":"#5050a0",
    "--divider":"#d0d0e8","--shadow":"0 24px 60px rgba(0,0,0,0.15)",
  };
  if (s.theme === "light") {
    for (const [k, v] of Object.entries(lightVars)) root.style.setProperty(k, v);
    root.style.setProperty("--online", "#059669");
  } else {
    for (const k of [...Object.keys(lightVars), "--online"]) root.style.removeProperty(k);
  }

  document.body.classList.toggle("compact", !!s.compactMode);
  localStorage.setItem("messenger-settings", JSON.stringify(s));
}
applySettings();

function groupKeysStorageKey(username) {
  return `${GROUP_KEYS_STORAGE_PREFIX}${username}`;
}

function archiveIdentityStorageKey(username) {
  return `${ARCHIVE_IDENTITY_STORAGE_PREFIX}${username}`;
}

function identityCacheKey(username, deviceId) {
  return `${username}:${deviceId}`;
}

async function persistIdentityState() {
  if (!state.username || !state.identity) {
    return;
  }
  localStorage.setItem(identityStorageKey(state.username, state.deviceId), JSON.stringify(await exportIdentityState(state.identity)));
}

async function persistArchiveIdentityState() {
  if (!state.username || !state.archiveIdentity) {
    return;
  }
  localStorage.setItem(archiveIdentityStorageKey(state.username), JSON.stringify(await exportArchiveIdentityState(state.archiveIdentity)));
}

function loadOrCreateDeviceId() {
  let deviceId = localStorage.getItem(DEVICE_ID_STORAGE_KEY);
  if (deviceId) {
    return deviceId;
  }
  deviceId = `web-${randomId()}`;
  localStorage.setItem(DEVICE_ID_STORAGE_KEY, deviceId);
  return deviceId;
}

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

function buildSettingsModal() {
  if (!state.showSettings) return "";
  const s = state.settings;
  const moon = `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>`;
  const sun  = `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>`;
  const themes = [
    { key: "dark",  icon: moon, label: "Тёмная",  sub: "Для ночи" },
    { key: "light", icon: sun,  label: "Светлая", sub: "Для дня"  },
  ];
  const fonts = ["Inter", "IBM Plex", "Space G."];
  const fontFamilies = { "Inter": "Inter,sans-serif", "IBM Plex": "'IBM Plex Mono',monospace", "Space G.": "'Space Grotesk',sans-serif" };
  return `
    <div class="modal-overlay" id="settings-overlay">
      <div class="modal-card settings-card">
        <div class="modal-header">
          <div class="modal-title">Настройки</div>
          <button id="close-settings" class="icon-btn">${ic("close", 18)}</button>
        </div>

        <div class="settings-section-label">Внешний вид</div>

        <div class="settings-theme-row">
          ${themes.map(t => `
            <div class="settings-theme-card ${s.theme === t.key ? "active" : ""}" data-settings-theme="${t.key}">
              <div class="settings-theme-icon">${t.icon}</div>
              <div>
                <div class="settings-theme-name">${t.label}</div>
                <div class="settings-theme-sub">${t.sub}</div>
              </div>
              ${s.theme === t.key ? `<div class="settings-theme-check">${ic("check", 11, "#fff")}</div>` : ""}
            </div>
          `).join("")}
        </div>

        <div class="settings-label">Акцентный цвет</div>
        <div class="accent-swatches">
          ${ACCENT_COLORS.map(([color]) => `
            <div class="accent-swatch ${s.accent === color ? "active" : ""}" data-settings-accent="${color}" style="background:${color};">
              ${s.accent === color ? ic("check", 13, "#fff") : ""}
            </div>
          `).join("")}
        </div>

        <div class="settings-label">Шрифт</div>
        <div class="font-tabs">
          ${fonts.map(f => `
            <button class="font-tab ${s.font === f ? "active" : ""}" data-settings-font="${f}" style="font-family:${fontFamilies[f]};">${f}</button>
          `).join("")}
        </div>

        <div class="settings-section-label" style="margin-top:6px;">Уведомления</div>

        <div class="settings-toggle-row">
          <span>Push-уведомления</span>
          <label class="toggle"><input type="checkbox" id="toggle-push" ${s.pushNotifications ? "checked" : ""}><span class="toggle-track"><span class="toggle-thumb"></span></span></label>
        </div>
        <div class="settings-toggle-row">
          <span>Звуки сообщений</span>
          <label class="toggle"><input type="checkbox" id="toggle-sounds" ${s.messageSounds ? "checked" : ""}><span class="toggle-track"><span class="toggle-thumb"></span></span></label>
        </div>
        <div class="settings-toggle-row">
          <span>Компактный режим</span>
          <label class="toggle"><input type="checkbox" id="toggle-compact" ${s.compactMode ? "checked" : ""}><span class="toggle-track"><span class="toggle-thumb"></span></span></label>
        </div>

        <div class="settings-connection">
          <div class="settings-connection-dot ${state.status === "connected" ? "connected" : ""}"></div>
          <span>${state.status === "connected" ? "Подключено" : state.status === "reconnecting" ? "Переподключение..." : "Нет подключения"}</span>
        </div>
      </div>
    </div>
  `;
}

function patchSearchUI() {
  const query = state.messageSearchQuery.trim();
  const searchInConv = query
    ? state.messageSearchResults.filter((m) => m.conversationId === state.activeConversationId)
    : [];
  const searchIdx = Math.min(state.messageSearchIndex, Math.max(0, searchInConv.length - 1));
  const searchCurrentId = searchInConv[searchIdx]?.messageId ?? 0;

  // Patch messages area
  const messagesEl = document.getElementById("messages");
  if (messagesEl) {
    messagesEl.innerHTML = renderMessages(getActiveMessages(), query, searchCurrentId);
    messagesEl.querySelectorAll("[data-message-select]").forEach((el) => {
      el.addEventListener("click", () => {
        const mid = Number(el.getAttribute("data-message-select"));
        state.selectedMessageId = state.selectedMessageId === mid ? 0 : mid;
        render();
      });
    });
  }

  // Patch nav counter (prev/next buttons + counter)
  const navArea = document.getElementById("search-nav-area");
  if (navArea) {
    navArea.innerHTML = searchInConv.length > 0 ? `
      <span class="msg-search-counter">${searchIdx + 1}/${searchInConv.length}</span>
      <button id="search-prev" class="icon-btn" type="button">${ic("chevronUp", 15)}</button>
      <button id="search-next" class="icon-btn" type="button">${ic("chevronDown", 15)}</button>
    ` : "";
    document.getElementById("search-prev")?.addEventListener("click", () => {
      const inConv = state.messageSearchResults.filter((m) => m.conversationId === state.activeConversationId);
      if (!inConv.length) return;
      state.messageSearchIndex = (state.messageSearchIndex - 1 + inConv.length) % inConv.length;
      patchSearchUI();
      scrollToCurrentSearchResult();
    });
    document.getElementById("search-next")?.addEventListener("click", () => {
      const inConv = state.messageSearchResults.filter((m) => m.conversationId === state.activeConversationId);
      if (!inConv.length) return;
      state.messageSearchIndex = (state.messageSearchIndex + 1) % inConv.length;
      patchSearchUI();
      scrollToCurrentSearchResult();
    });
  }
}

function buildGroupUserListHtml(label, users, selectedSet) {
  const empty = users.length === 0 && state.groupMemberQuery.length >= 1
    ? `<div style="color:var(--text-muted);font-size:13px;text-align:center;padding:16px 0;">Ничего не найдено</div>`
    : "";
  return `
    ${label ? `<div class="section-label" style="padding:0 4px 8px;">${label}</div>` : ""}
    ${empty}
    ${users.map((u) => {
      const isSelected = selectedSet.has(u.username);
      const uName = displayName(u.username);
      return `
        <div class="user-toggle-row" data-group-toggle="${escapeHtml(u.username)}">
          <div style="display:flex;align-items:center;gap:10px;">
            ${avatarHtml(uName, u, getAvatarColor(u.username), 36, state.onlineUsers.has(u.username), 'var(--panel)')}
            <div>
              <div style="font-size:13px;font-weight:500;color:var(--text);">${escapeHtml(uName)}</div>
              <div style="font-size:11px;color:var(--text-muted);">@${escapeHtml(u.username)}</div>
            </div>
          </div>
          <div class="checkbox ${isSelected ? "checked" : ""}">
            ${isSelected ? ic("check", 11, "#fff") : ""}
          </div>
        </div>
      `;
    }).join("")}
  `;
}

function patchGroupUserList() {
  const container = document.getElementById("group-user-list");
  if (!container) return;

  let listUsers = [];
  let listLabel = "";
  if (state.groupMemberQuery.length >= 1) {
    listUsers = state.groupSearchResults.filter((u) => u.username !== state.username);
    listLabel = "Результаты поиска";
  } else {
    const recentPeers = state.conversations
      .filter((c) => c.kind !== 2)
      .map((c) => c.peerUsername || c.peerProfile?.username || "")
      .filter(Boolean);
    listUsers = recentPeers.map((un) => state.profiles.get(un) || { username: un }).filter((u) => u.username !== state.username);
    listLabel = listUsers.length ? "Недавние чаты" : "";
  }
  const selectedSet = new Set(state.groupSelectedMembers.map((u) => u.username));
  container.innerHTML = buildGroupUserListHtml(listLabel, listUsers, selectedSet);
  bindGroupToggleHandlers();
}

function bindGroupToggleHandlers() {
  document.querySelectorAll("[data-group-toggle]").forEach((element) => {
    element.addEventListener("click", () => {
      const username = element.getAttribute("data-group-toggle");
      if (!username) return;
      const alreadySelected = state.groupSelectedMembers.some((m) => m.username === username);
      if (alreadySelected) {
        state.groupSelectedMembers = removeDraftMember(state.groupSelectedMembers, username);
      } else {
        const user = state.groupSearchResults.find((u) => u.username === username)
          || state.profiles.get(username)
          || { username };
        state.groupSelectedMembers = addDraftMember(state.groupSelectedMembers, user);
      }
      const isNow = state.groupSelectedMembers.some((m) => m.username === username);
      const cb = element.querySelector(".checkbox");
      if (cb) {
        cb.className = `checkbox${isNow ? " checked" : ""}`;
        cb.innerHTML = isNow ? ic("check", 11, "#fff") : "";
      }
      const submitBtn = document.getElementById("submit-create-group");
      if (submitBtn) {
        const ok = state.groupTitleDraft.trim().length > 0 && state.groupSelectedMembers.length > 0;
        submitBtn.disabled = !ok;
        submitBtn.textContent = `Создать группу${state.groupSelectedMembers.length > 0 ? ` (${state.groupSelectedMembers.length})` : ""}`;
      }
    });
  });
}

function renderChat() {
  const activeMessages = getActiveMessages();
  const profile = state.profile || {};
  const activeConversation = state.conversations.find((item) => item.conversationId === state.activeConversationId) || null;
  const activeEncryptionEnabled = activeConversation
    ? isConversationEncryptionEnabled(state.encryptionPrefs, activeConversation.conversationId)
    : true;
  const selectedMessage = findMessageById(state.selectedMessageId);
  const selectableGroupUsers = filterSelectableUsers(state.groupSearchResults, state.groupSelectedMembers, state.username);
  const activeGroupCanManage = canManageGroupMembers(activeConversation, state.username);
  const editableProfile = state.showProfileEditor ? state.profileDraft : profile;

  const myName = displayName(state.username);
  const myColor = getAvatarColor(state.username);
  const editableName = `${editableProfile.firstName || ""} ${editableProfile.lastName || ""}`.trim() || myName;

  // ── Sidebar ──────────────────────────────────────────────────────────────
  const sidebarHtml = `
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-header-top">
          <div class="sidebar-logo"><span class="logo-star">✦</span> Messenger</div>
          <button id="open-settings" class="icon-btn sidebar-settings-btn" title="Настройки">${ic("settings", 17)}</button>
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
              ${avatarHtml(displayName(u.username), u, getAvatarColor(u.username), 34, state.onlineUsers.has(u.username))}
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
          const unread = state.localUnread.get(conv.conversationId) || 0;
          return `
            <button class="conv-item ${active}" data-conversation-open="${escapeHtml(conv.conversationId)}" type="button">
              ${avatarHtml(label, conv.peerProfile, color, 44, online)}
              <div class="conv-body">
                <div class="conv-top">
                  <div class="conv-name">${escapeHtml(label)}</div>
                  <div class="conv-time">${lastTime}</div>
                </div>
                <div class="conv-bottom">
                  <div class="conv-last">${lastText || (isGroup ? `${conv.memberUsernames?.length || 0} участников` : (online ? "в сети" : ""))}</div>
                  ${unread > 0 ? `<div class="unread-badge">${unread}</div>` : ""}
                </div>
              </div>
            </button>
          `;
        }).join("") || `<div class="convs-empty">Диалогов пока нет.</div>`}
      </div>

      <div class="sidebar-footer">
        <div class="profile-row">
          <div class="profile-clickable" id="open-self-profile">
            ${avatarHtml(myName, state.profile, myColor, 38, true)}
            <div style="min-width:0;">
              <div class="profile-name">${escapeHtml(myName)}</div>
              <div class="profile-username">@${escapeHtml(state.username)}</div>
            </div>
          </div>
          <button class="logout-btn" id="logout" title="Выйти">${ic("logout", 16)}</button>
        </div>
      </div>
    </div>
  `;

  // ── Group editor modal ───────────────────────────────────────────────────
  const selectedUsernames = new Set(state.groupSelectedMembers.map((u) => u.username));

  // Users to show in the list: search results if query active, else recent DM peers
  let groupListUsers = [];
  let groupListLabel = "";
  if (state.groupMemberQuery.length >= 1) {
    groupListUsers = state.groupSearchResults.filter((u) => u.username !== state.username);
    groupListLabel = "Результаты поиска";
  } else {
    const recentPeerUsernames = state.conversations
      .filter((c) => c.kind !== 2)
      .map((c) => c.peerUsername || c.peerProfile?.username || "")
      .filter(Boolean);
    groupListUsers = recentPeerUsernames
      .map((un) => state.profiles.get(un) || { username: un })
      .filter((u) => u.username !== state.username);
    groupListLabel = groupListUsers.length ? "Недавние чаты" : "";
  }

  const groupEditorModal = state.groupEditorOpen ? `
    <div class="modal-overlay" id="group-editor-overlay">
      <div class="modal-card" style="width:min(440px,92vw);">
        <div class="modal-header">
          <div class="modal-title">${state.groupEditorMode === "create" ? "Создать группу" : "Управление участниками"}</div>
          <button id="close-group-editor" class="icon-btn" type="button">${ic("close", 18)}</button>
        </div>

        ${state.groupEditorMode === "create" ? `
          <input id="group-title" placeholder="Название группы"
            value="${escapeHtml(state.groupTitleDraft)}"
            style="background:var(--input-bg);border:1px solid var(--accent);border-radius:10px;padding:10px 14px;color:var(--text);font-size:14px;outline:none;width:100%;" />
        ` : `
          <div style="background:var(--input-bg);border:1px solid var(--input-border);border-radius:10px;padding:10px 14px;color:var(--text-muted);font-size:14px;">
            ${escapeHtml(state.groupTitleDraft)}
          </div>
        `}

        <div style="position:relative;">
          <div style="position:absolute;left:11px;top:50%;transform:translateY(-50%);color:var(--text-muted);pointer-events:none;display:flex;">${ic("search", 15)}</div>
          <input id="group-user-search" placeholder="Поиск пользователей..."
            value="${escapeHtml(state.groupMemberQuery)}"
            style="width:100%;background:var(--input-bg);border:1px solid var(--input-border);border-radius:10px;padding:9px 12px 9px 36px;color:var(--text);font-size:13px;outline:none;" />
        </div>

        <div id="group-user-list" style="max-height:280px;overflow-y:auto;">
          ${buildGroupUserListHtml(groupListLabel, groupListUsers, selectedUsernames)}
        </div>

        ${state.groupEditorMode === "edit" ? `
          <div>
            <div class="section-label">Текущие участники (${state.groupSelectedMembers.length})</div>
            ${state.groupSelectedMembers.map((u) => `
              <div class="ge-item" style="margin-bottom:6px;">
                ${avatarHtml(displayName(u.username), u, getAvatarColor(u.username), 32, false, 'var(--panel)')}
                <div class="ge-item-info">
                  <div class="ge-item-name">${escapeHtml(displayName(u.username))}</div>
                  <div class="ge-item-sub">${u.role === 1 ? "Администратор" : "@" + escapeHtml(u.username)}</div>
                </div>
                <div class="ge-item-actions">
                  ${canTransferAdmin(activeConversation, state.username) && u.username !== state.username && isExistingGroupMember(u.username)
                    ? `<button class="btn btn-subtle btn-sm" data-group-transfer-admin="${escapeHtml(u.username)}" type="button">Сделать админом</button>` : ""}
                  ${(canRemoveGroupMember(activeConversation, state.username, u.username) || !isExistingGroupMember(u.username))
                    ? `<button class="btn btn-danger btn-sm" data-group-remove="${escapeHtml(u.username)}" type="button">Исключить</button>` : ""}
                </div>
              </div>
            `).join("")}
          </div>
        ` : ""}

        ${state.groupEditorMode === "create"
          ? `<button id="submit-create-group" class="btn btn-primary btn-full" type="button" ${!state.groupTitleDraft || selectedUsernames.size === 0 ? "disabled" : ""}>
               Создать группу${selectedUsernames.size > 0 ? ` (${selectedUsernames.size})` : ""}
             </button>`
          : (activeGroupCanManage ? `<button id="submit-add-group-members" class="btn btn-primary btn-full" type="button">Добавить выбранных</button>` : "")}
      </div>
    </div>
  ` : "";

  // ── Chat-level derived values (needed by both chat area and profile modals) ─
  const isGroup     = activeConversation ? activeConversation.kind === 2 : false;
  const chatLabel   = activeConversation ? conversationLabel(state.profiles, state.profile, activeConversation) : "";
  const chatPeer    = activeConversation ? (activeConversation.peerUsername || activeConversation.peerProfile?.username || "") : "";
  const chatOnline  = activeConversation && !isGroup && state.onlineUsers.has(chatPeer);
  const chatColor   = activeConversation ? (isGroup ? "#5b8dee" : getAvatarColor(chatPeer || chatLabel)) : "";
  const chatSub     = activeConversation
    ? (isGroup ? `${activeConversation.memberUsernames?.length || 0} участников` : (chatOnline ? "в сети" : "не в сети"))
    : "";

  // Search results filtered to current conversation
  const searchInConv = state.messageSearchQuery.trim()
    ? state.messageSearchResults.filter((m) => m.conversationId === state.activeConversationId)
    : [];
  const searchIdx = Math.min(state.messageSearchIndex, Math.max(0, searchInConv.length - 1));
  const searchCurrentId = searchInConv[searchIdx]?.messageId ?? 0;

  // ── Chat area ────────────────────────────────────────────────────────────
  let chatAreaHtml;
  if (!activeConversation) {
    chatAreaHtml = `
      <div class="chat-main empty-state">
        <div class="empty-star">✦</div>
        <div class="empty-title">Выберите диалог</div>
        <div class="empty-sub">или найдите пользователя для начала переписки</div>
      </div>
    `;
  } else {
    chatAreaHtml = `
      <div class="chat-main">
        <div class="chat-header">
          <div class="chat-header-clickable" id="open-conv-profile">
            ${avatarHtml(chatLabel, activeConversation?.peerProfile, chatColor, 40, chatOnline, 'var(--panel)')}
            <div class="chat-header-info">
              <div class="chat-header-name">${escapeHtml(chatLabel)}</div>
              <div class="chat-header-sub ${chatOnline ? "online-sub" : ""}">
                ${chatOnline ? `<div class="online-pulse"></div>` : ""}
                ${escapeHtml(chatSub)}
              </div>
            </div>
          </div>
          <div class="chat-header-actions">
            ${state.messageSearchOpen ? `
              <div class="msg-search-wrap">
                <input id="message-search" class="msg-search-input" placeholder="Поиск по сообщениям..." value="${escapeHtml(state.messageSearchQuery)}" autofocus />
                <div id="search-nav-area" style="display:contents;">
                  ${searchInConv.length > 0 ? `
                    <span class="msg-search-counter">${searchIdx + 1}/${searchInConv.length}</span>
                    <button id="search-prev" class="icon-btn" type="button">${ic("chevronUp", 15)}</button>
                    <button id="search-next" class="icon-btn" type="button">${ic("chevronDown", 15)}</button>
                  ` : ""}
                </div>
                <button id="search-clear" class="icon-btn" type="button">${ic("close", 15)}</button>
              </div>
            ` : `
              <button id="open-message-search" class="icon-btn" type="button" title="Поиск">${ic("search", 17)}</button>
            `}
            <label class="chat-e2ee-toggle" title="Шифрование сообщений в этом чате">
              <input id="toggle-chat-encryption" type="checkbox" ${activeEncryptionEnabled ? "checked" : ""} />
              <span class="chat-e2ee-toggle-track"><span class="chat-e2ee-toggle-thumb"></span></span>
              <span class="chat-e2ee-toggle-label">E2EE</span>
            </label>
            ${isGroup ? `<button id="leave-group" class="btn btn-danger btn-sm" type="button">Выйти</button>` : ""}
          </div>
        </div>

        <div class="messages-scroll" id="messages">
          ${renderMessages(activeMessages, state.messageSearchQuery.trim(), searchCurrentId)}
        </div>


        <div class="composer-wrap">
          ${selectedMessage ? `
            <div class="selected-bar">
              <span>Сообщение #${selectedMessage.messageId} выбрано</span>
              ${selectedMessage.from === state.username ? `<button id="delete-message" class="btn btn-danger btn-sm" type="button">${ic("trash", 14)} Удалить</button>` : ""}
            </div>
          ` : ""}
          ${state.pendingAttachments.length > 0 ? `
            <div class="selected-bar" style="gap:8px;flex-wrap:wrap;">
              ${state.pendingAttachments.map((item, index) => `
                <span class="member-chip">
                  ${escapeHtml(item.filename)}
                  <button type="button" class="chip-remove" data-remove-attachment="${index}" aria-label="Удалить вложение">×</button>
                </span>
              `).join("")}
            </div>
          ` : ""}
          <div class="composer-inner">
            <input id="message-attachment-input" type="file" hidden multiple />
            <button id="pick-attachment" class="icon-btn" type="button" title="Добавить файл">${ic("plus", 16)}</button>
            <textarea id="message-text" placeholder="Написать сообщение..." rows="1"></textarea>
            <button id="send-message" class="send-btn" type="button">${ic("send", 16)}</button>
          </div>
        </div>
      </div>
    `;
  }

  // ── Self profile modal ───────────────────────────────────────────────────
  const selfProfileModal = state.showSelfProfile ? (() => {
    const editInputStyle = `class="modal-input"`;
    return `
      <div class="modal-overlay" id="self-profile-overlay">
        <div class="modal-card" style="width:min(380px,92vw);">
          <div class="modal-header">
            <div class="modal-title">Мой профиль</div>
            <button id="close-self-profile" class="icon-btn">${ic("close", 18)}</button>
          </div>
          <div class="profile-modal-avatar">
            ${avatarHtml(editableName, editableProfile, myColor, 80, true, 'var(--panel)', state.showProfileEditor ? 'avatar-editable' : '')}
          </div>
          ${!state.showProfileEditor ? `
            <div class="profile-modal-info">
              <div class="profile-modal-name">${escapeHtml(myName)}</div>
              <div class="profile-modal-user">@${escapeHtml(state.username)}</div>
              <div class="profile-modal-status online">
                <div class="profile-modal-status-dot" style="background:var(--online);"></div>
                в сети
              </div>
            </div>
            <button id="start-edit-profile" class="btn btn-primary btn-full">
              ${ic("edit", 15)} Редактировать
            </button>
          ` : `
            <div style="display:grid;gap:10px;">
              <input id="profile-avatar-file" type="file" accept="image/*" hidden />
              <button id="profile-avatar-picker" class="avatar-picker-btn" type="button" aria-label="Загрузить фото профиля">
                Загрузить фото
              </button>
              <input id="profile-first-name" ${editInputStyle} placeholder="Имя" value="${escapeHtml(editableProfile.firstName || "")}" />
              <input id="profile-last-name"  ${editInputStyle} placeholder="Фамилия" value="${escapeHtml(editableProfile.lastName || "")}" />
            </div>
            <div style="display:flex;gap:8px;">
              <button id="save-profile"   class="btn btn-primary" style="flex:1;">Сохранить</button>
              <button id="cancel-profile" class="btn btn-ghost"   style="flex:1;">Отмена</button>
            </div>
            <button id="delete-account" class="btn btn-danger btn-full">Удалить аккаунт</button>
          `}
        </div>
      </div>
    `;
  })() : "";

  // ── Conv profile modal (DM or group) ─────────────────────────────────────
  const convProfileModal = (state.showConvProfile && activeConversation) ? (() => {
    if (!isGroup) {
      // DM peer profile
      const peerOnline = chatOnline;
      return `
        <div class="modal-overlay" id="conv-profile-overlay">
          <div class="modal-card" style="width:min(360px,92vw);">
            <div class="modal-header">
              <div class="modal-title">Профиль</div>
              <button id="close-conv-profile" class="icon-btn">${ic("close", 18)}</button>
            </div>
            <div class="profile-modal-avatar">
              ${avatarHtml(chatLabel, activeConversation?.peerProfile, chatColor, 72, peerOnline, 'var(--panel)')}
            </div>
            <div class="profile-modal-info">
              <div class="profile-modal-name">${escapeHtml(chatLabel)}</div>
              <div class="profile-modal-user">@${escapeHtml(chatPeer)}</div>
              <div class="profile-modal-status ${peerOnline ? "online" : "offline"}">
                <div class="profile-modal-status-dot" style="background:${peerOnline ? "var(--online)" : "var(--text-muted)"};"></div>
                ${peerOnline ? "в сети" : "не в сети"}
              </div>
            </div>
            <div style="border-top:1px solid var(--divider);padding-top:14px;">
              <button id="send-from-conv-profile" class="btn btn-primary btn-full">
                ${ic("send", 15)} Написать сообщение
              </button>
            </div>
          </div>
        </div>
      `;
    } else {
      // Group profile
      const members = activeConversation.members?.length
        ? activeConversation.members
        : (activeConversation.memberUsernames || []).map((u) => ({ username: u, role: 0 }));
      const canManage = canManageGroupMembers(activeConversation, state.username);

      const membersHtml = members.map((m) => {
        const mName = displayName(m.username);
        const mColor = getAvatarColor(m.username);
        const mOnline = state.onlineUsers.has(m.username);
        const isAdmin = m.role === 1;
        const isSelf = m.username === state.username;
        return `
          <div class="group-member-row${isSelf ? "" : " group-member-clickable"}"${isSelf ? "" : ` data-open-peer="${escapeHtml(m.username)}"`}>
            ${avatarHtml(mName, state.profiles.get(member.username), mColor, 36, mOnline, 'var(--panel)')}
            <div style="flex:1;min-width:0;">
              <div style="font-size:13px;font-weight:500;color:var(--text);display:flex;align-items:center;gap:6px;">
                ${escapeHtml(mName)}
                ${isAdmin ? `<span class="admin-badge">Админ</span>` : ""}
              </div>
              <div style="font-size:11px;color:var(--text-muted);">@${escapeHtml(m.username)}</div>
            </div>
            ${mOnline ? `<div class="online-label"><div class="online-label-dot"></div>в сети</div>` : ""}
          </div>
        `;
      }).join("");

      return `
        <div class="modal-overlay" id="conv-profile-overlay">
          <div class="modal-card" style="width:min(380px,92vw);">
            <div class="modal-header">
              <div class="modal-title">Информация о группе</div>
              <button id="close-conv-profile" class="icon-btn">${ic("close", 18)}</button>
            </div>
            <div class="profile-modal-avatar">
              ${avatarHtml(chatLabel, null, chatColor, 72, false, 'var(--panel)')}
            </div>
            <div class="profile-modal-info">
              <div class="profile-modal-name">${escapeHtml(chatLabel)}</div>
              <div class="profile-modal-user">${members.length} участников</div>
            </div>
            <div style="border-top:1px solid var(--divider);padding-top:16px;">
              <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;">
                <div class="section-label" style="margin:0;">Участники</div>
                ${canManage ? `<button id="open-edit-group" class="btn-subtle" style="display:flex;align-items:center;gap:4px;">${ic("plus", 13)} Добавить</button>` : ""}
              </div>
              ${membersHtml}
            </div>
          </div>
        </div>
      `;
    }
  })() : "";

  const imagePreviewModal = state.imagePreview ? `
    <div class="modal-overlay" id="image-preview-overlay">
      <div class="modal-card" style="width:min(92vw,980px);max-height:92vh;display:flex;flex-direction:column;">
        <div class="modal-header">
          <div class="modal-title">${escapeHtml(state.imagePreview.filename || "Изображение")}</div>
          <div style="display:flex;align-items:center;gap:8px;">
            <a class="btn-subtle" href="${escapeHtml(state.imagePreview.src)}" download="${escapeHtml(state.imagePreview.filename || "image")}">Скачать</a>
            <button id="close-image-preview" class="icon-btn">${ic("close", 18)}</button>
          </div>
        </div>
        <div style="display:flex;align-items:center;justify-content:center;overflow:auto;padding:8px 0 0;">
          <img src="${escapeHtml(state.imagePreview.src)}" alt="${escapeHtml(state.imagePreview.filename || "image")}" style="max-width:100%;max-height:76vh;border-radius:18px;display:block;" />
        </div>
      </div>
    </div>
  ` : "";

  app.innerHTML = `<div class="app-layout">${sidebarHtml}${chatAreaHtml}</div>${groupEditorModal}${selfProfileModal}${convProfileModal}${buildSettingsModal()}${imagePreviewModal}`;

  // Update send button state reactively via input event
  const textarea = document.getElementById("message-text");
  const sendBtn = document.getElementById("send-message");
  if (textarea && sendBtn) {
    const updateSendBtn = () => {
      sendBtn.classList.toggle("ready", textarea.value.trim().length > 0 || state.pendingAttachments.length > 0);
    };
    textarea.addEventListener("input", updateSendBtn);
    updateSendBtn();
  }

  if (activeConversation) {
    document.title = `${conversationLabel(state.profiles, state.profile, activeConversation)} · Messenger`;
  } else {
    document.title = "Messenger";
  }

  document.getElementById("close-image-preview")?.addEventListener("click", () => {
    state.imagePreview = null;
    render();
  });
  document.getElementById("image-preview-overlay")?.addEventListener("click", (event) => {
    if (event.target === event.currentTarget) {
      state.imagePreview = null;
      render();
    }
  });
  document.querySelectorAll("[data-preview-src]").forEach((element) => {
    element.addEventListener("click", () => {
      state.imagePreview = {
        src: element.getAttribute("data-preview-src") || "",
        filename: element.getAttribute("data-preview-name") || "image",
      };
      render();
    });
  });
}

function renderMessages(messages, searchQuery = "", searchCurrentMsgId = 0) {
  if (messages.length === 0) {
    return `<div style="margin:auto;color:var(--text-muted);font-size:14px;text-align:center;">История пока пустая. Отправьте первое сообщение.</div>`;
  }

  const convKind = state.conversations.find((c) => c.conversationId === state.activeConversationId)?.kind;
  const inGroup = convKind === 2;
  let lastDay = "";

  return messages.map((msg, i) => {
    const own = msg.from === state.username;
    const isSelectedMsg = state.selectedMessageId === msg.messageId;
    const isSearchCurrent = searchCurrentMsgId && msg.messageId === searchCurrentMsgId;
    const isSearchMatch = searchQuery && String(msg.text || "").toLowerCase().includes(searchQuery.toLowerCase());

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

    const showSender = inGroup && !own && (i === 0 || messages[i - 1].from !== msg.from);
    const isLast = own && i === messages.length - 1;
    const avatarEl = !own ? avatarHtml(senderName, state.profiles.get(msg.from), senderColor, 28, false, 'var(--chat-bg)') : "";

    const bubbleClasses = [
      "bubble",
      own ? "bubble-me" : "bubble-other",
      isSelectedMsg ? "selected-msg" : "",
      isSearchCurrent ? "search-current" : (isSearchMatch ? "search-match" : ""),
    ].filter(Boolean).join(" ");

    const bodyText = searchQuery ? highlightText(String(msg.text || ""), searchQuery) : escapeHtml(msg.text || "");
    const attachmentsHtml = renderAttachmentBodies(msg.attachments || []);
    const e2eeBadge = msg.encrypted ? `<span class="bubble-e2ee ${msg.decryptionError ? "error" : ""}">${msg.decryptionError ? "ошибка E2EE" : "E2EE"}</span>` : "";

    return `
      ${showDay ? `<div class="date-chip"><span>${escapeHtml(day)}</span></div>` : ""}
      <div class="msg-row ${own ? "msg-me" : ""}" data-message-id="${msg.messageId}">
        ${!own ? avatarEl : ""}
        <div style="display:flex;flex-direction:column;max-width:65%;min-width:0;">
          ${showSender ? `<div class="bubble-sender" style="color:${senderColor};">${escapeHtml(senderName)}</div>` : ""}
          <div class="${bubbleClasses}" data-message-select="${msg.messageId}">
            ${bodyText ? `<div>${bodyText}</div>` : ""}
            ${attachmentsHtml}
            <div class="bubble-foot">
              ${e2eeBadge}
              ${escapeHtml(time)}
              ${own ? doubleCheck(!isLast) : ""}
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

      const response = await authClient.login({ username, password, deviceId: state.deviceId });
      state.deviceId = response.deviceId || loadOrCreateDeviceId();
      state.token = response.token;
      state.username = username;
      localStorage.setItem("token", response.token);
      localStorage.setItem("username", username);
      setAuthMessage("");
      await initializeSession(password);
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
      const conversation = state.conversations.find((item) => item.conversationId === state.activeConversationId);
      const remainingMembers = (conversation?.members?.length
        ? conversation.members.map((member) => member.username)
        : (conversation?.memberUsernames || []))
        .filter((username) => username !== state.username);
      let nextKey = undefined;
      if (remainingMembers.length > 0) {
        const latest = await userClient.getConversationKey({ conversationId: state.activeConversationId, version: 0 }).catch(() => null);
        const nextVersion = (latest?.version || 0) + 1;
        nextKey = (await buildNextGroupKeyUpdate(state.activeConversationId, nextVersion, remainingMembers)).keyUpdate;
      }
      await userClient.leaveGroupConversation({ conversationId: state.activeConversationId, nextKey });
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

  document.getElementById("pick-attachment")?.addEventListener("click", () => {
    document.getElementById("message-attachment-input")?.click();
  });

  document.getElementById("message-attachment-input")?.addEventListener("change", async (event) => {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) {
      return;
    }
    try {
      for (const file of files) {
        if (file.size > 25 * 1024 * 1024) {
          throw new Error(`Файл ${file.name} больше 25 МБ.`);
        }
        state.pendingAttachments.push({
          file,
          filename: file.name,
          mimeType: file.type || "application/octet-stream",
        });
      }
      render();
    } catch (err) {
      alert(readError(err));
    } finally {
      event.target.value = "";
    }
  });

  document.querySelectorAll("[data-remove-attachment]").forEach((element) => {
    element.addEventListener("click", () => {
      const index = Number(element.getAttribute("data-remove-attachment"));
      if (!Number.isNaN(index)) {
        state.pendingAttachments.splice(index, 1);
        render();
      }
    });
  });

  async function sendCurrentMessage() {
    const textarea = document.getElementById("message-text");
    const text = String(textarea?.value || "").trim();
    const activeConversation = state.conversations.find((item) => item.conversationId === state.activeConversationId);
    if (!activeConversation || (!text && state.pendingAttachments.length === 0)) return;
    const encryptionEnabled = isConversationEncryptionEnabled(state.encryptionPrefs, activeConversation.conversationId);

    try {
      let payload;
      let attachments = [];
      let preparedAttachments = [];
      let archiveRecords = [];
      const createdAt = new Date();
      if (state.pendingAttachments.length > 0 && !encryptionEnabled) {
        throw new Error("Вложения доступны только при включенном E2EE.");
      }
      if (state.pendingAttachments.length > 0) {
        preparedAttachments = await prepareOutgoingAttachments(activeConversation);
        attachments = preparedAttachments;
      }
      if (!encryptionEnabled) {
        const owners = activeConversation.kind === 2
          ? (activeConversation.memberUsernames || activeConversation.members?.map((member) => member.username) || [state.username])
          : [state.username, state.activePeer];
        if (text) {
          archiveRecords.push(...await buildMessageArchiveRecords(owners, activeConversation.conversationId, {
            kind: "message",
            from: state.username,
            to: activeConversation.kind === 2 ? activeConversation.conversationId : state.activePeer,
            text,
            senderDeviceId: state.deviceId,
            createdAt: createdAt.toISOString(),
            conversationKeyVersion: 0,
          }, { createdAt }));
        }
        payload = activeConversation.kind === 2
          ? {
            conversationId: activeConversation.conversationId,
            text,
            encrypted: false,
            attachments,
            archiveRecords,
          }
          : {
            to: state.activePeer,
            text,
            encrypted: false,
            attachments,
            archiveRecords,
          };
      } else if (activeConversation.kind === 2) {
        const { version, groupKeyBytes } = await ensureConversationKey(activeConversation);
        const encrypted = text ? await encryptGroupMessage(text, groupKeyBytes, version) : null;
        if (preparedAttachments.length > 0) {
          attachments = await encryptOutgoingAttachmentDescriptorsForGroup(preparedAttachments, groupKeyBytes, version);
        }
        const owners = activeConversation.memberUsernames || activeConversation.members?.map((member) => member.username) || [state.username];
        if (text) {
          archiveRecords.push(...await buildMessageArchiveRecords(owners, activeConversation.conversationId, {
            kind: "message",
            from: state.username,
            to: activeConversation.conversationId,
            text,
            senderDeviceId: state.deviceId,
            createdAt: createdAt.toISOString(),
            conversationKeyVersion: version,
          }, {
            createdAt,
          }));
        }
        if (preparedAttachments.length > 0) {
          archiveRecords.push(...await buildAttachmentArchiveRecords(owners, activeConversation.conversationId, preparedAttachments, {
            from: state.username,
            recipient: activeConversation.conversationId,
            text,
            createdAt,
            conversationKeyVersion: version,
          }));
        }
        payload = {
          conversationId: activeConversation.conversationId,
          ciphertext: encrypted?.ciphertext,
          nonce: encrypted?.nonce,
          senderKeyId: state.identity.keyId,
          conversationKeyVersion: version,
          encrypted: true,
          attachments,
          archiveRecords,
        };
      } else {
        const directEnvelopes = text ? await buildDirectEnvelopes(text) : [];
        if (preparedAttachments.length > 0) {
          attachments = await encryptOutgoingAttachmentDescriptorsForDirect(preparedAttachments);
        }
        const owners = [state.username, state.activePeer];
        if (text) {
          archiveRecords.push(...await buildMessageArchiveRecords(owners, makeConversationId(state.username, state.activePeer), {
            kind: "message",
            from: state.username,
            to: state.activePeer,
            text,
            senderDeviceId: state.deviceId,
            createdAt: createdAt.toISOString(),
            conversationKeyVersion: 1,
          }, {
            createdAt,
          }));
        }
        if (preparedAttachments.length > 0) {
          archiveRecords.push(...await buildAttachmentArchiveRecords(owners, makeConversationId(state.username, state.activePeer), preparedAttachments, {
            from: state.username,
            recipient: state.activePeer,
            text,
            createdAt,
            conversationKeyVersion: 1,
          }));
        }
        payload = {
          to: state.activePeer,
          ciphertext: new Uint8Array(),
          nonce: new Uint8Array(),
          senderKeyId: state.identity.keyId,
          conversationKeyVersion: 1,
          encrypted: true,
          directEnvelopes,
          attachments,
          archiveRecords,
        };
      }
      const message = await materializeMessage(await messageClient.sendMessage(payload));
      upsertMessage(message);
      if (textarea) {
        textarea.value = "";
        textarea.style.height = "auto"; // сброс высоты после отправки
      }
      state.pendingAttachments = [];
      state.selectedMessageId = 0;
      render();
      scrollMessagesToBottom();
    } catch (err) {
      if (activeConversation.kind !== 2 && state.activePeer) {
        alert(directIdentityErrorMessage(err, state.activePeer));
        return;
      }
      alert(readError(err));
    }
  }

  document.getElementById("send-message")?.addEventListener("click", sendCurrentMessage);
  document.getElementById("toggle-chat-encryption")?.addEventListener("change", (event) => {
    if (!state.activeConversationId) {
      return;
    }
    state.encryptionPrefs = setConversationEncryption(
      state.encryptionPrefs,
      state.activeConversationId,
      event.target.checked,
    );
    saveEncryptionPrefs(state.username, state.encryptionPrefs, localStorage);
    render();
  });

  const composerTextarea = document.getElementById("message-text");
  if (composerTextarea) {
    // Auto-resize: grows with content up to max-height (10 lines), then scrolls
    function resizeTextarea() {
      composerTextarea.style.height = "auto";
      composerTextarea.style.height = composerTextarea.scrollHeight + "px";
    }
    composerTextarea.addEventListener("input", resizeTextarea);

    // Enter = send, Shift+Enter = newline
    composerTextarea.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        sendCurrentMessage().then(() => resizeTextarea());
      }
    });

    // Mouse wheel scrolls textarea content without propagating to the page
    composerTextarea.addEventListener("wheel", (e) => {
      const { scrollTop, scrollHeight, clientHeight } = composerTextarea;
      const atTop = scrollTop === 0 && e.deltaY < 0;
      const atBottom = scrollTop + clientHeight >= scrollHeight && e.deltaY > 0;
      if (!atTop && !atBottom) {
        e.stopPropagation();
      }
    }, { passive: true });
  }

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

  // Own profile modal
  document.getElementById("open-self-profile")?.addEventListener("click", () => {
    state.showSelfProfile = true;
    state.showProfileEditor = false;
    render();
  });
  document.getElementById("close-self-profile")?.addEventListener("click", () => {
    state.showSelfProfile = false;
    state.showProfileEditor = false;
    render();
  });
  document.getElementById("self-profile-overlay")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget) { state.showSelfProfile = false; state.showProfileEditor = false; render(); }
  });
  document.getElementById("start-edit-profile")?.addEventListener("click", () => {
    state.profileDraft = {
      firstName: state.profile?.firstName || "",
      lastName: state.profile?.lastName || "",
      avatarData: state.profile?.avatarData || "",
    };
    state.showProfileEditor = true;
    render();
  });
  document.getElementById("cancel-profile")?.addEventListener("click", () => {
    state.showProfileEditor = false;
    render();
  });
  document.getElementById("profile-avatar-picker")?.addEventListener("click", () => {
    document.getElementById("profile-avatar-file")?.click();
  });
  document.getElementById("profile-avatar-file")?.addEventListener("change", async (e) => {
    try {
      const file = e.target.files?.[0];
      if (!file) {
        return;
      }
      state.profileDraft.avatarData = await readAvatarFile(file);
      render();
    } catch (err) {
      alert(readError(err));
    }
  });
  document.getElementById("profile-first-name")?.addEventListener("input", (e) => {
    state.profileDraft.firstName = e.target.value;
  });
  document.getElementById("profile-last-name")?.addEventListener("input", (e) => {
    state.profileDraft.lastName = e.target.value;
  });
  document.querySelector(".profile-modal-avatar .avatar-editable")?.addEventListener("click", () => {
    document.getElementById("profile-avatar-file")?.click();
  });
  document.getElementById("save-profile")?.addEventListener("click", async () => {
    try {
      const profile = await userClient.updateProfile({
        firstName: String(state.profileDraft.firstName || "").trim(),
        lastName: String(state.profileDraft.lastName || "").trim(),
        avatarHex: "",
        avatarData: state.profileDraft.avatarData || "",
      });
      applyProfile(profile);
      state.showProfileEditor = false;
      render();
    } catch (err) {
      alert(readError(err));
    }
  });

  // Conv profile modal (peer / group)
  document.getElementById("open-conv-profile")?.addEventListener("click", () => {
    state.showConvProfile = true;
    render();
  });
  document.getElementById("close-conv-profile")?.addEventListener("click", () => {
    state.showConvProfile = false;
    render();
  });
  document.getElementById("conv-profile-overlay")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget) { state.showConvProfile = false; render(); }
  });
  document.getElementById("send-from-conv-profile")?.addEventListener("click", () => {
    state.showConvProfile = false;
    render();
  });
  // Clicking a group member opens a DM with them
  document.querySelectorAll("[data-open-peer]").forEach((element) => {
    element.addEventListener("click", async () => {
      const username = element.getAttribute("data-open-peer");
      if (!username || username === state.username) return;
      state.showConvProfile = false;
      state.activePeer = username;
      const next = prepareDirectConversation({
        conversations: state.conversations,
        messages: state.messages,
        profiles: state.profiles,
        selfUsername: state.username,
        peerUsername: username,
      });
      state.activeConversationId = next.conversationId;
      state.conversations = next.conversations;
      state.messages = next.messages;
      render();
      try {
        await loadMessages({ withUsername: username });
        render();
        scrollMessagesToBottom();
      } catch (err) {
        alert(readError(err));
      }
    });
  });

  // "Добавить" inside group profile opens group editor
  document.getElementById("open-edit-group")?.addEventListener("click", () => {
    const conversation = state.conversations.find((item) => item.conversationId === state.activeConversationId);
    if (!conversation || conversation.kind !== 2) return;
    state.showConvProfile = false;
    openGroupEditorForConversation(conversation);
    render();
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

      state.activePeer = username;
      const next = prepareDirectConversation({
        conversations: state.conversations,
        messages: state.messages,
        profiles: state.profiles,
        selfUsername: state.username,
        peerUsername: username,
      });
      state.activeConversationId = next.conversationId;
      state.conversations = next.conversations;
      state.messages = next.messages;
      render();
      try {
        await loadMessages({ withUsername: username });
        render();
        scrollMessagesToBottom();
      } catch (err) {
        alert(readError(err));
      }
    });
  });

  document.getElementById("group-title")?.addEventListener("input", (event) => {
    state.groupTitleDraft = event.target.value;
    // Update submit button disabled state without full re-render
    const btn = document.getElementById("submit-create-group");
    if (btn) btn.disabled = !state.groupTitleDraft || state.groupSelectedMembers.length === 0;
  });

  let groupSearchTimer = null;
  document.getElementById("group-user-search")?.addEventListener("input", async (event) => {
    state.groupMemberQuery = event.target.value.trim();
    clearTimeout(groupSearchTimer);
    if (!state.groupMemberQuery) {
      state.groupSearchResults = [];
      patchGroupUserList();
      return;
    }
    groupSearchTimer = setTimeout(async () => {
      try {
        const response = await userClient.searchUsers({ query: state.groupMemberQuery, limit: 12 });
        state.groupSearchResults = response.items;
        response.items.forEach(applyProfile);
        patchGroupUserList();
      } catch (err) {
        console.error(err);
      }
    }, 300);
  });

  // Close modal when clicking overlay backdrop
  document.getElementById("group-editor-overlay")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget) {
      state.groupEditorOpen = false;
      state.groupMemberQuery = "";
      state.groupSearchResults = [];
      render();
    }
  });

  // Toggle user selection in group editor modal — direct DOM, no full re-render
  bindGroupToggleHandlers();

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
          const currentConversation = state.conversations.find((item) => item.conversationId === state.groupEditorConversationId);
          const remainingMembers = (currentConversation?.members?.length
            ? currentConversation.members.map((member) => member.username)
            : (currentConversation?.memberUsernames || []))
            .filter((memberUsername) => memberUsername !== username);
          const latest = await userClient.getConversationKey({ conversationId: state.groupEditorConversationId, version: 0 }).catch(() => null);
          const nextVersion = (latest?.version || 0) + 1;
          const nextKey = await buildNextGroupKeyUpdate(state.groupEditorConversationId, nextVersion, remainingMembers);
          const conversation = await userClient.removeGroupMember({
            conversationId: state.groupEditorConversationId,
            username,
            nextKey: nextKey.keyUpdate,
          });
          rememberConversationKey(conversation.conversationId, nextVersion, nextKey.groupKeyBytes);
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
    const title = String(document.getElementById("group-title")?.value || state.groupTitleDraft || "").trim();
    if (!title || state.groupSelectedMembers.length === 0) {
      alert("Нужны название группы и хотя бы один участник.");
      return;
    }
    try {
      const members = [state.username, ...state.groupSelectedMembers.map((item) => item.username)];
      const initialKey = await buildNextGroupKeyUpdate("pending-group", 1, members);
      const conversation = await userClient.createGroupConversation({
        title,
        memberUsernames: state.groupSelectedMembers.map((item) => item.username),
        initialKey: initialKey.keyUpdate,
      });
      rememberConversationKey(conversation.conversationId, 1, initialKey.groupKeyBytes);
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
      const nextMembers = [...existingMembers, ...additions];
      const latest = await userClient.getConversationKey({ conversationId: state.groupEditorConversationId, version: 0 }).catch(() => null);
      const nextVersion = (latest?.version || 0) + 1;
      const nextKey = await buildNextGroupKeyUpdate(state.groupEditorConversationId, nextVersion, nextMembers);
      const conversation = await userClient.addGroupMembers({
        conversationId: state.groupEditorConversationId,
        memberUsernames: additions,
        nextKey: nextKey.keyUpdate,
      });
      rememberConversationKey(conversation.conversationId, nextVersion, nextKey.groupKeyBytes);
      replaceConversation(conversation);
      openGroupEditorForConversation(conversation);
      render();
    } catch (err) {
      alert(readError(err));
    }
  });

  document.getElementById("open-message-search")?.addEventListener("click", () => {
    state.messageSearchOpen = true;
    render();
    document.getElementById("message-search")?.focus();
  });

  let msgSearchTimer = null;
  document.getElementById("message-search")?.addEventListener("input", async (e) => {
    const raw = e.target.value;
    state.messageSearchQuery = raw;
    state.messageSearchIndex = 0;
    const query = raw.trim();
    if (!query) {
      clearTimeout(msgSearchTimer);
      state.messageSearchResults = [];
      patchSearchUI();
      return;
    }
    clearTimeout(msgSearchTimer);
    msgSearchTimer = setTimeout(async () => {
      try {
        const normalized = query.toLowerCase();
        state.messageSearchResults = [...state.messages.values()]
          .flat()
          .filter((message) => String(message.text || "").toLowerCase().includes(normalized))
          .slice(-50);
        patchSearchUI();
        scrollToCurrentSearchResult();
      } catch (err) {
        console.error(err);
      }
    }, 300);
  });

  document.getElementById("search-clear")?.addEventListener("click", () => {
    state.messageSearchOpen = false;
    state.messageSearchQuery = "";
    state.messageSearchResults = [];
    state.messageSearchIndex = 0;
    render();
  });

  // ── Settings modal ───────────────────────────────────────────────────────
  document.getElementById("open-settings")?.addEventListener("click", () => {
    state.showSettings = true;
    render();
  });
  document.getElementById("close-settings")?.addEventListener("click", () => {
    state.showSettings = false;
    render();
  });
  document.getElementById("settings-overlay")?.addEventListener("click", (e) => {
    if (e.target === e.currentTarget) { state.showSettings = false; render(); }
  });
  document.querySelectorAll("[data-settings-theme]").forEach((el) => {
    el.addEventListener("click", () => {
      state.settings.theme = el.getAttribute("data-settings-theme");
      applySettings();
      render();
    });
  });
  document.querySelectorAll("[data-settings-accent]").forEach((el) => {
    el.addEventListener("click", () => {
      state.settings.accent = el.getAttribute("data-settings-accent");
      applySettings();
      render();
    });
  });
  document.querySelectorAll("[data-settings-font]").forEach((el) => {
    el.addEventListener("click", () => {
      state.settings.font = el.getAttribute("data-settings-font");
      applySettings();
      render();
    });
  });
  document.getElementById("toggle-push")?.addEventListener("change", (e) => {
    state.settings.pushNotifications = e.target.checked;
    applySettings();
  });
  document.getElementById("toggle-sounds")?.addEventListener("change", (e) => {
    state.settings.messageSounds = e.target.checked;
    applySettings();
  });
  document.getElementById("toggle-compact")?.addEventListener("change", (e) => {
    state.settings.compactMode = e.target.checked;
    applySettings();
    render();
  });
}

async function bootstrap() {
  render();
  if (!state.token) {
    return;
  }
  await initializeSession();
}

async function initializeSession(password = "") {
  await ensureIdentityReady();
  await ensureHistoryArchiveReady(password);
  await Promise.all([loadProfile(), loadConversations()]);
  loadUnreadCounts();
  loadStoredGroupKeys();
  await loadHistoryArchive();
  state.encryptionPrefs = loadEncryptionPrefs(state.username, localStorage);
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
  state.showConvProfile = false;
  state.messageSearchOpen = false;
  state.messageSearchQuery = "";
  state.messageSearchResults = [];
  state.messageSearchIndex = 0;
  state.localUnread.delete(conversationId);
  saveUnreadCounts();
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
  const items = await Promise.all((response.items || []).map((message) => materializeMessage(message)));
  state.messages.set(targetId, mergeArchivedMessages(targetId, items));
  await backfillConversationHistory(targetId, items);
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
    case "message": {
      const incomingMsg = event.payload.value;
      void materializeMessage(incomingMsg).then((message) => {
        upsertMessage(message);
        if (message.from !== state.username && message.conversationId !== state.activeConversationId) {
          state.localUnread.set(message.conversationId, (state.localUnread.get(message.conversationId) || 0) + 1);
          saveUnreadCounts();
        }
        if (isGroupMessage(message) && !state.conversations.some((item) => item.conversationId === message.conversationId)) {
          void loadConversations().then(() => {
            render();
            scrollMessagesToBottom();
          });
        }
        ensureConversationForMessage(message);
        render();
        if (message.conversationId === state.activeConversationId) {
          scrollMessagesToBottom();
        }
      }).catch((error) => {
        console.error(error);
      });
      return;
    }
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
  const conversationId = message?.conversationId;
  if (!conversationId) {
    return;
  }
  const items = state.messages.get(conversationId) || [];
  state.messages.set(conversationId, normalizeConversationMessages(items));
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
  for (const key of [...state.groupKeys.keys()]) {
    if (key.startsWith(`${conversationId}:`)) {
      state.groupKeys.delete(key);
    }
  }
  saveStoredGroupKeys();

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

function loadStoredGroupKeys() {
  state.groupKeys = new Map();
  if (!state.username) {
    return;
  }
  try {
    const raw = JSON.parse(localStorage.getItem(groupKeysStorageKey(state.username)) || "{}");
    for (const [key, value] of Object.entries(raw)) {
      state.groupKeys.set(key, deserializeGroupKey(value));
    }
  } catch (error) {
    console.error(error);
  }
}

function saveStoredGroupKeys() {
  if (!state.username) {
    return;
  }
  const raw = {};
  for (const [key, value] of state.groupKeys.entries()) {
    raw[key] = serializeGroupKey(value);
  }
  localStorage.setItem(groupKeysStorageKey(state.username), JSON.stringify(raw));
}

function conversationKeyCacheKey(conversationId, version) {
  return `${conversationId}:${version}`;
}

function rememberConversationKey(conversationId, version, groupKeyBytes) {
  state.groupKeys.set(conversationKeyCacheKey(conversationId, version), groupKeyBytes);
  saveStoredGroupKeys();
}

function getRememberedConversationKey(conversationId, version) {
  return state.groupKeys.get(conversationKeyCacheKey(conversationId, version)) || null;
}

async function ensureIdentityReady() {
  const storageKey = identityStorageKey(state.username, state.deviceId);
  const legacyStorageKey = identityStorageKey(state.username, "");
  const stored = localStorage.getItem(storageKey) || localStorage.getItem(legacyStorageKey);
  if (stored) {
    try {
      state.identity = await importIdentityState(JSON.parse(stored));
    } catch (error) {
      console.warn("failed to import stored identity, regenerating", error);
      state.identity = await createIdentity(state.username);
    }
  } else {
    state.identity = await createIdentity(state.username);
  }

  if (!hasUsableDirectIdentityMaterial(state.identity)) {
    state.identity = await createIdentity(state.username);
  }

  let publishedOnServer = null;
  try {
    publishedOnServer = await userClient.getIdentityKey({ username: state.username, deviceId: state.deviceId });
  } catch (error) {
    if (!(error instanceof ConnectError) || error.code !== Code.NotFound) {
      throw error;
    }
  }
  state.identity = await topUpOneTimePrekeys(state.identity, 5);
  await persistIdentityState();
  if (legacyStorageKey !== storageKey) {
    localStorage.removeItem(legacyStorageKey);
  }

  const published = await userClient.publishIdentityKey({
    keyId: state.identity.keyId,
    algorithm: state.identity.algorithm,
    publicKey: state.identity.publicKeyBytes,
  });
  state.identityKeys.set(identityCacheKey(state.username, published.deviceId), published);
  await userClient.publishPrekeyBundle(buildPublishPrekeyBundle(state.identity));
  state.identity = markPrekeysAsPublished(state.identity);
  await persistIdentityState();
  state.e2eeReady = true;
}

async function ensureHistoryArchiveReady(password = "") {
  const storageKey = archiveIdentityStorageKey(state.username);
  const stored = localStorage.getItem(storageKey);
  let localIdentity = null;
  if (stored) {
    try {
      localIdentity = await importArchiveIdentityState(JSON.parse(stored));
    } catch (error) {
      console.warn("failed to import stored archive identity, reinitializing", error);
    }
  }

  let header = null;
  try {
    header = await userClient.getHistoryArchiveHeader(new Empty());
  } catch (error) {
    if (!(error instanceof ConnectError) || error.code !== Code.NotFound) {
      throw error;
    }
  }

  if (localIdentity && header) {
    if (bytesEqual(localIdentity.publicKeyBytes, header.publicKey)) {
      state.archiveIdentity = localIdentity;
      await persistArchiveIdentityState();
      return;
    }
    if (!password) {
      console.warn("archive identity mismatch detected, keeping local identity until next password login");
      state.archiveIdentity = localIdentity;
      return;
    }
    state.archiveIdentity = await restoreArchiveIdentityFromServer(header, password, state.username);
    await persistArchiveIdentityState();
    return;
  }

  if (localIdentity && !header) {
    state.archiveIdentity = localIdentity;
    if (!password) {
      console.warn("history archive header is missing on the server; it will be uploaded on the next password login");
      return;
    }
    const wrapped = await wrapArchiveIdentityForServer(localIdentity, password);
    await userClient.initializeHistoryArchive({
      publicKey: wrapped.publicKey,
      encryptedPrivateKey: wrapped.encryptedPrivateKey,
      kdfSalt: wrapped.kdfSalt,
      kdfParams: wrapped.kdfParams,
      version: wrapped.version,
    });
    await persistArchiveIdentityState();
    return;
  }

  if (header) {
    if (!password) {
      throw new Error("Для восстановления истории на этом устройстве нужен пароль входа.");
    }
    state.archiveIdentity = await restoreArchiveIdentityFromServer(header, password, state.username);
    await persistArchiveIdentityState();
    return;
  }

  if (!password) {
    state.archiveIdentity = null;
    console.warn("history archive is not initialized yet and cannot be bootstrapped without the login password");
    return;
  }

  state.archiveIdentity = await createArchiveIdentity(state.username);
  const wrapped = await wrapArchiveIdentityForServer(state.archiveIdentity, password);
  await userClient.initializeHistoryArchive({
    publicKey: wrapped.publicKey,
    encryptedPrivateKey: wrapped.encryptedPrivateKey,
    kdfSalt: wrapped.kdfSalt,
    kdfParams: wrapped.kdfParams,
    version: wrapped.version,
  });
  await persistArchiveIdentityState();
}

async function loadHistoryArchive() {
  if (!state.archiveIdentity) {
    return;
  }
  let afterSequence = 0;
  const archivedGroupKeys = [];
  const archivedMessages = new Map();
  for (;;) {
    const response = await messageClient.listHistoryArchiveRecords({
      afterSequence,
      limit: 200,
    });
    const items = response.items || [];
    if (items.length === 0) {
      break;
    }
    for (const item of items) {
      afterSequence = Math.max(afterSequence, Number(item.sequence || 0));
      const decrypted = await decryptArchivePayload({
        ciphertext: item.ciphertext,
        nonce: item.nonce,
        ephemeralPublicKey: item.ephemeralPublicKey,
      }, state.archiveIdentity);
      const payload = JSON.parse(new TextDecoder().decode(decrypted));
      if (item.recordType === 5) {
        archivedGroupKeys.push(payload);
        continue;
      }
      const conversationMessages = archivedMessages.get(item.conversationId) || new Map();
      const messageId = Number(item.messageId || payload.messageId || 0);
      const existing = conversationMessages.get(messageId);
      if (payload.kind === "message") {
        conversationMessages.set(messageId, {
          messageId,
          conversationId: item.conversationId,
          from: payload.from,
          to: payload.to,
          senderDeviceId: payload.senderDeviceId || "",
          text: payload.text || "",
          createdAt: { toDate: () => new Date(payload.createdAt) },
          encrypted: false,
          conversationKeyVersion: payload.conversationKeyVersion || 0,
          attachments: existing?.attachments || [],
          archived: true,
        });
      } else if (payload.kind === "attachment") {
        const base = existing || {
          messageId,
          conversationId: item.conversationId,
          from: payload.from,
          to: payload.to,
          senderDeviceId: payload.senderDeviceId || "",
          text: payload.text || "",
          createdAt: { toDate: () => new Date(payload.createdAt) },
          encrypted: false,
          conversationKeyVersion: payload.conversationKeyVersion || 0,
          attachments: [],
          archived: true,
        };
        base.attachments = [
          ...(base.attachments || []).filter((attachment) => attachment.attachmentId !== payload.attachmentId),
          {
            attachmentId: payload.attachmentId,
            kind: attachmentKindToProtoValue(payload.descriptor.kind),
            filename: payload.descriptor.originalFilename,
            mimeType: payload.descriptor.mimeType,
            sizeBytes: payload.descriptor.sizeBytes,
            mediaId: payload.mediaId,
            archiveDescriptor: payload.descriptor,
            sha256: new Uint8Array(payload.sha256 || []),
            ciphertextSize: payload.descriptor.ciphertextSize,
          },
        ];
        conversationMessages.set(messageId, base);
      }
      archivedMessages.set(item.conversationId, conversationMessages);
    }
    if (items.length < 200) {
      break;
    }
  }

  for (const payload of archivedGroupKeys) {
    rememberConversationKey(payload.conversationId, payload.version, new Uint8Array(payload.groupKeyBytes));
  }

  for (const [conversationId, byMessageId] of archivedMessages.entries()) {
    const materialized = await Promise.all([...byMessageId.values()].map((message) => materializeMessage(message)));
    const merged = mergeArchivedMessages(conversationId, materialized);
    state.messages.set(conversationId, merged);
  }
  state.archiveSequence = afterSequence;
}

async function fetchIdentityKey(username, deviceId) {
  const cacheKey = identityCacheKey(username, deviceId);
  if (state.identityKeys.has(cacheKey)) {
    return state.identityKeys.get(cacheKey);
  }
  const key = await userClient.getIdentityKey({ username, deviceId });
  state.identityKeys.set(cacheKey, key);
  return key;
}

async function fetchIdentityKeys(usernames) {
  const unique = [...new Set(usernames.filter(Boolean))];
  const response = await userClient.getIdentityKeys({ usernames: unique });
  for (const item of response.items) {
    state.identityKeys.set(identityCacheKey(item.username, item.deviceId), item);
  }
  const unresolved = unique.filter((username) => !response.items.some((item) => item.username === username));
  if (unresolved.length > 0) {
    throw new Error(`У пользователей ещё нет опубликованных ключей: ${unresolved.join(", ")}`);
  }
  return response.items;
}

async function fetchSenderIdentity(username, keyId) {
  let identity = findIdentityByKeyId(username, keyId);
  if (identity) {
    return identity;
  }
  await fetchIdentityKeys([username]);
  identity = findIdentityByKeyId(username, keyId);
  if (!identity) {
    throw new Error(`Не найден identity key ${keyId} для ${username}`);
  }
  return identity;
}

function findIdentityByKeyId(username, keyId) {
  for (const item of state.identityKeys.values()) {
    if (item.username === username && item.keyId === keyId) {
      return item;
    }
  }
  return null;
}

async function acquirePrekeyBundles(username) {
  const response = await userClient.acquirePrekeyBundles({ username });
  return response.items || [];
}

async function fetchArchivePublicKeys(usernames) {
  const unique = [...new Set(usernames.filter(Boolean))];
  const response = await userClient.getArchivePublicKeys({ usernames: unique });
  const map = new Map();
  for (const item of response.items || []) {
    map.set(item.username, item);
  }
  return map;
}

function logMissingArchiveOwners(ownerUsernames, bundles) {
  const unresolved = [...new Set(ownerUsernames.filter(Boolean))].filter((username) => !bundles.has(username));
  if (unresolved.length > 0) {
    console.warn("history archive fan-out skipped for users without archive key", unresolved);
  }
}

async function buildArchiveRecord(bundle, recordType, conversationId, payload, meta = {}) {
  const encrypted = await encryptArchivePayload(
    new TextEncoder().encode(JSON.stringify(payload)),
    bundle.publicKey,
  );
  return {
    recordId: meta.recordId || `${bundle.username}-${recordType}-${randomId()}`,
    ownerUsername: bundle.username,
    conversationId,
    recordType,
    messageId: meta.messageId || 0,
    attachmentId: meta.attachmentId || "",
    groupKeyVersion: meta.groupKeyVersion || 0,
    sender: state.username,
    createdAt: meta.createdAt || new Date(),
    ciphertext: encrypted.ciphertext,
    nonce: encrypted.nonce,
    ephemeralPublicKey: encrypted.ephemeralPublicKey,
    archiveKeyVersion: bundle.version || 1,
  };
}

async function buildMessageArchiveRecords(ownerUsernames, conversationId, payload, meta = {}) {
  const publicKeys = await fetchArchivePublicKeys(ownerUsernames);
  logMissingArchiveOwners(ownerUsernames, publicKeys);
  const records = [];
  for (const ownerUsername of [...new Set(ownerUsernames.filter(Boolean))]) {
    const bundle = publicKeys.get(ownerUsername);
    if (!bundle) {
      continue;
    }
    records.push(await buildArchiveRecord(bundle, payload.to === conversationId ? 2 : 1, conversationId, payload, meta));
  }
  return records;
}

async function buildAttachmentArchiveRecords(ownerUsernames, conversationId, attachments, meta = {}) {
  const publicKeys = await fetchArchivePublicKeys(ownerUsernames);
  logMissingArchiveOwners(ownerUsernames, publicKeys);
  const records = [];
  for (const ownerUsername of [...new Set(ownerUsernames.filter(Boolean))]) {
    const bundle = publicKeys.get(ownerUsername);
    if (!bundle) {
      continue;
    }
    for (const attachment of attachments) {
      records.push(await buildArchiveRecord(bundle, conversationId === meta.recipient ? 4 : 3, conversationId, {
        kind: "attachment",
        attachmentId: attachment.attachmentId,
        from: meta.from,
        to: meta.recipient,
        text: meta.text || "",
        senderDeviceId: state.deviceId,
        createdAt: meta.createdAt.toISOString(),
        conversationKeyVersion: meta.conversationKeyVersion || 0,
        mediaId: attachment.mediaId,
        descriptor: attachment.descriptor,
        sha256: Array.from(attachment.sha256 || []),
      }, {
        ...meta,
        attachmentId: attachment.attachmentId,
      }));
    }
  }
  return records;
}

async function buildGroupKeyArchiveRecords(ownerUsernames, conversationId, version, groupKeyBytes) {
  const publicKeys = await fetchArchivePublicKeys(ownerUsernames);
  logMissingArchiveOwners(ownerUsernames, publicKeys);
  const records = [];
  for (const ownerUsername of [...new Set(ownerUsernames.filter(Boolean))]) {
    const bundle = publicKeys.get(ownerUsername);
    if (!bundle) {
      continue;
    }
    records.push(await buildArchiveRecord(bundle, 5, conversationId, {
      kind: "group_key",
      conversationId,
      version,
      groupKeyBytes: Array.from(groupKeyBytes),
    }, {
      groupKeyVersion: version,
      createdAt: new Date(),
    }));
  }
  return records;
}

async function buildDirectEnvelopes(plaintext) {
  const recipientBundles = await acquirePrekeyBundles(state.activePeer);
  if (recipientBundles.length === 0) {
    throw new Error("У получателя нет опубликованных E2EE-ключей.");
  }
  const ownBundles = await acquirePrekeyBundles(state.username);
  const targets = [
    ...recipientBundles,
    ...ownBundles.filter((bundle) => bundle.deviceId !== state.deviceId),
  ].filter(hasDirectBundleMaterial);
  if (targets.length === 0) {
    throw new Error("У участников чата нет корректного E2EE-материала.");
  }
  const envelopes = [];
  for (const bundle of targets) {
    const encrypted = await encryptDirectMessage(plaintext, state.identity, bundle);
    envelopes.push({
      targetUsername: bundle.username,
      targetDeviceId: bundle.deviceId,
      ciphertext: encrypted.ciphertext,
      nonce: encrypted.nonce,
      recipientSignedPrekeyId: encrypted.recipientSignedPrekeyId,
      recipientSignedPrekeyPublic: encrypted.recipientSignedPrekeyPublic,
      recipientOneTimePrekeyId: encrypted.recipientOneTimePrekeyId || "",
      recipientOneTimePrekeyPublic: encrypted.recipientOneTimePrekeyPublic || new Uint8Array(),
    });
  }
  return envelopes;
}

async function loadConversationKey(conversationId, version) {
  const cached = getRememberedConversationKey(conversationId, version);
  if (cached) {
    return cached;
  }

  const keyPackage = await userClient.getConversationKey({ conversationId, version });
  const envelope = keyPackage.envelopes.find((item) => item.username === state.username && item.deviceId === state.deviceId);
  if (!envelope) {
    throw new Error("group key envelope not found");
  }
  const senderIdentity = await fetchSenderIdentity(keyPackage.createdBy, envelope.senderKeyId);
  const groupKeyBytes = await decryptGroupKeyEnvelope(envelope, state.identity, senderIdentity.publicKey);
  rememberConversationKey(conversationId, version, groupKeyBytes);
  return groupKeyBytes;
}

async function loadLatestConversationKey(conversationId) {
  const keyPackage = await userClient.getConversationKey({ conversationId, version: 0 });
  const cached = getRememberedConversationKey(conversationId, keyPackage.version);
  if (cached) {
    return { version: keyPackage.version, groupKeyBytes: cached };
  }
  const envelope = keyPackage.envelopes.find((item) => item.username === state.username && item.deviceId === state.deviceId);
  if (!envelope) {
    throw new Error("latest group key envelope not found");
  }
  const senderIdentity = await fetchSenderIdentity(keyPackage.createdBy, envelope.senderKeyId);
  const groupKeyBytes = await decryptGroupKeyEnvelope(envelope, state.identity, senderIdentity.publicKey);
  rememberConversationKey(conversationId, keyPackage.version, groupKeyBytes);
  return { version: keyPackage.version, groupKeyBytes };
}

async function rotateConversationKey(conversation, version) {
  const members = conversation.members?.length
    ? conversation.members.map((member) => member.username)
    : (conversation.memberUsernames || []);
  const identities = await fetchIdentityKeys(members);
  const packageData = await createGroupKeyPackage(
    conversation.conversationId,
    version,
    state.identity,
    identities.map((identity) => ({
      username: identity.username,
      deviceId: identity.deviceId,
      keyId: identity.keyId,
      publicKeyBytes: identity.publicKey,
    })),
  );
  await userClient.upsertConversationKey({
    conversationId: packageData.conversationId,
    version: packageData.version,
    algorithm: packageData.algorithm,
    envelopes: packageData.envelopes,
    archiveRecords: await buildGroupKeyArchiveRecords(members, packageData.conversationId, packageData.version, packageData.groupKeyBytes),
  });
  rememberConversationKey(conversation.conversationId, version, packageData.groupKeyBytes);
  return packageData.groupKeyBytes;
}

async function buildNextGroupKeyUpdate(conversationId, version, members) {
  const identities = await fetchIdentityKeys(members);
  const packageData = await createGroupKeyPackage(
    conversationId,
    version,
    state.identity,
    identities.map((identity) => ({
      username: identity.username,
      deviceId: identity.deviceId,
      keyId: identity.keyId,
      publicKeyBytes: identity.publicKey,
    })),
  );
  return {
    keyUpdate: {
      version: packageData.version,
      algorithm: packageData.algorithm,
      envelopes: packageData.envelopes,
      archiveRecords: await buildGroupKeyArchiveRecords(members, packageData.conversationId, packageData.version, packageData.groupKeyBytes),
    },
    groupKeyBytes: packageData.groupKeyBytes,
  };
}

async function ensureConversationKey(conversation) {
  let version;
  let groupKeyBytes;
  try {
    ({ version, groupKeyBytes } = await loadLatestConversationKey(conversation.conversationId));
  } catch {
    version = 1;
    groupKeyBytes = await rotateConversationKey(conversation, version);
  }
  return { version, groupKeyBytes };
}

async function prepareOutgoingAttachments(activeConversation) {
  const out = [];
  for (const draft of state.pendingAttachments) {
    const bytes = new Uint8Array(await draft.file.arrayBuffer());
    const encrypted = await encryptMediaBytes(bytes, draft.mimeType, draft.filename);
    const prepared = await messageClient.prepareMediaUpload({
      filename: draft.filename,
      mimeType: draft.mimeType,
      sizeBytes: draft.file.size,
      kind: attachmentKindToProtoValue(encrypted.kind),
    });
    await messageClient.uploadMedia({
      mediaId: prepared.mediaId,
      ciphertext: encrypted.ciphertext,
      nonce: encrypted.nonce,
      sha256: encrypted.sha256,
      sizeBytes: draft.file.size,
      mimeType: draft.mimeType,
      filename: draft.filename,
      kind: attachmentKindToProtoValue(encrypted.kind),
    });
    out.push({
      attachmentId: `${prepared.mediaId}-att`,
      kind: attachmentKindToProtoValue(encrypted.kind),
      filename: draft.filename,
      mimeType: draft.mimeType,
      sizeBytes: draft.file.size,
      mediaId: prepared.mediaId,
      descriptorPlaintext: serializeMediaDescriptor(encrypted.descriptor),
      descriptor: encrypted.descriptor,
      sha256: encrypted.sha256,
      ciphertextSize: encrypted.ciphertextSize,
      preview: encrypted.kind === "image" ? { width: 0, height: 0 } : undefined,
      conversationId: activeConversation.conversationId,
    });
  }
  return out;
}

async function encryptOutgoingAttachmentDescriptorsForDirect(attachments) {
  const recipientBundles = await acquirePrekeyBundles(state.activePeer);
  if (recipientBundles.length === 0) {
    throw new Error("У получателя нет опубликованных E2EE-ключей.");
  }
  const ownBundles = await acquirePrekeyBundles(state.username);
  const targets = [
    ...recipientBundles,
    ...ownBundles.filter((bundle) => bundle.deviceId !== state.deviceId),
  ].filter(hasDirectBundleMaterial);
  if (targets.length === 0) {
    throw new Error("У участников чата нет корректного E2EE-материала.");
  }
  const out = [];
  for (const attachment of attachments) {
    out.push({
      attachmentId: attachment.attachmentId,
      kind: attachment.kind,
      filename: attachment.filename,
      mimeType: attachment.mimeType,
      sizeBytes: attachment.sizeBytes,
      mediaId: attachment.mediaId,
      sha256: attachment.sha256,
      ciphertextSize: attachment.ciphertextSize,
      preview: attachment.preview,
      directEnvelopes: await Promise.all(targets.map(async (bundle) => {
        const encryptedDescriptor = await encryptDirectMessage(
          new TextDecoder().decode(attachment.descriptorPlaintext),
          state.identity,
          bundle,
        );
        return {
          targetUsername: bundle.username,
          targetDeviceId: bundle.deviceId,
          encryptedDescriptor: encryptedDescriptor.ciphertext,
          descriptorNonce: encryptedDescriptor.nonce,
          recipientSignedPrekeyId: encryptedDescriptor.recipientSignedPrekeyId,
          recipientSignedPrekeyPublic: encryptedDescriptor.recipientSignedPrekeyPublic,
          recipientOneTimePrekeyId: encryptedDescriptor.recipientOneTimePrekeyId || "",
          recipientOneTimePrekeyPublic: encryptedDescriptor.recipientOneTimePrekeyPublic || new Uint8Array(),
        };
      })),
    });
  }
  return out;
}

async function encryptOutgoingAttachmentDescriptorsForGroup(attachments, groupKeyBytes, version) {
  const out = [];
  for (const attachment of attachments) {
    const encryptedDescriptor = await encryptGroupMessage(
      new TextDecoder().decode(attachment.descriptorPlaintext),
      groupKeyBytes,
      version,
    );
    out.push({
      attachmentId: attachment.attachmentId,
      kind: attachment.kind,
      filename: attachment.filename,
      mimeType: attachment.mimeType,
      sizeBytes: attachment.sizeBytes,
      mediaId: attachment.mediaId,
      encryptedDescriptor: encryptedDescriptor.ciphertext,
      descriptorNonce: encryptedDescriptor.nonce,
      sha256: attachment.sha256,
      ciphertextSize: attachment.ciphertextSize,
      preview: attachment.preview,
    });
  }
  return out;
}

function attachmentKindToProtoValue(kind) {
  if (kind === "image") {
    return 1;
  }
  if (kind === "video") {
    return 2;
  }
  return 3;
}

async function materializeMessage(message) {
  let text = message?.text || "";
  let decryptionError = false;
  if (!message?.encrypted) {
    const attachments = await Promise.all((message.attachments || []).map((item) => materializeAttachment(message, item)));
    return { ...message, attachments };
  }

  try {
    if (message.ciphertext?.length) {
      if (message.conversationId === message.to) {
        const groupKeyBytes = await loadConversationKey(message.conversationId, message.conversationKeyVersion);
        text = await decryptGroupMessage(message, groupKeyBytes);
      } else if (shouldDecryptDirectAsSender({ message, username: state.username, identity: state.identity })) {
        text = await decryptDirectMessageForSender(message, state.identity);
      } else {
        const senderIdentity = await fetchSenderIdentity(message.from, message.senderKeyId);
        text = await decryptDirectMessageForRecipient(message, state.identity, senderIdentity.publicKey);
      }
    }
  } catch (error) {
    console.error(error);
    text = "[Не удалось расшифровать]";
    decryptionError = true;
  }

  const attachments = await Promise.all((message.attachments || []).map((item) => materializeAttachment(message, item)));
  return { ...message, text, decryptionError, attachments };
}

async function materializeAttachment(message, attachment) {
  try {
    const cacheKey = `${message.conversationId}:${attachment.mediaId}`;
    if (state.mediaCache.has(cacheKey)) {
      return { ...attachment, ...state.mediaCache.get(cacheKey), decryptionError: false };
    }
    let descriptor;
    if (attachment.archiveDescriptor) {
      descriptor = attachment.archiveDescriptor;
    } else {
      let descriptorText;
      if (message.conversationId === message.to) {
        const groupKeyBytes = await loadConversationKey(message.conversationId, message.conversationKeyVersion);
        descriptorText = await decryptGroupMessage({
          ciphertext: attachment.encryptedDescriptor,
          nonce: attachment.descriptorNonce,
        }, groupKeyBytes);
      } else if (shouldDecryptDirectAsSender({ message, username: state.username, identity: state.identity })) {
        descriptorText = await decryptDirectMessageForSender({
          ciphertext: attachment.encryptedDescriptor,
          nonce: attachment.descriptorNonce,
          recipientSignedPrekeyPublic: message.recipientSignedPrekeyPublic,
          recipientOneTimePrekeyPublic: message.recipientOneTimePrekeyPublic,
        }, state.identity);
      } else {
        const senderIdentity = await fetchSenderIdentity(message.from, message.senderKeyId);
        descriptorText = await decryptDirectMessageForRecipient({
          ciphertext: attachment.encryptedDescriptor,
          nonce: attachment.descriptorNonce,
          recipientOneTimePrekeyId: message.recipientOneTimePrekeyId,
        }, state.identity, senderIdentity.publicKey);
      }
      descriptor = deserializeMediaDescriptor(new TextEncoder().encode(descriptorText));
    }
    const media = await messageClient.getMedia({ mediaId: attachment.mediaId });
    const plaintext = await decryptMediaBytes(media.ciphertext, descriptor, media.nonce);
    const blob = new Blob([plaintext], { type: descriptor.mimeType });
    const objectUrl = URL.createObjectURL(blob);
    const materialized = {
      objectUrl,
      mimeType: descriptor.mimeType,
      filename: descriptor.originalFilename,
      kind: descriptor.kind,
      sizeBytes: descriptor.sizeBytes,
      archiveDescriptor: descriptor,
    };
    state.mediaCache.set(cacheKey, materialized);
    return { ...attachment, ...materialized, decryptionError: false };
  } catch (error) {
    console.error(error);
    return { ...attachment, decryptionError: true };
  }
}

function renderAttachmentBodies(attachments) {
  return attachments.map((item) => {
    if (item.decryptionError) {
      return `<div class="attach-card">[Не удалось расшифровать вложение]</div>`;
    }
    if (item.kind === 1 || item.kind === "image") {
      return `<button class="attach-card" type="button" data-preview-src="${escapeHtml(item.objectUrl || "")}" data-preview-name="${escapeHtml(item.filename || "image")}" style="padding:0;border:none;background:none;cursor:pointer;"><img src="${escapeHtml(item.objectUrl || "")}" alt="${escapeHtml(item.filename || "image")}" style="max-width:240px;border-radius:14px;display:block;" /></button>`;
    }
    const label = item.kind === 2 || item.kind === "video" ? "Видео" : "Файл";
    return `<a class="attach-card" href="${escapeHtml(item.objectUrl || "#")}" download="${escapeHtml(item.filename || "file")}">${label}: ${escapeHtml(item.filename || "attachment")}</a>`;
  }).join("");
}

function bytesEqual(left, right) {
  if (!left || !right || left.length !== right.length) {
    return false;
  }
  for (let i = 0; i < left.length; i++) {
    if (left[i] !== right[i]) {
      return false;
    }
  }
  return true;
}

function archiveRecordId(ownerUsername, recordType, conversationId, messageId, attachmentId = "", groupKeyVersion = 0) {
  return `backfill:v2:${ownerUsername}:${recordType}:${conversationId}:${messageId}:${attachmentId}:${groupKeyVersion}`;
}

function messageTimestamp(message) {
  if (message?.createdAt?.toDate) {
    return message.createdAt.toDate();
  }
  if (message?.ts) {
    return new Date(message.ts);
  }
  if (message?.createdAt) {
    return new Date(message.createdAt);
  }
  return new Date();
}

function isUnavailableArchivedText(text) {
  return text === "[Сообщение недоступно на этом устройстве]" || text === "[Не удалось расшифровать]";
}

function isUnavailablePlaceholderMessage(message) {
  return isUnavailableArchivedText(message?.text || "") && (!message?.attachments || message.attachments.length === 0);
}

function hasRenderableMessageContent(message) {
  return (typeof message?.text === "string" && message.text.trim() && !isUnavailableArchivedText(message.text))
    || Boolean(message?.attachments?.length);
}

function isSameConversationDirection(a, b) {
  return a?.from === b?.from && a?.to === b?.to;
}

function isNearMessageTimestamp(a, b, maxDeltaMs = 15000) {
  return Math.abs(messageTimestamp(a).getTime() - messageTimestamp(b).getTime()) <= maxDeltaMs;
}

function shouldSuppressUnavailablePlaceholder(placeholder, messages) {
  if (!isUnavailablePlaceholderMessage(placeholder)) {
    return false;
  }
  return messages.some((candidate) => candidate !== placeholder
    && isSameConversationDirection(candidate, placeholder)
    && isNearMessageTimestamp(candidate, placeholder)
    && hasRenderableMessageContent(candidate));
}

function normalizeConversationMessages(messages) {
  return messages.filter((message, _, items) => !shouldSuppressUnavailablePlaceholder(message, items));
}

async function backfillConversationHistory(conversationId, messages) {
  if (!state.archiveIdentity || !conversationId || messages.length === 0) {
    return;
  }
  const readableMessages = messages.filter((message) => Number(message.messageId || 0) > 0 && !message.archived);
  if (readableMessages.length === 0) {
    return;
  }
  const conversation = state.conversations.find((item) => item.conversationId === conversationId);
  const isGroup = conversation?.kind === 2 || readableMessages.some((message) => message.conversationId === message.to);
  const owners = [state.username];
  const publicKeys = await fetchArchivePublicKeys(owners);
  logMissingArchiveOwners(owners, publicKeys);
  if (publicKeys.size === 0) {
    return;
  }

  const records = [];
  for (const message of readableMessages) {
    const createdAt = messageTimestamp(message);
    const messageId = Number(message.messageId || 0);
    if (typeof message.text === "string" && message.text.trim() && !message.decryptionError && !isUnavailableArchivedText(message.text)) {
      const recordType = isGroup ? 2 : 1;
      const payload = {
        kind: "message",
        from: message.from,
        to: message.to,
        text: message.text,
        senderDeviceId: message.senderDeviceId || "",
        createdAt: createdAt.toISOString(),
        conversationKeyVersion: message.conversationKeyVersion || 0,
      };
      for (const [ownerUsername, bundle] of publicKeys.entries()) {
        records.push(await buildArchiveRecord(bundle, recordType, conversationId, payload, {
          messageId,
          createdAt,
          recordId: archiveRecordId(ownerUsername, recordType, conversationId, messageId),
        }));
      }
    }

    for (const attachment of message.attachments || []) {
      if (attachment.decryptionError || !attachment.archiveDescriptor) {
        continue;
      }
      const recordType = isGroup ? 4 : 3;
      const payload = {
        kind: "attachment",
        attachmentId: attachment.attachmentId,
        from: message.from,
        to: message.to,
        text: typeof message.text === "string" && !isUnavailableArchivedText(message.text) ? message.text : "",
        senderDeviceId: message.senderDeviceId || "",
        createdAt: createdAt.toISOString(),
        conversationKeyVersion: message.conversationKeyVersion || 0,
        mediaId: attachment.mediaId,
        descriptor: attachment.archiveDescriptor,
        sha256: Array.from(attachment.sha256 || []),
      };
      for (const [ownerUsername, bundle] of publicKeys.entries()) {
        records.push(await buildArchiveRecord(bundle, recordType, conversationId, payload, {
          messageId,
          attachmentId: attachment.attachmentId,
          createdAt,
          recordId: archiveRecordId(ownerUsername, recordType, conversationId, messageId, attachment.attachmentId),
        }));
      }
    }
  }

  if (records.length === 0) {
    return;
  }
  try {
    await messageClient.appendHistoryArchiveRecords({ items: records });
  } catch (error) {
    console.warn("failed to backfill readable conversation history into archive", error);
  }
}

function mergeArchivedMessages(conversationId, liveMessages) {
  const archived = state.messages.get(conversationId) || [];
  if (archived.length === 0) {
    return normalizeConversationMessages(liveMessages);
  }
  const merged = new Map();
  for (const message of liveMessages) {
    merged.set(message.messageId, message);
  }
  for (const archivedMessage of archived) {
    const existing = merged.get(archivedMessage.messageId);
    if (!existing) {
      merged.set(archivedMessage.messageId, archivedMessage);
      continue;
    }
    if (existing.text === "[Сообщение недоступно на этом устройстве]") {
      merged.set(archivedMessage.messageId, {
        ...archivedMessage,
        attachments: archivedMessage.attachments?.length ? archivedMessage.attachments : existing.attachments,
      });
      continue;
    }
    if ((!existing.attachments || existing.attachments.every((item) => item.decryptionError)) && archivedMessage.attachments?.length) {
      merged.set(archivedMessage.messageId, {
        ...existing,
        attachments: archivedMessage.attachments,
      });
    }
  }
  return normalizeConversationMessages([...merged.values()])
    .sort((a, b) => Number(a.messageId) - Number(b.messageId));
}

function getActiveMessages() {
  return state.messages.get(state.activeConversationId) || [];
}

function findMessageById(messageId) {
  return lookupMessageById(state.messages, messageId);
}

function saveUnreadCounts() {
  const obj = {};
  for (const [k, v] of state.localUnread) obj[k] = v;
  localStorage.setItem("messenger-unread", JSON.stringify(obj));
}

function loadUnreadCounts() {
  try {
    const saved = JSON.parse(localStorage.getItem("messenger-unread") || "{}");
    for (const [k, v] of Object.entries(saved)) {
      if (state.conversations.some((c) => c.conversationId === k)) {
        state.localUnread.set(k, Number(v));
      }
    }
  } catch {}
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
  state.identity = null;
  state.archiveIdentity = null;
  state.identityKeys = new Map();
  state.groupKeys = new Map();
  state.mediaCache = new Map();
  state.encryptionPrefs = new Map();
  state.e2eeReady = false;
  state.archiveSequence = 0;
  state.pendingAttachments = [];
  state.activeConversationId = "";
  state.activePeer = "";
  state.userSearchQuery = "";
  state.userSearchResults = [];
  state.messageSearchOpen = false;
  state.messageSearchQuery = "";
  state.messageSearchResults = [];
  state.messageSearchIndex = 0;
  state.localUnread = new Map();
  localStorage.removeItem("messenger-unread");
  state.selectedMessageId = 0;
  state.onlineUsers = new Set();
  state.status = "disconnected";
  state.showProfileEditor = false;
  state.showSelfProfile = false;
  state.showConvProfile = false;
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

function scrollToCurrentSearchResult() {
  const inConv = state.messageSearchResults.filter((m) => m.conversationId === state.activeConversationId);
  const idx = Math.min(state.messageSearchIndex, Math.max(0, inConv.length - 1));
  const msgId = inConv[idx]?.messageId;
  if (!msgId) return;
  requestAnimationFrame(() => {
    const el = document.querySelector(`[data-message-id="${msgId}"]`);
    el?.scrollIntoView({ behavior: "smooth", block: "center" });
  });
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
