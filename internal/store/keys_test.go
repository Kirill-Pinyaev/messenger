package store

import (
	"context"
	"testing"
)

func TestMemoryKeyStoreIdentityAndConversationKeys(t *testing.T) {
	t.Parallel()

	store := NewMemoryKeyStore()
	ctx := context.Background()

	identity, err := store.UpsertIdentityKey(ctx, IdentityKey{
		Username:  "alice",
		DeviceID:  "web",
		KeyID:     "alice-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{1, 2, 3},
	})
	if err != nil {
		t.Fatalf("UpsertIdentityKey() error = %v", err)
	}
	if identity.KeyID != "alice-key-1" || identity.Algorithm != "P256-HKDF-AESGCM" {
		t.Fatalf("UpsertIdentityKey() = %+v", identity)
	}

	gotIdentity, err := store.GetIdentityKey(ctx, "alice", "web")
	if err != nil {
		t.Fatalf("GetIdentityKey() error = %v", err)
	}
	if gotIdentity.KeyID != "alice-key-1" || len(gotIdentity.PublicKey) != 3 {
		t.Fatalf("GetIdentityKey() = %+v", gotIdentity)
	}

	groupKey, err := store.UpsertConversationKey(ctx, ConversationKey{
		ConversationID: "group-1",
		Version:        2,
		Algorithm:      "AES-GCM",
		CreatedBy:      "alice",
		Envelopes: []ConversationKeyEnvelope{
			{
				Username:       "alice",
				DeviceID:       "web",
				EncryptedKey:   []byte{9, 9, 9},
				Nonce:          []byte{7, 7, 7},
				SenderKeyID:    "alice-key-1",
				RecipientKeyID: "alice-key-1",
			},
			{
				Username:       "bob",
				DeviceID:       "phone",
				EncryptedKey:   []byte{8, 8, 8},
				Nonce:          []byte{6, 6, 6},
				SenderKeyID:    "alice-key-1",
				RecipientKeyID: "bob-key-1",
			},
		},
	})
	if err != nil {
		t.Fatalf("UpsertConversationKey() error = %v", err)
	}
	if groupKey.Version != 2 || len(groupKey.Envelopes) != 2 {
		t.Fatalf("UpsertConversationKey() = %+v", groupKey)
	}

	gotGroupKey, err := store.GetConversationKey(ctx, "group-1", 2)
	if err != nil {
		t.Fatalf("GetConversationKey() error = %v", err)
	}
	if gotGroupKey.CreatedBy != "alice" || gotGroupKey.Envelopes[1].RecipientKeyID != "bob-key-1" {
		t.Fatalf("GetConversationKey() = %+v", gotGroupKey)
	}
}

func TestMemoryKeyStorePrekeyBundleAcquisitionConsumesOneTimeKeys(t *testing.T) {
	t.Parallel()

	store := NewMemoryKeyStore()
	ctx := context.Background()

	if _, err := store.UpsertIdentityKey(ctx, IdentityKey{
		Username:  "bob",
		DeviceID:  "phone",
		KeyID:     "bob-identity-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{1, 2, 3},
	}); err != nil {
		t.Fatalf("UpsertIdentityKey() error = %v", err)
	}

	if _, err := store.UpsertSignedPrekey(ctx, SignedPrekey{
		Username:  "bob",
		DeviceID:  "phone",
		KeyID:     "bob-signed-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{4, 5, 6},
	}); err != nil {
		t.Fatalf("UpsertSignedPrekey() error = %v", err)
	}

	if err := store.PutOneTimePrekeys(ctx, "bob", "phone", []OneTimePrekey{
		{Username: "bob", DeviceID: "phone", KeyID: "bob-otp-1", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{7, 8, 9}},
		{Username: "bob", DeviceID: "phone", KeyID: "bob-otp-2", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{10, 11, 12}},
	}); err != nil {
		t.Fatalf("PutOneTimePrekeys() error = %v", err)
	}

	first, err := store.AcquirePrekeyBundle(ctx, "bob", "phone")
	if err != nil {
		t.Fatalf("AcquirePrekeyBundle(first) error = %v", err)
	}
	if first.IdentityKey.KeyID != "bob-identity-1" || first.SignedPrekey.KeyID != "bob-signed-1" || first.OneTimePrekey.KeyID != "bob-otp-1" {
		t.Fatalf("AcquirePrekeyBundle(first) = %+v", first)
	}

	second, err := store.AcquirePrekeyBundle(ctx, "bob", "phone")
	if err != nil {
		t.Fatalf("AcquirePrekeyBundle(second) error = %v", err)
	}
	if second.OneTimePrekey.KeyID != "bob-otp-2" {
		t.Fatalf("AcquirePrekeyBundle(second) = %+v", second)
	}

	third, err := store.AcquirePrekeyBundle(ctx, "bob", "phone")
	if err != nil {
		t.Fatalf("AcquirePrekeyBundle(third) error = %v", err)
	}
	if third.OneTimePrekey.KeyID != "" {
		t.Fatalf("AcquirePrekeyBundle(third) = %+v", third)
	}
}

func TestMemoryKeyStoreDeleteOneTimePrekeysResetsQueue(t *testing.T) {
	t.Parallel()

	store := NewMemoryKeyStore()
	ctx := context.Background()

	if _, err := store.UpsertIdentityKey(ctx, IdentityKey{
		Username:  "bob",
		DeviceID:  "phone",
		KeyID:     "bob-identity-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{1, 2, 3},
	}); err != nil {
		t.Fatalf("UpsertIdentityKey() error = %v", err)
	}
	if _, err := store.UpsertSignedPrekey(ctx, SignedPrekey{
		Username:  "bob",
		DeviceID:  "phone",
		KeyID:     "bob-signed-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{4, 5, 6},
	}); err != nil {
		t.Fatalf("UpsertSignedPrekey() error = %v", err)
	}
	if err := store.PutOneTimePrekeys(ctx, "bob", "phone", []OneTimePrekey{
		{Username: "bob", DeviceID: "phone", KeyID: "bob-otp-old", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{7}},
	}); err != nil {
		t.Fatalf("PutOneTimePrekeys(old) error = %v", err)
	}
	if err := store.DeleteOneTimePrekeys(ctx, "bob", "phone"); err != nil {
		t.Fatalf("DeleteOneTimePrekeys() error = %v", err)
	}
	if err := store.PutOneTimePrekeys(ctx, "bob", "phone", []OneTimePrekey{
		{Username: "bob", DeviceID: "phone", KeyID: "bob-otp-new", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{8}},
	}); err != nil {
		t.Fatalf("PutOneTimePrekeys(new) error = %v", err)
	}

	bundle, err := store.AcquirePrekeyBundle(ctx, "bob", "phone")
	if err != nil {
		t.Fatalf("AcquirePrekeyBundle() error = %v", err)
	}
	if bundle.OneTimePrekey.KeyID != "bob-otp-new" {
		t.Fatalf("AcquirePrekeyBundle() = %+v", bundle)
	}
}

func TestMemoryKeyStoreAcquirePrekeyBundlesReturnsAllDevices(t *testing.T) {
	t.Parallel()

	store := NewMemoryKeyStore()
	ctx := context.Background()

	for _, deviceID := range []string{"web", "phone"} {
		if _, err := store.UpsertIdentityKey(ctx, IdentityKey{
			Username:  "alice",
			DeviceID:  deviceID,
			KeyID:     "alice-" + deviceID + "-identity",
			Algorithm: "P256-HKDF-AESGCM",
			PublicKey: []byte{1, 2, 3},
		}); err != nil {
			t.Fatalf("UpsertIdentityKey(%s) error = %v", deviceID, err)
		}
		if _, err := store.UpsertSignedPrekey(ctx, SignedPrekey{
			Username:  "alice",
			DeviceID:  deviceID,
			KeyID:     "alice-" + deviceID + "-spk",
			Algorithm: "P256-HKDF-AESGCM",
			PublicKey: []byte{4, 5, 6},
		}); err != nil {
			t.Fatalf("UpsertSignedPrekey(%s) error = %v", deviceID, err)
		}
	}

	bundles, err := store.AcquirePrekeyBundles(ctx, "alice")
	if err != nil {
		t.Fatalf("AcquirePrekeyBundles() error = %v", err)
	}
	if len(bundles) != 2 {
		t.Fatalf("AcquirePrekeyBundles() len = %d, want 2", len(bundles))
	}
}
