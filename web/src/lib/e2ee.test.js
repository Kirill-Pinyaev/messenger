import { test } from "vitest";
import assert from "node:assert/strict";

import {
  buildPublishPrekeyBundle,
  createIdentity,
  decryptDirectMessageForRecipient,
  decryptDirectMessageForSender,
  decryptGroupKeyEnvelope,
  decryptGroupMessage,
  encryptDirectMessage,
  encryptGroupMessage,
  exportIdentityState,
  importIdentityState,
  markPrekeysAsPublished,
  createGroupKeyPackage,
  topUpOneTimePrekeys,
} from "./e2ee.js";

test("direct E2EE with prekey bundle decrypts for recipient", async () => {
  const alice = await createIdentity("alice");
  const bob = await createIdentity("bob");
  const bundle = bob.toPrekeyBundle();

  const encrypted = await encryptDirectMessage("hello encrypted", alice, bundle);
  const decrypted = await decryptDirectMessageForRecipient(encrypted, bob, alice.publicKeyBytes);

  assert.equal(decrypted, "hello encrypted");
  assert.equal(encrypted.senderKeyId, alice.keyId);
  assert.equal(encrypted.recipientSignedPrekeyId, bob.signedPrekey.keyId);
  assert.equal(encrypted.e2eeAlgorithm, "DR-X25519-HKDF-SHA256-AESGCM-Ed25519-v1");
});

test("created v2 identity binds signed prekey signature to explicit device id", async () => {
  const alice = await createIdentity("alice", "web-fixed");
  const bundle = alice.toPrekeyBundle();

  assert.equal(alice.deviceId, "web-fixed");
  assert.equal(bundle.deviceId, "web-fixed");
  assert.equal(bundle.signedPrekey.deviceId, "web-fixed");
});

test("direct E2EE lets sender decrypt its own outbound message", async () => {
  const alice = await createIdentity("alice");
  const bob = await createIdentity("bob");
  const bundle = bob.toPrekeyBundle();

  const encrypted = await encryptDirectMessage("self-view", alice, bundle);
  const decryptedBySender = await decryptDirectMessageForSender(encrypted, alice);

  assert.equal(decryptedBySender, "self-view");
});

test("group key package encrypts a shared key for every member", async () => {
  const alice = await createIdentity("alice");
  const bob = await createIdentity("bob");

  const pkg = await createGroupKeyPackage("group-1", 1, alice, [
    { username: "alice", keyId: alice.signedPrekey.keyId, signedPrekeyPublicBytes: alice.signedPrekey.publicKeyBytes },
    { username: "bob", keyId: bob.signedPrekey.keyId, signedPrekeyPublicBytes: bob.signedPrekey.publicKeyBytes },
  ]);

  assert.equal(pkg.version, 1);
  assert.equal(pkg.envelopes.length, 2);

  const bobGroupKey = await decryptGroupKeyEnvelope(pkg.envelopes[1], bob, alice.signedPrekey.publicKeyBytes);
  const encrypted = await encryptGroupMessage("secret group", bobGroupKey, pkg.version);
  const decrypted = await decryptGroupMessage(encrypted, bobGroupKey);

  assert.equal(decrypted, "secret group");
});

test("identity state can be exported and imported back with prekeys", async () => {
  const alice = await createIdentity("alice");
  const exported = await exportIdentityState(alice);
  const restored = await importIdentityState(exported);
  const bundle = restored.toPrekeyBundle();

  const encrypted = await encryptDirectMessage("persisted", alice, bundle);
  const decrypted = await decryptDirectMessageForRecipient(encrypted, restored, alice.publicKeyBytes);

  assert.equal(decrypted, "persisted");
  assert.equal(restored.keyId, alice.keyId);
  assert.equal(restored.signedPrekey.keyId, alice.signedPrekey.keyId);
});

test("identity state export/import works without Buffer global", async () => {
  const originalBuffer = globalThis.Buffer;
  const alice = await createIdentity("alice");

  try {
    globalThis.Buffer = undefined;
    const exported = await exportIdentityState(alice);
    const restored = await importIdentityState(exported);
    const bundle = restored.toPrekeyBundle();

    const encrypted = await encryptDirectMessage("browser-safe", alice, bundle);
    const decrypted = await decryptDirectMessageForRecipient(encrypted, restored, alice.publicKeyBytes);

    assert.equal(decrypted, "browser-safe");
    assert.equal(restored.keyId, alice.keyId);
  } finally {
    globalThis.Buffer = originalBuffer;
  }
});

test("publish bundle builder exposes signed prekey signature", async () => {
  const alice = await createIdentity("alice");

  const payload = buildPublishPrekeyBundle(alice);

  assert.equal(payload.signedPrekeyId, alice.signedPrekey.keyId);
  assert.deepEqual(Array.from(payload.signedPrekeySignature), Array.from(alice.signedPrekey.signature));
  assert.equal(payload.oneTimePrekeys.length, 0);
});

test("markPrekeysAsPublished updates signed and one-time prekeys", async () => {
  const alice = await createIdentity("alice");

  const published = markPrekeysAsPublished(alice);

  assert.equal(published.signedPrekey.published, true);
  assert.equal(published.oneTimePrekeys.every((item) => item.published === true), true);
});

test("topUpOneTimePrekeys is a no-op for v2 ratchet identities", async () => {
  const alice = await createIdentity("alice");
  alice.oneTimePrekeys = [];

  const toppedUp = await topUpOneTimePrekeys(alice, 3);

  assert.equal(toppedUp.oneTimePrekeys.length, 0);
});
