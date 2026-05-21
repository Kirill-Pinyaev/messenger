import { test } from "vitest";
import assert from "node:assert/strict";

import {
  attachmentKindForMime,
  decryptMediaBytes,
  deserializeMediaDescriptor,
  encryptMediaBytes,
  serializeMediaDescriptor,
} from "./media-e2ee.js";

test("attachmentKindForMime classifies image/video/file", () => {
  assert.equal(attachmentKindForMime("image/png"), "image");
  assert.equal(attachmentKindForMime("video/mp4"), "video");
  assert.equal(attachmentKindForMime("application/pdf"), "file");
});

test("encryptMediaBytes and decryptMediaBytes roundtrip", async () => {
  const plaintext = new Uint8Array([1, 2, 3, 4, 5, 6]);
  const encrypted = await encryptMediaBytes(plaintext, "image/png", "cat.png");
  const serialized = serializeMediaDescriptor(encrypted.descriptor);
  const descriptor = deserializeMediaDescriptor(serialized);
  const decrypted = await decryptMediaBytes(encrypted.ciphertext, descriptor, encrypted.nonce);

  assert.deepEqual([...decrypted], [...plaintext]);
  assert.equal(descriptor.mimeType, "image/png");
  assert.equal(descriptor.originalFilename, "cat.png");
});
