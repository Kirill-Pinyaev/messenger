import { x25519 } from "@noble/curves/ed25519.js";
import {
  createRatchetIdentity,
  decryptRatchetMessage,
  encryptRatchetMessage,
  exportRatchetIdentity,
  importRatchetIdentity,
} from "./double-ratchet.js";

const subtle = globalThis.crypto?.subtle;
const encoder = new TextEncoder();
const decoder = new TextDecoder();

const DIRECT_INFO = encoder.encode("messenger-direct-prekey-v1");
const GROUP_ENVELOPE_INFO = encoder.encode("messenger-group-envelope-v1");

function assertCrypto() {
  if (!subtle) {
    throw new Error("WebCrypto is not available");
  }
}

function randomKeyId(username, prefix = "key") {
  return `${username}-${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function bytesToBase64(bytes) {
  if (typeof btoa === "function") {
    let binary = "";
    for (const byte of bytes) {
      binary += String.fromCharCode(byte);
    }
    return btoa(binary);
  }
  if (typeof Buffer !== "undefined") {
    return Buffer.from(bytes).toString("base64");
  }
  throw new Error("Base64 encoding is not available");
}

function base64ToBytes(value) {
  if (typeof atob === "function") {
    const binary = atob(value);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  }
  if (typeof Buffer !== "undefined") {
    return new Uint8Array(Buffer.from(value, "base64"));
  }
  throw new Error("Base64 decoding is not available");
}

function concatUint8Arrays(chunks) {
  const total = chunks.reduce((sum, chunk) => sum + chunk.length, 0);
  const out = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    out.set(chunk, offset);
    offset += chunk.length;
  }
  return out;
}

async function generateKeyMaterial() {
  const pair = await subtle.generateKey(
    { name: "ECDH", namedCurve: "P-256" },
    true,
    ["deriveBits"],
  );
  const publicKeyBytes = new Uint8Array(await subtle.exportKey("raw", pair.publicKey));
  const privateKeyJwk = await subtle.exportKey("jwk", pair.privateKey);
  return {
    publicKey: pair.publicKey,
    privateKey: pair.privateKey,
    publicKeyBytes,
    privateKeyJwk,
  };
}

async function deriveX25519AesKey(privateKeyBytes, peerPublicKeyBytes, info) {
  const shared = x25519.getSharedSecret(privateKeyBytes, peerPublicKeyBytes);
  const hkdfKey = await subtle.importKey("raw", shared, "HKDF", false, ["deriveKey"]);
  return subtle.deriveKey(
    {
      name: "HKDF",
      hash: "SHA-256",
      salt: new Uint8Array(32),
      info,
    },
    hkdfKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

async function importPrivateKey(privateKeyJwk) {
  return subtle.importKey(
    "jwk",
    privateKeyJwk,
    { name: "ECDH", namedCurve: "P-256" },
    true,
    ["deriveBits"],
  );
}

async function importPublicKey(publicKeyBytes) {
  return subtle.importKey(
    "raw",
    publicKeyBytes,
    { name: "ECDH", namedCurve: "P-256" },
    false,
    [],
  );
}

async function deriveCombinedAesKey(privateKeys, peerPublicKeyBytesList, info) {
  const sharedChunks = [];
  for (let i = 0; i < privateKeys.length; i++) {
    const privateKey = privateKeys[i];
    const publicKeyBytes = peerPublicKeyBytesList[i];
    if (!privateKey || !publicKeyBytes || publicKeyBytes.length === 0) {
      continue;
    }
    const peerPublicKey = await importPublicKey(publicKeyBytes);
    const shared = await subtle.deriveBits(
      { name: "ECDH", public: peerPublicKey },
      privateKey,
      256,
    );
    sharedChunks.push(new Uint8Array(shared));
  }

  if (sharedChunks.length === 0) {
    throw new Error("No direct E2EE material available");
  }

  const hkdfKey = await subtle.importKey("raw", concatUint8Arrays(sharedChunks), "HKDF", false, ["deriveKey"]);
  return subtle.deriveKey(
    {
      name: "HKDF",
      hash: "SHA-256",
      salt: new Uint8Array(32),
      info,
    },
    hkdfKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

async function makePrekey(username, prefix) {
  const material = await generateKeyMaterial();
  return {
    username,
    keyId: randomKeyId(username, prefix),
    algorithm: "P256-HKDF-AESGCM",
    ...material,
    published: false,
  };
}

export async function createIdentity(username, deviceId = "") {
  assertCrypto();
  return createRatchetIdentity(username, deviceId);
}

export async function createOneTimePrekeys(username, count) {
  return [];
}

export async function topUpOneTimePrekeys(identity, minimumUnpublished = 5) {
  const unpublished = (identity.oneTimePrekeys || []).filter((item) => !item.published);
  if (unpublished.length >= minimumUnpublished) {
    return identity;
  }
  const additions = await createOneTimePrekeys(identity.username, minimumUnpublished - unpublished.length);
  return {
    ...identity,
    oneTimePrekeys: [...(identity.oneTimePrekeys || []), ...additions],
  };
}

export async function exportIdentityState(identity) {
  return exportRatchetIdentity(identity);
}

async function importStoredPrekey(state, prefix, username) {
  if (!state?.publicKey || !state?.privateKeyJwk) {
    const generated = await makePrekey(username, prefix);
    return generated;
  }
  const publicKeyBytes = base64ToBytes(state.publicKey);
  return {
    username: state.username || username,
    keyId: state.keyId,
    algorithm: state.algorithm,
    publicKey: await importPublicKey(publicKeyBytes),
    privateKey: await importPrivateKey(state.privateKeyJwk),
    publicKeyBytes,
    privateKeyJwk: state.privateKeyJwk,
    published: state.published === true,
  };
}

export async function importIdentityState(state) {
  assertCrypto();
  if (state?.algorithm === "Ed25519" || state?.privateKey) {
    return importRatchetIdentity(state);
  }
  const publicKeyBytes = base64ToBytes(state.publicKey);
  const identity = {
    username: state.username,
    keyId: state.keyId,
    algorithm: state.algorithm,
    publicKey: await importPublicKey(publicKeyBytes),
    privateKey: await importPrivateKey(state.privateKeyJwk),
    publicKeyBytes,
    privateKeyJwk: state.privateKeyJwk,
    signedPrekey: await importStoredPrekey(state.signedPrekey, "signed", state.username),
    oneTimePrekeys: [],
  };

  if (Array.isArray(state.oneTimePrekeys)) {
    for (const item of state.oneTimePrekeys) {
      identity.oneTimePrekeys.push(await importStoredPrekey(item, "otp", state.username));
    }
  }
  return identity;
}

export function buildPublishPrekeyBundle(identity) {
  return {
    signedPrekeyId: identity.signedPrekey.keyId,
    signedPrekeyAlgorithm: identity.signedPrekey.algorithm,
    signedPrekeyPublicKey: identity.signedPrekey.publicKeyBytes,
    signedPrekeySignature: identity.signedPrekey.signature,
    signedPrekeySignatureAlgorithm: identity.signedPrekey.signatureAlgorithm || "Ed25519",
    oneTimePrekeys: (identity.oneTimePrekeys || [])
      .filter((item) => !item.published)
      .map((item) => ({
        keyId: item.keyId,
        algorithm: item.algorithm,
        publicKey: item.publicKeyBytes,
      })),
  };
}

export function markPrekeysAsPublished(identity) {
  return {
    ...identity,
    signedPrekey: identity.signedPrekey ? { ...identity.signedPrekey, published: true } : null,
    oneTimePrekeys: (identity.oneTimePrekeys || []).map((item) => ({ ...item, published: true })),
  };
}

export async function encryptDirectMessage(plaintext, senderIdentity, recipientBundle) {
  return encryptRatchetMessage(plaintext, senderIdentity, recipientBundle);
}

function findLocalOneTimePrekey(identity, keyId) {
  return (identity.oneTimePrekeys || []).find((item) => item.keyId === keyId) || null;
}

export async function decryptDirectMessageForRecipient(payload, recipientIdentity, senderIdentityPublicKeyBytes) {
  return decryptRatchetMessage(payload, recipientIdentity, { publicKey: senderIdentityPublicKeyBytes, username: payload.from, deviceId: payload.senderDeviceId });
}

export async function decryptDirectMessageForSender(payload, senderIdentity) {
  return decryptRatchetMessage(payload, senderIdentity, { username: payload.to || payload.targetUsername || "peer", deviceId: payload.targetDeviceId || "" });
}

export async function createGroupKeyPackage(conversationId, version, senderIdentity, recipients) {
  assertCrypto();
  const groupKeyBytes = crypto.getRandomValues(new Uint8Array(32));
  const envelopes = [];

  for (const recipient of recipients) {
    const aesKey = await deriveX25519AesKey(
      senderIdentity.signedPrekey.privateKeyBytes,
      recipient.signedPrekeyPublicBytes || recipient.publicKeyBytes,
      GROUP_ENVELOPE_INFO,
    );
    const nonce = crypto.getRandomValues(new Uint8Array(12));
    const encryptedKey = new Uint8Array(await subtle.encrypt(
      { name: "AES-GCM", iv: nonce },
      aesKey,
      groupKeyBytes,
    ));
    envelopes.push({
      username: recipient.username,
      deviceId: recipient.deviceId,
      encryptedKey,
      nonce,
      senderKeyId: senderIdentity.signedPrekey.keyId,
      recipientKeyId: recipient.keyId,
    });
  }

  return {
    conversationId,
    version,
    algorithm: "AES-GCM",
    groupKeyBytes,
    envelopes,
  };
}

export async function decryptGroupKeyEnvelope(envelope, recipientIdentity, senderPublicKeyBytes) {
  assertCrypto();
  const aesKey = await deriveX25519AesKey(
    recipientIdentity.signedPrekey.privateKeyBytes,
    senderPublicKeyBytes,
    GROUP_ENVELOPE_INFO,
  );
  const rawKey = await subtle.decrypt(
    { name: "AES-GCM", iv: envelope.nonce },
    aesKey,
    envelope.encryptedKey,
  );
  return new Uint8Array(rawKey);
}

export async function encryptGroupMessage(plaintext, groupKeyBytes, version) {
  assertCrypto();
  const groupKey = await subtle.importKey("raw", groupKeyBytes, "AES-GCM", false, ["encrypt"]);
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = new Uint8Array(await subtle.encrypt(
    { name: "AES-GCM", iv: nonce },
    groupKey,
    encoder.encode(plaintext),
  ));
  return { ciphertext, nonce, version };
}

export async function decryptGroupMessage(payload, groupKeyBytes) {
  assertCrypto();
  const groupKey = await subtle.importKey("raw", groupKeyBytes, "AES-GCM", false, ["decrypt"]);
  const plaintext = await subtle.decrypt(
    { name: "AES-GCM", iv: payload.nonce },
    groupKey,
    payload.ciphertext,
  );
  return decoder.decode(plaintext);
}

export function serializeGroupKey(groupKeyBytes) {
  return bytesToBase64(groupKeyBytes);
}

export function deserializeGroupKey(value) {
  return base64ToBytes(value);
}
