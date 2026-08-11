package store

import (
	"context"
	"errors"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/jackc/pgx/v5/pgxpool"
)

type PostgresMessageStore struct {
	pool *pgxpool.Pool
}

func NewPostgresMessageStore(ctx context.Context, pool *pgxpool.Pool) (*PostgresMessageStore, error) {
	s := &PostgresMessageStore{pool: pool}
	if err := s.initSchema(ctx); err != nil {
		return nil, err
	}
	return s, nil
}

func (s *PostgresMessageStore) Save(ctx context.Context, msg Message) (Message, error) {
	if msg.ConversationID == "" || msg.From == "" || msg.To == "" {
		return Message{}, ErrBadInput
	}
	if !msg.Encrypted && strings.TrimSpace(msg.Text) == "" && len(msg.Attachments) == 0 {
		return Message{}, ErrBadInput
	}
	if msg.Encrypted && len(msg.Ciphertext) == 0 && len(msg.DirectEnvelopes) == 0 && strings.TrimSpace(msg.Text) == "" && len(msg.Attachments) == 0 {
		return Message{}, ErrBadInput
	}
	if msg.Encrypted && len(msg.Ciphertext) > 0 && (len(msg.Nonce) == 0 || msg.SenderKeyID == "") {
		return Message{}, ErrBadInput
	}
	for _, attachment := range msg.Attachments {
		if err := validateAttachment(attachment); err != nil {
			return Message{}, err
		}
	}
	if msg.Ciphertext == nil {
		msg.Ciphertext = []byte{}
	}
	if msg.Nonce == nil {
		msg.Nonce = []byte{}
	}
	if msg.RecipientSignedPrekeyPublic == nil {
		msg.RecipientSignedPrekeyPublic = []byte{}
	}
	if msg.RecipientOneTimePrekeyPublic == nil {
		msg.RecipientOneTimePrekeyPublic = []byte{}
	}
	if msg.RatchetPublicKey == nil {
		msg.RatchetPublicKey = []byte{}
	}
	msg.DirectEnvelopes = dedupeDirectEnvelopes(cloneDirectEnvelopes(msg.DirectEnvelopes))
	msg.Attachments = cloneAttachments(msg.Attachments)

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return Message{}, err
	}
	defer tx.Rollback(ctx)

	err = tx.QueryRow(ctx, `
		INSERT INTO messages (
			conversation_id, sender, recipient, sender_device_id, body, ciphertext, nonce, sender_key_id, conversation_key_version, encrypted,
			recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public,
			e2ee_algorithm, ratchet_public_key, previous_chain_length, message_number, ts
		)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19)
		RETURNING id
	`, msg.ConversationID, msg.From, msg.To, msg.SenderDeviceID, msg.Text, msg.Ciphertext, msg.Nonce, msg.SenderKeyID, msg.KeyVersion, msg.Encrypted,
		msg.RecipientSignedPrekeyID, msg.RecipientSignedPrekeyPublic, msg.RecipientOneTimePrekeyID, msg.RecipientOneTimePrekeyPublic,
		msg.E2EEAlgorithm, msg.RatchetPublicKey, msg.PreviousChainLength, msg.MessageNumber, msg.TS).Scan(&msg.ID)
	if err != nil {
		return Message{}, err
	}

	for _, attachment := range msg.Attachments {
		tag, err := tx.Exec(ctx, `
			INSERT INTO message_attachments (
				message_id, attachment_id, kind, filename, mime_type, size_bytes, media_id,
				encrypted_descriptor, descriptor_nonce, preview_width, preview_height, sha256, ciphertext_size
			)
			SELECT $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13
			WHERE EXISTS (
				SELECT 1 FROM media_objects WHERE media_id = $7 AND uploaded = TRUE
			)
		`, msg.ID, attachment.AttachmentID, string(attachment.Kind), attachment.Filename, attachment.MimeType, attachment.SizeBytes,
			attachment.MediaID, attachment.EncryptedDescriptor, attachment.DescriptorNonce, attachment.PreviewWidth, attachment.PreviewHeight,
			attachment.SHA256, attachment.CiphertextSize)
		if err != nil {
			return Message{}, err
		}
		if tag.RowsAffected() == 0 {
			return Message{}, ErrBadInput
		}
		for _, envelope := range attachment.DirectEnvelopes {
			if _, err := tx.Exec(ctx, `
				INSERT INTO attachment_descriptor_envelopes (
					message_id, attachment_id, target_username, target_device_id, encrypted_descriptor, descriptor_nonce,
					recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public,
					e2ee_algorithm, ratchet_public_key, previous_chain_length, message_number
				)
				VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
			`, msg.ID, attachment.AttachmentID, envelope.TargetUsername, envelope.TargetDeviceID, envelope.EncryptedDescriptor, envelope.DescriptorNonce, envelope.RecipientSignedPrekeyID, envelope.RecipientSignedPrekeyPublic, envelope.RecipientOneTimePrekeyID, envelope.RecipientOneTimePrekeyPublic, envelope.E2EEAlgorithm, envelope.RatchetPublicKey, envelope.PreviousChainLength, envelope.MessageNumber); err != nil {
				return Message{}, err
			}
		}
	}

	for _, envelope := range msg.DirectEnvelopes {
		if _, err := tx.Exec(ctx, `
			INSERT INTO direct_message_envelopes (
				message_id, target_username, target_device_id, ciphertext, nonce,
				recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public,
				e2ee_algorithm, ratchet_public_key, previous_chain_length, message_number
			)
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
		`, msg.ID, envelope.TargetUsername, envelope.TargetDeviceID, envelope.Ciphertext, envelope.Nonce, envelope.RecipientSignedPrekeyID, envelope.RecipientSignedPrekeyPublic, envelope.RecipientOneTimePrekeyID, envelope.RecipientOneTimePrekeyPublic, envelope.E2EEAlgorithm, envelope.RatchetPublicKey, envelope.PreviousChainLength, envelope.MessageNumber); err != nil {
			return Message{}, err
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return Message{}, err
	}
	return msg, nil
}

func (s *PostgresMessageStore) History(ctx context.Context, conversationID string, limit int) ([]Message, error) {
	if conversationID == "" {
		return nil, ErrBadInput
	}
	if limit <= 0 {
		limit = 50
	}

	rows, err := s.pool.Query(ctx, `
		SELECT id, conversation_id, sender, recipient, sender_device_id, body, ciphertext, nonce, sender_key_id, conversation_key_version, encrypted,
		       recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public,
		       e2ee_algorithm, ratchet_public_key, previous_chain_length, message_number, ts
		FROM messages
		WHERE conversation_id = $1
		ORDER BY ts DESC
		LIMIT $2
	`, conversationID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []Message
	for rows.Next() {
		var msg Message
		if err := rows.Scan(&msg.ID, &msg.ConversationID, &msg.From, &msg.To, &msg.SenderDeviceID, &msg.Text, &msg.Ciphertext, &msg.Nonce, &msg.SenderKeyID, &msg.KeyVersion, &msg.Encrypted,
			&msg.RecipientSignedPrekeyID, &msg.RecipientSignedPrekeyPublic, &msg.RecipientOneTimePrekeyID, &msg.RecipientOneTimePrekeyPublic,
			&msg.E2EEAlgorithm, &msg.RatchetPublicKey, &msg.PreviousChainLength, &msg.MessageNumber, &msg.TS); err != nil {
			return nil, err
		}
		out = append(out, msg)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	for i := range out {
		directEnvelopes, err := s.directEnvelopesForMessage(ctx, out[i].ID)
		if err != nil {
			return nil, err
		}
		out[i].DirectEnvelopes = directEnvelopes
		attachments, err := s.attachmentsForMessage(ctx, out[i].ID)
		if err != nil {
			return nil, err
		}
		out[i].Attachments = attachments
	}

	for i, j := 0, len(out)-1; i < j; i, j = i+1, j-1 {
		out[i], out[j] = out[j], out[i]
	}
	return out, nil
}

func (s *PostgresMessageStore) DeleteUser(ctx context.Context, username string) error {
	if username == "" {
		return ErrBadInput
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	if _, err := tx.Exec(ctx, `DELETE FROM media_objects WHERE owner_username = $1`, username); err != nil {
		return err
	}
	if _, err := tx.Exec(ctx, `DELETE FROM messages WHERE sender = $1 OR recipient = $1`, username); err != nil {
		return err
	}
	return tx.Commit(ctx)
}

func (s *PostgresMessageStore) Conversations(ctx context.Context, username string) ([]string, error) {
	if username == "" {
		return nil, ErrBadInput
	}

	rows, err := s.pool.Query(ctx, `
		SELECT conversation_id
		FROM messages
		WHERE sender = $1 OR recipient = $1
		GROUP BY conversation_id
		ORDER BY MAX(ts) DESC
	`, username)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []string
	for rows.Next() {
		var convID string
		if err := rows.Scan(&convID); err != nil {
			return nil, err
		}
		out = append(out, convID)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return out, nil
}

func (s *PostgresMessageStore) SearchMessages(ctx context.Context, username, query string, limit int) ([]Message, error) {
	if username == "" || strings.TrimSpace(query) == "" {
		return nil, ErrBadInput
	}
	if limit <= 0 {
		limit = 50
	}

	rows, err := s.pool.Query(ctx, `
		SELECT id, conversation_id, sender, recipient, sender_device_id, body, ciphertext, nonce, sender_key_id, conversation_key_version, encrypted,
		       recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public,
		       e2ee_algorithm, ratchet_public_key, previous_chain_length, message_number, ts
		FROM messages
		WHERE (sender = $1 OR recipient = $1
		       OR conversation_id IN (
		           SELECT conversation_id FROM conversation_members WHERE username = $1
		       ))
		  AND encrypted = FALSE
		  AND body ILIKE $2
		ORDER BY ts DESC
		LIMIT $3
	`, username, "%"+query+"%", limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []Message
	for rows.Next() {
		var msg Message
		if err := rows.Scan(&msg.ID, &msg.ConversationID, &msg.From, &msg.To, &msg.SenderDeviceID, &msg.Text, &msg.Ciphertext, &msg.Nonce, &msg.SenderKeyID, &msg.KeyVersion, &msg.Encrypted,
			&msg.RecipientSignedPrekeyID, &msg.RecipientSignedPrekeyPublic, &msg.RecipientOneTimePrekeyID, &msg.RecipientOneTimePrekeyPublic,
			&msg.E2EEAlgorithm, &msg.RatchetPublicKey, &msg.PreviousChainLength, &msg.MessageNumber, &msg.TS); err != nil {
			return nil, err
		}
		attachments, err := s.attachmentsForMessage(ctx, msg.ID)
		if err != nil {
			return nil, err
		}
		msg.Attachments = attachments
		out = append(out, msg)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return out, nil
}

func (s *PostgresMessageStore) GetByID(ctx context.Context, id int64) (Message, error) {
	if id <= 0 {
		return Message{}, ErrBadInput
	}

	var msg Message
	err := s.pool.QueryRow(ctx, `
		SELECT id, conversation_id, sender, recipient, sender_device_id, body, ciphertext, nonce, sender_key_id, conversation_key_version, encrypted,
		       recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public,
		       e2ee_algorithm, ratchet_public_key, previous_chain_length, message_number, ts
		FROM messages
		WHERE id = $1
	`, id).Scan(&msg.ID, &msg.ConversationID, &msg.From, &msg.To, &msg.SenderDeviceID, &msg.Text, &msg.Ciphertext, &msg.Nonce, &msg.SenderKeyID, &msg.KeyVersion, &msg.Encrypted,
		&msg.RecipientSignedPrekeyID, &msg.RecipientSignedPrekeyPublic, &msg.RecipientOneTimePrekeyID, &msg.RecipientOneTimePrekeyPublic,
		&msg.E2EEAlgorithm, &msg.RatchetPublicKey, &msg.PreviousChainLength, &msg.MessageNumber, &msg.TS)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Message{}, ErrMessageNotFound
		}
		return Message{}, err
	}
	msg.Attachments, err = s.attachmentsForMessage(ctx, msg.ID)
	if err != nil {
		return Message{}, err
	}
	msg.DirectEnvelopes, err = s.directEnvelopesForMessage(ctx, msg.ID)
	if err != nil {
		return Message{}, err
	}
	return msg, nil
}

func (s *PostgresMessageStore) DeleteMessage(ctx context.Context, id int64) error {
	if id <= 0 {
		return ErrBadInput
	}
	tag, err := s.pool.Exec(ctx, `
		DELETE FROM messages WHERE id = $1
	`, id)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return ErrMessageNotFound
	}
	return nil
}

func (s *PostgresMessageStore) PrepareMedia(ctx context.Context, media MediaObject) (MediaObject, error) {
	if err := validateMediaDraft(media); err != nil {
		return MediaObject{}, err
	}
	if media.CreatedAt.IsZero() {
		media.CreatedAt = time.Now().UTC()
	}

	_, err := s.pool.Exec(ctx, `
		INSERT INTO media_objects (
			media_id, owner_username, storage_key, size_bytes, ciphertext_size, mime_type, filename, kind,
			nonce, sha256, created_at, uploaded_at, uploaded
		)
		VALUES ($1, $2, $3, $4, 0, $5, $6, $7, ''::bytea, ''::bytea, $8, NULL, FALSE)
		ON CONFLICT (media_id) DO UPDATE
		SET owner_username = EXCLUDED.owner_username,
		    storage_key = EXCLUDED.storage_key,
		    size_bytes = EXCLUDED.size_bytes,
		    mime_type = EXCLUDED.mime_type,
		    filename = EXCLUDED.filename,
		    kind = EXCLUDED.kind,
		    created_at = EXCLUDED.created_at
	`, media.MediaID, media.OwnerUsername, media.StorageKey, media.SizeBytes, media.MimeType, media.Filename, string(media.Kind), media.CreatedAt)
	if err != nil {
		return MediaObject{}, err
	}
	return media, nil
}

func (s *PostgresMessageStore) CompleteMedia(ctx context.Context, media MediaObject) (MediaObject, error) {
	if err := validateMediaUpload(media); err != nil {
		return MediaObject{}, err
	}
	if media.UploadedAt.IsZero() {
		media.UploadedAt = time.Now().UTC()
	}

	tag, err := s.pool.Exec(ctx, `
		UPDATE media_objects
		SET size_bytes = $2,
		    ciphertext_size = $3,
		    mime_type = $4,
		    filename = $5,
		    kind = $6,
		    nonce = $7,
		    sha256 = $8,
		    uploaded_at = $9,
		    uploaded = TRUE
		WHERE media_id = $1
	`, media.MediaID, media.SizeBytes, media.CiphertextSize, media.MimeType, media.Filename, string(media.Kind), media.Nonce, media.SHA256, media.UploadedAt)
	if err != nil {
		return MediaObject{}, err
	}
	if tag.RowsAffected() == 0 {
		return MediaObject{}, ErrMediaNotFound
	}
	media.Uploaded = true
	return media, nil
}

func (s *PostgresMessageStore) GetMedia(ctx context.Context, mediaID string) (MediaObject, error) {
	if strings.TrimSpace(mediaID) == "" {
		return MediaObject{}, ErrBadInput
	}

	var media MediaObject
	var kind string
	var uploadedAt pgtype.Timestamptz
	err := s.pool.QueryRow(ctx, `
		SELECT media_id, owner_username, storage_key, size_bytes, ciphertext_size, mime_type, filename, kind, nonce, sha256, created_at, uploaded_at, uploaded
		FROM media_objects
		WHERE media_id = $1
	`, mediaID).Scan(&media.MediaID, &media.OwnerUsername, &media.StorageKey, &media.SizeBytes, &media.CiphertextSize, &media.MimeType, &media.Filename, &kind,
		&media.Nonce, &media.SHA256, &media.CreatedAt, &uploadedAt, &media.Uploaded)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return MediaObject{}, ErrMediaNotFound
		}
		return MediaObject{}, err
	}
	media.Kind = AttachmentKind(kind)
	if uploadedAt.Valid {
		media.UploadedAt = uploadedAt.Time
	}
	return media, nil
}

func (s *PostgresMessageStore) ListMediaByOwner(ctx context.Context, username string) ([]MediaObject, error) {
	if strings.TrimSpace(username) == "" {
		return nil, ErrBadInput
	}

	rows, err := s.pool.Query(ctx, `
		SELECT media_id, owner_username, storage_key, size_bytes, ciphertext_size, mime_type, filename, kind, nonce, sha256, created_at, uploaded_at, uploaded
		FROM media_objects
		WHERE owner_username = $1
	`, username)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []MediaObject
	for rows.Next() {
		var media MediaObject
		var kind string
		var uploadedAt pgtype.Timestamptz
		if err := rows.Scan(&media.MediaID, &media.OwnerUsername, &media.StorageKey, &media.SizeBytes, &media.CiphertextSize, &media.MimeType, &media.Filename, &kind,
			&media.Nonce, &media.SHA256, &media.CreatedAt, &uploadedAt, &media.Uploaded); err != nil {
			return nil, err
		}
		media.Kind = AttachmentKind(kind)
		if uploadedAt.Valid {
			media.UploadedAt = uploadedAt.Time
		}
		out = append(out, media)
	}
	return out, rows.Err()
}

func (s *PostgresMessageStore) CanAccessMedia(ctx context.Context, username, mediaID string) (bool, error) {
	if strings.TrimSpace(username) == "" || strings.TrimSpace(mediaID) == "" {
		return false, ErrBadInput
	}

	var ok bool
	err := s.pool.QueryRow(ctx, `
		SELECT EXISTS (
			SELECT 1
			FROM message_attachments ma
			JOIN messages m ON m.id = ma.message_id
			LEFT JOIN conversations c ON c.id = m.conversation_id
			LEFT JOIN conversation_members cm ON cm.conversation_id = c.id AND cm.username = $1
			WHERE ma.media_id = $2
			  AND (
				m.sender = $1 OR
				m.recipient = $1 OR
				cm.username IS NOT NULL
			  )
		)
	`, username, mediaID).Scan(&ok)
	return ok, err
}

func (s *PostgresMessageStore) attachmentsForMessage(ctx context.Context, messageID int64) ([]Attachment, error) {
	rows, err := s.pool.Query(ctx, `
		SELECT attachment_id, kind, filename, mime_type, size_bytes, media_id, encrypted_descriptor, descriptor_nonce,
		       preview_width, preview_height, sha256, ciphertext_size
		FROM message_attachments
		WHERE message_id = $1
		ORDER BY attachment_id
	`, messageID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []Attachment
	for rows.Next() {
		var item Attachment
		var kind string
		if err := rows.Scan(&item.AttachmentID, &kind, &item.Filename, &item.MimeType, &item.SizeBytes, &item.MediaID,
			&item.EncryptedDescriptor, &item.DescriptorNonce, &item.PreviewWidth, &item.PreviewHeight, &item.SHA256, &item.CiphertextSize); err != nil {
			return nil, err
		}
		item.Kind = AttachmentKind(kind)
		item.DirectEnvelopes, err = s.attachmentDirectEnvelopesForAttachment(ctx, messageID, item.AttachmentID)
		if err != nil {
			return nil, err
		}
		out = append(out, item)
	}
	return out, rows.Err()
}

func (s *PostgresMessageStore) directEnvelopesForMessage(ctx context.Context, messageID int64) ([]DirectEnvelope, error) {
	rows, err := s.pool.Query(ctx, `
		SELECT target_username, target_device_id, ciphertext, nonce,
		       recipient_signed_prekey_id, recipient_signed_prekey_public,
		       recipient_one_time_prekey_id, recipient_one_time_prekey_public,
		       e2ee_algorithm, ratchet_public_key, previous_chain_length, message_number
		FROM direct_message_envelopes
		WHERE message_id = $1
		ORDER BY target_username, target_device_id
	`, messageID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []DirectEnvelope
	for rows.Next() {
		var item DirectEnvelope
		if err := rows.Scan(&item.TargetUsername, &item.TargetDeviceID, &item.Ciphertext, &item.Nonce, &item.RecipientSignedPrekeyID, &item.RecipientSignedPrekeyPublic, &item.RecipientOneTimePrekeyID, &item.RecipientOneTimePrekeyPublic, &item.E2EEAlgorithm, &item.RatchetPublicKey, &item.PreviousChainLength, &item.MessageNumber); err != nil {
			return nil, err
		}
		out = append(out, item)
	}
	return out, rows.Err()
}

func (s *PostgresMessageStore) attachmentDirectEnvelopesForAttachment(ctx context.Context, messageID int64, attachmentID string) ([]AttachmentDirectEnvelope, error) {
	rows, err := s.pool.Query(ctx, `
		SELECT target_username, target_device_id, encrypted_descriptor, descriptor_nonce,
		       recipient_signed_prekey_id, recipient_signed_prekey_public,
		       recipient_one_time_prekey_id, recipient_one_time_prekey_public,
		       e2ee_algorithm, ratchet_public_key, previous_chain_length, message_number
		FROM attachment_descriptor_envelopes
		WHERE message_id = $1 AND attachment_id = $2
		ORDER BY target_username, target_device_id
	`, messageID, attachmentID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []AttachmentDirectEnvelope
	for rows.Next() {
		var item AttachmentDirectEnvelope
		if err := rows.Scan(&item.TargetUsername, &item.TargetDeviceID, &item.EncryptedDescriptor, &item.DescriptorNonce, &item.RecipientSignedPrekeyID, &item.RecipientSignedPrekeyPublic, &item.RecipientOneTimePrekeyID, &item.RecipientOneTimePrekeyPublic, &item.E2EEAlgorithm, &item.RatchetPublicKey, &item.PreviousChainLength, &item.MessageNumber); err != nil {
			return nil, err
		}
		out = append(out, item)
	}
	return out, rows.Err()
}

func (s *PostgresMessageStore) initSchema(ctx context.Context) error {
	_, err := s.pool.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS messages (
			id BIGSERIAL PRIMARY KEY,
			conversation_id TEXT NOT NULL,
			sender TEXT NOT NULL,
			recipient TEXT NOT NULL,
			sender_device_id TEXT NOT NULL DEFAULT '',
			body TEXT NOT NULL,
			ciphertext BYTEA NOT NULL DEFAULT ''::bytea,
			nonce BYTEA NOT NULL DEFAULT ''::bytea,
			sender_key_id TEXT NOT NULL DEFAULT '',
			conversation_key_version INTEGER NOT NULL DEFAULT 0,
			encrypted BOOLEAN NOT NULL DEFAULT FALSE,
			recipient_signed_prekey_id TEXT NOT NULL DEFAULT '',
			recipient_signed_prekey_public BYTEA NOT NULL DEFAULT ''::bytea,
			recipient_one_time_prekey_id TEXT NOT NULL DEFAULT '',
			recipient_one_time_prekey_public BYTEA NOT NULL DEFAULT ''::bytea,
			e2ee_algorithm TEXT NOT NULL DEFAULT '',
			ratchet_public_key BYTEA NOT NULL DEFAULT ''::bytea,
			previous_chain_length INTEGER NOT NULL DEFAULT 0,
			message_number INTEGER NOT NULL DEFAULT 0,
			ts TIMESTAMPTZ NOT NULL
		);

		CREATE TABLE IF NOT EXISTS media_objects (
			media_id TEXT PRIMARY KEY,
			owner_username TEXT NOT NULL,
			storage_key TEXT NOT NULL UNIQUE,
			size_bytes BIGINT NOT NULL,
			ciphertext_size BIGINT NOT NULL DEFAULT 0,
			mime_type TEXT NOT NULL,
			filename TEXT NOT NULL,
			kind TEXT NOT NULL,
			nonce BYTEA NOT NULL DEFAULT ''::bytea,
			sha256 BYTEA NOT NULL DEFAULT ''::bytea,
			created_at TIMESTAMPTZ NOT NULL,
			uploaded_at TIMESTAMPTZ,
			uploaded BOOLEAN NOT NULL DEFAULT FALSE
		);

		CREATE TABLE IF NOT EXISTS message_attachments (
			message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
			attachment_id TEXT NOT NULL,
			kind TEXT NOT NULL,
			filename TEXT NOT NULL,
			mime_type TEXT NOT NULL,
			size_bytes BIGINT NOT NULL,
			media_id TEXT NOT NULL REFERENCES media_objects(media_id) ON DELETE CASCADE,
			encrypted_descriptor BYTEA NOT NULL,
			descriptor_nonce BYTEA NOT NULL,
			preview_width INTEGER NOT NULL DEFAULT 0,
			preview_height INTEGER NOT NULL DEFAULT 0,
			sha256 BYTEA NOT NULL DEFAULT ''::bytea,
			ciphertext_size BIGINT NOT NULL DEFAULT 0,
			PRIMARY KEY (message_id, attachment_id)
		);
		CREATE TABLE IF NOT EXISTS direct_message_envelopes (
			message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
			target_username TEXT NOT NULL,
			target_device_id TEXT NOT NULL,
			ciphertext BYTEA NOT NULL,
			nonce BYTEA NOT NULL,
			recipient_signed_prekey_id TEXT NOT NULL DEFAULT '',
			recipient_signed_prekey_public BYTEA NOT NULL DEFAULT ''::bytea,
			recipient_one_time_prekey_id TEXT NOT NULL DEFAULT '',
			recipient_one_time_prekey_public BYTEA NOT NULL DEFAULT ''::bytea,
			e2ee_algorithm TEXT NOT NULL DEFAULT '',
			ratchet_public_key BYTEA NOT NULL DEFAULT ''::bytea,
			previous_chain_length INTEGER NOT NULL DEFAULT 0,
			message_number INTEGER NOT NULL DEFAULT 0,
			PRIMARY KEY (message_id, target_username, target_device_id)
		);
		CREATE TABLE IF NOT EXISTS attachment_descriptor_envelopes (
			message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
			attachment_id TEXT NOT NULL,
			target_username TEXT NOT NULL,
			target_device_id TEXT NOT NULL,
			encrypted_descriptor BYTEA NOT NULL,
			descriptor_nonce BYTEA NOT NULL,
			recipient_signed_prekey_id TEXT NOT NULL DEFAULT '',
			recipient_signed_prekey_public BYTEA NOT NULL DEFAULT ''::bytea,
			recipient_one_time_prekey_id TEXT NOT NULL DEFAULT '',
			recipient_one_time_prekey_public BYTEA NOT NULL DEFAULT ''::bytea,
			e2ee_algorithm TEXT NOT NULL DEFAULT '',
			ratchet_public_key BYTEA NOT NULL DEFAULT ''::bytea,
			previous_chain_length INTEGER NOT NULL DEFAULT 0,
			message_number INTEGER NOT NULL DEFAULT 0,
			PRIMARY KEY (message_id, attachment_id, target_username, target_device_id)
		);

		CREATE INDEX IF NOT EXISTS messages_conversation_ts_idx
			ON messages (conversation_id, ts DESC);
		CREATE INDEX IF NOT EXISTS message_attachments_media_idx
			ON message_attachments (media_id);
		CREATE INDEX IF NOT EXISTS media_objects_owner_idx
			ON media_objects (owner_username);
		CREATE INDEX IF NOT EXISTS direct_message_envelopes_message_idx
			ON direct_message_envelopes (message_id);
		CREATE INDEX IF NOT EXISTS attachment_descriptor_envelopes_message_idx
			ON attachment_descriptor_envelopes (message_id, attachment_id);

		ALTER TABLE messages
			ADD COLUMN IF NOT EXISTS sender_device_id TEXT NOT NULL DEFAULT '',
			ADD COLUMN IF NOT EXISTS e2ee_algorithm TEXT NOT NULL DEFAULT '',
			ADD COLUMN IF NOT EXISTS ratchet_public_key BYTEA NOT NULL DEFAULT ''::bytea,
			ADD COLUMN IF NOT EXISTS previous_chain_length INTEGER NOT NULL DEFAULT 0,
			ADD COLUMN IF NOT EXISTS message_number INTEGER NOT NULL DEFAULT 0,
			ALTER COLUMN body SET DEFAULT '';
		ALTER TABLE direct_message_envelopes
			ADD COLUMN IF NOT EXISTS e2ee_algorithm TEXT NOT NULL DEFAULT '',
			ADD COLUMN IF NOT EXISTS ratchet_public_key BYTEA NOT NULL DEFAULT ''::bytea,
			ADD COLUMN IF NOT EXISTS previous_chain_length INTEGER NOT NULL DEFAULT 0,
			ADD COLUMN IF NOT EXISTS message_number INTEGER NOT NULL DEFAULT 0;
		ALTER TABLE attachment_descriptor_envelopes
			ADD COLUMN IF NOT EXISTS e2ee_algorithm TEXT NOT NULL DEFAULT '',
			ADD COLUMN IF NOT EXISTS ratchet_public_key BYTEA NOT NULL DEFAULT ''::bytea,
			ADD COLUMN IF NOT EXISTS previous_chain_length INTEGER NOT NULL DEFAULT 0,
			ADD COLUMN IF NOT EXISTS message_number INTEGER NOT NULL DEFAULT 0;
	`)
	return err
}
