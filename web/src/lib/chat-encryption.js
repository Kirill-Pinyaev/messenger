const STORAGE_PREFIX = "messenger-encryption-prefs:";

export function encryptionPrefsStorageKey(username) {
  return `${STORAGE_PREFIX}${username}`;
}

export function isConversationEncryptionEnabled(prefs, conversationId) {
  if (!conversationId) {
    return true;
  }
  return prefs.get(conversationId) !== false;
}

export function loadEncryptionPrefs(username, storage) {
  const prefs = new Map();
  if (!username) {
    return prefs;
  }
  try {
    const raw = JSON.parse(storage.getItem(encryptionPrefsStorageKey(username)) || "{}");
    for (const [conversationId, enabled] of Object.entries(raw)) {
      prefs.set(conversationId, enabled !== false);
    }
  } catch {
    return new Map();
  }
  return prefs;
}

export function saveEncryptionPrefs(username, prefs, storage) {
  if (!username) {
    return;
  }
  const raw = {};
  for (const [conversationId, enabled] of prefs.entries()) {
    raw[conversationId] = enabled !== false;
  }
  storage.setItem(encryptionPrefsStorageKey(username), JSON.stringify(raw));
}

export function setConversationEncryption(prefs, conversationId, enabled) {
  const next = new Map(prefs);
  if (!conversationId) {
    return next;
  }
  next.set(conversationId, enabled !== false);
  return next;
}
