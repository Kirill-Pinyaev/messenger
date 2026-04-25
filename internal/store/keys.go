package store

import (
	"context"
	"errors"
	"strings"
	"sync"
	"time"
)

var (
	ErrIdentityKeyNotFound     = errors.New("identity key not found")
	ErrSignedPrekeyNotFound    = errors.New("signed prekey not found")
	ErrConversationKeyNotFound = errors.New("conversation key not found")
)

type IdentityKey struct {
	Username    string
	KeyID       string
	Algorithm   string
	PublicKey   []byte
	PublishedAt time.Time
}

type SignedPrekey struct {
	Username    string
	KeyID       string
	Algorithm   string
	PublicKey   []byte
	PublishedAt time.Time
}

type OneTimePrekey struct {
	Username    string
	KeyID       string
	Algorithm   string
	PublicKey   []byte
	PublishedAt time.Time
}

type PrekeyBundle struct {
	Username      string
	IdentityKey   IdentityKey
	SignedPrekey  SignedPrekey
	OneTimePrekey OneTimePrekey
}

type ConversationKeyEnvelope struct {
	Username       string
	EncryptedKey   []byte
	Nonce          []byte
	SenderKeyID    string
	RecipientKeyID string
}

type ConversationKey struct {
	ConversationID string
	Version        int32
	Algorithm      string
	CreatedBy      string
	Envelopes      []ConversationKeyEnvelope
	CreatedAt      time.Time
}

type KeyStore interface {
	UpsertIdentityKey(ctx context.Context, key IdentityKey) (IdentityKey, error)
	GetIdentityKey(ctx context.Context, username string) (IdentityKey, error)
	GetIdentityKeys(ctx context.Context, usernames []string) ([]IdentityKey, error)
	UpsertSignedPrekey(ctx context.Context, key SignedPrekey) (SignedPrekey, error)
	GetSignedPrekey(ctx context.Context, username string) (SignedPrekey, error)
	DeleteOneTimePrekeys(ctx context.Context, username string) error
	PutOneTimePrekeys(ctx context.Context, username string, keys []OneTimePrekey) error
	AcquirePrekeyBundle(ctx context.Context, username string) (PrekeyBundle, error)
	UpsertConversationKey(ctx context.Context, key ConversationKey) (ConversationKey, error)
	GetConversationKey(ctx context.Context, conversationID string, version int32) (ConversationKey, error)
	DeleteUser(ctx context.Context, username string) error
}

type MemoryKeyStore struct {
	mu               sync.RWMutex
	identityKeys     map[string]IdentityKey
	signedPrekeys    map[string]SignedPrekey
	oneTimePrekeys   map[string][]OneTimePrekey
	conversationKeys map[string]map[int32]ConversationKey
}

func NewMemoryKeyStore() *MemoryKeyStore {
	return &MemoryKeyStore{
		identityKeys:     make(map[string]IdentityKey),
		signedPrekeys:    make(map[string]SignedPrekey),
		oneTimePrekeys:   make(map[string][]OneTimePrekey),
		conversationKeys: make(map[string]map[int32]ConversationKey),
	}
}

func (s *MemoryKeyStore) UpsertIdentityKey(_ context.Context, key IdentityKey) (IdentityKey, error) {
	if err := validateIdentityKey(key); err != nil {
		return IdentityKey{}, err
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	if key.PublishedAt.IsZero() {
		key.PublishedAt = time.Now().UTC()
	}
	key.PublicKey = append([]byte(nil), key.PublicKey...)
	s.identityKeys[key.Username] = key
	return cloneIdentityKey(key), nil
}

func (s *MemoryKeyStore) GetIdentityKey(_ context.Context, username string) (IdentityKey, error) {
	if strings.TrimSpace(username) == "" {
		return IdentityKey{}, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	key, ok := s.identityKeys[username]
	if !ok {
		return IdentityKey{}, ErrIdentityKeyNotFound
	}
	return cloneIdentityKey(key), nil
}

func (s *MemoryKeyStore) GetIdentityKeys(_ context.Context, usernames []string) ([]IdentityKey, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()

	out := make([]IdentityKey, 0, len(usernames))
	for _, username := range usernames {
		key, ok := s.identityKeys[username]
		if !ok {
			continue
		}
		out = append(out, cloneIdentityKey(key))
	}
	return out, nil
}

func (s *MemoryKeyStore) UpsertSignedPrekey(_ context.Context, key SignedPrekey) (SignedPrekey, error) {
	if err := validateSignedPrekey(key); err != nil {
		return SignedPrekey{}, err
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	if key.PublishedAt.IsZero() {
		key.PublishedAt = time.Now().UTC()
	}
	key.PublicKey = append([]byte(nil), key.PublicKey...)
	s.signedPrekeys[key.Username] = key
	return cloneSignedPrekey(key), nil
}

func (s *MemoryKeyStore) GetSignedPrekey(_ context.Context, username string) (SignedPrekey, error) {
	if strings.TrimSpace(username) == "" {
		return SignedPrekey{}, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	key, ok := s.signedPrekeys[username]
	if !ok {
		return SignedPrekey{}, ErrSignedPrekeyNotFound
	}
	return cloneSignedPrekey(key), nil
}

func (s *MemoryKeyStore) PutOneTimePrekeys(_ context.Context, username string, keys []OneTimePrekey) error {
	if strings.TrimSpace(username) == "" {
		return ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	for _, key := range keys {
		key.Username = username
		if err := validateOneTimePrekey(key); err != nil {
			return err
		}
		if key.PublishedAt.IsZero() {
			key.PublishedAt = time.Now().UTC()
		}
		key.PublicKey = append([]byte(nil), key.PublicKey...)
		s.oneTimePrekeys[username] = append(s.oneTimePrekeys[username], key)
	}
	return nil
}

func (s *MemoryKeyStore) DeleteOneTimePrekeys(_ context.Context, username string) error {
	if strings.TrimSpace(username) == "" {
		return ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.oneTimePrekeys, username)
	return nil
}

func (s *MemoryKeyStore) AcquirePrekeyBundle(_ context.Context, username string) (PrekeyBundle, error) {
	if strings.TrimSpace(username) == "" {
		return PrekeyBundle{}, ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	identity, ok := s.identityKeys[username]
	if !ok {
		return PrekeyBundle{}, ErrIdentityKeyNotFound
	}
	signedPrekey, ok := s.signedPrekeys[username]
	if !ok {
		return PrekeyBundle{}, ErrSignedPrekeyNotFound
	}

	bundle := PrekeyBundle{
		Username:     username,
		IdentityKey:  cloneIdentityKey(identity),
		SignedPrekey: cloneSignedPrekey(signedPrekey),
	}
	if queue := s.oneTimePrekeys[username]; len(queue) > 0 {
		bundle.OneTimePrekey = cloneOneTimePrekey(queue[0])
		s.oneTimePrekeys[username] = append([]OneTimePrekey(nil), queue[1:]...)
	}
	return bundle, nil
}

func (s *MemoryKeyStore) UpsertConversationKey(_ context.Context, key ConversationKey) (ConversationKey, error) {
	if err := validateConversationKey(key); err != nil {
		return ConversationKey{}, err
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	if key.CreatedAt.IsZero() {
		key.CreatedAt = time.Now().UTC()
	}
	key = cloneConversationKey(key)
	if _, ok := s.conversationKeys[key.ConversationID]; !ok {
		s.conversationKeys[key.ConversationID] = make(map[int32]ConversationKey)
	}
	s.conversationKeys[key.ConversationID][key.Version] = key
	return cloneConversationKey(key), nil
}

func (s *MemoryKeyStore) GetConversationKey(_ context.Context, conversationID string, version int32) (ConversationKey, error) {
	if strings.TrimSpace(conversationID) == "" {
		return ConversationKey{}, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	versions, ok := s.conversationKeys[conversationID]
	if !ok {
		return ConversationKey{}, ErrConversationKeyNotFound
	}
	if version <= 0 {
		var latestVersion int32
		for itemVersion := range versions {
			if itemVersion > latestVersion {
				latestVersion = itemVersion
			}
		}
		version = latestVersion
	}
	key, ok := versions[version]
	if !ok {
		return ConversationKey{}, ErrConversationKeyNotFound
	}
	return cloneConversationKey(key), nil
}

func (s *MemoryKeyStore) DeleteUser(_ context.Context, username string) error {
	if strings.TrimSpace(username) == "" {
		return ErrBadInput
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	delete(s.identityKeys, username)
	delete(s.signedPrekeys, username)
	delete(s.oneTimePrekeys, username)
	for conversationID, versions := range s.conversationKeys {
		for version, key := range versions {
			filtered := make([]ConversationKeyEnvelope, 0, len(key.Envelopes))
			for _, envelope := range key.Envelopes {
				if envelope.Username != username {
					filtered = append(filtered, envelope)
				}
			}
			key.Envelopes = filtered
			versions[version] = key
		}
		s.conversationKeys[conversationID] = versions
	}
	return nil
}

func validateIdentityKey(key IdentityKey) error {
	key.Username = strings.TrimSpace(key.Username)
	key.KeyID = strings.TrimSpace(key.KeyID)
	key.Algorithm = strings.TrimSpace(key.Algorithm)
	if key.Username == "" || key.KeyID == "" || key.Algorithm == "" || len(key.PublicKey) == 0 {
		return ErrBadInput
	}
	return nil
}

func validateSignedPrekey(key SignedPrekey) error {
	key.Username = strings.TrimSpace(key.Username)
	key.KeyID = strings.TrimSpace(key.KeyID)
	key.Algorithm = strings.TrimSpace(key.Algorithm)
	if key.Username == "" || key.KeyID == "" || key.Algorithm == "" || len(key.PublicKey) == 0 {
		return ErrBadInput
	}
	return nil
}

func validateOneTimePrekey(key OneTimePrekey) error {
	key.Username = strings.TrimSpace(key.Username)
	key.KeyID = strings.TrimSpace(key.KeyID)
	key.Algorithm = strings.TrimSpace(key.Algorithm)
	if key.Username == "" || key.KeyID == "" || key.Algorithm == "" || len(key.PublicKey) == 0 {
		return ErrBadInput
	}
	return nil
}

func validateConversationKey(key ConversationKey) error {
	key.ConversationID = strings.TrimSpace(key.ConversationID)
	key.Algorithm = strings.TrimSpace(key.Algorithm)
	key.CreatedBy = strings.TrimSpace(key.CreatedBy)
	if key.ConversationID == "" || key.Version <= 0 || key.Algorithm == "" || key.CreatedBy == "" || len(key.Envelopes) == 0 {
		return ErrBadInput
	}
	for _, envelope := range key.Envelopes {
		if strings.TrimSpace(envelope.Username) == "" || len(envelope.EncryptedKey) == 0 || len(envelope.Nonce) == 0 ||
			strings.TrimSpace(envelope.SenderKeyID) == "" || strings.TrimSpace(envelope.RecipientKeyID) == "" {
			return ErrBadInput
		}
	}
	return nil
}

func cloneIdentityKey(key IdentityKey) IdentityKey {
	key.PublicKey = append([]byte(nil), key.PublicKey...)
	return key
}

func cloneSignedPrekey(key SignedPrekey) SignedPrekey {
	key.PublicKey = append([]byte(nil), key.PublicKey...)
	return key
}

func cloneOneTimePrekey(key OneTimePrekey) OneTimePrekey {
	key.PublicKey = append([]byte(nil), key.PublicKey...)
	return key
}

func cloneConversationKey(key ConversationKey) ConversationKey {
	key.Envelopes = append([]ConversationKeyEnvelope(nil), key.Envelopes...)
	for i := range key.Envelopes {
		key.Envelopes[i].EncryptedKey = append([]byte(nil), key.Envelopes[i].EncryptedKey...)
		key.Envelopes[i].Nonce = append([]byte(nil), key.Envelopes[i].Nonce...)
	}
	return key
}
