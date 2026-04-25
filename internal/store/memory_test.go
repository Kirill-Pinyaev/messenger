package store

import (
	"context"
	"errors"
	"testing"
	"time"
)

func TestMemoryUserStoreCRUDAndSearch(t *testing.T) {
	t.Parallel()

	s := NewMemoryUserStore()
	ctx := context.Background()

	users := []User{
		{Username: "alice", FirstName: "Alice", LastName: "Doe", AvatarHex: "111111", Salt: []byte{1}, Hash: []byte{2}, CreatedAt: time.Now().UTC()},
		{Username: "bob", FirstName: "Bob", LastName: "Smith", AvatarHex: "222222", Salt: []byte{3}, Hash: []byte{4}, CreatedAt: time.Now().UTC()},
	}
	for _, user := range users {
		if err := s.Create(ctx, user); err != nil {
			t.Fatalf("Create(%q) error = %v", user.Username, err)
		}
	}

	got, err := s.Search(ctx, "o", 10)
	if err != nil {
		t.Fatalf("Search() error = %v", err)
	}
	if len(got) != 1 {
		t.Fatalf("Search() len = %d, want 1", len(got))
	}
	if got[0].Username != "bob" {
		t.Fatalf("Search() first result = %q, want %q", got[0].Username, "bob")
	}

	updated, err := s.UpdateProfile(ctx, "alice", "Alicia", "", "abcdef", "data:image/png;base64,aaa")
	if err != nil {
		t.Fatalf("UpdateProfile() error = %v", err)
	}
	if updated.FirstName != "Alicia" || updated.AvatarHex != "abcdef" || updated.AvatarData == "" {
		t.Fatalf("UpdateProfile() = %+v", updated)
	}

	many, err := s.GetMany(ctx, []string{"alice", "missing", "bob"})
	if err != nil {
		t.Fatalf("GetMany() error = %v", err)
	}
	if len(many) != 2 {
		t.Fatalf("GetMany() len = %d, want 2", len(many))
	}

	if err := s.Delete(ctx, "bob"); err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if _, err := s.Get(ctx, "bob"); !errors.Is(err, ErrUserNotFound) {
		t.Fatalf("Get() after delete error = %v, want %v", err, ErrUserNotFound)
	}
}

func TestMemoryMessageStoreFlow(t *testing.T) {
	t.Parallel()

	s := NewMemoryMessageStore()
	ctx := context.Background()
	now := time.Now().UTC().Truncate(time.Second)

	first, err := s.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "alice",
		To:             "bob",
		Text:           "hello",
		TS:             now,
	})
	if err != nil {
		t.Fatalf("Save(first) error = %v", err)
	}
	second, err := s.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "bob",
		To:             "alice",
		Text:           "world",
		TS:             now.Add(time.Second),
	})
	if err != nil {
		t.Fatalf("Save(second) error = %v", err)
	}

	history, err := s.History(ctx, "alice|bob", 10)
	if err != nil {
		t.Fatalf("History() error = %v", err)
	}
	if len(history) != 2 || history[0].ID != first.ID || history[1].ID != second.ID {
		t.Fatalf("History() = %+v", history)
	}

	found, err := s.SearchMessages(ctx, "alice", "wor", 10)
	if err != nil {
		t.Fatalf("SearchMessages() error = %v", err)
	}
	if len(found) != 1 || found[0].ID != second.ID {
		t.Fatalf("SearchMessages() = %+v, want second message", found)
	}

	conversations, err := s.Conversations(ctx, "alice")
	if err != nil {
		t.Fatalf("Conversations() error = %v", err)
	}
	if len(conversations) != 1 || conversations[0] != "alice|bob" {
		t.Fatalf("Conversations() = %+v", conversations)
	}

	got, err := s.GetByID(ctx, first.ID)
	if err != nil {
		t.Fatalf("GetByID() error = %v", err)
	}
	if got.Text != "hello" {
		t.Fatalf("GetByID().Text = %q, want %q", got.Text, "hello")
	}

	if err := s.DeleteMessage(ctx, first.ID); err != nil {
		t.Fatalf("DeleteMessage() error = %v", err)
	}
	if _, err := s.GetByID(ctx, first.ID); !errors.Is(err, ErrMessageNotFound) {
		t.Fatalf("GetByID() after delete error = %v, want %v", err, ErrMessageNotFound)
	}

	if err := s.DeleteUser(ctx, "bob"); err != nil {
		t.Fatalf("DeleteUser() error = %v", err)
	}
	history, err = s.History(ctx, "alice|bob", 10)
	if err != nil {
		t.Fatalf("History() after DeleteUser error = %v", err)
	}
	if len(history) != 0 {
		t.Fatalf("History() after DeleteUser len = %d, want 0", len(history))
	}
}

func TestMemoryMessageStoreMediaAndAttachments(t *testing.T) {
	t.Parallel()

	s := NewMemoryMessageStore()
	ctx := context.Background()
	now := time.Now().UTC().Truncate(time.Second)

	media, err := s.PrepareMedia(ctx, MediaObject{
		MediaID:       "media-1",
		OwnerUsername: "alice",
		StorageKey:    "aa/media-1.bin",
		Filename:      "cat.png",
		MimeType:      "image/png",
		Kind:          AttachmentKindImage,
		SizeBytes:     128,
		CreatedAt:     now,
	})
	if err != nil {
		t.Fatalf("PrepareMedia() error = %v", err)
	}
	if media.Uploaded {
		t.Fatalf("PrepareMedia() uploaded = true, want false")
	}

	media, err = s.CompleteMedia(ctx, MediaObject{
		MediaID:        "media-1",
		OwnerUsername:  "alice",
		StorageKey:     "aa/media-1.bin",
		Filename:       "cat.png",
		MimeType:       "image/png",
		Kind:           AttachmentKindImage,
		SizeBytes:      128,
		CiphertextSize: 144,
		Nonce:          []byte{1, 2, 3},
		SHA256:         []byte{9, 9, 9},
		UploadedAt:     now.Add(time.Second),
	})
	if err != nil {
		t.Fatalf("CompleteMedia() error = %v", err)
	}
	if !media.Uploaded || media.CiphertextSize != 144 {
		t.Fatalf("CompleteMedia() = %+v", media)
	}

	msg, err := s.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "alice",
		To:             "bob",
		Attachments: []Attachment{{
			AttachmentID:        "att-1",
			Kind:                AttachmentKindImage,
			Filename:            "cat.png",
			MimeType:            "image/png",
			SizeBytes:           128,
			MediaID:             "media-1",
			EncryptedDescriptor: []byte{7, 7, 7},
			DescriptorNonce:     []byte{8, 8, 8},
			SHA256:              []byte{9, 9, 9},
			CiphertextSize:      144,
			PreviewWidth:        320,
			PreviewHeight:       240,
		}},
		TS: now.Add(2 * time.Second),
	})
	if err != nil {
		t.Fatalf("Save(attachment-only) error = %v", err)
	}
	if len(msg.Attachments) != 1 {
		t.Fatalf("Save(attachment-only) attachments len = %d, want 1", len(msg.Attachments))
	}

	history, err := s.History(ctx, "alice|bob", 10)
	if err != nil {
		t.Fatalf("History() error = %v", err)
	}
	if len(history) != 1 || len(history[0].Attachments) != 1 || history[0].Attachments[0].MediaID != "media-1" {
		t.Fatalf("History() = %+v", history)
	}

	canAccess, err := s.CanAccessMedia(ctx, "alice", "media-1")
	if err != nil {
		t.Fatalf("CanAccessMedia(alice) error = %v", err)
	}
	if !canAccess {
		t.Fatal("CanAccessMedia(alice) = false, want true")
	}

	mediaItems, err := s.ListMediaByOwner(ctx, "alice")
	if err != nil {
		t.Fatalf("ListMediaByOwner() error = %v", err)
	}
	if len(mediaItems) != 1 || mediaItems[0].MediaID != "media-1" {
		t.Fatalf("ListMediaByOwner() = %+v", mediaItems)
	}
}
