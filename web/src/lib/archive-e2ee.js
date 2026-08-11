const subtle = globalThis.crypto?.subtle;
const encoder = new TextEncoder();
const decoder = new TextDecoder();

function assertCrypto() {
  if (!subtle) {
    throw new Error("WebCrypto is not available");
  }
}

function randomKeyId(username, prefix = "archive") {
  return `${username}-${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function bytesToBase64(bytes) {
  if (typeof btoa === "function") {
    let binary = "";
    for (const byte of bytes) binary += String.fromCharCode(byte);
    return btoa(binary);
  }
  return Buffer.from(bytes).toString("base64");
}

function base64ToBytes(value) {
  if (typeof atob === "function") {
    const binary = atob(value);
    const out = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
    return out;
  }
  return new Uint8Array(Buffer.from(value, "base64"));
}

async function generateKeyMaterial() {
  const pair = await subtle.generateKey({ name: "ECDH", namedCurve: "P-256" }, true, ["deriveBits"]);
  return {
    publicKey: pair.publicKey,
    privateKey: pair.privateKey,
    publicKeyBytes: new Uint8Array(await subtle.exportKey("raw", pair.publicKey)),
    privateKeyJwk: await subtle.exportKey("jwk", pair.privateKey),
  };
}

async function importPrivateKey(privateKeyJwk) {
  return subtle.importKey("jwk", privateKeyJwk, { name: "ECDH", namedCurve: "P-256" }, true, ["deriveBits"]);
}

async function importPrivateKeyPkcs8(privateKeyBytes) {
  return subtle.importKey("pkcs8", privateKeyBytes, { name: "ECDH", namedCurve: "P-256" }, true, ["deriveBits"]);
}

async function importPublicKey(publicKeyBytes) {
  return subtle.importKey("raw", publicKeyBytes, { name: "ECDH", namedCurve: "P-256" }, false, []);
}

async function derivePasswordKey(password, salt, iterations) {
  const base = await subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveKey"]);
  return subtle.deriveKey(
    {
      name: "PBKDF2",
      hash: "SHA-256",
      salt,
      iterations,
    },
    base,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

async function deriveArchiveAesKey(privateKey, peerPublicKeyBytes) {
  const peerPublicKey = await importPublicKey(peerPublicKeyBytes);
  const shared = await subtle.deriveBits({ name: "ECDH", public: peerPublicKey }, privateKey, 256);
  const hkdfKey = await subtle.importKey("raw", shared, "HKDF", false, ["deriveKey"]);
  return subtle.deriveKey(
    {
      name: "HKDF",
      hash: "SHA-256",
      salt: new Uint8Array(32),
      info: encoder.encode("messenger-history-archive-v1"),
    },
    hkdfKey,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
}

export async function createArchiveIdentity(username) {
  assertCrypto();
  const material = await generateKeyMaterial();
  return {
    username,
    keyId: randomKeyId(username),
    algorithm: "P256-ARCHIVE-AESGCM",
    ...material,
    version: 1,
  };
}

export async function exportArchiveIdentityState(identity) {
  return {
    username: identity.username,
    keyId: identity.keyId,
    algorithm: identity.algorithm,
    version: identity.version || 1,
    publicKey: bytesToBase64(identity.publicKeyBytes),
    privateKeyJwk: identity.privateKeyJwk,
  };
}

export async function importArchiveIdentityState(state) {
  assertCrypto();
  const publicKeyBytes = base64ToBytes(state.publicKey);
  return {
    username: state.username,
    keyId: state.keyId,
    algorithm: state.algorithm,
    version: state.version || 1,
    publicKey: await importPublicKey(publicKeyBytes),
    privateKey: await importPrivateKey(state.privateKeyJwk),
    publicKeyBytes,
    privateKeyJwk: state.privateKeyJwk,
  };
}

export async function wrapArchiveIdentityForServer(identity, password) {
  assertCrypto();
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iterations = 120000;
  const kek = await derivePasswordKey(password, salt, iterations);
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const privateKeyPkcs8 = new Uint8Array(await subtle.exportKey("pkcs8", identity.privateKey));
  const ciphertext = new Uint8Array(await subtle.encrypt(
    { name: "AES-GCM", iv: nonce },
    kek,
    encoder.encode(bytesToBase64(privateKeyPkcs8)),
  ));
  const combined = new Uint8Array(nonce.length + ciphertext.length);
  combined.set(nonce, 0);
  combined.set(ciphertext, nonce.length);
  return {
    publicKey: identity.publicKeyBytes,
    encryptedPrivateKey: combined,
    kdfSalt: salt,
    kdfParams: JSON.stringify({ name: "PBKDF2", hash: "SHA-256", iterations }),
    version: identity.version || 1,
  };
}

export async function restoreArchiveIdentityFromServer(header, password, username) {
  assertCrypto();
  const params = JSON.parse(header.kdfParams || "{}");
  const encrypted = header.encryptedPrivateKey;
  const nonce = encrypted.slice(0, 12);
  const ciphertext = encrypted.slice(12);
  const kek = await derivePasswordKey(password, header.kdfSalt, Number(params.iterations || 120000));
  const plaintext = await subtle.decrypt({ name: "AES-GCM", iv: nonce }, kek, ciphertext);
  const restored = decoder.decode(plaintext);
  if (restored.trim().startsWith("{")) {
    return importArchiveIdentityState({
      username,
      keyId: `${username}-archive-restored`,
      algorithm: "P256-ARCHIVE-AESGCM",
      version: header.version || 1,
      publicKey: bytesToBase64(header.publicKey),
      privateKeyJwk: JSON.parse(restored),
    });
  }
  const privateKeyPkcs8 = base64ToBytes(restored);
  return importArchiveIdentityState({
    username,
    keyId: `${username}-archive-restored`,
    algorithm: "P256-ARCHIVE-AESGCM",
    version: header.version || 1,
    publicKey: bytesToBase64(header.publicKey),
    privateKeyJwk: await subtle.exportKey("jwk", await importPrivateKeyPkcs8(privateKeyPkcs8)),
  });
}

export async function encryptArchivePayload(plaintextBytes, recipientPublicKeyBytes) {
  assertCrypto();
  const ephemeral = await generateKeyMaterial();
  const aesKey = await deriveArchiveAesKey(ephemeral.privateKey, recipientPublicKeyBytes);
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = new Uint8Array(await subtle.encrypt({ name: "AES-GCM", iv: nonce }, aesKey, plaintextBytes));
  return {
    ciphertext,
    nonce,
    ephemeralPublicKey: ephemeral.publicKeyBytes,
  };
}

export async function decryptArchivePayload(payload, identity) {
  assertCrypto();
  const aesKey = await deriveArchiveAesKey(identity.privateKey, payload.ephemeralPublicKey);
  const plaintext = await subtle.decrypt({ name: "AES-GCM", iv: payload.nonce }, aesKey, payload.ciphertext);
  return new Uint8Array(plaintext);
}
