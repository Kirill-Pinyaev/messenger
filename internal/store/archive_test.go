package store

import (
	"context"
	"testing"
	"time"
)

func TestMemoryArchiveStoreHeaderAndRecords(t *testing.T) {
	t.Parallel()

	ctx := context.Background()
	store := NewMemoryArchiveStore()

	header, err := store.InitializeHeader(ctx, ArchiveKeyBundle{
		Username:              "alice",
		PublicKey:             []byte{1, 2, 3},
		EncryptedPrivateKey:   []byte{4, 5, 6},
		KDFSalt:               []byte{7, 8, 9},
		KDFParams:             `{"name":"PBKDF2","iterations":100000}`,
		Version:               1,
	})
	if err != nil {
		t.Fatalf("InitializeHeader() error = %v", err)
	}
	if header.Username != "alice" || header.Version != 1 {
		t.Fatalf("InitializeHeader() = %+v", header)
	}

	gotHeader, err := store.GetHeader(ctx, "alice")
	if err != nil {
		t.Fatalf("GetHeader() error = %v", err)
	}
	if gotHeader.KDFParams == "" || len(gotHeader.PublicKey) != 3 {
		t.Fatalf("GetHeader() = %+v", gotHeader)
	}

	if err := store.AppendRecords(ctx, []HistoryArchiveRecord{
		{
			OwnerUsername:      "alice",
			RecordID:           "r1",
			ConversationID:     "alice|bob",
			RecordType:         HistoryArchiveRecordTypeDirectMessage,
			MessageID:          1,
			Sender:             "alice",
			Ciphertext:         []byte{10, 11},
			Nonce:              []byte{12, 13},
			EphemeralPublicKey: []byte{14, 15},
			ArchiveKeyVersion:  1,
			CreatedAt:          time.Now().UTC(),
		},
		{
			OwnerUsername:      "alice",
			RecordID:           "r2",
			ConversationID:     "group-1",
			RecordType:         HistoryArchiveRecordTypeGroupKeyVersion,
			GroupKeyVersion:    2,
			Sender:             "alice",
			Ciphertext:         []byte{16, 17},
			Nonce:              []byte{18, 19},
			EphemeralPublicKey: []byte{20, 21},
			ArchiveKeyVersion:  1,
			CreatedAt:          time.Now().UTC().Add(time.Second),
		},
	}); err != nil {
		t.Fatalf("AppendRecords() error = %v", err)
	}

	records, err := store.ListRecords(ctx, "alice", 0, 10)
	if err != nil {
		t.Fatalf("ListRecords() error = %v", err)
	}
	if len(records) != 2 || records[0].Sequence == 0 || records[1].Sequence <= records[0].Sequence {
		t.Fatalf("ListRecords() = %+v", records)
	}

	next, err := store.ListRecords(ctx, "alice", records[0].Sequence, 10)
	if err != nil {
		t.Fatalf("ListRecords(after) error = %v", err)
	}
	if len(next) != 1 || next[0].RecordID != "r2" {
		t.Fatalf("ListRecords(after) = %+v", next)
	}

	keys, err := store.GetPublicKeys(ctx, []string{"alice", "bob"})
	if err != nil {
		t.Fatalf("GetPublicKeys() error = %v", err)
	}
	if len(keys) != 1 || keys[0].Username != "alice" {
		t.Fatalf("GetPublicKeys() = %+v", keys)
	}
}
