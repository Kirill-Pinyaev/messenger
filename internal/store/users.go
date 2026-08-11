package store

import (
	"context"
	"errors"
	"sort"
	"strings"
	"sync"
	"time"
)

var (
	ErrUserExists   = errors.New("user already exists")
	ErrUserNotFound = errors.New("user not found")
)

type User struct {
	Username  string
	FirstName string
	LastName  string
	AvatarHex string
	AvatarData string
	Salt      []byte
	Hash      []byte
	CreatedAt time.Time
}

type UserStore interface {
	Create(ctx context.Context, user User) error
	Get(ctx context.Context, username string) (User, error)
	Delete(ctx context.Context, username string) error
	Search(ctx context.Context, query string, limit int) ([]User, error)
	GetMany(ctx context.Context, usernames []string) (map[string]User, error)
	UpdateProfile(ctx context.Context, username, firstName, lastName, avatarHex, avatarData string) (User, error)
}

type MemoryUserStore struct {
	mu    sync.RWMutex
	users map[string]User
}

func NewMemoryUserStore() *MemoryUserStore {
	return &MemoryUserStore{
		users: make(map[string]User),
	}
}

func (s *MemoryUserStore) Create(_ context.Context, user User) error {
	if user.Username == "" || len(user.Hash) == 0 || len(user.Salt) == 0 {
		return ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	if _, exists := s.users[user.Username]; exists {
		return ErrUserExists
	}

	s.users[user.Username] = user
	return nil
}

func (s *MemoryUserStore) Get(_ context.Context, username string) (User, error) {
	if username == "" {
		return User{}, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	user, ok := s.users[username]
	if !ok {
		return User{}, ErrUserNotFound
	}
	return user, nil
}

func (s *MemoryUserStore) Delete(_ context.Context, username string) error {
	if username == "" {
		return ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	if _, ok := s.users[username]; !ok {
		return ErrUserNotFound
	}
	delete(s.users, username)
	return nil
}

func (s *MemoryUserStore) Search(_ context.Context, query string, limit int) ([]User, error) {
	query = strings.TrimSpace(query)
	if query == "" {
		return nil, ErrBadInput
	}
	if limit <= 0 {
		limit = 20
	}

	needle := strings.ToLower(query)

	s.mu.RLock()
	defer s.mu.RUnlock()

	var out []User
	for _, user := range s.users {
		if strings.Contains(strings.ToLower(user.Username), needle) {
			out = append(out, user)
		}
	}
	sort.Slice(out, func(i, j int) bool {
		return out[i].Username < out[j].Username
	})
	if len(out) > limit {
		out = out[:limit]
	}
	return out, nil
}

func (s *MemoryUserStore) GetMany(_ context.Context, usernames []string) (map[string]User, error) {
	out := make(map[string]User, len(usernames))
	if len(usernames) == 0 {
		return out, nil
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	for _, username := range usernames {
		if user, ok := s.users[username]; ok {
			out[username] = user
		}
	}
	return out, nil
}

func (s *MemoryUserStore) UpdateProfile(_ context.Context, username, firstName, lastName, avatarHex, avatarData string) (User, error) {
	if username == "" {
		return User{}, ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	user, ok := s.users[username]
	if !ok {
		return User{}, ErrUserNotFound
	}
	if firstName != "" {
		user.FirstName = firstName
	}
	if lastName != "" {
		user.LastName = lastName
	}
	if avatarHex != "" {
		user.AvatarHex = avatarHex
	}
	if avatarData != "" {
		user.AvatarData = avatarData
	}
	s.users[username] = user
	return user, nil
}
