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
var ErrMediaNotFound = errors.New("media not found")

const (
	MaxMediaSizeBytes = 25 << 20
)

type AttachmentKind string

const (
	AttachmentKindImage AttachmentKind = "image"
	AttachmentKindVideo AttachmentKind = "video"
	AttachmentKindFile  AttachmentKind = "file"
)

type Attachment struct {
	AttachmentID         string
	Kind                 AttachmentKind
	Filename             string
	MimeType             string
	SizeBytes            int64
	MediaID              string
	EncryptedDescriptor  []byte
	DescriptorNonce      []byte
	PreviewWidth         int32
	PreviewHeight        int32
	SHA256               []byte
	CiphertextSize       int64
}

type MediaObject struct {
	MediaID       string
	OwnerUsername string
	StorageKey    string
	Filename      string
	MimeType      string
	Kind          AttachmentKind
	SizeBytes     int64
	CiphertextSize int64
	Nonce         []byte
	SHA256        []byte
	CreatedAt     time.Time
	UploadedAt    time.Time
	Uploaded      bool
}

type Message struct {
	ID             int64
	ConversationID string
	From           string
	To             string
	Text           string
	Ciphertext     []byte
	Nonce          []byte
	SenderKeyID    string
	KeyVersion     int32
	Encrypted      bool
	RecipientSignedPrekeyID     string
	RecipientSignedPrekeyPublic []byte
	RecipientOneTimePrekeyID    string
	RecipientOneTimePrekeyPublic []byte
	Attachments    []Attachment
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
	PrepareMedia(ctx context.Context, media MediaObject) (MediaObject, error)
	CompleteMedia(ctx context.Context, media MediaObject) (MediaObject, error)
	GetMedia(ctx context.Context, mediaID string) (MediaObject, error)
	ListMediaByOwner(ctx context.Context, username string) ([]MediaObject, error)
	CanAccessMedia(ctx context.Context, username, mediaID string) (bool, error)
}

type MemoryMessageStore struct {
	mu     sync.RWMutex
	nextID int64
	byConv map[string][]Message
	media  map[string]MediaObject
}

func NewMemoryMessageStore() *MemoryMessageStore {
	return &MemoryMessageStore{
		byConv: make(map[string][]Message),
		media:  make(map[string]MediaObject),
	}
}

func (s *MemoryMessageStore) Save(_ context.Context, msg Message) (Message, error) {
	if msg.ConversationID == "" || msg.From == "" || msg.To == "" {
		return Message{}, ErrBadInput
	}
	if !msg.Encrypted && strings.TrimSpace(msg.Text) == "" && len(msg.Attachments) == 0 {
		return Message{}, ErrBadInput
	}
	if msg.Encrypted && len(msg.Ciphertext) == 0 && strings.TrimSpace(msg.Text) == "" && len(msg.Attachments) == 0 {
		return Message{}, ErrBadInput
	}
	if msg.Encrypted && len(msg.Ciphertext) > 0 && (len(msg.Nonce) == 0 || msg.SenderKeyID == "") {
		return Message{}, ErrBadInput
	}
	s.mu.Lock()
	defer s.mu.Unlock()

	for _, attachment := range msg.Attachments {
		if err := validateAttachment(attachment); err != nil {
			return Message{}, err
		}
		media, ok := s.media[attachment.MediaID]
		if !ok || !media.Uploaded {
			return Message{}, ErrBadInput
		}
	}

	s.nextID++
	msg.ID = s.nextID
	msg.Ciphertext = append([]byte(nil), msg.Ciphertext...)
	msg.Nonce = append([]byte(nil), msg.Nonce...)
	msg.RecipientSignedPrekeyPublic = append([]byte(nil), msg.RecipientSignedPrekeyPublic...)
	msg.RecipientOneTimePrekeyPublic = append([]byte(nil), msg.RecipientOneTimePrekeyPublic...)
	msg.Attachments = cloneAttachments(msg.Attachments)
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
	for i := range out {
		out[i].Ciphertext = append([]byte(nil), out[i].Ciphertext...)
		out[i].Nonce = append([]byte(nil), out[i].Nonce...)
		out[i].RecipientSignedPrekeyPublic = append([]byte(nil), out[i].RecipientSignedPrekeyPublic...)
		out[i].RecipientOneTimePrekeyPublic = append([]byte(nil), out[i].RecipientOneTimePrekeyPublic...)
		out[i].Attachments = cloneAttachments(out[i].Attachments)
	}
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
	for mediaID, media := range s.media {
		if media.OwnerUsername == username {
			delete(s.media, mediaID)
		}
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
				msg.Ciphertext = append([]byte(nil), msg.Ciphertext...)
				msg.Nonce = append([]byte(nil), msg.Nonce...)
				msg.RecipientSignedPrekeyPublic = append([]byte(nil), msg.RecipientSignedPrekeyPublic...)
				msg.RecipientOneTimePrekeyPublic = append([]byte(nil), msg.RecipientOneTimePrekeyPublic...)
				msg.Attachments = cloneAttachments(msg.Attachments)
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

func (s *MemoryMessageStore) PrepareMedia(_ context.Context, media MediaObject) (MediaObject, error) {
	if err := validateMediaDraft(media); err != nil {
		return MediaObject{}, err
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	if media.CreatedAt.IsZero() {
		media.CreatedAt = time.Now().UTC()
	}
	media.Uploaded = false
	media.Nonce = append([]byte(nil), media.Nonce...)
	media.SHA256 = append([]byte(nil), media.SHA256...)
	s.media[media.MediaID] = cloneMediaObject(media)
	return cloneMediaObject(media), nil
}

func (s *MemoryMessageStore) CompleteMedia(_ context.Context, media MediaObject) (MediaObject, error) {
	if err := validateMediaUpload(media); err != nil {
		return MediaObject{}, err
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	existing, ok := s.media[media.MediaID]
	if !ok {
		return MediaObject{}, ErrMediaNotFound
	}
	existing.MimeType = media.MimeType
	existing.Filename = media.Filename
	existing.Kind = media.Kind
	existing.SizeBytes = media.SizeBytes
	existing.CiphertextSize = media.CiphertextSize
	existing.Nonce = append([]byte(nil), media.Nonce...)
	existing.SHA256 = append([]byte(nil), media.SHA256...)
	existing.Uploaded = true
	existing.UploadedAt = media.UploadedAt
	if existing.UploadedAt.IsZero() {
		existing.UploadedAt = time.Now().UTC()
	}
	s.media[media.MediaID] = existing
	return cloneMediaObject(existing), nil
}

func (s *MemoryMessageStore) GetMedia(_ context.Context, mediaID string) (MediaObject, error) {
	if strings.TrimSpace(mediaID) == "" {
		return MediaObject{}, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	media, ok := s.media[mediaID]
	if !ok {
		return MediaObject{}, ErrMediaNotFound
	}
	return cloneMediaObject(media), nil
}

func (s *MemoryMessageStore) ListMediaByOwner(_ context.Context, username string) ([]MediaObject, error) {
	if strings.TrimSpace(username) == "" {
		return nil, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	var out []MediaObject
	for _, media := range s.media {
		if media.OwnerUsername == username {
			out = append(out, cloneMediaObject(media))
		}
	}
	return out, nil
}

func (s *MemoryMessageStore) CanAccessMedia(_ context.Context, username, mediaID string) (bool, error) {
	if strings.TrimSpace(username) == "" || strings.TrimSpace(mediaID) == "" {
		return false, ErrBadInput
	}

	s.mu.RLock()
	defer s.mu.RUnlock()

	for _, messages := range s.byConv {
		for _, msg := range messages {
			if msg.From != username && msg.To != username && !strings.Contains(msg.ConversationID, username) {
				continue
			}
			for _, attachment := range msg.Attachments {
				if attachment.MediaID == mediaID {
					return true, nil
				}
			}
		}
	}
	return false, nil
}

func cloneAttachments(items []Attachment) []Attachment {
	if len(items) == 0 {
		return nil
	}
	out := make([]Attachment, 0, len(items))
	for _, item := range items {
		out = append(out, Attachment{
			AttachmentID:        item.AttachmentID,
			Kind:                item.Kind,
			Filename:            item.Filename,
			MimeType:            item.MimeType,
			SizeBytes:           item.SizeBytes,
			MediaID:             item.MediaID,
			EncryptedDescriptor: append([]byte(nil), item.EncryptedDescriptor...),
			DescriptorNonce:     append([]byte(nil), item.DescriptorNonce...),
			PreviewWidth:        item.PreviewWidth,
			PreviewHeight:       item.PreviewHeight,
			SHA256:              append([]byte(nil), item.SHA256...),
			CiphertextSize:      item.CiphertextSize,
		})
	}
	return out
}

func cloneMediaObject(item MediaObject) MediaObject {
	item.Nonce = append([]byte(nil), item.Nonce...)
	item.SHA256 = append([]byte(nil), item.SHA256...)
	return item
}

func validateAttachment(item Attachment) error {
	if strings.TrimSpace(item.AttachmentID) == "" || strings.TrimSpace(item.MediaID) == "" {
		return ErrBadInput
	}
	if strings.TrimSpace(item.Filename) == "" || strings.TrimSpace(item.MimeType) == "" {
		return ErrBadInput
	}
	if item.Kind != AttachmentKindImage && item.Kind != AttachmentKindVideo && item.Kind != AttachmentKindFile {
		return ErrBadInput
	}
	if item.SizeBytes <= 0 || item.SizeBytes > MaxMediaSizeBytes {
		return ErrBadInput
	}
	if len(item.EncryptedDescriptor) == 0 || len(item.DescriptorNonce) == 0 {
		return ErrBadInput
	}
	if len(item.SHA256) == 0 || item.CiphertextSize <= 0 {
		return ErrBadInput
	}
	return nil
}

func validateMediaDraft(media MediaObject) error {
	if strings.TrimSpace(media.MediaID) == "" || strings.TrimSpace(media.OwnerUsername) == "" || strings.TrimSpace(media.StorageKey) == "" {
		return ErrBadInput
	}
	if strings.TrimSpace(media.Filename) == "" || strings.TrimSpace(media.MimeType) == "" {
		return ErrBadInput
	}
	if media.Kind != AttachmentKindImage && media.Kind != AttachmentKindVideo && media.Kind != AttachmentKindFile {
		return ErrBadInput
	}
	if media.SizeBytes <= 0 || media.SizeBytes > MaxMediaSizeBytes {
		return ErrBadInput
	}
	return nil
}

func validateMediaUpload(media MediaObject) error {
	if err := validateMediaDraft(media); err != nil {
		return err
	}
	if len(media.Nonce) == 0 || len(media.SHA256) == 0 {
		return ErrBadInput
	}
	if media.CiphertextSize <= 0 {
		return ErrBadInput
	}
	return nil
}
