package store

import (
	"context"
	"errors"
	"strings"
	"sync"
	"time"
)

var ErrArchiveHeaderNotFound = errors.New("archive header not found")

type HistoryArchiveRecordType string

const (
	HistoryArchiveRecordTypeDirectMessage              HistoryArchiveRecordType = "direct_message"
	HistoryArchiveRecordTypeGroupMessage               HistoryArchiveRecordType = "group_message"
	HistoryArchiveRecordTypeDirectAttachmentDescriptor HistoryArchiveRecordType = "direct_attachment_descriptor"
	HistoryArchiveRecordTypeGroupAttachmentDescriptor  HistoryArchiveRecordType = "group_attachment_descriptor"
	HistoryArchiveRecordTypeGroupKeyVersion            HistoryArchiveRecordType = "group_key_version"
)

type ArchiveKeyBundle struct {
	Username            string
	PublicKey           []byte
	EncryptedPrivateKey []byte
	KDFSalt             []byte
	KDFParams           string
	Version             int32
	UpdatedAt           time.Time
}

type HistoryArchiveRecord struct {
	Sequence           int64
	RecordID           string
	OwnerUsername      string
	ConversationID     string
	RecordType         HistoryArchiveRecordType
	MessageID          int64
	AttachmentID       string
	GroupKeyVersion    int32
	Sender             string
	CreatedAt          time.Time
	Ciphertext         []byte
	Nonce              []byte
	EphemeralPublicKey []byte
	ArchiveKeyVersion  int32
}

type ArchiveStore interface {
	InitializeHeader(ctx context.Context, header ArchiveKeyBundle) (ArchiveKeyBundle, error)
	GetHeader(ctx context.Context, username string) (ArchiveKeyBundle, error)
	GetPublicKeys(ctx context.Context, usernames []string) ([]ArchiveKeyBundle, error)
	AppendRecords(ctx context.Context, records []HistoryArchiveRecord) error
	ListRecords(ctx context.Context, username string, afterSequence int64, limit int) ([]HistoryArchiveRecord, error)
	DeleteUser(ctx context.Context, username string) error
}

type MemoryArchiveStore struct {
	mu      sync.RWMutex
	headers map[string]ArchiveKeyBundle
	records map[string][]HistoryArchiveRecord
	nextSeq int64
}

func NewMemoryArchiveStore() *MemoryArchiveStore {
	return &MemoryArchiveStore{
		headers: make(map[string]ArchiveKeyBundle),
		records: make(map[string][]HistoryArchiveRecord),
	}
}

func (s *MemoryArchiveStore) InitializeHeader(_ context.Context, header ArchiveKeyBundle) (ArchiveKeyBundle, error) {
	if err := validateArchiveHeader(header); err != nil {
		return ArchiveKeyBundle{}, err
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if header.UpdatedAt.IsZero() {
		header.UpdatedAt = time.Now().UTC()
	}
	header = cloneArchiveHeader(header)
	s.headers[header.Username] = header
	return cloneArchiveHeader(header), nil
}

func (s *MemoryArchiveStore) GetHeader(_ context.Context, username string) (ArchiveKeyBundle, error) {
	if strings.TrimSpace(username) == "" {
		return ArchiveKeyBundle{}, ErrBadInput
	}
	s.mu.RLock()
	defer s.mu.RUnlock()
	header, ok := s.headers[username]
	if !ok {
		return ArchiveKeyBundle{}, ErrArchiveHeaderNotFound
	}
	return cloneArchiveHeader(header), nil
}

func (s *MemoryArchiveStore) GetPublicKeys(_ context.Context, usernames []string) ([]ArchiveKeyBundle, error) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	allowed := make(map[string]struct{}, len(usernames))
	for _, username := range usernames {
		if trimmed := strings.TrimSpace(username); trimmed != "" {
			allowed[trimmed] = struct{}{}
		}
	}
	out := make([]ArchiveKeyBundle, 0, len(allowed))
	for username := range allowed {
		header, ok := s.headers[username]
		if !ok {
			continue
		}
		out = append(out, ArchiveKeyBundle{
			Username:  header.Username,
			PublicKey: append([]byte(nil), header.PublicKey...),
			Version:   header.Version,
			UpdatedAt: header.UpdatedAt,
		})
	}
	return out, nil
}

func (s *MemoryArchiveStore) AppendRecords(_ context.Context, records []HistoryArchiveRecord) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	for _, record := range records {
		if err := validateArchiveRecord(record); err != nil {
			return err
		}
		s.nextSeq++
		record.Sequence = s.nextSeq
		if record.CreatedAt.IsZero() {
			record.CreatedAt = time.Now().UTC()
		}
		s.records[record.OwnerUsername] = append(s.records[record.OwnerUsername], cloneArchiveRecord(record))
	}
	return nil
}

func (s *MemoryArchiveStore) ListRecords(_ context.Context, username string, afterSequence int64, limit int) ([]HistoryArchiveRecord, error) {
	if strings.TrimSpace(username) == "" {
		return nil, ErrBadInput
	}
	if limit <= 0 {
		limit = 100
	}
	s.mu.RLock()
	defer s.mu.RUnlock()
	all := s.records[username]
	out := make([]HistoryArchiveRecord, 0, min(limit, len(all)))
	for _, record := range all {
		if record.Sequence <= afterSequence {
			continue
		}
		out = append(out, cloneArchiveRecord(record))
		if len(out) >= limit {
			break
		}
	}
	return out, nil
}

func (s *MemoryArchiveStore) DeleteUser(_ context.Context, username string) error {
	if strings.TrimSpace(username) == "" {
		return ErrBadInput
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.headers, username)
	delete(s.records, username)
	return nil
}

func validateArchiveHeader(header ArchiveKeyBundle) error {
	if strings.TrimSpace(header.Username) == "" || len(header.PublicKey) == 0 || len(header.EncryptedPrivateKey) == 0 || len(header.KDFSalt) == 0 || strings.TrimSpace(header.KDFParams) == "" {
		return ErrBadInput
	}
	if header.Version <= 0 {
		return ErrBadInput
	}
	return nil
}

func validateArchiveRecord(record HistoryArchiveRecord) error {
	if strings.TrimSpace(record.OwnerUsername) == "" || strings.TrimSpace(record.RecordID) == "" || strings.TrimSpace(record.ConversationID) == "" || strings.TrimSpace(record.Sender) == "" {
		return ErrBadInput
	}
	switch record.RecordType {
	case HistoryArchiveRecordTypeDirectMessage, HistoryArchiveRecordTypeGroupMessage, HistoryArchiveRecordTypeDirectAttachmentDescriptor, HistoryArchiveRecordTypeGroupAttachmentDescriptor, HistoryArchiveRecordTypeGroupKeyVersion:
	default:
		return ErrBadInput
	}
	if len(record.Ciphertext) == 0 || len(record.Nonce) == 0 || len(record.EphemeralPublicKey) == 0 || record.ArchiveKeyVersion <= 0 {
		return ErrBadInput
	}
	return nil
}

func cloneArchiveHeader(header ArchiveKeyBundle) ArchiveKeyBundle {
	header.PublicKey = append([]byte(nil), header.PublicKey...)
	header.EncryptedPrivateKey = append([]byte(nil), header.EncryptedPrivateKey...)
	header.KDFSalt = append([]byte(nil), header.KDFSalt...)
	return header
}

func cloneArchiveRecord(record HistoryArchiveRecord) HistoryArchiveRecord {
	record.Ciphertext = append([]byte(nil), record.Ciphertext...)
	record.Nonce = append([]byte(nil), record.Nonce...)
	record.EphemeralPublicKey = append([]byte(nil), record.EphemeralPublicKey...)
	return record
}
