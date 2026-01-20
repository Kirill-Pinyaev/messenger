package auth

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"errors"
	"strings"
	"sync"
	"time"

	"messenger/internal/store"
)

var (
	ErrInvalidCredentials = errors.New("invalid credentials")
	ErrUserExists         = errors.New("user already exists")
	ErrBadInput           = errors.New("bad input")
)

type Service struct {
	mu     sync.RWMutex
	tokens map[string]string
	users  store.UserStore
}

func NewService(userStore store.UserStore) *Service {
	return &Service{
		tokens: make(map[string]string),
		users:  userStore,
	}
}

func (s *Service) Register(username, password, firstName, lastName, avatarHex string) error {
	username = strings.TrimSpace(username)
	if username == "" || password == "" {
		return ErrBadInput
	}
	firstName = strings.TrimSpace(firstName)
	lastName = strings.TrimSpace(lastName)

	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		return err
	}

	user := store.User{
		Username:  username,
		FirstName: firstName,
		LastName:  lastName,
		AvatarHex: avatarHex,
		Salt:      salt,
		Hash:      hashPassword(password, salt),
		CreatedAt: time.Now().UTC(),
	}
	if err := s.users.Create(context.Background(), user); err != nil {
		if errors.Is(err, store.ErrUserExists) {
			return ErrUserExists
		}
		return err
	}
	return nil
}

func (s *Service) Login(username, password string) (string, error) {
	username = strings.TrimSpace(username)
	if username == "" || password == "" {
		return "", ErrBadInput
	}

	rec, err := s.users.Get(context.Background(), username)
	if err != nil {
		if errors.Is(err, store.ErrUserNotFound) {
			return "", ErrInvalidCredentials
		}
		return "", err
	}
	if rec.Username == "" {
		return "", ErrInvalidCredentials
	}

	hash := hashPassword(password, rec.Salt)
	if subtle.ConstantTimeCompare(hash, rec.Hash) != 1 {
		return "", ErrInvalidCredentials
	}

	token, err := newToken()
	if err != nil {
		return "", err
	}

	s.mu.Lock()
	s.tokens[token] = username
	s.mu.Unlock()
	return token, nil
}

func (s *Service) UsernameForToken(token string) (string, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	username, ok := s.tokens[token]
	return username, ok
}

func (s *Service) InvalidateToken(token string) {
	if token == "" {
		return
	}
	s.mu.Lock()
	delete(s.tokens, token)
	s.mu.Unlock()
}

func (s *Service) DeleteAccount(ctx context.Context, username string) error {
	if username == "" {
		return ErrBadInput
	}
	if err := s.users.Delete(ctx, username); err != nil {
		return err
	}

	s.mu.Lock()
	for t, u := range s.tokens {
		if u == username {
			delete(s.tokens, t)
		}
	}
	s.mu.Unlock()
	return nil
}

func hashPassword(password string, salt []byte) []byte {
	sum := sha256.Sum256(append(salt, []byte(password)...))
	return sum[:]
}

func newToken() (string, error) {
	buf := make([]byte, 32)
	if _, err := rand.Read(buf); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(buf), nil
}
