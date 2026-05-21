import { test } from "vitest";
import assert from "node:assert/strict";

import {
  encryptionPrefsStorageKey,
  isConversationEncryptionEnabled,
  loadEncryptionPrefs,
  saveEncryptionPrefs,
  setConversationEncryption,
} from "./chat-encryption.js";

test("isConversationEncryptionEnabled defaults to true", () => {
  assert.equal(isConversationEncryptionEnabled(new Map(), "alice|bob"), true);
});

test("setConversationEncryption stores explicit false and true values", () => {
  let prefs = new Map();
  prefs = setConversationEncryption(prefs, "alice|bob", false);
  prefs = setConversationEncryption(prefs, "group-1", true);

  assert.equal(isConversationEncryptionEnabled(prefs, "alice|bob"), false);
  assert.equal(isConversationEncryptionEnabled(prefs, "group-1"), true);
});

test("loadEncryptionPrefs restores saved values from storage", () => {
  const storage = {
    data: {
      [encryptionPrefsStorageKey("alice")]: JSON.stringify({
        "alice|bob": false,
        "group-1": true,
      }),
    },
    getItem(key) {
      return this.data[key] ?? null;
    },
    setItem() {},
  };

  const prefs = loadEncryptionPrefs("alice", storage);

  assert.equal(isConversationEncryptionEnabled(prefs, "alice|bob"), false);
  assert.equal(isConversationEncryptionEnabled(prefs, "group-1"), true);
  assert.equal(isConversationEncryptionEnabled(prefs, "unknown"), true);
});

test("saveEncryptionPrefs persists preferences into storage", () => {
  const storage = {
    data: {},
    getItem(key) {
      return this.data[key] ?? null;
    },
    setItem(key, value) {
      this.data[key] = value;
    },
  };

  const prefs = new Map([
    ["alice|bob", false],
    ["group-1", true],
  ]);

  saveEncryptionPrefs("alice", prefs, storage);

  assert.deepEqual(JSON.parse(storage.data[encryptionPrefsStorageKey("alice")]), {
    "alice|bob": false,
    "group-1": true,
  });
});
