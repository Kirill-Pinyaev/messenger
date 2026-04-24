package store

import (
	"context"
	"strings"
)

type GroupStateStore interface {
	CreateConversationWithKey(ctx context.Context, conversation Conversation, key ConversationKey) (Conversation, ConversationKey, error)
	AddMembersWithKey(ctx context.Context, conversationID, addedBy string, usernames []string, key ConversationKey) (Conversation, ConversationKey, error)
	RemoveMemberWithKey(ctx context.Context, conversationID, actor, username string, key ConversationKey) (Conversation, ConversationKey, error)
	LeaveConversationWithKey(ctx context.Context, conversationID, username string, key *ConversationKey) (Conversation, *ConversationKey, error)
}

func NewGroupStateStore(convStore ConversationStore, keyStore KeyStore) GroupStateStore {
	switch conv := convStore.(type) {
	case *MemoryConversationStore:
		if keys, ok := keyStore.(*MemoryKeyStore); ok {
			return &MemoryGroupStateStore{convStore: conv, keyStore: keys}
		}
	case *PostgresConversationStore:
		if keys, ok := keyStore.(*PostgresKeyStore); ok && conv.pool == keys.pool {
			return &PostgresGroupStateStore{pool: conv.pool}
		}
	}
	return nil
}

func validateGroupKeyMembers(conversation Conversation, key ConversationKey) error {
	if err := validateConversationKey(key); err != nil {
		return err
	}
	if strings.TrimSpace(key.ConversationID) != strings.TrimSpace(conversation.ID) {
		return ErrBadInput
	}

	expected := map[string]struct{}{}
	for _, username := range conversation.MemberUsernames() {
		expected[username] = struct{}{}
	}
	if len(expected) != len(key.Envelopes) {
		return ErrBadInput
	}
	for _, envelope := range key.Envelopes {
		if _, ok := expected[envelope.Username]; !ok {
			return ErrBadInput
		}
		delete(expected, envelope.Username)
	}
	if len(expected) != 0 {
		return ErrBadInput
	}
	return nil
}

type MemoryGroupStateStore struct {
	convStore *MemoryConversationStore
	keyStore  *MemoryKeyStore
}

func (s *MemoryGroupStateStore) CreateConversationWithKey(ctx context.Context, conversation Conversation, key ConversationKey) (Conversation, ConversationKey, error) {
	conversation, err := s.convStore.CreateGroupConversation(ctx, conversation)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	key.ConversationID = conversation.ID
	if err := validateGroupKeyMembers(conversation, key); err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	groupKey, err := s.keyStore.UpsertConversationKey(ctx, key)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	return conversation, groupKey, nil
}

func (s *MemoryGroupStateStore) AddMembersWithKey(ctx context.Context, conversationID, addedBy string, usernames []string, key ConversationKey) (Conversation, ConversationKey, error) {
	conversation, err := s.convStore.AddMembers(ctx, conversationID, addedBy, usernames)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	if err := validateGroupKeyMembers(conversation, key); err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	groupKey, err := s.keyStore.UpsertConversationKey(ctx, key)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	return conversation, groupKey, nil
}

func (s *MemoryGroupStateStore) RemoveMemberWithKey(ctx context.Context, conversationID, actor, username string, key ConversationKey) (Conversation, ConversationKey, error) {
	conversation, err := s.convStore.RemoveMember(ctx, conversationID, actor, username)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	if err := validateGroupKeyMembers(conversation, key); err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	groupKey, err := s.keyStore.UpsertConversationKey(ctx, key)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	return conversation, groupKey, nil
}

func (s *MemoryGroupStateStore) LeaveConversationWithKey(ctx context.Context, conversationID, username string, key *ConversationKey) (Conversation, *ConversationKey, error) {
	conversation, err := s.convStore.LeaveConversation(ctx, conversationID, username)
	if err != nil {
		return Conversation{}, nil, err
	}
	if len(conversation.Members) == 0 {
		return conversation, nil, nil
	}
	if key == nil {
		return Conversation{}, nil, ErrBadInput
	}
	if err := validateGroupKeyMembers(conversation, *key); err != nil {
		return Conversation{}, nil, err
	}
	groupKey, err := s.keyStore.UpsertConversationKey(ctx, *key)
	if err != nil {
		return Conversation{}, nil, err
	}
	return conversation, &groupKey, nil
}
