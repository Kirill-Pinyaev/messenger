package store

import (
	"context"
	"errors"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgxpool"
)

type PostgresUserStore struct {
	pool *pgxpool.Pool
}

func NewPostgresUserStore(ctx context.Context, databaseURL string) (*PostgresUserStore, error) {
	if databaseURL == "" {
		return nil, ErrNoDatabaseURL
	}

	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		return nil, err
	}

	store := &PostgresUserStore{pool: pool}
	if err := store.initSchema(ctx); err != nil {
		pool.Close()
		return nil, err
	}

	return store, nil
}

func (s *PostgresUserStore) Close() {
	if s.pool != nil {
		s.pool.Close()
	}
}

func (s *PostgresUserStore) Create(ctx context.Context, user User) error {
	if user.Username == "" || len(user.Hash) == 0 || len(user.Salt) == 0 {
		return ErrBadInput
	}

	_, err := s.pool.Exec(ctx, `
		INSERT INTO users (username, first_name, last_name, avatar_hex, avatar_data, salt, hash, created_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
	`, user.Username, user.FirstName, user.LastName, user.AvatarHex, user.AvatarData, user.Salt, user.Hash, user.CreatedAt)
	if err != nil {
		var pgErr *pgconn.PgError
		if errors.As(err, &pgErr) && pgErr.Code == "23505" {
			return ErrUserExists
		}
		return err
	}
	return nil
}

func (s *PostgresUserStore) Get(ctx context.Context, username string) (User, error) {
	if username == "" {
		return User{}, ErrBadInput
	}

	var user User
	err := s.pool.QueryRow(ctx, `
		SELECT username, first_name, last_name, avatar_hex, avatar_data, salt, hash, created_at
		FROM users
		WHERE username = $1
	`, username).Scan(&user.Username, &user.FirstName, &user.LastName, &user.AvatarHex, &user.AvatarData, &user.Salt, &user.Hash, &user.CreatedAt)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return User{}, ErrUserNotFound
		}
		return User{}, err
	}
	return user, nil
}

func (s *PostgresUserStore) Delete(ctx context.Context, username string) error {
	if username == "" {
		return ErrBadInput
	}

	tag, err := s.pool.Exec(ctx, `
		DELETE FROM users WHERE username = $1
	`, username)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		return ErrUserNotFound
	}
	return nil
}

func (s *PostgresUserStore) Search(ctx context.Context, query string, limit int) ([]User, error) {
	query = strings.TrimSpace(query)
	if query == "" {
		return nil, ErrBadInput
	}
	if limit <= 0 {
		limit = 20
	}

	rows, err := s.pool.Query(ctx, `
		SELECT username, first_name, last_name, avatar_hex, avatar_data
		FROM users
		WHERE username ILIKE $1
		ORDER BY username
		LIMIT $2
	`, "%"+query+"%", limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []User
	for rows.Next() {
		var user User
		if err := rows.Scan(&user.Username, &user.FirstName, &user.LastName, &user.AvatarHex, &user.AvatarData); err != nil {
			return nil, err
		}
		out = append(out, user)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return out, nil
}

func (s *PostgresUserStore) GetMany(ctx context.Context, usernames []string) (map[string]User, error) {
	out := make(map[string]User, len(usernames))
	if len(usernames) == 0 {
		return out, nil
	}

	rows, err := s.pool.Query(ctx, `
		SELECT username, first_name, last_name, avatar_hex, avatar_data
		FROM users
		WHERE username = ANY($1)
	`, usernames)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	for rows.Next() {
		var user User
		if err := rows.Scan(&user.Username, &user.FirstName, &user.LastName, &user.AvatarHex, &user.AvatarData); err != nil {
			return nil, err
		}
		out[user.Username] = user
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return out, nil
}

func (s *PostgresUserStore) initSchema(ctx context.Context) error {
	_, err := s.pool.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS users (
			username TEXT PRIMARY KEY,
			first_name TEXT NOT NULL DEFAULT '',
			last_name TEXT NOT NULL DEFAULT '',
			avatar_hex TEXT NOT NULL DEFAULT '',
			avatar_data TEXT NOT NULL DEFAULT '',
			salt BYTEA NOT NULL,
			hash BYTEA NOT NULL,
			created_at TIMESTAMPTZ NOT NULL
		);
		ALTER TABLE users
			ADD COLUMN IF NOT EXISTS first_name TEXT NOT NULL DEFAULT '',
			ADD COLUMN IF NOT EXISTS last_name TEXT NOT NULL DEFAULT '',
			ADD COLUMN IF NOT EXISTS avatar_hex TEXT NOT NULL DEFAULT '',
			ADD COLUMN IF NOT EXISTS avatar_data TEXT NOT NULL DEFAULT '';
	`)
	return err
}

func (s *PostgresUserStore) UpdateProfile(ctx context.Context, username, firstName, lastName, avatarHex, avatarData string) (User, error) {
	if username == "" {
		return User{}, ErrBadInput
	}

	var user User
	err := s.pool.QueryRow(ctx, `
		UPDATE users
		SET first_name = COALESCE(NULLIF($2, ''), first_name),
			last_name = COALESCE(NULLIF($3, ''), last_name),
			avatar_hex = COALESCE(NULLIF($4, ''), avatar_hex),
			avatar_data = COALESCE(NULLIF($5, ''), avatar_data)
		WHERE username = $1
		RETURNING username, first_name, last_name, avatar_hex, avatar_data, salt, hash, created_at
	`, username, firstName, lastName, avatarHex, avatarData).Scan(
		&user.Username, &user.FirstName, &user.LastName, &user.AvatarHex, &user.AvatarData, &user.Salt, &user.Hash, &user.CreatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return User{}, ErrUserNotFound
		}
		return User{}, err
	}
	return user, nil
}
