import { test } from "vitest";
import assert from "node:assert/strict";

import {
  conversationLabel,
  conversationMetaLine,
  displayName,
  ensureConversationForMessage,
  findMessageById,
  removeConversationById,
  upsertConversation,
  makeConversationId,
  normalizeAvatarHex,
  peerFromConversationId,
  isGroupMessage,
  removeMessageCollection,
  upsertMessageCollection,
} from "./chat-state.js";

test("makeConversationId sorts usernames", () => {
  assert.equal(makeConversationId("bob", "alice"), "alice|bob");
});

test("peerFromConversationId returns the peer username", () => {
  assert.equal(peerFromConversationId("alice|bob", "alice"), "bob");
  assert.equal(peerFromConversationId("alice|bob", "bob"), "alice");
  assert.equal(peerFromConversationId("broken", "alice"), "");
});

test("displayName prefers profile full name and falls back to username", () => {
  const profiles = new Map([
    ["alice", { username: "alice", firstName: "Alice", lastName: "Doe" }],
    ["bob", { username: "bob", firstName: "", lastName: "" }],
  ]);

  assert.equal(displayName(profiles, null, "alice"), "Alice Doe");
  assert.equal(displayName(profiles, null, "bob"), "bob");
  assert.equal(displayName(profiles, { username: "me", firstName: "Me", lastName: "User" }, "me"), "Me User");
  assert.equal(displayName(profiles, null, "ghost"), "ghost");
});

test("conversationLabel uses group title for group chats", () => {
  const profiles = new Map();
  const ownProfile = { username: "alice", firstName: "Alice", lastName: "Doe" };

  assert.equal(conversationLabel(profiles, ownProfile, {
    kind: 2,
    title: "Diploma team",
    peerUsername: "",
  }), "Diploma team");
});

test("conversationMetaLine returns group members summary", () => {
  const profiles = new Map();
  const ownProfile = { username: "alice", firstName: "Alice", lastName: "Doe" };

  assert.equal(conversationMetaLine(profiles, ownProfile, {
    kind: 2,
    memberUsernames: ["alice", "bob", "carol"],
  }), "3 участников");
});

test("normalizeAvatarHex validates and normalizes values", () => {
  assert.equal(normalizeAvatarHex(" ABCDEF "), "abcdef");
  assert.equal(normalizeAvatarHex("#123456"), "123456");
  assert.equal(normalizeAvatarHex("12"), "");
  assert.equal(normalizeAvatarHex("zzzzzz"), "");
});

test("ensureConversationForMessage inserts missing conversation at top", () => {
  const profiles = new Map([["bob", { username: "bob" }]]);
  const conversations = [];
  const next = ensureConversationForMessage(conversations, profiles, "alice", {
    conversationId: "alice|bob",
    from: "alice",
    to: "bob",
  });

  assert.deepEqual(next, [{
    conversationId: "alice|bob",
    peerUsername: "bob",
    peerProfile: { username: "bob" },
  }]);
});

test("ensureConversationForMessage moves existing conversation to top", () => {
  const profiles = new Map();
  const conversations = [
    { conversationId: "alice|carol", peerUsername: "carol", peerProfile: null },
    { conversationId: "alice|bob", peerUsername: "bob", peerProfile: null },
  ];

  const next = ensureConversationForMessage(conversations, profiles, "alice", {
    conversationId: "alice|bob",
    from: "bob",
    to: "alice",
  });

  assert.equal(next[0].conversationId, "alice|bob");
  assert.equal(next[1].conversationId, "alice|carol");
});

test("upsertMessageCollection inserts, updates and sorts messages", () => {
  const messages = new Map();

  const first = upsertMessageCollection(messages, { conversationId: "alice|bob", messageId: 2, text: "b" });
  const second = upsertMessageCollection(first, { conversationId: "alice|bob", messageId: 1, text: "a" });
  const third = upsertMessageCollection(second, { conversationId: "alice|bob", messageId: 2, text: "bb" });

  assert.deepEqual(third.get("alice|bob").map((item) => [item.messageId, item.text]), [
    [1, "a"],
    [2, "bb"],
  ]);
});

test("removeMessageCollection removes message across conversations", () => {
  const messages = new Map([
    ["alice|bob", [{ messageId: 1 }, { messageId: 2 }]],
    ["alice|carol", [{ messageId: 3 }]],
  ]);

  const next = removeMessageCollection(messages, 2);

  assert.deepEqual(next.get("alice|bob"), [{ messageId: 1 }]);
  assert.deepEqual(next.get("alice|carol"), [{ messageId: 3 }]);
});

test("findMessageById returns message or null", () => {
  const messages = new Map([
    ["alice|bob", [{ messageId: 1, text: "hello" }]],
  ]);

  assert.deepEqual(findMessageById(messages, 1), { messageId: 1, text: "hello" });
  assert.equal(findMessageById(messages, 2), null);
});

test("isGroupMessage detects group messages", () => {
  assert.equal(isGroupMessage({ conversationId: "group-1", to: "group-1" }), true);
  assert.equal(isGroupMessage({ conversationId: "alice|bob", to: "bob" }), false);
});

test("upsertConversation moves updated conversation to top", () => {
  const next = upsertConversation([
    { conversationId: "group-2", title: "Two" },
    { conversationId: "group-1", title: "One" },
  ], { conversationId: "group-1", title: "One updated" });

  assert.deepEqual(next, [
    { conversationId: "group-1", title: "One updated" },
    { conversationId: "group-2", title: "Two" },
  ]);
});

test("removeConversationById removes deleted group from sidebar state", () => {
  const next = removeConversationById([
    { conversationId: "group-1" },
    { conversationId: "group-2" },
  ], "group-1");

  assert.deepEqual(next, [{ conversationId: "group-2" }]);
});
