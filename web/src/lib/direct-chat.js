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

export function currentDevicePrekeyBundle({ username, deviceId, identity }) {
  return {
    username,
    deviceId,
    identityKey: {
      username,
      deviceId,
      keyId: identity.keyId,
      algorithm: identity.algorithm,
      publicKey: identity.publicKeyBytes,
    },
    signedPrekey: {
      keyId: identity.signedPrekey.keyId,
      algorithm: identity.signedPrekey.algorithm,
      publicKey: identity.signedPrekey.publicKeyBytes,
      publicKeyBytes: identity.signedPrekey.publicKeyBytes,
      signature: identity.signedPrekey.signature,
      signatureAlgorithm: identity.signedPrekey.signatureAlgorithm,
    },
  };
}

export function hasDirectBundleMaterial(bundle) {
  const signedPublicKey = bundle?.signedPrekey?.publicKey || bundle?.signedPrekey?.publicKeyBytes;
  const signature = bundle?.signedPrekey?.signature;
  const identityPublicKey = bundle?.identityKey?.publicKey || bundle?.identityKey?.publicKeyBytes || bundle?.identityPublicKey;
  return !!(signedPublicKey && signedPublicKey.length > 0 && signature && signature.length > 0 && identityPublicKey && identityPublicKey.length > 0);
}

export function shouldDecryptDirectAsSender({ message, username, identity }) {
  if (message.from !== username) {
    return false;
  }
  const signedPrekeyMatchesCurrentDevice = message.recipientSignedPrekeyId
    && message.recipientSignedPrekeyId === identity?.signedPrekey?.keyId;
  if (signedPrekeyMatchesCurrentDevice) {
    return false;
  }
  const otpMatchesCurrentDevice = !!(message.recipientOneTimePrekeyId
    && (identity?.oneTimePrekeys || []).some((item) => item.keyId === message.recipientOneTimePrekeyId));
  if (otpMatchesCurrentDevice) {
    return false;
  }
  return true;
}
