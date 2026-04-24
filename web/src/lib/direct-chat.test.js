import test from "node:test";
import assert from "node:assert/strict";

import { Code, ConnectError } from "@connectrpc/connect";

import { prepareDirectConversation, directIdentityErrorMessage } from "./direct-chat.js";

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
