package store

import (
	"context"
	"testing"
	"time"
)

func TestMemoryMessageStoreSaveAndLoadDirectEnvelopes(t *testing.T) {
	t.Parallel()

	ctx := context.Background()
	store := NewMemoryMessageStore()

	if _, err := store.PrepareMedia(ctx, MediaObject{
		MediaID:       "media-1",
		OwnerUsername: "alice",
		StorageKey:    "media-1.bin",
		Filename:      "photo.png",
		MimeType:      "image/png",
		Kind:          AttachmentKindImage,
		SizeBytes:     128,
	}); err != nil {
		t.Fatalf("PrepareMedia() error = %v", err)
	}
	if _, err := store.CompleteMedia(ctx, MediaObject{
		MediaID:         "media-1",
		OwnerUsername:   "alice",
		StorageKey:      "media-1.bin",
		Filename:        "photo.png",
		MimeType:        "image/png",
		Kind:            AttachmentKindImage,
		SizeBytes:       128,
		CiphertextSize:  160,
		Nonce:           []byte{1, 2, 3},
		SHA256:          []byte{4, 5, 6},
		Uploaded:        true,
		UploadedAt:      time.Now().UTC(),
	}); err != nil {
		t.Fatalf("CompleteMedia() error = %v", err)
	}

	saved, err := store.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "alice",
		To:             "bob",
		SenderDeviceID: "alice-web",
		SenderKeyID:    "alice-key",
		Encrypted:      true,
		DirectEnvelopes: []DirectEnvelope{
			{
				TargetUsername:              "alice",
				TargetDeviceID:              "alice-web",
				Ciphertext:                  []byte{1, 1, 1},
				Nonce:                       []byte{2, 2, 2},
				RecipientSignedPrekeyID:     "alice-spk",
				RecipientSignedPrekeyPublic: []byte{3, 3, 3},
			},
			{
				TargetUsername:              "bob",
				TargetDeviceID:              "bob-phone",
				Ciphertext:                  []byte{4, 4, 4},
				Nonce:                       []byte{5, 5, 5},
				RecipientSignedPrekeyID:     "bob-spk",
				RecipientSignedPrekeyPublic: []byte{6, 6, 6},
				RecipientOneTimePrekeyID:    "bob-otp",
				RecipientOneTimePrekeyPublic: []byte{7, 7, 7},
			},
		},
		Attachments: []Attachment{
			{
				AttachmentID: "att-1",
				Kind:         AttachmentKindImage,
				Filename:     "photo.png",
				MimeType:     "image/png",
				SizeBytes:    128,
				MediaID:      "media-1",
				SHA256:       []byte{8, 8, 8},
				CiphertextSize: 160,
				DirectEnvelopes: []AttachmentDirectEnvelope{
					{
						TargetUsername:          "bob",
						TargetDeviceID:          "bob-phone",
						EncryptedDescriptor:     []byte{9, 9, 9},
						DescriptorNonce:         []byte{10, 10, 10},
						RecipientSignedPrekeyID: "bob-spk",
					},
				},
			},
		},
		TS: time.Now().UTC(),
	})
	if err != nil {
		t.Fatalf("Save() error = %v", err)
	}

	history, err := store.History(ctx, "alice|bob", 10)
	if err != nil {
		t.Fatalf("History() error = %v", err)
	}
	if len(history) != 1 {
		t.Fatalf("History() len = %d, want 1", len(history))
	}
	if len(history[0].DirectEnvelopes) != 2 || len(history[0].Attachments) != 1 || len(history[0].Attachments[0].DirectEnvelopes) != 1 {
		t.Fatalf("History() = %+v", history[0])
	}
	if history[0].ID != saved.ID {
		t.Fatalf("History().ID = %d, want %d", history[0].ID, saved.ID)
	}
}

func TestMemoryMessageStoreSaveDeduplicatesDirectEnvelopeTargets(t *testing.T) {
	t.Parallel()

	ctx := context.Background()
	store := NewMemoryMessageStore()

	saved, err := store.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "alice",
		To:             "bob",
		SenderDeviceID: "alice-web",
		SenderKeyID:    "alice-key",
		Encrypted:      true,
		DirectEnvelopes: []DirectEnvelope{
			{
				TargetUsername:              "bob",
				TargetDeviceID:              "bob-phone",
				Ciphertext:                  []byte{1, 1, 1},
				Nonce:                       []byte{2, 2, 2},
				RecipientSignedPrekeyID:     "bob-spk-a",
				RecipientSignedPrekeyPublic: []byte{3, 3, 3},
			},
			{
				TargetUsername:              "bob",
				TargetDeviceID:              "bob-phone",
				Ciphertext:                  []byte{4, 4, 4},
				Nonce:                       []byte{5, 5, 5},
				RecipientSignedPrekeyID:     "bob-spk-b",
				RecipientSignedPrekeyPublic: []byte{6, 6, 6},
			},
		},
		TS: time.Now().UTC(),
	})
	if err != nil {
		t.Fatalf("Save() error = %v", err)
	}

	history, err := store.History(ctx, "alice|bob", 10)
	if err != nil {
		t.Fatalf("History() error = %v", err)
	}
	if len(history) != 1 {
		t.Fatalf("History() len = %d, want 1", len(history))
	}
	if got := len(history[0].DirectEnvelopes); got != 1 {
		t.Fatalf("History().DirectEnvelopes len = %d, want 1", got)
	}
	if history[0].DirectEnvelopes[0].RecipientSignedPrekeyID != "bob-spk-b" {
		t.Fatalf("dedupe kept wrong envelope: %+v", history[0].DirectEnvelopes[0])
	}
	if history[0].ID != saved.ID {
		t.Fatalf("History().ID = %d, want %d", history[0].ID, saved.ID)
	}
}

func TestMemoryMessageStoreSaveAllowsDirectEnvelopeWithoutOneTimePublic(t *testing.T) {
	t.Parallel()

	ctx := context.Background()
	store := NewMemoryMessageStore()

	_, err := store.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "alice",
		To:             "bob",
		SenderDeviceID: "alice-web",
		SenderKeyID:    "alice-key",
		Encrypted:      true,
		DirectEnvelopes: []DirectEnvelope{
			{
				TargetUsername:              "bob",
				TargetDeviceID:              "bob-phone",
				Ciphertext:                  []byte{1, 2, 3},
				Nonce:                       []byte{4, 5, 6},
				RecipientSignedPrekeyID:     "bob-spk",
				RecipientSignedPrekeyPublic: []byte{7, 8, 9},
				RecipientOneTimePrekeyID:    "",
			},
		},
		TS: time.Now().UTC(),
	})
	if err != nil {
		t.Fatalf("Save() error = %v", err)
	}
}

func TestMemoryMessageStoreSaveAllowsDirectAttachmentOnlyMessage(t *testing.T) {
	t.Parallel()

	ctx := context.Background()
	store := NewMemoryMessageStore()

	if _, err := store.PrepareMedia(ctx, MediaObject{
		MediaID:       "media-2",
		OwnerUsername: "alice",
		StorageKey:    "media-2.bin",
		Filename:      "doc.pdf",
		MimeType:      "application/pdf",
		Kind:          AttachmentKindFile,
		SizeBytes:     64,
	}); err != nil {
		t.Fatalf("PrepareMedia() error = %v", err)
	}
	if _, err := store.CompleteMedia(ctx, MediaObject{
		MediaID:        "media-2",
		OwnerUsername:  "alice",
		StorageKey:     "media-2.bin",
		Filename:       "doc.pdf",
		MimeType:       "application/pdf",
		Kind:           AttachmentKindFile,
		SizeBytes:      64,
		CiphertextSize: 96,
		Nonce:          []byte{1, 2, 3},
		SHA256:         []byte{4, 5, 6},
		Uploaded:       true,
		UploadedAt:     time.Now().UTC(),
	}); err != nil {
		t.Fatalf("CompleteMedia() error = %v", err)
	}

	_, err := store.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "alice",
		To:             "bob",
		SenderDeviceID: "alice-web",
		SenderKeyID:    "alice-key",
		Encrypted:      true,
		Attachments: []Attachment{
			{
				AttachmentID:   "att-2",
				Kind:           AttachmentKindFile,
				Filename:       "doc.pdf",
				MimeType:       "application/pdf",
				SizeBytes:      64,
				MediaID:        "media-2",
				SHA256:         []byte{7, 8, 9},
				CiphertextSize: 96,
				DirectEnvelopes: []AttachmentDirectEnvelope{
					{
						TargetUsername:              "bob",
						TargetDeviceID:              "bob-phone",
						EncryptedDescriptor:         []byte{9, 9, 9},
						DescriptorNonce:             []byte{10, 10, 10},
						RecipientSignedPrekeyID:     "bob-spk",
						RecipientSignedPrekeyPublic: []byte{11, 11, 11},
					},
				},
			},
		},
		TS: time.Now().UTC(),
	})
	if err != nil {
		t.Fatalf("Save() error = %v", err)
	}
}
