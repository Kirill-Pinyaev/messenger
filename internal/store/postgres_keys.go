package store

import (
	"context"
	"errors"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type PostgresKeyStore struct {
	pool *pgxpool.Pool
}

func NewPostgresKeyStore(ctx context.Context, pool *pgxpool.Pool) (*PostgresKeyStore, error) {
	s := &PostgresKeyStore{pool: pool}
	if err := s.initSchema(ctx); err != nil {
		return nil, err
	}
	return s, nil
}

func (s *PostgresKeyStore) UpsertIdentityKey(ctx context.Context, key IdentityKey) (IdentityKey, error) {
	if err := validateIdentityKey(key); err != nil {
		return IdentityKey{}, err
	}

	err := s.pool.QueryRow(ctx, `
		INSERT INTO identity_key_devices (username, device_id, key_id, algorithm, public_key)
		VALUES ($1, $2, $3, $4, $5)
		ON CONFLICT (username, device_id) DO UPDATE
		SET key_id = EXCLUDED.key_id,
			algorithm = EXCLUDED.algorithm,
			public_key = EXCLUDED.public_key,
			published_at = NOW()
		RETURNING published_at
	`, key.Username, key.DeviceID, key.KeyID, key.Algorithm, key.PublicKey).Scan(&key.PublishedAt)
	if err != nil {
		return IdentityKey{}, err
	}
	return key, nil
}

func (s *PostgresKeyStore) GetIdentityKey(ctx context.Context, username, deviceID string) (IdentityKey, error) {
	if strings.TrimSpace(username) == "" || strings.TrimSpace(deviceID) == "" {
		return IdentityKey{}, ErrBadInput
	}

	var key IdentityKey
	err := s.pool.QueryRow(ctx, `
		SELECT username, device_id, key_id, algorithm, public_key, published_at
		FROM identity_key_devices
		WHERE username = $1 AND device_id = $2
	`, username, deviceID).Scan(&key.Username, &key.DeviceID, &key.KeyID, &key.Algorithm, &key.PublicKey, &key.PublishedAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return IdentityKey{}, ErrIdentityKeyNotFound
		}
		return IdentityKey{}, err
	}
	return key, nil
}

func (s *PostgresKeyStore) GetIdentityKeys(ctx context.Context, usernames []string) ([]IdentityKey, error) {
	if len(usernames) == 0 {
		return nil, nil
	}

	rows, err := s.pool.Query(ctx, `
		SELECT username, device_id, key_id, algorithm, public_key, published_at
		FROM identity_key_devices
		WHERE username = ANY($1)
		ORDER BY username, device_id
	`, usernames)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []IdentityKey
	for rows.Next() {
		var key IdentityKey
		if err := rows.Scan(&key.Username, &key.DeviceID, &key.KeyID, &key.Algorithm, &key.PublicKey, &key.PublishedAt); err != nil {
			return nil, err
		}
		out = append(out, key)
	}
	return out, rows.Err()
}

func (s *PostgresKeyStore) UpsertSignedPrekey(ctx context.Context, key SignedPrekey) (SignedPrekey, error) {
	if err := validateSignedPrekey(key); err != nil {
		return SignedPrekey{}, err
	}

	err := s.pool.QueryRow(ctx, `
		INSERT INTO signed_prekey_devices (username, device_id, key_id, algorithm, public_key, signature, signature_algorithm)
		VALUES ($1, $2, $3, $4, $5, $6, $7)
		ON CONFLICT (username, device_id) DO UPDATE
		SET key_id = EXCLUDED.key_id,
			algorithm = EXCLUDED.algorithm,
			public_key = EXCLUDED.public_key,
			signature = EXCLUDED.signature,
			signature_algorithm = EXCLUDED.signature_algorithm,
			published_at = NOW()
		RETURNING published_at
	`, key.Username, key.DeviceID, key.KeyID, key.Algorithm, key.PublicKey, key.Signature, key.SignatureAlgorithm).Scan(&key.PublishedAt)
	if err != nil {
		return SignedPrekey{}, err
	}
	return key, nil
}

func (s *PostgresKeyStore) GetSignedPrekey(ctx context.Context, username, deviceID string) (SignedPrekey, error) {
	if strings.TrimSpace(username) == "" || strings.TrimSpace(deviceID) == "" {
		return SignedPrekey{}, ErrBadInput
	}

	var key SignedPrekey
	err := s.pool.QueryRow(ctx, `
		SELECT username, device_id, key_id, algorithm, public_key, signature, signature_algorithm, published_at
		FROM signed_prekey_devices
		WHERE username = $1 AND device_id = $2
	`, username, deviceID).Scan(&key.Username, &key.DeviceID, &key.KeyID, &key.Algorithm, &key.PublicKey, &key.Signature, &key.SignatureAlgorithm, &key.PublishedAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return SignedPrekey{}, ErrSignedPrekeyNotFound
		}
		return SignedPrekey{}, err
	}
	return key, nil
}

func (s *PostgresKeyStore) PutOneTimePrekeys(ctx context.Context, username, deviceID string, keys []OneTimePrekey) error {
	if strings.TrimSpace(username) == "" || strings.TrimSpace(deviceID) == "" {
		return ErrBadInput
	}
	if len(keys) == 0 {
		return nil
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)

	for _, key := range keys {
		key.Username = username
		key.DeviceID = deviceID
		if err := validateOneTimePrekey(key); err != nil {
			return err
		}
		if _, err := tx.Exec(ctx, `
			INSERT INTO one_time_prekey_devices (username, device_id, key_id, algorithm, public_key)
			VALUES ($1, $2, $3, $4, $5)
			ON CONFLICT (username, device_id, key_id) DO UPDATE
			SET algorithm = EXCLUDED.algorithm,
				public_key = EXCLUDED.public_key,
				published_at = NOW(),
				consumed_at = NULL
		`, key.Username, key.DeviceID, key.KeyID, key.Algorithm, key.PublicKey); err != nil {
			return err
		}
	}
	return tx.Commit(ctx)
}

func (s *PostgresKeyStore) DeleteOneTimePrekeys(ctx context.Context, username, deviceID string) error {
	if strings.TrimSpace(username) == "" || strings.TrimSpace(deviceID) == "" {
		return ErrBadInput
	}
	_, err := s.pool.Exec(ctx, `
		DELETE FROM one_time_prekey_devices
		WHERE username = $1 AND device_id = $2
	`, username, deviceID)
	return err
}

func (s *PostgresKeyStore) AcquirePrekeyBundle(ctx context.Context, username, deviceID string) (PrekeyBundle, error) {
	if strings.TrimSpace(username) == "" || strings.TrimSpace(deviceID) == "" {
		return PrekeyBundle{}, ErrBadInput
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return PrekeyBundle{}, err
	}
	defer tx.Rollback(ctx)

	var bundle PrekeyBundle
	bundle.Username = username
	bundle.DeviceID = deviceID
	if err := tx.QueryRow(ctx, `
		SELECT username, device_id, key_id, algorithm, public_key, published_at
		FROM identity_key_devices
		WHERE username = $1 AND device_id = $2
	`, username, deviceID).Scan(&bundle.IdentityKey.Username, &bundle.IdentityKey.DeviceID, &bundle.IdentityKey.KeyID, &bundle.IdentityKey.Algorithm, &bundle.IdentityKey.PublicKey, &bundle.IdentityKey.PublishedAt); err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return PrekeyBundle{}, ErrIdentityKeyNotFound
		}
		return PrekeyBundle{}, err
	}

	if err := tx.QueryRow(ctx, `
		SELECT username, device_id, key_id, algorithm, public_key, signature, signature_algorithm, published_at
		FROM signed_prekey_devices
		WHERE username = $1 AND device_id = $2
	`, username, deviceID).Scan(&bundle.SignedPrekey.Username, &bundle.SignedPrekey.DeviceID, &bundle.SignedPrekey.KeyID, &bundle.SignedPrekey.Algorithm, &bundle.SignedPrekey.PublicKey, &bundle.SignedPrekey.Signature, &bundle.SignedPrekey.SignatureAlgorithm, &bundle.SignedPrekey.PublishedAt); err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return PrekeyBundle{}, ErrSignedPrekeyNotFound
		}
		return PrekeyBundle{}, err
	}

	err = tx.QueryRow(ctx, `
		WITH picked AS (
			SELECT username, device_id, key_id, algorithm, public_key, published_at
			FROM one_time_prekey_devices
			WHERE username = $1 AND device_id = $2 AND consumed_at IS NULL
			ORDER BY published_at, key_id
			LIMIT 1
			FOR UPDATE SKIP LOCKED
		)
		UPDATE one_time_prekey_devices otp
		SET consumed_at = NOW()
		FROM picked
		WHERE otp.username = picked.username
		  AND otp.device_id = picked.device_id
		  AND otp.key_id = picked.key_id
		RETURNING picked.username, picked.device_id, picked.key_id, picked.algorithm, picked.public_key, picked.published_at
	`, username, deviceID).Scan(&bundle.OneTimePrekey.Username, &bundle.OneTimePrekey.DeviceID, &bundle.OneTimePrekey.KeyID, &bundle.OneTimePrekey.Algorithm, &bundle.OneTimePrekey.PublicKey, &bundle.OneTimePrekey.PublishedAt)
	if err != nil && !errors.Is(err, pgx.ErrNoRows) {
		return PrekeyBundle{}, err
	}

	if err := tx.Commit(ctx); err != nil {
		return PrekeyBundle{}, err
	}
	return bundle, nil
}

func (s *PostgresKeyStore) AcquirePrekeyBundles(ctx context.Context, username string) ([]PrekeyBundle, error) {
	if strings.TrimSpace(username) == "" {
		return nil, ErrBadInput
	}

	rows, err := s.pool.Query(ctx, `
		SELECT i.username, i.device_id, i.key_id, i.algorithm, i.public_key, i.published_at,
		       s.key_id, s.algorithm, s.public_key, s.signature, s.signature_algorithm, s.published_at
		FROM identity_key_devices i
		JOIN signed_prekey_devices s
		  ON s.username = i.username AND s.device_id = i.device_id
		WHERE i.username = $1
		ORDER BY i.device_id
	`, username)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []PrekeyBundle
	for rows.Next() {
		var bundle PrekeyBundle
		bundle.Username = username
		if err := rows.Scan(
			&bundle.IdentityKey.Username,
			&bundle.IdentityKey.DeviceID,
			&bundle.IdentityKey.KeyID,
			&bundle.IdentityKey.Algorithm,
			&bundle.IdentityKey.PublicKey,
			&bundle.IdentityKey.PublishedAt,
			&bundle.SignedPrekey.KeyID,
			&bundle.SignedPrekey.Algorithm,
			&bundle.SignedPrekey.PublicKey,
			&bundle.SignedPrekey.Signature,
			&bundle.SignedPrekey.SignatureAlgorithm,
			&bundle.SignedPrekey.PublishedAt,
		); err != nil {
			return nil, err
		}
		bundle.DeviceID = bundle.IdentityKey.DeviceID
		bundle.SignedPrekey.Username = username
		bundle.SignedPrekey.DeviceID = bundle.IdentityKey.DeviceID
		out = append(out, bundle)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	for i := range out {
		_ = s.pool.QueryRow(ctx, `
			SELECT username, device_id, key_id, algorithm, public_key, published_at
			FROM one_time_prekey_devices
			WHERE username = $1 AND device_id = $2 AND consumed_at IS NULL
			ORDER BY published_at, key_id
			LIMIT 1
		`, username, out[i].DeviceID).Scan(
			&out[i].OneTimePrekey.Username,
			&out[i].OneTimePrekey.DeviceID,
			&out[i].OneTimePrekey.KeyID,
			&out[i].OneTimePrekey.Algorithm,
			&out[i].OneTimePrekey.PublicKey,
			&out[i].OneTimePrekey.PublishedAt,
		)
	}

	return out, nil
}

func (s *PostgresKeyStore) UpsertConversationKey(ctx context.Context, key ConversationKey) (ConversationKey, error) {
	if err := validateConversationKey(key); err != nil {
		return ConversationKey{}, err
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return ConversationKey{}, err
	}
	defer tx.Rollback(ctx)

	err = tx.QueryRow(ctx, `
		INSERT INTO conversation_keys (conversation_id, version, algorithm, created_by)
		VALUES ($1, $2, $3, $4)
		ON CONFLICT (conversation_id, version) DO UPDATE
		SET algorithm = EXCLUDED.algorithm,
			created_by = EXCLUDED.created_by,
			created_at = NOW()
		RETURNING created_at
	`, key.ConversationID, key.Version, key.Algorithm, key.CreatedBy).Scan(&key.CreatedAt)
	if err != nil {
		return ConversationKey{}, err
	}

	if _, err := tx.Exec(ctx, `
		DELETE FROM conversation_key_envelopes
		WHERE conversation_id = $1 AND version = $2
	`, key.ConversationID, key.Version); err != nil {
		return ConversationKey{}, err
	}

	for _, envelope := range key.Envelopes {
		if _, err := tx.Exec(ctx, `
			INSERT INTO conversation_key_envelopes (
				conversation_id, version, username, device_id, encrypted_key, nonce, sender_key_id, recipient_key_id
			) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
		`, key.ConversationID, key.Version, envelope.Username, envelope.DeviceID, envelope.EncryptedKey, envelope.Nonce, envelope.SenderKeyID, envelope.RecipientKeyID); err != nil {
			return ConversationKey{}, err
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return ConversationKey{}, err
	}
	return s.GetConversationKey(ctx, key.ConversationID, key.Version)
}

func (s *PostgresKeyStore) GetConversationKey(ctx context.Context, conversationID string, version int32) (ConversationKey, error) {
	if strings.TrimSpace(conversationID) == "" {
		return ConversationKey{}, ErrBadInput
	}

	var key ConversationKey
	query := `
		SELECT conversation_id, version, algorithm, created_by, created_at
		FROM conversation_keys
		WHERE conversation_id = $1 AND version = $2
	`
	args := []any{conversationID, version}
	if version <= 0 {
		query = `
			SELECT conversation_id, version, algorithm, created_by, created_at
			FROM conversation_keys
			WHERE conversation_id = $1
			ORDER BY version DESC
			LIMIT 1
		`
		args = []any{conversationID}
	}
	err := s.pool.QueryRow(ctx, query, args...).Scan(&key.ConversationID, &key.Version, &key.Algorithm, &key.CreatedBy, &key.CreatedAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return ConversationKey{}, ErrConversationKeyNotFound
		}
		return ConversationKey{}, err
	}

	rows, err := s.pool.Query(ctx, `
		SELECT username, device_id, encrypted_key, nonce, sender_key_id, recipient_key_id
		FROM conversation_key_envelopes
		WHERE conversation_id = $1 AND version = $2
		ORDER BY username, device_id
	`, conversationID, key.Version)
	if err != nil {
		return ConversationKey{}, err
	}
	defer rows.Close()

	for rows.Next() {
		var envelope ConversationKeyEnvelope
		if err := rows.Scan(&envelope.Username, &envelope.DeviceID, &envelope.EncryptedKey, &envelope.Nonce, &envelope.SenderKeyID, &envelope.RecipientKeyID); err != nil {
			return ConversationKey{}, err
		}
		key.Envelopes = append(key.Envelopes, envelope)
	}
	return key, rows.Err()
}

func (s *PostgresKeyStore) DeleteUser(ctx context.Context, username string) error {
	if strings.TrimSpace(username) == "" {
		return ErrBadInput
	}
	_, err := s.pool.Exec(ctx, `
		DELETE FROM identity_key_devices WHERE username = $1;
		DELETE FROM signed_prekey_devices WHERE username = $1;
		DELETE FROM one_time_prekey_devices WHERE username = $1;
		DELETE FROM conversation_key_envelopes WHERE username = $1;
	`, username)
	return err
}

func (s *PostgresKeyStore) initSchema(ctx context.Context) error {
	_, err := s.pool.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS identity_key_devices (
			username TEXT NOT NULL,
			device_id TEXT NOT NULL,
			key_id TEXT NOT NULL,
			algorithm TEXT NOT NULL,
			public_key BYTEA NOT NULL,
			signature BYTEA NOT NULL DEFAULT ''::bytea,
			signature_algorithm TEXT NOT NULL DEFAULT '',
			published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
			PRIMARY KEY (username, device_id)
		);
		CREATE TABLE IF NOT EXISTS signed_prekey_devices (
			username TEXT NOT NULL,
			device_id TEXT NOT NULL,
			key_id TEXT NOT NULL,
			algorithm TEXT NOT NULL,
			public_key BYTEA NOT NULL,
			published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
			PRIMARY KEY (username, device_id)
		);
		CREATE TABLE IF NOT EXISTS one_time_prekey_devices (
			username TEXT NOT NULL,
			device_id TEXT NOT NULL,
			key_id TEXT NOT NULL,
			algorithm TEXT NOT NULL,
			public_key BYTEA NOT NULL,
			published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
			consumed_at TIMESTAMPTZ,
			PRIMARY KEY (username, device_id, key_id)
		);
		CREATE TABLE IF NOT EXISTS conversation_keys (
			conversation_id TEXT NOT NULL,
			version INTEGER NOT NULL,
			algorithm TEXT NOT NULL,
			created_by TEXT NOT NULL,
			created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
			PRIMARY KEY (conversation_id, version)
		);
		CREATE TABLE IF NOT EXISTS conversation_key_envelopes (
			conversation_id TEXT NOT NULL,
			version INTEGER NOT NULL,
			username TEXT NOT NULL,
			device_id TEXT NOT NULL,
			encrypted_key BYTEA NOT NULL,
			nonce BYTEA NOT NULL,
			sender_key_id TEXT NOT NULL,
			recipient_key_id TEXT NOT NULL,
			PRIMARY KEY (conversation_id, version, username, device_id)
		);
		ALTER TABLE signed_prekey_devices ADD COLUMN IF NOT EXISTS signature BYTEA NOT NULL DEFAULT ''::bytea;
		ALTER TABLE signed_prekey_devices ADD COLUMN IF NOT EXISTS signature_algorithm TEXT NOT NULL DEFAULT '';
	`)
	return err
}
