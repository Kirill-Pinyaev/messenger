package store

import (
	"context"
	"errors"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type PostgresConversationStore struct {
	pool *pgxpool.Pool
}

func NewPostgresConversationStore(ctx context.Context, pool *pgxpool.Pool) (*PostgresConversationStore, error) {
	s := &PostgresConversationStore{pool: pool}
	if err := s.initSchema(ctx); err != nil {
		return nil, err
	}
	return s, nil
}

func (s *PostgresConversationStore) CreateGroupConversation(ctx context.Context, conversation Conversation) (Conversation, error) {
	conversation, err := normalizeGroupConversation(conversation)
	if err != nil {
		return Conversation{}, err
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return Conversation{}, err
	}
	defer tx.Rollback(ctx)

	if _, err := tx.Exec(ctx, `
		INSERT INTO conversations (id, kind, title)
		VALUES ($1, $2, $3)
	`, conversation.ID, "group", conversation.Title); err != nil {
		return Conversation{}, err
	}

	for _, member := range conversation.Members {
		if _, err := tx.Exec(ctx, `
			INSERT INTO conversation_members (conversation_id, username, role, added_by, joined_order)
			VALUES ($1, $2, $3, $4, $5)
		`, conversation.ID, member.Username, conversationRoleDB(member.Role), nullString(member.AddedBy), member.JoinedOrder); err != nil {
			return Conversation{}, err
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return Conversation{}, err
	}
	return conversation, nil
}

func (s *PostgresConversationStore) GetConversation(ctx context.Context, conversationID string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" {
		return Conversation{}, ErrBadInput
	}

	var id, kind, title string
	err := s.pool.QueryRow(ctx, `
		SELECT id, kind, title
		FROM conversations
		WHERE id = $1
	`, conversationID).Scan(&id, &kind, &title)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Conversation{}, ErrConversationNotFound
		}
		return Conversation{}, err
	}

	rows, err := s.pool.Query(ctx, `
		SELECT username, role, COALESCE(added_by, ''), joined_order
		FROM conversation_members
		WHERE conversation_id = $1
		ORDER BY joined_order, username
	`, conversationID)
	if err != nil {
		return Conversation{}, err
	}
	defer rows.Close()

	conversation := Conversation{
		ID:    id,
		Title: title,
	}
	if kind == "group" {
		conversation.Kind = ConversationKindGroup
	}

	for rows.Next() {
		var member ConversationMember
		var role string
		if err := rows.Scan(&member.Username, &role, &member.AddedBy, &member.JoinedOrder); err != nil {
			return Conversation{}, err
		}
		member.Role = conversationRoleFromDB(role)
		conversation.Members = append(conversation.Members, member)
	}
	if err := rows.Err(); err != nil {
		return Conversation{}, err
	}

	return conversation, nil
}

func (s *PostgresConversationStore) ListGroupConversationsForUser(ctx context.Context, username string) ([]Conversation, error) {
	if strings.TrimSpace(username) == "" {
		return nil, ErrBadInput
	}

	rows, err := s.pool.Query(ctx, `
		SELECT c.id
		FROM conversations c
		JOIN conversation_members cm ON cm.conversation_id = c.id
		WHERE c.kind = 'group' AND cm.username = $1
		ORDER BY c.id
	`, username)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []Conversation
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		conversation, err := s.GetConversation(ctx, id)
		if err != nil {
			return nil, err
		}
		out = append(out, conversation)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return out, nil
}

func (s *PostgresConversationStore) AddMembers(ctx context.Context, conversationID, addedBy string, usernames []string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" || strings.TrimSpace(addedBy) == "" || len(usernames) == 0 {
		return Conversation{}, ErrBadInput
	}

	conversation, err := s.GetConversation(ctx, conversationID)
	if err != nil {
		return Conversation{}, err
	}
	if !conversation.HasMember(addedBy) {
		return Conversation{}, ErrConversationForbidden
	}

	memberSet := make(map[string]struct{}, len(conversation.Members))
	var maxOrder int64
	for _, member := range conversation.Members {
		memberSet[member.Username] = struct{}{}
		if member.JoinedOrder > maxOrder {
			maxOrder = member.JoinedOrder
		}
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return Conversation{}, err
	}
	defer tx.Rollback(ctx)

	for _, username := range usernames {
		username = strings.TrimSpace(username)
		if username == "" {
			return Conversation{}, ErrBadInput
		}
		if _, ok := memberSet[username]; ok {
			continue
		}
		maxOrder++
		memberSet[username] = struct{}{}
		if _, err := tx.Exec(ctx, `
			INSERT INTO conversation_members (conversation_id, username, role, added_by, joined_order)
			VALUES ($1, $2, 'member', $3, $4)
			ON CONFLICT (conversation_id, username) DO NOTHING
		`, conversationID, username, addedBy, maxOrder); err != nil {
			return Conversation{}, err
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return Conversation{}, err
	}
	return s.GetConversation(ctx, conversationID)
}

func (s *PostgresConversationStore) RemoveMember(ctx context.Context, conversationID, actor, username string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" || strings.TrimSpace(actor) == "" || strings.TrimSpace(username) == "" {
		return Conversation{}, ErrBadInput
	}
	if actor == username {
		return Conversation{}, ErrBadInput
	}

	conversation, err := s.GetConversation(ctx, conversationID)
	if err != nil {
		return Conversation{}, err
	}
	if err := ensureCanRemove(conversation, actor, username); err != nil {
		return Conversation{}, err
	}

	if _, err := s.pool.Exec(ctx, `
		DELETE FROM conversation_members
		WHERE conversation_id = $1 AND username = $2
	`, conversationID, username); err != nil {
		return Conversation{}, err
	}

	return s.GetConversation(ctx, conversationID)
}

func (s *PostgresConversationStore) LeaveConversation(ctx context.Context, conversationID, username string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" || strings.TrimSpace(username) == "" {
		return Conversation{}, ErrBadInput
	}

	conversation, err := s.GetConversation(ctx, conversationID)
	if err != nil {
		return Conversation{}, err
	}
	member, ok := conversation.Member(username)
	if !ok {
		return Conversation{}, ErrConversationForbidden
	}

	nextConversation := cloneConversation(conversation)
	nextConversation.Members = removeConversationMember(nextConversation.Members, username)
	if member.Role == ConversationRoleAdmin {
		promoteNextAdmin(nextConversation.Members)
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return Conversation{}, err
	}
	defer tx.Rollback(ctx)

	if _, err := tx.Exec(ctx, `
		DELETE FROM conversation_members
		WHERE conversation_id = $1 AND username = $2
	`, conversationID, username); err != nil {
		return Conversation{}, err
	}

	if len(nextConversation.Members) == 0 {
		if _, err := tx.Exec(ctx, `
			DELETE FROM conversations
			WHERE id = $1
		`, conversationID); err != nil {
			return Conversation{}, err
		}
		if err := tx.Commit(ctx); err != nil {
			return Conversation{}, err
		}
		return Conversation{ID: conversationID, Kind: ConversationKindGroup, Title: conversation.Title}, nil
	}

	if member.Role == ConversationRoleAdmin {
		if _, err := tx.Exec(ctx, `
			UPDATE conversation_members
			SET role = 'member'
			WHERE conversation_id = $1
		`, conversationID); err != nil {
			return Conversation{}, err
		}
		if _, err := tx.Exec(ctx, `
			UPDATE conversation_members
			SET role = 'admin'
			WHERE conversation_id = $1 AND username = $2
		`, conversationID, nextConversation.Members[0].Username); err != nil {
			return Conversation{}, err
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return Conversation{}, err
	}
	return s.GetConversation(ctx, conversationID)
}

func (s *PostgresConversationStore) TransferAdmin(ctx context.Context, conversationID, username string) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" || strings.TrimSpace(username) == "" {
		return Conversation{}, ErrBadInput
	}

	conversation, err := s.GetConversation(ctx, conversationID)
	if err != nil {
		return Conversation{}, err
	}
	if _, ok := conversation.Member(username); !ok {
		return Conversation{}, ErrBadInput
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return Conversation{}, err
	}
	defer tx.Rollback(ctx)

	if _, err := tx.Exec(ctx, `
		UPDATE conversation_members
		SET role = 'member'
		WHERE conversation_id = $1
	`, conversationID); err != nil {
		return Conversation{}, err
	}
	if _, err := tx.Exec(ctx, `
		UPDATE conversation_members
		SET role = 'admin'
		WHERE conversation_id = $1 AND username = $2
	`, conversationID, username); err != nil {
		return Conversation{}, err
	}

	if err := tx.Commit(ctx); err != nil {
		return Conversation{}, err
	}
	return s.GetConversation(ctx, conversationID)
}

func (s *PostgresConversationStore) DeleteUser(ctx context.Context, username string) error {
	if strings.TrimSpace(username) == "" {
		return ErrBadInput
	}

	rows, err := s.pool.Query(ctx, `
		SELECT conversation_id
		FROM conversation_members
		WHERE username = $1
	`, username)
	if err != nil {
		return err
	}
	defer rows.Close()

	var conversationIDs []string
	for rows.Next() {
		var conversationID string
		if err := rows.Scan(&conversationID); err != nil {
			return err
		}
		conversationIDs = append(conversationIDs, conversationID)
	}
	if err := rows.Err(); err != nil {
		return err
	}

	for _, conversationID := range conversationIDs {
		if _, err := s.LeaveConversation(ctx, conversationID, username); err != nil && !errors.Is(err, ErrConversationNotFound) {
			return err
		}
	}
	return nil
}

func (s *PostgresConversationStore) initSchema(ctx context.Context) error {
	_, err := s.pool.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS conversations (
			id TEXT PRIMARY KEY,
			kind TEXT NOT NULL,
			title TEXT NOT NULL DEFAULT ''
		);
		CREATE TABLE IF NOT EXISTS conversation_members (
			conversation_id TEXT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
			username TEXT NOT NULL,
			role TEXT NOT NULL DEFAULT 'member',
			added_by TEXT,
			joined_order BIGINT NOT NULL,
			PRIMARY KEY (conversation_id, username)
		);
		CREATE INDEX IF NOT EXISTS conversation_members_username_idx
			ON conversation_members (username);
	`)
	return err
}

func conversationRoleDB(role ConversationRole) string {
	if role == ConversationRoleAdmin {
		return "admin"
	}
	return "member"
}

func conversationRoleFromDB(role string) ConversationRole {
	if role == "admin" {
		return ConversationRoleAdmin
	}
	return ConversationRoleMember
}

func nullString(value string) any {
	if strings.TrimSpace(value) == "" {
		return nil
	}
	return value
}
