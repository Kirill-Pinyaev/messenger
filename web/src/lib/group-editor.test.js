import test from "node:test";
import assert from "node:assert/strict";

import {
  addDraftMember,
  removeDraftMember,
  filterSelectableUsers,
} from "./group-editor.js";

test("addDraftMember adds unique users and ignores duplicates", () => {
  const selected = [{ username: "bob" }];

  const next = addDraftMember(selected, { username: "carol" });
  const duplicate = addDraftMember(next, { username: "bob" });

  assert.deepEqual(next, [{ username: "bob" }, { username: "carol" }]);
  assert.deepEqual(duplicate, next);
});

test("removeDraftMember removes by username", () => {
  const next = removeDraftMember([{ username: "bob" }, { username: "carol" }], "bob");

  assert.deepEqual(next, [{ username: "carol" }]);
});

test("filterSelectableUsers hides already selected and current user", () => {
  const users = [
    { username: "alice" },
    { username: "bob" },
    { username: "carol" },
  ];

  const filtered = filterSelectableUsers(users, [{ username: "bob" }], "alice");

  assert.deepEqual(filtered, [{ username: "carol" }]);
});
