const subtle = globalThis.crypto?.subtle;
const encoder = new TextEncoder();
const decoder = new TextDecoder();

function assertCrypto() {
  if (!subtle) {
    throw new Error("WebCrypto is not available");
  }
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
    const out = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      out[i] = binary.charCodeAt(i);
    }
    return out;
  }
  if (typeof Buffer !== "undefined") {
    return new Uint8Array(Buffer.from(value, "base64"));
  }
  throw new Error("Base64 decoding is not available");
}

export function attachmentKindForMime(mimeType) {
  if (mimeType.startsWith("image/")) {
    return "image";
  }
  if (mimeType.startsWith("video/")) {
    return "video";
  }
  return "file";
}

export async function sha256Bytes(bytes) {
  assertCrypto();
  return new Uint8Array(await subtle.digest("SHA-256", bytes));
}

export async function encryptMediaBytes(bytes, mimeType, filename) {
  assertCrypto();
  const mediaKey = crypto.getRandomValues(new Uint8Array(32));
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const aesKey = await subtle.importKey("raw", mediaKey, "AES-GCM", false, ["encrypt", "decrypt"]);
  const ciphertext = new Uint8Array(await subtle.encrypt({ name: "AES-GCM", iv: nonce }, aesKey, bytes));
  const sha256 = await sha256Bytes(bytes);
  return {
    kind: attachmentKindForMime(mimeType),
    mimeType,
    filename,
    sizeBytes: bytes.length,
    ciphertext,
    ciphertextSize: ciphertext.length,
    nonce,
    sha256,
    descriptor: {
      mediaKey: bytesToBase64(mediaKey),
      originalFilename: filename,
      mimeType,
      kind: attachmentKindForMime(mimeType),
      sizeBytes: bytes.length,
      ciphertextSize: ciphertext.length,
      sha256: bytesToBase64(sha256),
    },
  };
}

export function serializeMediaDescriptor(descriptor) {
  return encoder.encode(JSON.stringify(descriptor));
}

export function deserializeMediaDescriptor(bytes) {
  return JSON.parse(decoder.decode(bytes));
}

export async function decryptMediaBytes(ciphertext, descriptor, nonce) {
  assertCrypto();
  const aesKey = await subtle.importKey("raw", base64ToBytes(descriptor.mediaKey), "AES-GCM", false, ["decrypt"]);
  const plaintext = new Uint8Array(await subtle.decrypt({ name: "AES-GCM", iv: nonce }, aesKey, ciphertext));
  const actualHash = bytesToBase64(await sha256Bytes(plaintext));
  if (actualHash !== descriptor.sha256) {
    throw new Error("attachment hash mismatch");
  }
  return plaintext;
}
