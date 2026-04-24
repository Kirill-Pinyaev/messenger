import { Code, ConnectError } from "@connectrpc/connect";

import { makeConversationId } from "./chat-state.js";

export function prepareDirectConversation({
  conversations,
  messages,
  profiles,
  selfUsername,
  peerUsername,
}) {
  const conversationId = makeConversationId(selfUsername, peerUsername);
  const nextMessages = new Map(messages);
  if (!nextMessages.has(conversationId)) {
    nextMessages.set(conversationId, []);
  }

  let nextConversations = conversations;
  if (!conversations.some((item) => item.conversationId === conversationId)) {
    nextConversations = [{
      conversationId,
      peerUsername,
      peerProfile: profiles.get(peerUsername) || null,
    }, ...conversations];
  }

  return {
    conversationId,
    conversations: nextConversations,
    messages: nextMessages,
  };
}

export function directIdentityErrorMessage(err, username) {
  const rawMessage = err instanceof ConnectError ? (err.rawMessage || err.message) : (err instanceof Error ? err.message : String(err || "unknown error"));
  const missingIdentity = (
    (err instanceof ConnectError && err.code === Code.NotFound && (rawMessage === "identity key not found" || rawMessage === "prekey bundle not found"))
    || rawMessage === "identity key not found"
    || rawMessage === "prekey bundle not found"
  );
  if (missingIdentity) {
    return `Пользователь @${username} ещё не входил в зашифрованную версию и не опубликовал ключ. Попросите его войти в систему.`;
  }
  return rawMessage;
}
