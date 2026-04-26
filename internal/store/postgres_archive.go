package store

import (
	"context"
	"errors"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type PostgresArchiveStore struct {
	pool *pgxpool.Pool
}

func NewPostgresArchiveStore(ctx context.Context, pool *pgxpool.Pool) (*PostgresArchiveStore, error) {
	s := &PostgresArchiveStore{pool: pool}
	if err := s.initSchema(ctx); err != nil {
		return nil, err
	}
	return s, nil
}

func (s *PostgresArchiveStore) InitializeHeader(ctx context.Context, header ArchiveKeyBundle) (ArchiveKeyBundle, error) {
	if err := validateArchiveHeader(header); err != nil {
		return ArchiveKeyBundle{}, err
	}
	err := s.pool.QueryRow(ctx, `
		INSERT INTO history_archive_headers (
			username, public_key, encrypted_private_key, kdf_salt, kdf_params, archive_version
		)
		VALUES ($1, $2, $3, $4, $5, $6)
		ON CONFLICT (username) DO UPDATE
		SET public_key = EXCLUDED.public_key,
		    encrypted_private_key = EXCLUDED.encrypted_private_key,
		    kdf_salt = EXCLUDED.kdf_salt,
		    kdf_params = EXCLUDED.kdf_params,
		    archive_version = EXCLUDED.archive_version,
		    updated_at = NOW()
		RETURNING updated_at
	`, header.Username, header.PublicKey, header.EncryptedPrivateKey, header.KDFSalt, header.KDFParams, header.Version).Scan(&header.UpdatedAt)
	if err != nil {
		return ArchiveKeyBundle{}, err
	}
	return header, nil
}

func (s *PostgresArchiveStore) GetHeader(ctx context.Context, username string) (ArchiveKeyBundle, error) {
	if strings.TrimSpace(username) == "" {
		return ArchiveKeyBundle{}, ErrBadInput
	}
	var header ArchiveKeyBundle
	err := s.pool.QueryRow(ctx, `
		SELECT username, public_key, encrypted_private_key, kdf_salt, kdf_params, archive_version, updated_at
		FROM history_archive_headers
		WHERE username = $1
	`, username).Scan(&header.Username, &header.PublicKey, &header.EncryptedPrivateKey, &header.KDFSalt, &header.KDFParams, &header.Version, &header.UpdatedAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ArchiveKeyBundle{}, ErrArchiveHeaderNotFound
		}
		return ArchiveKeyBundle{}, err
	}
	return header, nil
}

func (s *PostgresArchiveStore) GetPublicKeys(ctx context.Context, usernames []string) ([]ArchiveKeyBundle, error) {
	if len(usernames) == 0 {
		return nil, nil
	}
	rows, err := s.pool.Query(ctx, `
		SELECT username, public_key, archive_version, updated_at
		FROM history_archive_headers
		WHERE username = ANY($1)
		ORDER BY username
	`, usernames)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []ArchiveKeyBundle
	for rows.Next() {
		var item ArchiveKeyBundle
		if err := rows.Scan(&item.Username, &item.PublicKey, &item.Version, &item.UpdatedAt); err != nil {
			return nil, err
		}
		out = append(out, item)
	}
	return out, rows.Err()
}

func (s *PostgresArchiveStore) AppendRecords(ctx context.Context, records []HistoryArchiveRecord) error {
	if len(records) == 0 {
		return nil
	}
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	for _, record := range records {
		if err := validateArchiveRecord(record); err != nil {
			return err
		}
		if _, err := tx.Exec(ctx, `
			INSERT INTO history_archive_records (
				record_id, owner_username, conversation_id, record_type, message_id,
				attachment_id, group_key_version, sender, created_at, ciphertext,
				nonce, ephemeral_public_key, archive_key_version
			)
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8, COALESCE($9, NOW()), $10, $11, $12, $13)
			ON CONFLICT (owner_username, record_id) DO NOTHING
		`, record.RecordID, record.OwnerUsername, record.ConversationID, string(record.RecordType), record.MessageID, record.AttachmentID, record.GroupKeyVersion, record.Sender, record.CreatedAt, record.Ciphertext, record.Nonce, record.EphemeralPublicKey, record.ArchiveKeyVersion); err != nil {
			return err
		}
	}
	return tx.Commit(ctx)
}

func (s *PostgresArchiveStore) ListRecords(ctx context.Context, username string, afterSequence int64, limit int) ([]HistoryArchiveRecord, error) {
	if strings.TrimSpace(username) == "" {
		return nil, ErrBadInput
	}
	if limit <= 0 {
		limit = 100
	}
	rows, err := s.pool.Query(ctx, `
		SELECT sequence_id, record_id, owner_username, conversation_id, record_type, message_id,
		       attachment_id, group_key_version, sender, created_at, ciphertext,
		       nonce, ephemeral_public_key, archive_key_version
		FROM history_archive_records
		WHERE owner_username = $1 AND sequence_id > $2
		ORDER BY sequence_id
		LIMIT $3
	`, username, afterSequence, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []HistoryArchiveRecord
	for rows.Next() {
		var item HistoryArchiveRecord
		var recordType string
		if err := rows.Scan(&item.Sequence, &item.RecordID, &item.OwnerUsername, &item.ConversationID, &recordType, &item.MessageID, &item.AttachmentID, &item.GroupKeyVersion, &item.Sender, &item.CreatedAt, &item.Ciphertext, &item.Nonce, &item.EphemeralPublicKey, &item.ArchiveKeyVersion); err != nil {
			return nil, err
		}
		item.RecordType = HistoryArchiveRecordType(recordType)
		out = append(out, item)
	}
	return out, rows.Err()
}

func (s *PostgresArchiveStore) DeleteUser(ctx context.Context, username string) error {
	if strings.TrimSpace(username) == "" {
		return ErrBadInput
	}
	_, err := s.pool.Exec(ctx, `
		DELETE FROM history_archive_records WHERE owner_username = $1;
		DELETE FROM history_archive_headers WHERE username = $1;
	`, username)
	return err
}

func (s *PostgresArchiveStore) initSchema(ctx context.Context) error {
	_, err := s.pool.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS history_archive_headers (
			username TEXT PRIMARY KEY,
			public_key BYTEA NOT NULL,
			encrypted_private_key BYTEA NOT NULL,
			kdf_salt BYTEA NOT NULL,
			kdf_params TEXT NOT NULL,
			archive_version INTEGER NOT NULL,
			updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
		);
		CREATE TABLE IF NOT EXISTS history_archive_records (
			sequence_id BIGSERIAL PRIMARY KEY,
			record_id TEXT NOT NULL,
			owner_username TEXT NOT NULL,
			conversation_id TEXT NOT NULL,
			record_type TEXT NOT NULL,
			message_id BIGINT NOT NULL DEFAULT 0,
			attachment_id TEXT NOT NULL DEFAULT '',
			group_key_version INTEGER NOT NULL DEFAULT 0,
			sender TEXT NOT NULL,
			created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
			ciphertext BYTEA NOT NULL,
			nonce BYTEA NOT NULL,
			ephemeral_public_key BYTEA NOT NULL,
			archive_key_version INTEGER NOT NULL,
			UNIQUE (owner_username, record_id)
		);
		CREATE INDEX IF NOT EXISTS history_archive_records_owner_seq_idx
			ON history_archive_records (owner_username, sequence_id);
	`)
	return err
}
