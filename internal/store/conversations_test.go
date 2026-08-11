package store

import (
	"context"
	"testing"
)

func TestMemoryConversationStoreCreateAndListGroupConversations(t *testing.T) {
	t.Parallel()

	store := NewMemoryConversationStore()
	ctx := context.Background()

	group, err := store.CreateGroupConversation(ctx, Conversation{
		ID:    "group-1",
		Kind:  ConversationKindGroup,
		Title: "Diploma",
		Members: []ConversationMember{
			{Username: "alice", Role: ConversationRoleAdmin},
			{Username: "bob", Role: ConversationRoleMember, AddedBy: "alice"},
			{Username: "carol", Role: ConversationRoleMember, AddedBy: "alice"},
		},
	})
	if err != nil {
		t.Fatalf("CreateGroupConversation() error = %v", err)
	}
	if group.Kind != ConversationKindGroup || group.Title != "Diploma" {
		t.Fatalf("CreateGroupConversation() = %+v", group)
	}

	items, err := store.ListGroupConversationsForUser(ctx, "bob")
	if err != nil {
		t.Fatalf("ListGroupConversationsForUser() error = %v", err)
	}
	if len(items) != 1 || items[0].ID != "group-1" {
		t.Fatalf("ListGroupConversationsForUser() = %+v", items)
	}

	got, err := store.GetConversation(ctx, "group-1")
	if err != nil {
		t.Fatalf("GetConversation() error = %v", err)
	}
	if len(got.Members) != 3 || got.Members[2].Username != "carol" || got.Members[0].Role != ConversationRoleAdmin {
		t.Fatalf("GetConversation() = %+v", got)
	}
}

func TestMemoryConversationStoreDeleteUserRemovesMembership(t *testing.T) {
	t.Parallel()

	store := NewMemoryConversationStore()
	ctx := context.Background()

	if _, err := store.CreateGroupConversation(ctx, Conversation{
		ID:    "group-1",
		Kind:  ConversationKindGroup,
		Title: "Diploma",
		Members: []ConversationMember{
			{Username: "alice", Role: ConversationRoleAdmin},
			{Username: "bob", Role: ConversationRoleMember, AddedBy: "alice"},
		},
	}); err != nil {
		t.Fatalf("CreateGroupConversation() error = %v", err)
	}

	if err := store.DeleteUser(ctx, "bob"); err != nil {
		t.Fatalf("DeleteUser() error = %v", err)
	}

	items, err := store.ListGroupConversationsForUser(ctx, "bob")
	if err != nil {
		t.Fatalf("ListGroupConversationsForUser() error = %v", err)
	}
	if len(items) != 0 {
		t.Fatalf("ListGroupConversationsForUser() len = %d, want 0", len(items))
	}

	group, err := store.GetConversation(ctx, "group-1")
	if err != nil {
		t.Fatalf("GetConversation() error = %v", err)
	}
	if len(group.Members) != 1 || group.Members[0].Username != "alice" || group.Members[0].Role != ConversationRoleAdmin {
		t.Fatalf("GetConversation() after DeleteUser = %+v", group)
	}
}

func TestMemoryConversationStoreAddAndRemoveMembers(t *testing.T) {
	t.Parallel()

	store := NewMemoryConversationStore()
	ctx := context.Background()

	if _, err := store.CreateGroupConversation(ctx, Conversation{
		ID:    "group-1",
		Kind:  ConversationKindGroup,
		Title: "Diploma",
		Members: []ConversationMember{
			{Username: "alice", Role: ConversationRoleAdmin},
			{Username: "bob", Role: ConversationRoleMember, AddedBy: "alice"},
		},
	}); err != nil {
		t.Fatalf("CreateGroupConversation() error = %v", err)
	}

	group, err := store.AddMembers(ctx, "group-1", "bob", []string{"carol", "bob"})
	if err != nil {
		t.Fatalf("AddMembers() error = %v", err)
	}
	if len(group.Members) != 3 || group.Members[2].AddedBy != "bob" {
		t.Fatalf("AddMembers() members = %+v", group.Members)
	}

	group, err = store.RemoveMember(ctx, "group-1", "bob", "carol")
	if err != nil {
		t.Fatalf("RemoveMember() error = %v", err)
	}
	if len(group.Members) != 2 || group.Members[0].Username != "alice" || group.Members[1].Username != "bob" {
		t.Fatalf("RemoveMember() = %+v", group)
	}
}

func TestMemoryConversationStoreLeaveConversationTransfersAdmin(t *testing.T) {
	t.Parallel()

	store := NewMemoryConversationStore()
	ctx := context.Background()

	if _, err := store.CreateGroupConversation(ctx, Conversation{
		ID:    "group-1",
		Kind:  ConversationKindGroup,
		Title: "Diploma",
		Members: []ConversationMember{
			{Username: "alice", Role: ConversationRoleAdmin},
			{Username: "bob", Role: ConversationRoleMember, AddedBy: "alice"},
			{Username: "carol", Role: ConversationRoleMember, AddedBy: "alice"},
		},
	}); err != nil {
		t.Fatalf("CreateGroupConversation() error = %v", err)
	}

	group, err := store.LeaveConversation(ctx, "group-1", "alice")
	if err != nil {
		t.Fatalf("LeaveConversation() error = %v", err)
	}
	if len(group.Members) != 2 {
		t.Fatalf("LeaveConversation() members = %+v", group.Members)
	}
	if group.Members[0].Username != "bob" || group.Members[0].Role != ConversationRoleAdmin {
		t.Fatalf("LeaveConversation() admin = %+v", group.Members)
	}
}

func TestMemoryConversationStoreTransferAdmin(t *testing.T) {
	t.Parallel()

	store := NewMemoryConversationStore()
	ctx := context.Background()

	if _, err := store.CreateGroupConversation(ctx, Conversation{
		ID:    "group-1",
		Kind:  ConversationKindGroup,
		Title: "Diploma",
		Members: []ConversationMember{
			{Username: "alice", Role: ConversationRoleAdmin},
			{Username: "bob", Role: ConversationRoleMember, AddedBy: "alice"},
		},
	}); err != nil {
		t.Fatalf("CreateGroupConversation() error = %v", err)
	}

	group, err := store.TransferAdmin(ctx, "group-1", "bob")
	if err != nil {
		t.Fatalf("TransferAdmin() error = %v", err)
	}
	if group.Members[0].Role != ConversationRoleMember || group.Members[1].Role != ConversationRoleAdmin {
		t.Fatalf("TransferAdmin() = %+v", group.Members)
	}
}
