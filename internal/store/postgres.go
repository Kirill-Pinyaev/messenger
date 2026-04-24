package store

import (
	"context"
	"errors"
	"strings"

	"github.com/jackc/pgx/v5"
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
	if !msg.Encrypted && strings.TrimSpace(msg.Text) == "" {
		return Message{}, ErrBadInput
	}
	if msg.Encrypted && (len(msg.Ciphertext) == 0 || len(msg.Nonce) == 0 || msg.SenderKeyID == "") {
		return Message{}, ErrBadInput
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

	err := s.pool.QueryRow(ctx, `
		INSERT INTO messages (
			conversation_id, sender, recipient, body, ciphertext, nonce, sender_key_id, conversation_key_version, encrypted,
			recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public, ts
		)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)
		RETURNING id
	`, msg.ConversationID, msg.From, msg.To, msg.Text, msg.Ciphertext, msg.Nonce, msg.SenderKeyID, msg.KeyVersion, msg.Encrypted,
		msg.RecipientSignedPrekeyID, msg.RecipientSignedPrekeyPublic, msg.RecipientOneTimePrekeyID, msg.RecipientOneTimePrekeyPublic, msg.TS).Scan(&msg.ID)
	if err != nil {
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
		SELECT id, conversation_id, sender, recipient, body, ciphertext, nonce, sender_key_id, conversation_key_version, encrypted,
		       recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public, ts
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
		if err := rows.Scan(&msg.ID, &msg.ConversationID, &msg.From, &msg.To, &msg.Text, &msg.Ciphertext, &msg.Nonce, &msg.SenderKeyID, &msg.KeyVersion, &msg.Encrypted,
			&msg.RecipientSignedPrekeyID, &msg.RecipientSignedPrekeyPublic, &msg.RecipientOneTimePrekeyID, &msg.RecipientOneTimePrekeyPublic, &msg.TS); err != nil {
			return nil, err
		}
		out = append(out, msg)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	// Вернем в хронологическом порядке.
	for i, j := 0, len(out)-1; i < j; i, j = i+1, j-1 {
		out[i], out[j] = out[j], out[i]
	}
	return out, nil
}

func (s *PostgresMessageStore) DeleteUser(ctx context.Context, username string) error {
	if username == "" {
		return ErrBadInput
	}

	_, err := s.pool.Exec(ctx, `
		DELETE FROM messages
		WHERE sender = $1 OR recipient = $1
	`, username)
	return err
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
		SELECT id, conversation_id, sender, recipient, body, ciphertext, nonce, sender_key_id, conversation_key_version, encrypted,
		       recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public, ts
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
		if err := rows.Scan(&msg.ID, &msg.ConversationID, &msg.From, &msg.To, &msg.Text, &msg.Ciphertext, &msg.Nonce, &msg.SenderKeyID, &msg.KeyVersion, &msg.Encrypted,
			&msg.RecipientSignedPrekeyID, &msg.RecipientSignedPrekeyPublic, &msg.RecipientOneTimePrekeyID, &msg.RecipientOneTimePrekeyPublic, &msg.TS); err != nil {
			return nil, err
		}
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
		SELECT id, conversation_id, sender, recipient, body, ciphertext, nonce, sender_key_id, conversation_key_version, encrypted,
		       recipient_signed_prekey_id, recipient_signed_prekey_public, recipient_one_time_prekey_id, recipient_one_time_prekey_public, ts
		FROM messages
		WHERE id = $1
	`, id).Scan(&msg.ID, &msg.ConversationID, &msg.From, &msg.To, &msg.Text, &msg.Ciphertext, &msg.Nonce, &msg.SenderKeyID, &msg.KeyVersion, &msg.Encrypted,
		&msg.RecipientSignedPrekeyID, &msg.RecipientSignedPrekeyPublic, &msg.RecipientOneTimePrekeyID, &msg.RecipientOneTimePrekeyPublic, &msg.TS)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Message{}, ErrMessageNotFound
		}
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

func (s *PostgresMessageStore) initSchema(ctx context.Context) error {
	_, err := s.pool.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS messages (
			id BIGSERIAL PRIMARY KEY,
			conversation_id TEXT NOT NULL,
			sender TEXT NOT NULL,
			recipient TEXT NOT NULL,
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
			ts TIMESTAMPTZ NOT NULL
		);
		CREATE INDEX IF NOT EXISTS messages_conversation_ts_idx
			ON messages (conversation_id, ts DESC);
	`)
	return err
}
