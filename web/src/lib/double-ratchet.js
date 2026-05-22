import { ed25519, x25519 } from "@noble/curves/ed25519.js";

const subtle = globalThis.crypto?.subtle;
const encoder = new TextEncoder();
const decoder = new TextDecoder();

export const DR_ALGORITHM = "DR-X25519-HKDF-SHA256-AESGCM-Ed25519-v1";

const ROOT_INFO = encoder.encode("messenger-dr-root-v1");
const CHAIN_INFO = encoder.encode("messenger-dr-chain-v1");
const MSG_INFO = encoder.encode("messenger-dr-message-v1");
const SPK_PREFIX = encoder.encode("messenger-spk-v1");

function assertCrypto() {
  if (!subtle) throw new Error("WebCrypto is not available");
}

function randomKeyId(username, prefix) {
  return `${username}-${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function concat(chunks) {
  const total = chunks.reduce((sum, item) => sum + item.length, 0);
  const out = new Uint8Array(total);
  let offset = 0;
  for (const item of chunks) {
    out.set(item, offset);
    offset += item.length;
  }
  return out;
}

function utf8(value) {
  return encoder.encode(String(value ?? ""));
}

function canonicalSignedPrekeyBytes({ username, deviceId = "", keyId, publicKey }) {
  return concat([
    SPK_PREFIX,
    utf8(username),
    new Uint8Array([0]),
    utf8(deviceId),
    new Uint8Array([0]),
    utf8(keyId),
    new Uint8Array([0]),
    publicKey,
  ]);
}

async function hkdfBytes(ikm, info, outLen = 32, salt = new Uint8Array(32)) {
  assertCrypto();
  const key = await subtle.importKey("raw", ikm, "HKDF", false, ["deriveBits"]);
  const bits = await subtle.deriveBits({ name: "HKDF", hash: "SHA-256", salt, info }, key, outLen * 8);
  return new Uint8Array(bits);
}

async function aesGcmEncrypt(keyBytes, plaintext) {
  const key = await subtle.importKey("raw", keyBytes, "AES-GCM", false, ["encrypt"]);
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = new Uint8Array(await subtle.encrypt({ name: "AES-GCM", iv: nonce }, key, plaintext));
  return { ciphertext, nonce };
}

async function aesGcmDecrypt(keyBytes, ciphertext, nonce) {
  const key = await subtle.importKey("raw", keyBytes, "AES-GCM", false, ["decrypt"]);
  return new Uint8Array(await subtle.decrypt({ name: "AES-GCM", iv: nonce }, key, ciphertext));
}

function b64(bytes) {
  if (typeof btoa === "function") {
    let binary = "";
    for (const byte of bytes) binary += String.fromCharCode(byte);
    return btoa(binary);
  }
  return Buffer.from(bytes).toString("base64");
}

function fromB64(value) {
  if (typeof atob === "function") {
    const binary = atob(value);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes;
  }
  return new Uint8Array(Buffer.from(value, "base64"));
}

function keypair(curve) {
  const privateKey = curve.utils.randomSecretKey();
  return { privateKey, publicKey: curve.getPublicKey(privateKey) };
}

async function signSignedPrekey(identity) {
  return ed25519.sign(
    canonicalSignedPrekeyBytes({
      username: identity.username,
      deviceId: identity.deviceId,
      keyId: identity.signedPrekey.keyId,
      publicKey: identity.signedPrekey.publicKeyBytes,
    }),
    identity.privateKeyBytes,
  );
}

function normalizeBundle(bundle) {
  return {
    username: bundle.username || bundle.identityKey?.username || bundle.signedPrekey?.username,
    deviceId: bundle.deviceId || bundle.identityKey?.deviceId || bundle.signedPrekey?.deviceId || "",
    identityPublicKey: bundle.identityPublicKey || bundle.identityKey?.publicKey || bundle.identityKey?.publicKeyBytes,
    signedPrekey: {
      keyId: bundle.signedPrekey?.keyId,
      publicKey: bundle.signedPrekey?.publicKey || bundle.signedPrekey?.publicKeyBytes,
      signature: bundle.signedPrekey?.signature,
      signatureAlgorithm: bundle.signedPrekey?.signatureAlgorithm || bundle.signedPrekey?.signature_algorithm,
    },
    oneTimePrekey: bundle.oneTimePrekey ? {
      keyId: bundle.oneTimePrekey.keyId,
      publicKey: bundle.oneTimePrekey.publicKey || bundle.oneTimePrekey.publicKeyBytes,
    } : null,
  };
}

export async function verifySignedPrekey(bundle) {
  const normalized = normalizeBundle(bundle);
  if (!normalized.identityPublicKey || !normalized.signedPrekey.publicKey || !normalized.signedPrekey.signature) {
    return false;
  }
  return ed25519.verify(
    normalized.signedPrekey.signature,
    canonicalSignedPrekeyBytes({
      username: normalized.username,
      deviceId: normalized.deviceId,
      keyId: normalized.signedPrekey.keyId,
      publicKey: normalized.signedPrekey.publicKey,
    }),
    normalized.identityPublicKey,
  );
}

export async function createRatchetIdentity(username, deviceId = "") {
  const identity = keypair(ed25519);
  const spk = keypair(x25519);
  const oneTimePrekeys = [];
  const out = {
    username,
    deviceId,
    keyId: randomKeyId(username, "identity"),
    algorithm: "Ed25519",
    publicKeyBytes: identity.publicKey,
    privateKeyBytes: identity.privateKey,
    signedPrekey: {
      username,
      deviceId,
      keyId: randomKeyId(username, "spk"),
      algorithm: "X25519",
      publicKeyBytes: spk.publicKey,
      privateKeyBytes: spk.privateKey,
      signature: new Uint8Array(),
      signatureAlgorithm: "Ed25519",
      published: false,
    },
    oneTimePrekeys,
    ratchetSessions: {},
    sentMessageKeys: {},
    skippedMessageKeys: {},
    toIdentityKey() {
      return { username, deviceId, keyId: this.keyId, algorithm: "Ed25519", publicKey: this.publicKeyBytes };
    },
    toPrekeyBundle() {
      return {
        username,
        deviceId,
        identityKey: this.toIdentityKey(),
        signedPrekey: {
          username,
          deviceId,
          keyId: this.signedPrekey.keyId,
          algorithm: "X25519",
          publicKey: this.signedPrekey.publicKeyBytes,
          signature: this.signedPrekey.signature,
          signatureAlgorithm: "Ed25519",
        },
        oneTimePrekey: undefined,
      };
    },
  };
  out.signedPrekey.signature = await signSignedPrekey(out);
  return out;
}

function attachMethods(identity) {
  identity.toIdentityKey = function toIdentityKey() {
    return { username: this.username, deviceId: this.deviceId, keyId: this.keyId, algorithm: "Ed25519", publicKey: this.publicKeyBytes };
  };
  identity.toPrekeyBundle = function toPrekeyBundle() {
    return {
      username: this.username,
      deviceId: this.deviceId,
      identityKey: this.toIdentityKey(),
      signedPrekey: {
        username: this.username,
        deviceId: this.deviceId,
        keyId: this.signedPrekey.keyId,
        algorithm: "X25519",
        publicKey: this.signedPrekey.publicKeyBytes,
        signature: this.signedPrekey.signature,
        signatureAlgorithm: "Ed25519",
      },
      oneTimePrekey: undefined,
    };
  };
  return identity;
}

async function deriveInitialRootAsSender(senderIdentity, bundle, ratchetPrivate) {
  const secrets = [x25519.getSharedSecret(ratchetPrivate, bundle.signedPrekey.publicKey)];
  if (bundle.oneTimePrekey?.publicKey?.length) secrets.push(x25519.getSharedSecret(ratchetPrivate, bundle.oneTimePrekey.publicKey));
  return hkdfBytes(concat(secrets), ROOT_INFO);
}

async function deriveInitialRootAsRecipient(recipientIdentity, payload) {
  const secrets = [x25519.getSharedSecret(recipientIdentity.signedPrekey.privateKeyBytes, payload.ratchetPublicKey)];
  const otp = payload.recipientOneTimePrekeyId
    ? (recipientIdentity.oneTimePrekeys || []).find((item) => item.keyId === payload.recipientOneTimePrekeyId)
    : null;
  if (otp) secrets.push(x25519.getSharedSecret(otp.privateKeyBytes, payload.ratchetPublicKey));
  return hkdfBytes(concat(secrets), ROOT_INFO);
}

async function kdfRoot(rootKey, dhOut) {
  const material = await hkdfBytes(concat([rootKey, dhOut]), ROOT_INFO, 64);
  return { rootKey: material.slice(0, 32), chainKey: material.slice(32) };
}

async function kdfChain(chainKey) {
  const material = await hkdfBytes(chainKey, CHAIN_INFO, 64);
  return { nextChainKey: material.slice(0, 32), messageKey: await hkdfBytes(material.slice(32), MSG_INFO) };
}

function sessionKey(username, deviceId) {
  return `${username}|${deviceId || ""}`;
}

function sentKey(ratchetPublicKey, messageNumber) {
  return `${b64(ratchetPublicKey)}|${messageNumber}`;
}

function skippedKey(ratchetPublicKey, messageNumber) {
  return `${b64(ratchetPublicKey)}|${messageNumber}`;
}

function cloneSession(session) {
  return {
    ...session,
    rootKey: new Uint8Array(session.rootKey),
    sendingChainKey: session.sendingChainKey ? new Uint8Array(session.sendingChainKey) : null,
    receivingChainKey: session.receivingChainKey ? new Uint8Array(session.receivingChainKey) : null,
    localRatchetPrivateKey: new Uint8Array(session.localRatchetPrivateKey),
    localRatchetPublicKey: new Uint8Array(session.localRatchetPublicKey),
    remoteRatchetPublicKey: new Uint8Array(session.remoteRatchetPublicKey),
  };
}

export async function encryptRatchetMessage(plaintext, senderIdentity, recipientBundle, conversationId = "") {
  const bundle = normalizeBundle(recipientBundle);
  if (!(await verifySignedPrekey(bundle))) throw new Error("invalid signed prekey signature");
  const id = sessionKey(bundle.username, bundle.deviceId || conversationId);
  let session = senderIdentity.ratchetSessions?.[id];
  if (!senderIdentity.ratchetSessions) senderIdentity.ratchetSessions = {};
  if (!senderIdentity.sentMessageKeys) senderIdentity.sentMessageKeys = {};
  if (!session) {
    const ratchet = keypair(x25519);
    const rootKey = await deriveInitialRootAsSender(senderIdentity, bundle, ratchet.privateKey);
    const sending = await kdfRoot(rootKey, x25519.getSharedSecret(ratchet.privateKey, bundle.signedPrekey.publicKey));
    session = {
      peerUsername: bundle.username,
      peerDeviceId: bundle.deviceId,
      rootKey: sending.rootKey,
      sendingChainKey: sending.chainKey,
      receivingChainKey: null,
      localRatchetPrivateKey: ratchet.privateKey,
      localRatchetPublicKey: ratchet.publicKey,
      remoteRatchetPublicKey: bundle.signedPrekey.publicKey,
      previousChainLength: 0,
      sendingMessageNumber: 0,
      receivingMessageNumber: 0,
    };
  } else if (!session.sendingChainKey) {
    const ratchet = keypair(x25519);
    const send = await kdfRoot(session.rootKey, x25519.getSharedSecret(ratchet.privateKey, session.remoteRatchetPublicKey));
    session = {
      ...session,
      rootKey: send.rootKey,
      sendingChainKey: send.chainKey,
      localRatchetPrivateKey: ratchet.privateKey,
      localRatchetPublicKey: ratchet.publicKey,
      previousChainLength: session.receivingMessageNumber,
      sendingMessageNumber: 0,
    };
  }
  const chain = await kdfChain(session.sendingChainKey);
  const encrypted = await aesGcmEncrypt(chain.messageKey, encoder.encode(plaintext));
  const messageNumber = session.sendingMessageNumber;
  senderIdentity.sentMessageKeys[sentKey(session.localRatchetPublicKey, messageNumber)] = b64(chain.messageKey);
  session.sendingChainKey = chain.nextChainKey;
  session.sendingMessageNumber += 1;
  senderIdentity.ratchetSessions[id] = session;
  return {
    ciphertext: encrypted.ciphertext,
    nonce: encrypted.nonce,
    senderKeyId: senderIdentity.keyId,
    recipientSignedPrekeyId: bundle.signedPrekey.keyId,
    recipientSignedPrekeyPublic: bundle.signedPrekey.publicKey,
    recipientOneTimePrekeyId: bundle.oneTimePrekey?.keyId || "",
    recipientOneTimePrekeyPublic: bundle.oneTimePrekey?.publicKey || new Uint8Array(),
    e2eeAlgorithm: DR_ALGORITHM,
    ratchetPublicKey: session.localRatchetPublicKey,
    previousChainLength: session.previousChainLength,
    messageNumber,
  };
}

async function nextReceivingMessageKey(identity, payload, senderIdentityKey, conversationId) {
  const senderUsername = senderIdentityKey.username || payload.from || "peer";
  const senderDeviceId = senderIdentityKey.deviceId || payload.senderDeviceId || conversationId || "";
  const id = sessionKey(senderUsername, senderDeviceId);
  if (!identity.ratchetSessions) identity.ratchetSessions = {};
  let session = identity.ratchetSessions[id] ? cloneSession(identity.ratchetSessions[id]) : null;
  const skipped = {};
  if (!session) {
    const rootKey = await deriveInitialRootAsRecipient(identity, payload);
    const receiving = await kdfRoot(rootKey, x25519.getSharedSecret(identity.signedPrekey.privateKeyBytes, payload.ratchetPublicKey));
    session = {
      peerUsername: senderUsername,
      peerDeviceId: senderDeviceId,
      rootKey: receiving.rootKey,
      sendingChainKey: null,
      receivingChainKey: receiving.chainKey,
      localRatchetPrivateKey: identity.signedPrekey.privateKeyBytes,
      localRatchetPublicKey: identity.signedPrekey.publicKeyBytes,
      remoteRatchetPublicKey: payload.ratchetPublicKey,
      previousChainLength: 0,
      sendingMessageNumber: 0,
      receivingMessageNumber: 0,
    };
  } else if (b64(session.remoteRatchetPublicKey) !== b64(payload.ratchetPublicKey)) {
    while (session.receivingChainKey && session.receivingMessageNumber < payload.previousChainLength) {
      const messageNumber = session.receivingMessageNumber;
      const chain = await kdfChain(session.receivingChainKey);
      session.receivingChainKey = chain.nextChainKey;
      skipped[skippedKey(session.remoteRatchetPublicKey, messageNumber)] = b64(chain.messageKey);
      session.receivingMessageNumber += 1;
    }
    const recv = await kdfRoot(session.rootKey, x25519.getSharedSecret(session.localRatchetPrivateKey, payload.ratchetPublicKey));
    const ratchet = keypair(x25519);
    const send = await kdfRoot(recv.rootKey, x25519.getSharedSecret(ratchet.privateKey, payload.ratchetPublicKey));
    session = {
      ...session,
      rootKey: send.rootKey,
      receivingChainKey: recv.chainKey,
      sendingChainKey: send.chainKey,
      localRatchetPrivateKey: ratchet.privateKey,
      localRatchetPublicKey: ratchet.publicKey,
      remoteRatchetPublicKey: payload.ratchetPublicKey,
      previousChainLength: session.sendingMessageNumber,
      sendingMessageNumber: 0,
      receivingMessageNumber: 0,
    };
  }
  if (payload.messageNumber < session.receivingMessageNumber) {
    throw new Error("missing skipped message key");
  }
  let messageKey;
  while (session.receivingMessageNumber <= payload.messageNumber) {
    const messageNumber = session.receivingMessageNumber;
    const chain = await kdfChain(session.receivingChainKey);
    session.receivingChainKey = chain.nextChainKey;
    if (messageNumber === payload.messageNumber) {
      messageKey = chain.messageKey;
    } else {
      skipped[skippedKey(payload.ratchetPublicKey, messageNumber)] = b64(chain.messageKey);
    }
    session.receivingMessageNumber += 1;
  }
  return { messageKey, sessionId: id, session, skipped };
}

async function deriveInitialMessageKey(recipientIdentity, payload) {
  const rootKey = await deriveInitialRootAsRecipient(recipientIdentity, payload);
  const receiving = await kdfRoot(rootKey, x25519.getSharedSecret(recipientIdentity.signedPrekey.privateKeyBytes, payload.ratchetPublicKey));
  let chainKey = receiving.chainKey;
  let messageKey = null;
  for (let i = 0; i <= payload.messageNumber; i += 1) {
    const chain = await kdfChain(chainKey);
    chainKey = chain.nextChainKey;
    messageKey = chain.messageKey;
  }
  return messageKey;
}

async function nextInitialReceivingMessageKey(identity, payload, senderIdentityKey, conversationId) {
  const senderUsername = senderIdentityKey.username || payload.from || "peer";
  const senderDeviceId = senderIdentityKey.deviceId || payload.senderDeviceId || conversationId || "";
  const id = sessionKey(senderUsername, senderDeviceId);
  const rootKey = await deriveInitialRootAsRecipient(identity, payload);
  const receiving = await kdfRoot(rootKey, x25519.getSharedSecret(identity.signedPrekey.privateKeyBytes, payload.ratchetPublicKey));
  const session = {
    peerUsername: senderUsername,
    peerDeviceId: senderDeviceId,
    rootKey: receiving.rootKey,
    sendingChainKey: null,
    receivingChainKey: receiving.chainKey,
    localRatchetPrivateKey: identity.signedPrekey.privateKeyBytes,
    localRatchetPublicKey: identity.signedPrekey.publicKeyBytes,
    remoteRatchetPublicKey: payload.ratchetPublicKey,
    previousChainLength: 0,
    sendingMessageNumber: 0,
    receivingMessageNumber: 0,
  };
  const skipped = {};
  let messageKey;
  while (session.receivingMessageNumber <= payload.messageNumber) {
    const messageNumber = session.receivingMessageNumber;
    const chain = await kdfChain(session.receivingChainKey);
    session.receivingChainKey = chain.nextChainKey;
    if (messageNumber === payload.messageNumber) {
      messageKey = chain.messageKey;
    } else {
      skipped[skippedKey(payload.ratchetPublicKey, messageNumber)] = b64(chain.messageKey);
    }
    session.receivingMessageNumber += 1;
  }
  return { messageKey, sessionId: id, session, skipped };
}

export async function decryptRatchetMessage(payload, recipientIdentity, senderIdentityKey = {}, conversationId = "") {
  if (payload.e2eeAlgorithm && payload.e2eeAlgorithm !== DR_ALGORITHM) throw new Error("unsupported direct E2EE algorithm");
  const ownSent = recipientIdentity.sentMessageKeys?.[sentKey(payload.ratchetPublicKey, payload.messageNumber)];
  if (ownSent) {
    return decoder.decode(await aesGcmDecrypt(fromB64(ownSent), payload.ciphertext, payload.nonce));
  }
  const cachedSkippedKey = skippedKey(payload.ratchetPublicKey, payload.messageNumber);
  const cachedSkipped = recipientIdentity.skippedMessageKeys?.[cachedSkippedKey];
  if (cachedSkipped) {
    const plaintext = decoder.decode(await aesGcmDecrypt(fromB64(cachedSkipped), payload.ciphertext, payload.nonce));
    delete recipientIdentity.skippedMessageKeys[cachedSkippedKey];
    return plaintext;
  }
  let next;
  try {
    next = await nextReceivingMessageKey(recipientIdentity, payload, senderIdentityKey, conversationId);
  } catch (error) {
    if (!(error instanceof Error) || error.message !== "missing skipped message key") {
      throw error;
    }
    const messageKey = await deriveInitialMessageKey(recipientIdentity, payload);
    return decoder.decode(await aesGcmDecrypt(messageKey, payload.ciphertext, payload.nonce));
  }
  let plaintext;
  try {
    plaintext = decoder.decode(await aesGcmDecrypt(next.messageKey, payload.ciphertext, payload.nonce));
  } catch (error) {
    if (payload.recipientSignedPrekeyId !== recipientIdentity.signedPrekey?.keyId) {
      throw error;
    }
    next = await nextInitialReceivingMessageKey(recipientIdentity, payload, senderIdentityKey, conversationId);
    plaintext = decoder.decode(await aesGcmDecrypt(next.messageKey, payload.ciphertext, payload.nonce));
  }
  if (!recipientIdentity.ratchetSessions) recipientIdentity.ratchetSessions = {};
  if (!recipientIdentity.skippedMessageKeys) recipientIdentity.skippedMessageKeys = {};
  recipientIdentity.ratchetSessions[next.sessionId] = next.session;
  Object.assign(recipientIdentity.skippedMessageKeys, next.skipped);
  return plaintext;
}

export async function exportRatchetIdentity(identity) {
  return {
    username: identity.username,
    deviceId: identity.deviceId,
    keyId: identity.keyId,
    algorithm: "Ed25519",
    publicKey: b64(identity.publicKeyBytes),
    privateKey: b64(identity.privateKeyBytes),
    signedPrekey: {
      username: identity.username,
      deviceId: identity.deviceId,
      keyId: identity.signedPrekey.keyId,
      algorithm: "X25519",
      publicKey: b64(identity.signedPrekey.publicKeyBytes),
      privateKey: b64(identity.signedPrekey.privateKeyBytes),
      signature: b64(identity.signedPrekey.signature),
      signatureAlgorithm: "Ed25519",
      published: identity.signedPrekey.published === true,
    },
    oneTimePrekeys: (identity.oneTimePrekeys || []).map((item) => ({
      keyId: item.keyId,
      publicKey: b64(item.publicKeyBytes),
      privateKey: b64(item.privateKeyBytes),
      published: item.published === true,
    })),
    ratchetSessions: Object.fromEntries(Object.entries(identity.ratchetSessions || {}).map(([key, session]) => [key, {
      ...session,
      rootKey: b64(session.rootKey),
      sendingChainKey: session.sendingChainKey ? b64(session.sendingChainKey) : null,
      receivingChainKey: session.receivingChainKey ? b64(session.receivingChainKey) : null,
      localRatchetPrivateKey: b64(session.localRatchetPrivateKey),
      localRatchetPublicKey: b64(session.localRatchetPublicKey),
      remoteRatchetPublicKey: b64(session.remoteRatchetPublicKey),
    }])),
    sentMessageKeys: { ...(identity.sentMessageKeys || {}) },
    skippedMessageKeys: { ...(identity.skippedMessageKeys || {}) },
  };
}

export async function importRatchetIdentity(state) {
  if (state?.privateKeyJwk) {
    return createRatchetIdentity(state.username, state.deviceId || "");
  }
  return attachMethods({
    username: state.username,
    deviceId: state.deviceId || "",
    keyId: state.keyId,
    algorithm: "Ed25519",
    publicKeyBytes: fromB64(state.publicKey),
    privateKeyBytes: fromB64(state.privateKey),
    signedPrekey: {
      username: state.username,
      deviceId: state.deviceId || "",
      keyId: state.signedPrekey.keyId,
      algorithm: "X25519",
      publicKeyBytes: fromB64(state.signedPrekey.publicKey),
      privateKeyBytes: fromB64(state.signedPrekey.privateKey),
      signature: fromB64(state.signedPrekey.signature),
      signatureAlgorithm: "Ed25519",
      published: state.signedPrekey.published === true,
    },
    oneTimePrekeys: (state.oneTimePrekeys || []).map((item) => ({
      keyId: item.keyId,
      publicKeyBytes: fromB64(item.publicKey),
      privateKeyBytes: fromB64(item.privateKey),
      published: item.published === true,
    })),
    ratchetSessions: Object.fromEntries(Object.entries(state.ratchetSessions || {}).map(([key, session]) => [key, {
      ...session,
      rootKey: fromB64(session.rootKey),
      sendingChainKey: session.sendingChainKey ? fromB64(session.sendingChainKey) : null,
      receivingChainKey: session.receivingChainKey ? fromB64(session.receivingChainKey) : null,
      localRatchetPrivateKey: fromB64(session.localRatchetPrivateKey),
      localRatchetPublicKey: fromB64(session.localRatchetPublicKey),
      remoteRatchetPublicKey: fromB64(session.remoteRatchetPublicKey),
    }])),
    sentMessageKeys: { ...(state.sentMessageKeys || {}) },
    skippedMessageKeys: { ...(state.skippedMessageKeys || {}) },
  });
}
