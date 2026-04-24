//go:build integration

package store

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/testcontainers/testcontainers-go"
	tcpostgres "github.com/testcontainers/testcontainers-go/modules/postgres"
)

func TestPostgresStoresIntegration(t *testing.T) {
	t.Parallel()

	ctx := context.Background()
	databaseURL := startPostgresContainer(t, ctx)

	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		t.Fatalf("pgxpool.New() error = %v", err)
	}
	t.Cleanup(pool.Close)

	userStore, err := NewPostgresUserStore(ctx, pool)
	if err != nil {
		t.Fatalf("NewPostgresUserStore() error = %v", err)
	}

	messageStore, err := NewPostgresMessageStore(ctx, pool)
	if err != nil {
		t.Fatalf("NewPostgresMessageStore() error = %v", err)
	}

	conversationStore, err := NewPostgresConversationStore(ctx, pool)
	if err != nil {
		t.Fatalf("NewPostgresConversationStore() error = %v", err)
	}

	if _, err := pool.Exec(ctx, "TRUNCATE TABLE conversation_members, conversations, messages, users RESTART IDENTITY"); err != nil {
		t.Fatalf("TRUNCATE error = %v", err)
	}

	alice := User{
		Username:   "alice",
		FirstName:  "Alice",
		LastName:   "Doe",
		AvatarHex:  "112233",
		AvatarData: "avatar-a",
		Salt:       []byte{1, 2, 3},
		Hash:       []byte{4, 5, 6},
		CreatedAt:  time.Now().UTC().Truncate(time.Second),
	}
	bob := User{
		Username:   "bob",
		FirstName:  "Bob",
		LastName:   "Smith",
		AvatarHex:  "445566",
		AvatarData: "avatar-b",
		Salt:       []byte{7, 8, 9},
		Hash:       []byte{10, 11, 12},
		CreatedAt:  time.Now().UTC().Truncate(time.Second),
	}

	for _, user := range []User{alice, bob} {
		if err := userStore.Create(ctx, user); err != nil {
			t.Fatalf("Create(%q) error = %v", user.Username, err)
		}
	}
	if err := userStore.Create(ctx, alice); !errors.Is(err, ErrUserExists) {
		t.Fatalf("duplicate Create() error = %v, want %v", err, ErrUserExists)
	}

	gotAlice, err := userStore.Get(ctx, "alice")
	if err != nil {
		t.Fatalf("Get(alice) error = %v", err)
	}
	if gotAlice.FirstName != "Alice" || gotAlice.AvatarData != "avatar-a" {
		t.Fatalf("Get(alice) = %+v", gotAlice)
	}

	search, err := userStore.Search(ctx, "o", 10)
	if err != nil {
		t.Fatalf("Search() error = %v", err)
	}
	if len(search) != 1 {
		t.Fatalf("Search() len = %d, want 1", len(search))
	}
	if search[0].Username != "bob" {
		t.Fatalf("Search() first result = %q, want %q", search[0].Username, "bob")
	}

	updatedAlice, err := userStore.UpdateProfile(ctx, "alice", "Alicia", "", "abcdef", "avatar-a2")
	if err != nil {
		t.Fatalf("UpdateProfile() error = %v", err)
	}
	if updatedAlice.FirstName != "Alicia" || updatedAlice.AvatarHex != "abcdef" || updatedAlice.AvatarData != "avatar-a2" {
		t.Fatalf("UpdateProfile() = %+v", updatedAlice)
	}

	many, err := userStore.GetMany(ctx, []string{"alice", "bob", "missing"})
	if err != nil {
		t.Fatalf("GetMany() error = %v", err)
	}
	if len(many) != 2 {
		t.Fatalf("GetMany() len = %d, want 2", len(many))
	}

	group, err := conversationStore.CreateGroupConversation(ctx, Conversation{
		ID:    "group-1",
		Kind:  ConversationKindGroup,
		Title: "Diploma",
		Members: []ConversationMember{
			{Username: "alice", Role: ConversationRoleAdmin},
			{Username: "bob", Role: ConversationRoleMember, AddedBy: "alice"},
		},
	})
	if err != nil {
		t.Fatalf("CreateGroupConversation() error = %v", err)
	}
	if group.Title != "Diploma" || len(group.Members) != 2 || group.Members[0].Role != ConversationRoleAdmin {
		t.Fatalf("CreateGroupConversation() = %+v", group)
	}

	groupItems, err := conversationStore.ListGroupConversationsForUser(ctx, "alice")
	if err != nil {
		t.Fatalf("ListGroupConversationsForUser() error = %v", err)
	}
	if len(groupItems) != 1 || groupItems[0].ID != "group-1" {
		t.Fatalf("ListGroupConversationsForUser() = %+v", groupItems)
	}

	group, err = conversationStore.AddMembers(ctx, "group-1", "bob", []string{"carol"})
	if err != nil {
		t.Fatalf("AddMembers() error = %v", err)
	}
	if len(group.Members) != 3 || group.Members[2].AddedBy != "bob" {
		t.Fatalf("AddMembers() = %+v", group)
	}

	group, err = conversationStore.RemoveMember(ctx, "group-1", "bob", "carol")
	if err != nil {
		t.Fatalf("RemoveMember() error = %v", err)
	}
	if len(group.Members) != 2 || group.Members[0].Username != "alice" || group.Members[1].Username != "bob" {
		t.Fatalf("RemoveMember() = %+v", group)
	}

	group, err = conversationStore.TransferAdmin(ctx, "group-1", "bob")
	if err != nil {
		t.Fatalf("TransferAdmin() error = %v", err)
	}
	if group.Members[0].Role != ConversationRoleMember || group.Members[1].Role != ConversationRoleAdmin {
		t.Fatalf("TransferAdmin() = %+v", group.Members)
	}

	group, err = conversationStore.LeaveConversation(ctx, "group-1", "bob")
	if err != nil {
		t.Fatalf("LeaveConversation() error = %v", err)
	}
	if len(group.Members) != 1 || group.Members[0].Username != "alice" || group.Members[0].Role != ConversationRoleAdmin {
		t.Fatalf("LeaveConversation() = %+v", group.Members)
	}

	first, err := messageStore.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "alice",
		To:             "bob",
		Text:           "hello",
		TS:             time.Now().UTC().Add(-time.Minute).Truncate(time.Second),
	})
	if err != nil {
		t.Fatalf("Save(first) error = %v", err)
	}
	second, err := messageStore.Save(ctx, Message{
		ConversationID: "alice|bob",
		From:           "bob",
		To:             "alice",
		Text:           "world",
		TS:             time.Now().UTC().Truncate(time.Second),
	})
	if err != nil {
		t.Fatalf("Save(second) error = %v", err)
	}

	history, err := messageStore.History(ctx, "alice|bob", 10)
	if err != nil {
		t.Fatalf("History() error = %v", err)
	}
	if len(history) != 2 || history[0].ID != first.ID || history[1].ID != second.ID {
		t.Fatalf("History() = %+v", history)
	}

	found, err := messageStore.SearchMessages(ctx, "alice", "wor", 10)
	if err != nil {
		t.Fatalf("SearchMessages() error = %v", err)
	}
	if len(found) != 1 || found[0].ID != second.ID {
		t.Fatalf("SearchMessages() = %+v", found)
	}

	// Группа: alice — единственный участник group-1 после ухода bob.
	// Проверяем, что SearchMessages находит сообщения в группе через subquery.
	groupMsg, err := messageStore.Save(ctx, Message{
		ConversationID: "group-1",
		From:           "alice",
		To:             "group-1",
		Text:           "grptest message",
		TS:             time.Now().UTC().Truncate(time.Second),
	})
	if err != nil {
		t.Fatalf("Save(groupMsg) error = %v", err)
	}
	groupFound, err := messageStore.SearchMessages(ctx, "alice", "grptest", 10)
	if err != nil {
		t.Fatalf("SearchMessages(group) error = %v", err)
	}
	if len(groupFound) != 1 || groupFound[0].ID != groupMsg.ID {
		t.Fatalf("SearchMessages(group) = %+v", groupFound)
	}

	keyStore, err := NewPostgresKeyStore(ctx, pool)
	if err != nil {
		t.Fatalf("NewPostgresKeyStore() error = %v", err)
	}

	identityKey, err := keyStore.UpsertIdentityKey(ctx, IdentityKey{
		Username:  "alice",
		KeyID:     "alice-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{1, 2, 3},
	})
	if err != nil {
		t.Fatalf("UpsertIdentityKey() error = %v", err)
	}
	if identityKey.KeyID != "alice-key-1" {
		t.Fatalf("UpsertIdentityKey() = %+v", identityKey)
	}

	conversationKey, err := keyStore.UpsertConversationKey(ctx, ConversationKey{
		ConversationID: "group-1",
		Version:        1,
		Algorithm:      "AES-GCM",
		CreatedBy:      "alice",
		Envelopes: []ConversationKeyEnvelope{
			{
				Username:       "alice",
				EncryptedKey:   []byte{1, 1, 1},
				Nonce:          []byte{2, 2, 2},
				SenderKeyID:    "alice-key-1",
				RecipientKeyID: "alice-key-1",
			},
		},
	})
	if err != nil {
		t.Fatalf("UpsertConversationKey() error = %v", err)
	}
	if conversationKey.Version != 1 {
		t.Fatalf("UpsertConversationKey() = %+v", conversationKey)
	}

	fetchedConversationKey, err := keyStore.GetConversationKey(ctx, "group-1", 1)
	if err != nil {
		t.Fatalf("GetConversationKey() error = %v", err)
	}
	if fetchedConversationKey.CreatedBy != "alice" || len(fetchedConversationKey.Envelopes) != 1 {
		t.Fatalf("GetConversationKey() = %+v", fetchedConversationKey)
	}

	conversations, err := messageStore.Conversations(ctx, "alice")
	if err != nil {
		t.Fatalf("Conversations() error = %v", err)
	}
	if len(conversations) != 2 {
		t.Fatalf("Conversations() len = %d, want 2", len(conversations))
	}

	gotMessage, err := messageStore.GetByID(ctx, second.ID)
	if err != nil {
		t.Fatalf("GetByID() error = %v", err)
	}
	if gotMessage.Text != "world" {
		t.Fatalf("GetByID().Text = %q, want %q", gotMessage.Text, "world")
	}

	if err := messageStore.DeleteMessage(ctx, first.ID); err != nil {
		t.Fatalf("DeleteMessage() error = %v", err)
	}
	if _, err := messageStore.GetByID(ctx, first.ID); !errors.Is(err, ErrMessageNotFound) {
		t.Fatalf("GetByID() after delete error = %v, want %v", err, ErrMessageNotFound)
	}

	if err := messageStore.DeleteUser(ctx, "bob"); err != nil {
		t.Fatalf("DeleteUser() error = %v", err)
	}
	if err := conversationStore.DeleteUser(ctx, "bob"); err != nil {
		t.Fatalf("ConversationStore.DeleteUser() error = %v", err)
	}
	history, err = messageStore.History(ctx, "alice|bob", 10)
	if err != nil {
		t.Fatalf("History() after DeleteUser error = %v", err)
	}
	if len(history) != 0 {
		t.Fatalf("History() after DeleteUser len = %d, want 0", len(history))
	}

	group, err = conversationStore.GetConversation(ctx, "group-1")
	if err != nil {
		t.Fatalf("GetConversation() after DeleteUser error = %v", err)
	}
	if len(group.Members) != 1 || group.Members[0].Username != "alice" || group.Members[0].Role != ConversationRoleAdmin {
		t.Fatalf("GetConversation() after DeleteUser = %+v", group)
	}

	if err := userStore.Delete(ctx, "alice"); err != nil {
		t.Fatalf("Delete(alice) error = %v", err)
	}
	if _, err := userStore.Get(ctx, "alice"); !errors.Is(err, ErrUserNotFound) {
		t.Fatalf("Get(alice) after delete error = %v, want %v", err, ErrUserNotFound)
	}
}

func startPostgresContainer(t *testing.T, ctx context.Context) string {
	t.Helper()

	container, err := tcpostgres.Run(
		ctx,
		"postgres:16-alpine",
		tcpostgres.WithDatabase("messenger"),
		tcpostgres.WithUsername("messenger"),
		tcpostgres.WithPassword("messenger"),
		tcpostgres.BasicWaitStrategies(),
	)
	if err != nil {
		t.Fatalf("postgres.Run() error = %v", err)
	}

	t.Cleanup(func() {
		if err := testcontainers.TerminateContainer(container); err != nil {
			t.Errorf("TerminateContainer() error = %v", err)
		}
	})

	databaseURL, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("ConnectionString() error = %v", err)
	}

	return databaseURL
}
