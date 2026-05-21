import { test } from "vitest";
import assert from "node:assert/strict";

import { avatarView, initials } from "./avatar.js";

test("avatarView prefers uploaded avatar data", () => {
  const view = avatarView(
    { username: "alice", avatarData: "data:image/png;base64,aaa", avatarHex: "ff0000" },
    "#123456",
    "Alice Doe",
  );

  assert.deepEqual(view, {
    kind: "image",
    src: "data:image/png;base64,aaa",
    alt: "Alice Doe",
  });
});

test("avatarView falls back to legacy avatarHex", () => {
  const view = avatarView(
    { username: "alice", avatarHex: "abcdef" },
    "#123456",
    "Alice Doe",
  );

  assert.deepEqual(view, {
    kind: "initials",
    color: "#abcdef",
    text: "AD",
  });
});

test("initials uses name letters and falls back to question mark", () => {
  assert.equal(initials("Alice Doe"), "AD");
  assert.equal(initials(""), "?");
});
