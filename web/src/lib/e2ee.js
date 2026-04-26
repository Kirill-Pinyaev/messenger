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

export async function createIdentity(username) {
  assertCrypto();
  const material = await generateKeyMaterial();
  const signedPrekey = await makePrekey(username, "signed");
  const oneTimePrekeys = await createOneTimePrekeys(username, 5);
  return {
    username,
    keyId: randomKeyId(username, "identity"),
    algorithm: "P256-HKDF-AESGCM",
    ...material,
    signedPrekey,
    oneTimePrekeys,
  };
}

export async function createOneTimePrekeys(username, count) {
  const items = [];
  for (let i = 0; i < count; i++) {
    items.push(await makePrekey(username, "otp"));
  }
  return items;
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
  return {
    username: identity.username,
    keyId: identity.keyId,
    algorithm: identity.algorithm,
    publicKey: bytesToBase64(identity.publicKeyBytes),
    privateKeyJwk: identity.privateKeyJwk,
    signedPrekey: identity.signedPrekey ? {
      username: identity.signedPrekey.username,
      keyId: identity.signedPrekey.keyId,
      algorithm: identity.signedPrekey.algorithm,
      publicKey: bytesToBase64(identity.signedPrekey.publicKeyBytes),
      privateKeyJwk: identity.signedPrekey.privateKeyJwk,
      published: identity.signedPrekey.published === true,
    } : null,
    oneTimePrekeys: (identity.oneTimePrekeys || []).map((item) => ({
      username: item.username,
      keyId: item.keyId,
      algorithm: item.algorithm,
      publicKey: bytesToBase64(item.publicKeyBytes),
      privateKeyJwk: item.privateKeyJwk,
      published: item.published === true,
    })),
  };
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
  assertCrypto();
  const aesKey = await deriveCombinedAesKey(
    [senderIdentity.privateKey, senderIdentity.privateKey],
    [
      recipientBundle.signedPrekey?.publicKey || recipientBundle.signedPrekey?.publicKeyBytes,
      recipientBundle.oneTimePrekey?.publicKey || recipientBundle.oneTimePrekey?.publicKeyBytes,
    ],
    DIRECT_INFO,
  );
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = new Uint8Array(await subtle.encrypt(
    { name: "AES-GCM", iv: nonce },
    aesKey,
    encoder.encode(plaintext),
  ));
  return {
    ciphertext,
    nonce,
    senderKeyId: senderIdentity.keyId,
    recipientSignedPrekeyId: recipientBundle.signedPrekey.keyId,
    recipientSignedPrekeyPublic: recipientBundle.signedPrekey.publicKey || recipientBundle.signedPrekey.publicKeyBytes,
    recipientOneTimePrekeyId: recipientBundle.oneTimePrekey?.keyId || "",
    recipientOneTimePrekeyPublic: recipientBundle.oneTimePrekey?.publicKey || recipientBundle.oneTimePrekey?.publicKeyBytes || new Uint8Array(),
  };
}

function findLocalOneTimePrekey(identity, keyId) {
  return (identity.oneTimePrekeys || []).find((item) => item.keyId === keyId) || null;
}

export async function decryptDirectMessageForRecipient(payload, recipientIdentity, senderIdentityPublicKeyBytes) {
  assertCrypto();
  const oneTimePrekey = payload.recipientOneTimePrekeyId
    ? findLocalOneTimePrekey(recipientIdentity, payload.recipientOneTimePrekeyId)
    : null;
  const aesKey = await deriveCombinedAesKey(
    [recipientIdentity.signedPrekey?.privateKey, oneTimePrekey?.privateKey || null],
    [senderIdentityPublicKeyBytes, senderIdentityPublicKeyBytes],
    DIRECT_INFO,
  );
  const plaintext = await subtle.decrypt(
    { name: "AES-GCM", iv: payload.nonce },
    aesKey,
    payload.ciphertext,
  );
  return decoder.decode(plaintext);
}

export async function decryptDirectMessageForSender(payload, senderIdentity) {
  assertCrypto();
  const aesKey = await deriveCombinedAesKey(
    [senderIdentity.privateKey, senderIdentity.privateKey],
    [payload.recipientSignedPrekeyPublic, payload.recipientOneTimePrekeyPublic],
    DIRECT_INFO,
  );
  const plaintext = await subtle.decrypt(
    { name: "AES-GCM", iv: payload.nonce },
    aesKey,
    payload.ciphertext,
  );
  return decoder.decode(plaintext);
}

export async function createGroupKeyPackage(conversationId, version, senderIdentity, recipients) {
  assertCrypto();
  const groupKeyBytes = crypto.getRandomValues(new Uint8Array(32));
  const envelopes = [];

  for (const recipient of recipients) {
    const aesKey = await deriveCombinedAesKey(
      [senderIdentity.privateKey],
      [recipient.publicKeyBytes],
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
      senderKeyId: senderIdentity.keyId,
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
  const aesKey = await deriveCombinedAesKey(
    [recipientIdentity.privateKey],
    [senderPublicKeyBytes],
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
