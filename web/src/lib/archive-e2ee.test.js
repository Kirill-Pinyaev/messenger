import { test } from "vitest";
import assert from "node:assert/strict";

import {
  createArchiveIdentity,
  wrapArchiveIdentityForServer,
  restoreArchiveIdentityFromServer,
  encryptArchivePayload,
  decryptArchivePayload,
} from "./archive-e2ee.js";

test("archive identity can be wrapped for server and restored with password", async () => {
  const identity = await createArchiveIdentity("alice");
  const wrapped = await wrapArchiveIdentityForServer(identity, "secret");
  const restored = await restoreArchiveIdentityFromServer({
    publicKey: wrapped.publicKey,
    encryptedPrivateKey: wrapped.encryptedPrivateKey,
    kdfSalt: wrapped.kdfSalt,
    kdfParams: wrapped.kdfParams,
    version: wrapped.version,
  }, "secret", "alice");

  assert.deepEqual(Array.from(restored.publicKeyBytes), Array.from(identity.publicKeyBytes));
});

test("archive payload can be encrypted for public key and decrypted by owner", async () => {
  const identity = await createArchiveIdentity("alice");
  const payload = new TextEncoder().encode(JSON.stringify({ hello: "world" }));
  const encrypted = await encryptArchivePayload(payload, identity.publicKeyBytes);
  const decrypted = await decryptArchivePayload(encrypted, identity);

  assert.equal(new TextDecoder().decode(decrypted), JSON.stringify({ hello: "world" }));
});

test("archive identity can restore legacy server payload with JWK private key", async () => {
  const identity = await createArchiveIdentity("alice");
  const salt = crypto.getRandomValues(new Uint8Array(16));
  const iterations = 120000;
  const base = await crypto.subtle.importKey("raw", new TextEncoder().encode("secret"), "PBKDF2", false, ["deriveKey"]);
  const kek = await crypto.subtle.deriveKey(
    { name: "PBKDF2", hash: "SHA-256", salt, iterations },
    base,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt", "decrypt"],
  );
  const nonce = crypto.getRandomValues(new Uint8Array(12));
  const encryptedPrivateKey = new Uint8Array(await crypto.subtle.encrypt(
    { name: "AES-GCM", iv: nonce },
    kek,
    new TextEncoder().encode(JSON.stringify(identity.privateKeyJwk)),
  ));
  const combined = new Uint8Array(nonce.length + encryptedPrivateKey.length);
  combined.set(nonce, 0);
  combined.set(encryptedPrivateKey, nonce.length);

  const restored = await restoreArchiveIdentityFromServer({
    publicKey: identity.publicKeyBytes,
    encryptedPrivateKey: combined,
    kdfSalt: salt,
    kdfParams: JSON.stringify({ name: "PBKDF2", hash: "SHA-256", iterations }),
    version: 1,
  }, "secret", "alice");

  assert.deepEqual(Array.from(restored.publicKeyBytes), Array.from(identity.publicKeyBytes));
});
