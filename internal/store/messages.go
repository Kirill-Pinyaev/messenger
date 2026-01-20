package store

import (
	"context"
	"errors"
	"strings"
	"sync"
	"time"
)

var ErrBadInput = errors.New("bad input")
var ErrMessageNotFound = errors.New("message not found")

type Message struct {
	ID             int64
	ConversationID string
	From           string
	To             string
	Text           string
	TS             time.Time
}

type MessageStore interface {
	Save(ctx context.Context, msg Message) (Message, error)
	History(ctx context.Context, conversationID string, limit int) ([]Message, error)
	DeleteUser(ctx context.Context, username string) error
	Conversations(ctx context.Context, username string) ([]string, error)
	SearchMessages(ctx context.Context, username, query string, limit int) ([]Message, error)
	GetByID(ctx context.Context, id int64) (Message, error)
	DeleteMessage(ctx context.Context, id int64) error
}

type MemoryMessageStore struct {
	mu     sync.RWMutex
	nextID int64
	byConv map[string][]Message
}

func NewMemoryMessageStore() *MemoryMessageStore {
	return &MemoryMessageStore{
		byConv: make(map[string][]Message),
	}
}

func (s *MemoryMessageStore) Save(_ context.Context, msg Message) (Message, error) {
	if msg.ConversationID == "" || msg.From == "" || msg.To == "" {
		return Message{}, ErrBadInput
	}
	s.mu.Lock()
	defer s.mu.Unlock()

	s.nextID++
	msg.ID = s.nextID
	s.byConv[msg.ConversationID] = append(s.byConv[msg.ConversationID], msg)
	return msg, nil
}

func (s *MemoryMessageStore) History(_ context.Context, conversationID string, limit int) ([]Message, error) {
	if conversationID == "" {
		return nil, ErrBadInput
	}
	if limit <= 0 {
		limit = 50
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	msgs := s.byConv[conversationID]
	if len(msgs) == 0 {
		return nil, nil
	}

	if limit > len(msgs) {
		limit = len(msgs)
	}

	out := make([]Message, limit)
	copy(out, msgs[len(msgs)-limit:])
	return out, nil
}

func (s *MemoryMessageStore) DeleteUser(_ context.Context, username string) error {
	if username == "" {
		return ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	for convID, msgs := range s.byConv {
		filtered := make([]Message, 0, len(msgs))
		for _, msg := range msgs {
			if msg.From != username && msg.To != username {
				filtered = append(filtered, msg)
			}
		}
		if len(filtered) == 0 {
			delete(s.byConv, convID)
			continue
		}
		s.byConv[convID] = filtered
	}
	return nil
}

func (s *MemoryMessageStore) Conversations(_ context.Context, username string) ([]string, error) {
	if username == "" {
		return nil, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	convSet := make(map[string]struct{})
	for convID, msgs := range s.byConv {
		for _, msg := range msgs {
			if msg.From == username || msg.To == username {
				convSet[convID] = struct{}{}
				break
			}
		}
	}

	out := make([]string, 0, len(convSet))
	for convID := range convSet {
		out = append(out, convID)
	}
	return out, nil
}

func (s *MemoryMessageStore) SearchMessages(_ context.Context, username, query string, limit int) ([]Message, error) {
	if username == "" || strings.TrimSpace(query) == "" {
		return nil, ErrBadInput
	}
	if limit <= 0 {
		limit = 50
	}

	needle := strings.ToLower(query)

	s.mu.RLock()
	defer s.mu.RUnlock()

	var out []Message
	for _, msgs := range s.byConv {
		for i := len(msgs) - 1; i >= 0; i-- {
			msg := msgs[i]
			if msg.From != username && msg.To != username {
				continue
			}
			if strings.Contains(strings.ToLower(msg.Text), needle) {
				out = append(out, msg)
				if len(out) >= limit {
					return out, nil
				}
			}
		}
	}
	return out, nil
}

func (s *MemoryMessageStore) GetByID(_ context.Context, id int64) (Message, error) {
	if id <= 0 {
		return Message{}, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	for _, msgs := range s.byConv {
		for _, msg := range msgs {
			if msg.ID == id {
				return msg, nil
			}
		}
	}
	return Message{}, ErrMessageNotFound
}

func (s *MemoryMessageStore) DeleteMessage(_ context.Context, id int64) error {
	if id <= 0 {
		return ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	for convID, msgs := range s.byConv {
		for i, msg := range msgs {
			if msg.ID == id {
				msgs = append(msgs[:i], msgs[i+1:]...)
				if len(msgs) == 0 {
					delete(s.byConv, convID)
				} else {
					s.byConv[convID] = msgs
				}
				return nil
			}
		}
	}
	return ErrMessageNotFound
}
