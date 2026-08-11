import { test } from "vitest";
import assert from "node:assert/strict";

import { Code, ConnectError } from "@connectrpc/connect";

import {
  prepareDirectConversation,
  directIdentityErrorMessage,
  currentDevicePrekeyBundle,
  hasDirectBundleMaterial,
  shouldDecryptDirectAsSender,
} from "./direct-chat.js";

test("prepareDirectConversation inserts a missing DM and creates empty message collection", () => {
  const messages = new Map();
  const profiles = new Map([["bob", { username: "bob", firstName: "Bob" }]]);

  const next = prepareDirectConversation({
    conversations: [],
    messages,
    profiles,
    selfUsername: "alice",
    peerUsername: "bob",
  });

  assert.equal(next.conversationId, "alice|bob");
  assert.equal(next.conversations.length, 1);
  assert.deepEqual(next.conversations[0], {
    conversationId: "alice|bob",
    peerUsername: "bob",
    peerProfile: { username: "bob", firstName: "Bob" },
  });
  assert.deepEqual(next.messages.get("alice|bob"), []);
});

test("prepareDirectConversation reuses existing DM without duplicating it", () => {
  const messages = new Map([["alice|bob", [{ messageId: 1 }]]]);
  const conversations = [{ conversationId: "alice|bob", peerUsername: "bob", peerProfile: null }];

  const next = prepareDirectConversation({
    conversations,
    messages,
    profiles: new Map(),
    selfUsername: "alice",
    peerUsername: "bob",
  });

  assert.equal(next.conversations.length, 1);
  assert.equal(next.conversations[0].conversationId, "alice|bob");
  assert.deepEqual(next.messages.get("alice|bob"), [{ messageId: 1 }]);
});

test("directIdentityErrorMessage maps missing recipient key to a user-friendly explanation", () => {
  const err = new ConnectError("identity key not found", Code.NotFound);

  assert.equal(
    directIdentityErrorMessage(err, "bob"),
    "Пользователь @bob ещё не входил в зашифрованную версию и не опубликовал ключ. Попросите его войти в систему."
  );
});

test("directIdentityErrorMessage preserves unrelated errors", () => {
  const err = new Error("network failed");

  assert.equal(directIdentityErrorMessage(err, "bob"), "network failed");
});

test("currentDevicePrekeyBundle exposes the current device signed prekey in bundle shape", () => {
  const bundle = currentDevicePrekeyBundle({
    username: "alice",
    deviceId: "web-1",
    identity: {
      signedPrekey: {
        keyId: "spk-1",
        publicKeyBytes: new Uint8Array([1, 2, 3]),
      },
    },
  });

  assert.equal(bundle.username, "alice");
  assert.equal(bundle.deviceId, "web-1");
  assert.equal(bundle.signedPrekey.keyId, "spk-1");
  assert.deepEqual(Array.from(bundle.signedPrekey.publicKeyBytes), [1, 2, 3]);
  assert.equal(bundle.oneTimePrekey, undefined);
});

test("shouldDecryptDirectAsSender returns false for same-account messages targeted to current device", () => {
  assert.equal(
    shouldDecryptDirectAsSender({
      message: {
        from: "alice",
        recipientSignedPrekeyId: "local-spk",
        recipientOneTimePrekeyId: "",
      },
      username: "alice",
      identity: {
        signedPrekey: { keyId: "local-spk" },
        oneTimePrekeys: [],
      },
    }),
    false,
  );
});

test("shouldDecryptDirectAsSender returns true for locally sent same-account messages", () => {
  assert.equal(
    shouldDecryptDirectAsSender({
      message: {
        from: "alice",
        recipientSignedPrekeyId: "peer-spk",
        recipientOneTimePrekeyId: "",
      },
      username: "alice",
      identity: {
        signedPrekey: { keyId: "local-spk" },
        oneTimePrekeys: [],
      },
    }),
    true,
  );
});

test("hasDirectBundleMaterial requires signed prekey bytes", () => {
  assert.equal(
    hasDirectBundleMaterial({
      identityKey: {
        publicKey: new Uint8Array([9, 9, 9]),
      },
      signedPrekey: {
        publicKey: new Uint8Array([1, 2, 3]),
        signature: new Uint8Array([4, 5, 6]),
      },
    }),
    true,
  );

  assert.equal(
    hasDirectBundleMaterial({
      signedPrekey: {
        publicKey: new Uint8Array(),
      },
    }),
    false,
  );

  assert.equal(hasDirectBundleMaterial({}), false);
});
