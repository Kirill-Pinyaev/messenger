import { test } from "vitest";
import assert from "node:assert/strict";

import {
  DR_ALGORITHM,
  createRatchetIdentity,
  decryptRatchetMessage,
  encryptRatchetMessage,
  exportRatchetIdentity,
  importRatchetIdentity,
  verifySignedPrekey,
} from "./double-ratchet.js";

test("Ed25519 signed prekey verifies and tampering fails", async () => {
  const bob = await createRatchetIdentity("bob", "phone");
  const bundle = bob.toPrekeyBundle();

  assert.equal(await verifySignedPrekey(bundle), true);

  const tampered = {
    ...bundle,
    signedPrekey: {
      ...bundle.signedPrekey,
      publicKey: new Uint8Array(bundle.signedPrekey.publicKey),
    },
  };
  tampered.signedPrekey.publicKey[0] ^= 1;

  assert.equal(await verifySignedPrekey(tampered), false);
});

test("Double Ratchet decrypts first and sequential messages", async () => {
  const alice = await createRatchetIdentity("alice", "web");
  const bob = await createRatchetIdentity("bob", "phone");

  const first = await encryptRatchetMessage("hello", alice, bob.toPrekeyBundle(), "alice|bob");
  const firstPlain = await decryptRatchetMessage(first, bob, alice.toIdentityKey(), "alice|bob");
  const second = await encryptRatchetMessage("again", alice, bob.toPrekeyBundle(), "alice|bob");
  const secondPlain = await decryptRatchetMessage(second, bob, alice.toIdentityKey(), "alice|bob");

  assert.equal(firstPlain, "hello");
  assert.equal(secondPlain, "again");
  assert.equal(first.e2eeAlgorithm, DR_ALGORITHM);
  assert.equal(second.messageNumber, first.messageNumber + 1);
});

test("Double Ratchet advances after both sides send", async () => {
  const alice = await createRatchetIdentity("alice", "web");
  const bob = await createRatchetIdentity("bob", "phone");

  const a1 = await encryptRatchetMessage("a1", alice, bob.toPrekeyBundle(), "alice|bob");
  assert.equal(await decryptRatchetMessage(a1, bob, alice.toIdentityKey(), "alice|bob"), "a1");

  const b1 = await encryptRatchetMessage("b1", bob, alice.toPrekeyBundle(), "alice|bob");
  assert.equal(await decryptRatchetMessage(b1, alice, bob.toIdentityKey(), "alice|bob"), "b1");

  const a2 = await encryptRatchetMessage("a2", alice, bob.toPrekeyBundle(), "alice|bob");
  assert.equal(await decryptRatchetMessage(a2, bob, alice.toIdentityKey(), "alice|bob"), "a2");
  assert.notDeepEqual(Array.from(a1.ratchetPublicKey), Array.from(a2.ratchetPublicKey));
});

test("ratchet identity export/import preserves session state", async () => {
  const alice = await createRatchetIdentity("alice", "web");
  const bob = await createRatchetIdentity("bob", "phone");

  const first = await encryptRatchetMessage("persist me", alice, bob.toPrekeyBundle(), "alice|bob");
  assert.equal(await decryptRatchetMessage(first, bob, alice.toIdentityKey(), "alice|bob"), "persist me");

  const restored = await importRatchetIdentity(await exportRatchetIdentity(bob));
  const second = await encryptRatchetMessage("after restore", alice, bob.toPrekeyBundle(), "alice|bob");

  assert.equal(await decryptRatchetMessage(second, restored, alice.toIdentityKey(), "alice|bob"), "after restore");
});

test("invalid signed prekey prevents encryption", async () => {
  const alice = await createRatchetIdentity("alice", "web");
  const bob = await createRatchetIdentity("bob", "phone");
  const bundle = bob.toPrekeyBundle();
  bundle.signedPrekey.signature = new Uint8Array(bundle.signedPrekey.signature);
  bundle.signedPrekey.signature[0] ^= 1;

  await assert.rejects(
    () => encryptRatchetMessage("nope", alice, bundle, "alice|bob"),
    /invalid signed prekey signature/i,
  );
});

test("failed decrypt does not advance receiving ratchet session", async () => {
  const alice = await createRatchetIdentity("alice", "web");
  const bob = await createRatchetIdentity("bob", "phone");

  const message = await encryptRatchetMessage("retryable", alice, bob.toPrekeyBundle(), "alice|bob");
  const tampered = {
    ...message,
    ciphertext: new Uint8Array(message.ciphertext),
  };
  tampered.ciphertext[0] ^= 1;

  await assert.rejects(
    () => decryptRatchetMessage(tampered, bob, alice.toIdentityKey(), "alice|bob"),
    /operation-specific reason|decrypt|failed/i,
  );

  assert.equal(await decryptRatchetMessage(message, bob, alice.toIdentityKey(), "alice|bob"), "retryable");
});

test("Double Ratchet decrypts older skipped messages after a later message", async () => {
  const alice = await createRatchetIdentity("alice", "web");
  const bob = await createRatchetIdentity("bob", "phone");

  const first = await encryptRatchetMessage("first", alice, bob.toPrekeyBundle(), "alice|bob");
  const second = await encryptRatchetMessage("second", alice, bob.toPrekeyBundle(), "alice|bob");

  assert.equal(await decryptRatchetMessage(second, bob, alice.toIdentityKey(), "alice|bob"), "second");
  assert.equal(await decryptRatchetMessage(first, bob, alice.toIdentityKey(), "alice|bob"), "first");
});

test("Double Ratchet recovers an older initial-chain message if skipped cache was not persisted", async () => {
  const alice = await createRatchetIdentity("alice", "web");
  const bob = await createRatchetIdentity("bob", "phone");

  const first = await encryptRatchetMessage("first", alice, bob.toPrekeyBundle(), "alice|bob");
  const second = await encryptRatchetMessage("second", alice, bob.toPrekeyBundle(), "alice|bob");

  assert.equal(await decryptRatchetMessage(second, bob, alice.toIdentityKey(), "alice|bob"), "second");
  bob.skippedMessageKeys = {};

  assert.equal(await decryptRatchetMessage(first, bob, alice.toIdentityKey(), "alice|bob"), "first");
});

test("Double Ratchet decrypts independent initial messages after both sides send before receiving", async () => {
  const alice = await createRatchetIdentity("alice", "web");
  const bob = await createRatchetIdentity("bob", "android");

  const aliceFirst = await encryptRatchetMessage("alice first", alice, bob.toPrekeyBundle(), "alice|bob");
  const bobFirst = await encryptRatchetMessage("bob first", bob, alice.toPrekeyBundle(), "alice|bob");

  assert.equal(await decryptRatchetMessage(aliceFirst, bob, alice.toIdentityKey(), "alice|bob"), "alice first");
  assert.equal(await decryptRatchetMessage(bobFirst, alice, bob.toIdentityKey(), "alice|bob"), "bob first");
});
