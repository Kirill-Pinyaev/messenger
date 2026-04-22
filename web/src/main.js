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
    <main class="shell">
      <section class="auth-card">
        <div class="hero">
          <div class="eyebrow">gRPC-Web Client</div>
          <h1>Учебный мессенджер</h1>
          <p>Web-клиент работает через новый protobuf-контракт и общается с сервером по gRPC-Web.</p>
        </div>

        <div class="auth-tabs">
          <button id="auth-login-tab" class="${isLogin ? "active" : ""}" type="button">Вход</button>
          <button id="auth-register-tab" class="${!isLogin ? "active" : ""}" type="button">Регистрация</button>
        </div>

        <form id="auth-form" class="stack" style="margin-top:16px;">
          ${isLogin ? "" : `
            <div class="split">
              <input id="first-name" placeholder="Имя" />
              <input id="last-name" placeholder="Фамилия" />
            </div>
          `}
          <input id="username" placeholder="nickname" value="${escapeHtml(state.username)}" />
          <input id="password" type="password" placeholder="password" />
          ${isLogin ? "" : `<input id="password-repeat" type="password" placeholder="повторите пароль" />`}
          <button type="submit">${isLogin ? "Войти" : "Создать аккаунт"}</button>
          <div class="hint ${state.authError ? "error" : ""}">${escapeHtml(state.authMessage)}</div>
          ${isLogin && state.showRegisterPrompt ? `<button id="suggest-register" class="secondary" type="button">Перейти к регистрации</button>` : ""}
        </form>
      </section>
    </main>
  `;
}

function renderChat() {
  const activeMessages = getActiveMessages();
  const profile = state.profile || {};
  const activeConversation = state.conversations.find((item) => item.conversationId === state.activeConversationId) || null;
  const activeProfile = state.activePeer ? state.profiles.get(state.activePeer) || null : null;
  const selectedMessage = findMessageById(state.selectedMessageId);
  const selectableGroupUsers = filterSelectableUsers(state.groupSearchResults, state.groupSelectedMembers, state.username);
  const activeGroupRole = currentUserRole(activeConversation, state.username);
  const activeGroupCanManage = canManageGroupMembers(activeConversation, state.username);

  app.innerHTML = `
    <main class="chat-page">
      <section class="chat-frame">
        <aside class="sidebar">
          <div class="chat-top">
            <div>
              <div class="eyebrow">Messenger</div>
              <h1>${escapeHtml(displayName(state.username))}</h1>
              <p class="muted">@${escapeHtml(state.username)}</p>
            </div>
            <div class="pill"><span class="dot ${state.status === "connected" ? "online" : ""}"></span>${escapeHtml(state.status)}</div>
          </div>

          <section class="section">
            <div class="section-title">
              <h2>Поиск пользователей</h2>
            </div>
            <input id="user-search" placeholder="например, alice" value="${escapeHtml(state.userSearchQuery)}" />
            <div class="list">
              ${state.userSearchResults.map((user) => `
                <button class="user-item" data-user-open="${escapeHtml(user.username)}" type="button">
                  ${escapeHtml(displayName(user.username))}
                  <small>@${escapeHtml(user.username)}</small>
                </button>
              `).join("") || `<div class="muted">Введите ник для поиска нового диалога.</div>`}
            </div>
          </section>

          <section class="section">
            <div class="section-title">
              <h2>Диалоги</h2>
              <button id="open-create-group" class="secondary" type="button">Создать группу</button>
            </div>
            <div class="list">
              ${state.conversations.map((conversation) => {
                const active = conversation.conversationId === state.activeConversationId ? "active" : "";
                const peer = conversation.peerUsername || conversation.peerProfile?.username || "";
                const online = conversation.kind === 2 ? `${conversation.memberUsernames?.length || 0} участников` : (state.onlineUsers.has(peer) ? "в сети" : "offline");
                return `
                  <button class="conversation-item ${active}" data-conversation-open="${escapeHtml(conversation.conversationId)}" type="button">
                    ${escapeHtml(conversationLabel(state.profiles, state.profile, conversation))}
                    <small>${escapeHtml(online)}</small>
                  </button>
                `;
              }).join("") || `<div class="muted">Диалогов пока нет.</div>`}
            </div>
          </section>

          <section class="profile-panel">
            <div class="section-title">
              <h3>Профиль</h3>
              <button id="toggle-profile" class="secondary" type="button">${state.showProfileEditor ? "Скрыть" : "Изменить"}</button>
            </div>
            <div>
              <strong>${escapeHtml(displayName(state.username))}</strong>
              <div class="muted">@${escapeHtml(state.username)}</div>
            </div>
            ${state.showProfileEditor ? `
              <div class="stack">
                <input id="profile-first-name" placeholder="Имя" value="${escapeHtml(profile.firstName || "")}" />
                <input id="profile-last-name" placeholder="Фамилия" value="${escapeHtml(profile.lastName || "")}" />
                <input id="profile-avatar-hex" placeholder="Цвет аватара, например 23685b" value="${escapeHtml((profile.avatarHex || "").replace("#", ""))}" />
                <div class="row">
                  <button id="save-profile" type="button">Сохранить</button>
                  <button id="cancel-profile" class="secondary" type="button">Отмена</button>
                </div>
              </div>
            ` : ""}
            <div class="row">
              <button id="logout" class="secondary" type="button">Выйти</button>
              <button id="delete-account" class="danger" type="button">Удалить аккаунт</button>
            </div>
          </section>
        </aside>

        <section class="content">
          ${state.groupEditorOpen ? `
            <section class="section">
              <header class="chat-top">
                <div>
                  <div class="eyebrow">Группа</div>
                  <h1>${state.groupEditorMode === "create" ? "Создание группы" : "Управление участниками"}</h1>
                  <p class="muted">${state.groupEditorMode === "create" ? "Выберите участников через поиск и задайте название." : "Добавляйте участников, удаляйте тех, на кого у вас есть права, и при необходимости передавайте права администратора."}</p>
                </div>
                <button id="close-group-editor" class="secondary" type="button">Закрыть</button>
              </header>

              <div class="section">
                <input id="group-title" placeholder="название группы" value="${escapeHtml(state.groupTitleDraft)}" ${state.groupEditorMode === "edit" ? "disabled" : ""} />
                <input id="group-user-search" placeholder="начните вводить ник" value="${escapeHtml(state.groupMemberQuery)}" />
                <div class="list">
                  ${selectableGroupUsers.map((user) => `
                    <button class="user-item" data-group-add="${escapeHtml(user.username)}" type="button">
                      ${escapeHtml(displayName(user.username))}
                      <small>@${escapeHtml(user.username)}</small>
                    </button>
                  `).join("") || `<div class="muted">Подсказки появятся после поиска пользователей.</div>`}
                </div>
              </div>

              <div class="section">
                <div class="section-title"><h3>Участники</h3></div>
                <div class="list">
                  ${state.groupSelectedMembers.map((user) => `
                    <div class="conversation-item">
                      ${escapeHtml(displayName(user.username))}
                      <small>@${escapeHtml(user.username)}</small>
                      ${user.role === 1 ? `<small>Администратор</small>` : `<small>${user.addedBy ? `Добавил: @${escapeHtml(user.addedBy)}` : "Участник"}</small>`}
                      <div class="row" style="margin-top:8px;">
                        ${state.groupEditorMode === "edit" && isExistingGroupMember(user.username) && canTransferAdmin(activeConversation, state.username) && user.username !== state.username ? `<button class="secondary" data-group-transfer-admin="${escapeHtml(user.username)}" type="button">Сделать админом</button>` : ""}
                        ${(state.groupEditorMode === "create" || canRemoveGroupMember(activeConversation, state.username, user.username) || !isExistingGroupMember(user.username))
                          ? `<button class="danger" data-group-remove="${escapeHtml(user.username)}" type="button">${state.groupEditorMode === "edit" && isExistingGroupMember(user.username) ? "Исключить" : "Удалить"}</button>`
                          : ""}
                      </div>
                    </div>
                  `).join("") || `<div class="muted">Участники пока не выбраны.</div>`}
                </div>
              </div>

              <div class="row">
                ${state.groupEditorMode === "create" ? `<button id="submit-create-group" type="button">Создать группу</button>` : `${activeGroupCanManage ? `<button id="submit-add-group-members" type="button">Добавить выбранных участников</button>` : ""}`}
              </div>
            </section>
          ` : `
          <header class="chat-top">
            <div>
              <div class="eyebrow">Диалог</div>
              <h1>${activeConversation ? escapeHtml(conversationLabel(state.profiles, state.profile, activeConversation)) : "Выберите диалог"}</h1>
              <p class="muted">${activeConversation ? escapeHtml(conversationMetaLine(state.profiles, state.profile, activeConversation)) : "История и отправка сообщений работают через gRPC-Web."}</p>
            </div>
            <div class="row">
              ${activeConversation?.kind === 2 ? `<button id="open-edit-group" class="secondary" type="button">Участники</button>` : ""}
              ${activeConversation?.kind === 2 ? `<button id="leave-group" class="danger" type="button">Выйти из группы</button>` : ""}
              <input id="message-search" placeholder="поиск по сообщениям" value="${escapeHtml(state.messageSearchQuery)}" />
              <button id="search-messages" class="secondary" type="button">Найти</button>
            </div>
          </header>

          <div class="messages" id="messages">
            ${state.activeConversationId ? renderMessages(activeMessages) : `<div class="empty-state">Сначала выберите существующий диалог или найдите пользователя слева.</div>`}
          </div>

          ${state.messageSearchResults.length > 0 ? `
            <section class="section">
              <div class="section-title"><h3>Найденные сообщения</h3></div>
              <div class="list">
                ${state.messageSearchResults.map((message) => `
                  <button class="search-hit" data-search-open="${message.messageId}" type="button">
                    ${escapeHtml(message.text)}
                    <small>@${escapeHtml(message.from)} · ${escapeHtml(formatTimestamp(message.createdAt))}</small>
                  </button>
                `).join("")}
              </div>
            </section>
          ` : ""}

          <div class="composer">
            ${selectedMessage ? `
              <div class="selected-bar">
                Выбрано сообщение #${selectedMessage.messageId}. Можно удалить его у всех, если оно отправлено вами.
              </div>
            ` : ""}
            <div class="composer-head">
              <strong>${activeConversation ? `Сообщение в ${escapeHtml(conversationLabel(state.profiles, state.profile, activeConversation))}` : "Сообщение"}</strong>
              ${selectedMessage && selectedMessage.from === state.username ? `<button id="delete-message" class="danger" type="button">Удалить выбранное</button>` : ""}
            </div>
            <textarea id="message-text" placeholder="привет" ${activeConversation ? "" : "disabled"}></textarea>
            <div class="row">
              <button id="send-message" type="button" ${activeConversation ? "" : "disabled"}>Отправить</button>
            </div>
          </div>
          `}
        </section>
      </section>
    </main>
  `;

  if (activeConversation) {
    document.title = `${conversationLabel(state.profiles, state.profile, activeConversation)} · Messenger`;
  } else {
    document.title = "Messenger";
  }
}

function renderMessages(messages) {
  if (messages.length === 0) {
    return `<div class="empty-state">История пока пустая. Отправьте первое сообщение.</div>`;
  }

  return messages.map((message) => {
    const own = message.from === state.username;
    const selected = state.selectedMessageId === message.messageId ? ' style="outline: 2px solid rgba(31, 107, 95, 0.35);"' : "";
    return `
      <button class="message ${own ? "out" : ""}" data-message-select="${message.messageId}" type="button"${selected}>
        <span class="message-head">
          <span>${escapeHtml(own ? "Вы" : displayName(message.from))}</span>
          <span>${escapeHtml(formatTimestamp(message.createdAt))}</span>
        </span>
        <span class="message-text">${escapeHtml(message.text)}</span>
      </button>
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
