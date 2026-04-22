export function makeConversationId(left, right) {
  return [left, right].sort().join("|");
}

export function peerFromConversationId(conversationId, username) {
  const [left, right] = conversationId.split("|");
  if (!left || !right) {
    return "";
  }
  return left === username ? right : left;
}

export function displayName(profiles, ownProfile, username) {
  const profile = profiles.get(username) || (ownProfile?.username === username ? ownProfile : null);
  if (!profile) {
    return username;
  }
  const fullName = `${profile.firstName || ""} ${profile.lastName || ""}`.trim();
  return fullName || username;
}

export function conversationLabel(profiles, ownProfile, conversation) {
  if (conversation?.kind === 2 && conversation.title) {
    return conversation.title;
  }
  return displayName(profiles, ownProfile, conversation?.peerUsername || conversation?.peerProfile?.username || "");
}

export function conversationMetaLine(profiles, ownProfile, conversation) {
  if (conversation?.kind === 2) {
    const count = conversation.memberUsernames?.length || 0;
    return `${count} участников`;
  }
  const peer = conversation?.peerUsername || conversation?.peerProfile?.username || "";
  return peer ? `@${displayName(profiles, ownProfile, peer)}` : "";
}

export function normalizeAvatarHex(value) {
  const normalized = value.trim().replace(/^#/, "");
  if (!normalized) {
    return "";
  }
  return /^[0-9a-fA-F]{6}$/.test(normalized) ? normalized.toLowerCase() : "";
}

export function ensureConversationForMessage(conversations, profiles, currentUsername, message) {
  const conversationId = message?.conversationId;
  if (!conversationId) {
    return conversations;
  }

  const peerUsername = message.from === currentUsername ? message.to : message.from;
  if (!conversations.some((item) => item.conversationId === conversationId)) {
    return [{
      conversationId,
      peerUsername,
      peerProfile: profiles.get(peerUsername) || null,
    }, ...conversations];
  }

  return [
    ...conversations.filter((item) => item.conversationId === conversationId),
    ...conversations.filter((item) => item.conversationId !== conversationId),
  ];
}

export function upsertConversation(conversations, conversation) {
  if (!conversation?.conversationId) {
    return conversations;
  }
  return [conversation, ...conversations.filter((item) => item.conversationId !== conversation.conversationId)];
}

export function removeConversationById(conversations, conversationId) {
  if (!conversationId) {
    return conversations;
  }
  return conversations.filter((item) => item.conversationId !== conversationId);
}

export function upsertMessageCollection(messages, message) {
  if (!message?.conversationId || !message?.messageId) {
    return messages;
  }

  const next = new Map(messages);
  const items = [...(next.get(message.conversationId) || [])];
  const index = items.findIndex((item) => item.messageId === message.messageId);
  if (index >= 0) {
    items[index] = message;
  } else {
    items.push(message);
    items.sort((left, right) => Number(left.messageId) - Number(right.messageId));
  }
  next.set(message.conversationId, items);
  return next;
}

export function removeMessageCollection(messages, messageId) {
  const next = new Map(messages);
  for (const [conversationId, items] of next.entries()) {
    const filtered = items.filter((item) => item.messageId !== messageId);
    next.set(conversationId, filtered);
  }
  return next;
}

export function findMessageById(messages, messageId) {
  if (!messageId) {
    return null;
  }
  for (const items of messages.values()) {
    const message = items.find((item) => item.messageId === messageId);
    if (message) {
      return message;
    }
  }
  return null;
}

export function isGroupMessage(message) {
  return Boolean(message?.conversationId) && message?.conversationId === message?.to;
}
