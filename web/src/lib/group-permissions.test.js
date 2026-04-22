import test from "node:test";
import assert from "node:assert/strict";

import {
  canManageGroupMembers,
  canRemoveGroupMember,
  canTransferAdmin,
  currentUserRole,
} from "./group-permissions.js";

const conversation = {
  conversationId: "group-1",
  members: [
    { username: "alice", role: 1, addedBy: "" },
    { username: "bob", role: 2, addedBy: "alice" },
    { username: "carol", role: 2, addedBy: "bob" },
  ],
};

test("currentUserRole reads admin and member roles from conversation", () => {
  assert.equal(currentUserRole(conversation, "alice"), 1);
  assert.equal(currentUserRole(conversation, "bob"), 2);
  assert.equal(currentUserRole(conversation, "ghost"), 0);
});

test("canManageGroupMembers allows any group member to add users", () => {
  assert.equal(canManageGroupMembers(conversation, "alice"), true);
  assert.equal(canManageGroupMembers(conversation, "bob"), true);
  assert.equal(canManageGroupMembers(conversation, "ghost"), false);
});

test("canRemoveGroupMember allows admin to remove anyone except self", () => {
  assert.equal(canRemoveGroupMember(conversation, "alice", "bob"), true);
  assert.equal(canRemoveGroupMember(conversation, "alice", "alice"), false);
});

test("canRemoveGroupMember allows regular member to remove only invited users", () => {
  assert.equal(canRemoveGroupMember(conversation, "bob", "carol"), true);
  assert.equal(canRemoveGroupMember(conversation, "bob", "alice"), false);
  assert.equal(canRemoveGroupMember(conversation, "bob", "bob"), false);
});

test("canTransferAdmin only allows current admin", () => {
  assert.equal(canTransferAdmin(conversation, "alice"), true);
  assert.equal(canTransferAdmin(conversation, "bob"), false);
});
