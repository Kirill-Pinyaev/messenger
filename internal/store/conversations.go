package store

import (
	"context"
	"errors"
	"slices"
	"strings"
	"sync"
)

var (
	ErrConversationNotFound  = errors.New("conversation not found")
	ErrConversationForbidden = errors.New("conversation forbidden")
)

type ConversationKind int

const (
	ConversationKindDirect ConversationKind = iota + 1
	ConversationKindGroup
)

type ConversationRole int

const (
	ConversationRoleAdmin ConversationRole = iota + 1
	ConversationRoleMember
)

type ConversationMember struct {
	Username    string
	Role        ConversationRole
	AddedBy     string
	JoinedOrder int64
}

type Conversation struct {
	ID      string
	Kind    ConversationKind
	Title   string
	Members []ConversationMember
}

func (c Conversation) MemberUsernames() []string {
	items := make([]string, 0, len(c.Members))
	for _, member := range c.Members {
		items = append(items, member.Username)
	}
	return items
}

func (c Conversation) HasMember(username string) bool {
	_, ok := c.Member(username)
	return ok
}

func (c Conversation) Member(username string) (ConversationMember, bool) {
	for _, member := range c.Members {
		if member.Username == username {
			return member, true
		}
	}
	return ConversationMember{}, false
}

type ConversationStore interface {
	CreateGroupConversation(ctx context.Context, conversation Conversation) (Conversation, error)
	GetConversation(ctx context.Context, conversationID string) (Conversation, error)
	ListGroupConversationsForUser(ctx context.Context, username string) ([]Conversation, error)
	AddMembers(ctx context.Context, conversationID, addedBy string, usernames []string) (Conversation, error)
	RemoveMember(ctx context.Context, conversationID, actor, username string) (Conversation, error)
	LeaveConversation(ctx context.Context, conversationID, username string) (Conversation, error)
	TransferAdmin(ctx context.Context, conversationID, username string) (Conversation, error)
	DeleteUser(ctx context.Context, username string) error
}

type MemoryConversationStore struct {
	mu            sync.RWMutex
	conversations map[string]Conversation
}

func NewMemoryConversationStore() *MemoryConversationStore {
	return &MemoryConversationStore{
		conversations: make(map[string]Conversation),
	}
}

func (s *MemoryConversationStore) CreateGroupConversation(_ context.Context, conversation Conversation) (Conversation, error) {
	conversation, err := normalizeGroupConversation(conversation)
	if err != nil {
		return Conversation{}, err
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	s.conversations[conversation.ID] = conversation
	return cloneConversation(conversation), nil
}

func (s *MemoryConversationStore) GetConversation(_ context.Context, conversationID string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" {
		return Conversation{}, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	conversation, ok := s.conversations[conversationID]
	if !ok {
		return Conversation{}, ErrConversationNotFound
	}
	return cloneConversation(conversation), nil
}

func (s *MemoryConversationStore) ListGroupConversationsForUser(_ context.Context, username string) ([]Conversation, error) {
	if strings.TrimSpace(username) == "" {
		return nil, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	var out []Conversation
	for _, conversation := range s.conversations {
		if conversation.Kind != ConversationKindGroup || !conversation.HasMember(username) {
			continue
		}
		out = append(out, cloneConversation(conversation))
	}
	slices.SortFunc(out, func(a, b Conversation) int {
		return strings.Compare(a.ID, b.ID)
	})
	return out, nil
}

func (s *MemoryConversationStore) AddMembers(_ context.Context, conversationID, addedBy string, usernames []string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" || strings.TrimSpace(addedBy) == "" || len(usernames) == 0 {
		return Conversation{}, ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	conversation, ok := s.conversations[conversationID]
	if !ok {
		return Conversation{}, ErrConversationNotFound
	}
	if !conversation.HasMember(addedBy) {
		return Conversation{}, ErrConversationForbidden
	}

	memberSet := make(map[string]struct{}, len(conversation.Members))
	var maxOrder int64
	for _, member := range conversation.Members {
		memberSet[member.Username] = struct{}{}
		if member.JoinedOrder > maxOrder {
			maxOrder = member.JoinedOrder
		}
	}
	for _, username := range usernames {
		username = strings.TrimSpace(username)
		if username == "" {
			return Conversation{}, ErrBadInput
		}
		if _, ok := memberSet[username]; ok {
			continue
		}
		maxOrder++
		memberSet[username] = struct{}{}
		conversation.Members = append(conversation.Members, ConversationMember{
			Username:    username,
			Role:        ConversationRoleMember,
			AddedBy:     addedBy,
			JoinedOrder: maxOrder,
		})
	}

	s.conversations[conversationID] = conversation
	return cloneConversation(conversation), nil
}

func (s *MemoryConversationStore) RemoveMember(_ context.Context, conversationID, actor, username string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" || strings.TrimSpace(actor) == "" || strings.TrimSpace(username) == "" {
		return Conversation{}, ErrBadInput
	}
	if actor == username {
		return Conversation{}, ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	conversation, ok := s.conversations[conversationID]
	if !ok {
		return Conversation{}, ErrConversationNotFound
	}
	if err := ensureCanRemove(conversation, actor, username); err != nil {
		return Conversation{}, err
	}

	conversation.Members = removeConversationMember(conversation.Members, username)
	s.conversations[conversationID] = conversation
	return cloneConversation(conversation), nil
}

func (s *MemoryConversationStore) LeaveConversation(_ context.Context, conversationID, username string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" || strings.TrimSpace(username) == "" {
		return Conversation{}, ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	conversation, ok := s.conversations[conversationID]
	if !ok {
		return Conversation{}, ErrConversationNotFound
	}

	member, ok := conversation.Member(username)
	if !ok {
		return Conversation{}, ErrConversationForbidden
	}

	conversation.Members = removeConversationMember(conversation.Members, username)
	if member.Role == ConversationRoleAdmin {
		promoteNextAdmin(conversation.Members)
	}
	if len(conversation.Members) == 0 {
		delete(s.conversations, conversationID)
		return Conversation{ID: conversationID, Kind: ConversationKindGroup, Title: conversation.Title}, nil
	}

	s.conversations[conversationID] = conversation
	return cloneConversation(conversation), nil
}

func (s *MemoryConversationStore) TransferAdmin(_ context.Context, conversationID, username string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" || strings.TrimSpace(username) == "" {
		return Conversation{}, ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	conversation, ok := s.conversations[conversationID]
	if !ok {
		return Conversation{}, ErrConversationNotFound
	}
	if _, ok := conversation.Member(username); !ok {
		return Conversation{}, ErrBadInput
	}

	for i := range conversation.Members {
		if conversation.Members[i].Role == ConversationRoleAdmin {
			conversation.Members[i].Role = ConversationRoleMember
		}
		if conversation.Members[i].Username == username {
			conversation.Members[i].Role = ConversationRoleAdmin
		}
	}

	s.conversations[conversationID] = conversation
	return cloneConversation(conversation), nil
}

func (s *MemoryConversationStore) DeleteUser(_ context.Context, username string) error {
	if strings.TrimSpace(username) == "" {
		return ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	for id, conversation := range s.conversations {
		member, ok := conversation.Member(username)
		if !ok {
			continue
		}
		conversation.Members = removeConversationMember(conversation.Members, username)
		if member.Role == ConversationRoleAdmin {
			promoteNextAdmin(conversation.Members)
		}
		if len(conversation.Members) == 0 {
			delete(s.conversations, id)
			continue
		}
		s.conversations[id] = conversation
	}
	return nil
}

func normalizeGroupConversation(conversation Conversation) (Conversation, error) {
	conversation.ID = strings.TrimSpace(conversation.ID)
	conversation.Title = strings.TrimSpace(conversation.Title)
	if conversation.ID == "" || conversation.Title == "" || len(conversation.Members) < 2 {
		return Conversation{}, ErrBadInput
	}

	memberSet := make(map[string]struct{}, len(conversation.Members))
	adminCount := 0
	normalized := make([]ConversationMember, 0, len(conversation.Members))
	for index, member := range conversation.Members {
		member.Username = strings.TrimSpace(member.Username)
		member.AddedBy = strings.TrimSpace(member.AddedBy)
		if member.Username == "" {
			return Conversation{}, ErrBadInput
		}
		if _, ok := memberSet[member.Username]; ok {
			continue
		}
		memberSet[member.Username] = struct{}{}
		if member.Role == 0 {
			if index == 0 {
				member.Role = ConversationRoleAdmin
			} else {
				member.Role = ConversationRoleMember
			}
		}
		if member.Role == ConversationRoleAdmin {
			adminCount++
		}
		if member.Role == ConversationRoleMember && member.AddedBy == "" {
			member.AddedBy = normalized[0].Username
		}
		if member.JoinedOrder == 0 {
			member.JoinedOrder = int64(index + 1)
		}
		normalized = append(normalized, member)
	}
	if len(normalized) < 2 || adminCount != 1 {
		return Conversation{}, ErrBadInput
	}

	conversation.Kind = ConversationKindGroup
	conversation.Members = normalized
	return conversation, nil
}

func cloneConversation(conversation Conversation) Conversation {
	conversation.Members = append([]ConversationMember(nil), conversation.Members...)
	return conversation
}

func removeConversationMember(members []ConversationMember, username string) []ConversationMember {
	filtered := make([]ConversationMember, 0, len(members))
	for _, member := range members {
		if member.Username != username {
			filtered = append(filtered, member)
		}
	}
	return filtered
}

func promoteNextAdmin(members []ConversationMember) {
	if len(members) == 0 {
		return
	}
	for i := range members {
		members[i].Role = ConversationRoleMember
	}
	members[0].Role = ConversationRoleAdmin
}

func ensureCanRemove(conversation Conversation, actor, target string) error {
	actorMember, ok := conversation.Member(actor)
	if !ok {
		return ErrConversationForbidden
	}
	targetMember, ok := conversation.Member(target)
	if !ok {
		return ErrBadInput
	}
	if actorMember.Role == ConversationRoleAdmin {
		return nil
	}
	if targetMember.Role == ConversationRoleAdmin {
		return ErrConversationForbidden
	}
	if targetMember.AddedBy != actor {
		return ErrConversationForbidden
	}
	return nil
}
