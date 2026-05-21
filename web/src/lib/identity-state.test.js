import { test } from "vitest";
import assert from "node:assert/strict";

import { hasUsableDirectIdentityMaterial, identityStorageKey } from "./identity-state.js";

test("identityStorageKey is device-aware for multi-device web identities", () => {
  assert.equal(
    identityStorageKey("alice", "web-1"),
    "messenger-e2ee-identity:alice:web-1",
  );
  assert.equal(
    identityStorageKey("alice", ""),
    "messenger-e2ee-identity:alice",
  );
});

test("hasUsableDirectIdentityMaterial requires local private and signed-prekey material", () => {
  assert.equal(
    hasUsableDirectIdentityMaterial({
      privateKey: {},
      publicKeyBytes: new Uint8Array([1]),
      privateKeyJwk: { kty: "EC" },
      signedPrekey: {
        privateKey: {},
        publicKeyBytes: new Uint8Array([2]),
        privateKeyJwk: { kty: "EC" },
      },
    }),
    true,
  );

  assert.equal(
    hasUsableDirectIdentityMaterial({
      privateKey: null,
      publicKeyBytes: new Uint8Array([1]),
      privateKeyJwk: { kty: "EC" },
      signedPrekey: {
        privateKey: {},
        publicKeyBytes: new Uint8Array([2]),
        privateKeyJwk: { kty: "EC" },
      },
    }),
    false,
  );

  assert.equal(
    hasUsableDirectIdentityMaterial({
      privateKey: {},
      publicKeyBytes: new Uint8Array([1]),
      privateKeyJwk: { kty: "EC" },
      signedPrekey: {
        privateKey: {},
        publicKeyBytes: new Uint8Array(),
        privateKeyJwk: { kty: "EC" },
      },
    }),
    false,
  );
});

test("hasUsableDirectIdentityMaterial accepts v2 Ed25519/X25519 identity material", () => {
  assert.equal(
    hasUsableDirectIdentityMaterial({
      algorithm: "Ed25519",
      publicKeyBytes: new Uint8Array([1]),
      privateKeyBytes: new Uint8Array([2]),
      signedPrekey: {
        algorithm: "X25519",
        publicKeyBytes: new Uint8Array([3]),
        privateKeyBytes: new Uint8Array([4]),
        signature: new Uint8Array([5]),
      },
    }),
    true,
  );

  assert.equal(
    hasUsableDirectIdentityMaterial({
      algorithm: "Ed25519",
      publicKeyBytes: new Uint8Array([1]),
      privateKeyBytes: new Uint8Array([2]),
      signedPrekey: {
        algorithm: "X25519",
        publicKeyBytes: new Uint8Array([3]),
        privateKeyBytes: new Uint8Array([4]),
        signature: new Uint8Array(),
      },
    }),
    false,
  );
});
