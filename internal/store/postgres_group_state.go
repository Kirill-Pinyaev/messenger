package store

import (
	"context"
	"errors"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

type PostgresGroupStateStore struct {
	pool *pgxpool.Pool
}

func (s *PostgresGroupStateStore) CreateConversationWithKey(ctx context.Context, conversation Conversation, key ConversationKey) (Conversation, ConversationKey, error) {
	conversation, err := normalizeGroupConversation(conversation)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	key.ConversationID = conversation.ID
	if err := validateGroupKeyMembers(conversation, key); err != nil {
		return Conversation{}, ConversationKey{}, err
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	defer tx.Rollback(ctx)

	if err := insertConversationTx(ctx, tx, conversation); err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	if err := upsertConversationKeyTx(ctx, tx, key); err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	if err := tx.Commit(ctx); err != nil {
		return Conversation{}, ConversationKey{}, err
	}

	keyStore := PostgresKeyStore{pool: s.pool}
	groupKey, err := keyStore.GetConversationKey(ctx, key.ConversationID, key.Version)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	return conversation, groupKey, nil
}

func (s *PostgresGroupStateStore) AddMembersWithKey(ctx context.Context, conversationID, addedBy string, usernames []string, key ConversationKey) (Conversation, ConversationKey, error) {
	conversation, err := s.applyMembershipMutation(ctx, conversationID, func(conversation Conversation) (Conversation, error) {
		return applyAddMembers(conversation, addedBy, usernames)
	}, func(ctx context.Context, tx pgx.Tx, next Conversation) error {
		memberSet := map[string]struct{}{}
		var maxOrder int64
		for _, member := range next.Members {
			memberSet[member.Username] = struct{}{}
			if member.JoinedOrder > maxOrder {
				maxOrder = member.JoinedOrder
			}
		}
		current, err := loadConversationTx(ctx, tx, conversationID)
		if err != nil {
			return err
		}
		existing := map[string]struct{}{}
		for _, member := range current.Members {
			existing[member.Username] = struct{}{}
		}
		for _, username := range usernames {
			username = strings.TrimSpace(username)
			if _, ok := existing[username]; ok {
				continue
			}
			maxOrder++
			if _, err := tx.Exec(ctx, `
				INSERT INTO conversation_members (conversation_id, username, role, added_by, joined_order)
				VALUES ($1, $2, 'member', $3, $4)
			`, conversationID, username, addedBy, maxOrder); err != nil {
				return err
			}
		}
		_ = memberSet
		return nil
	}, key)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	keyStore := PostgresKeyStore{pool: s.pool}
	groupKey, err := keyStore.GetConversationKey(ctx, key.ConversationID, key.Version)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	return conversation, groupKey, nil
}

func (s *PostgresGroupStateStore) RemoveMemberWithKey(ctx context.Context, conversationID, actor, username string, key ConversationKey) (Conversation, ConversationKey, error) {
	conversation, err := s.applyMembershipMutation(ctx, conversationID, func(conversation Conversation) (Conversation, error) {
		if err := ensureCanRemove(conversation, actor, username); err != nil {
			return Conversation{}, err
		}
		next := cloneConversation(conversation)
		next.Members = removeConversationMember(next.Members, username)
		return next, nil
	}, func(ctx context.Context, tx pgx.Tx, _ Conversation) error {
		_, err := tx.Exec(ctx, `
			DELETE FROM conversation_members
			WHERE conversation_id = $1 AND username = $2
		`, conversationID, username)
		return err
	}, key)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	keyStore := PostgresKeyStore{pool: s.pool}
	groupKey, err := keyStore.GetConversationKey(ctx, key.ConversationID, key.Version)
	if err != nil {
		return Conversation{}, ConversationKey{}, err
	}
	return conversation, groupKey, nil
}

func (s *PostgresGroupStateStore) LeaveConversationWithKey(ctx context.Context, conversationID, username string, key *ConversationKey) (Conversation, *ConversationKey, error) {
	conversation, err := s.applyMembershipMutationOptionalKey(ctx, conversationID, func(conversation Conversation) (Conversation, error) {
		member, ok := conversation.Member(username)
		if !ok {
			return Conversation{}, ErrConversationForbidden
		}
		next := cloneConversation(conversation)
		next.Members = removeConversationMember(next.Members, username)
		if member.Role == ConversationRoleAdmin {
			promoteNextAdmin(next.Members)
		}
		return next, nil
	}, func(ctx context.Context, tx pgx.Tx, next Conversation) error {
		if _, err := tx.Exec(ctx, `
			DELETE FROM conversation_members WHERE conversation_id = $1 AND username = $2
		`, conversationID, username); err != nil {
			return err
		}
		if len(next.Members) == 0 {
			_, err := tx.Exec(ctx, `DELETE FROM conversations WHERE id = $1`, conversationID)
			return err
		}
		var nextAdmin string
		for _, member := range next.Members {
			if member.Role == ConversationRoleAdmin {
				nextAdmin = member.Username
				break
			}
		}
		if nextAdmin != "" {
			if _, err := tx.Exec(ctx, `
				UPDATE conversation_members SET role = 'member' WHERE conversation_id = $1
			`, conversationID); err != nil {
				return err
			}
			if _, err := tx.Exec(ctx, `
				UPDATE conversation_members SET role = 'admin' WHERE conversation_id = $1 AND username = $2
			`, conversationID, nextAdmin); err != nil {
				return err
			}
		}
		return nil
	}, key)
	if err != nil {
		return Conversation{}, nil, err
	}
	if key == nil || len(conversation.Members) == 0 {
		return conversation, nil, nil
	}
	keyStore := PostgresKeyStore{pool: s.pool}
	groupKey, err := keyStore.GetConversationKey(ctx, key.ConversationID, key.Version)
	if err != nil {
		return Conversation{}, nil, err
	}
	return conversation, &groupKey, nil
}

func (s *PostgresGroupStateStore) applyMembershipMutation(ctx context.Context, conversationID string, mutate func(Conversation) (Conversation, error), apply func(context.Context, pgx.Tx, Conversation) error, key ConversationKey) (Conversation, error) {
	next, err := s.applyMembershipMutationOptionalKey(ctx, conversationID, mutate, apply, &key)
	return next, err
}

func (s *PostgresGroupStateStore) applyMembershipMutationOptionalKey(ctx context.Context, conversationID string, mutate func(Conversation) (Conversation, error), apply func(context.Context, pgx.Tx, Conversation) error, key *ConversationKey) (Conversation, error) {
	if strings.TrimSpace(conversationID) == "" {
		return Conversation{}, ErrBadInput
	}

	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return Conversation{}, err
	}
	defer tx.Rollback(ctx)

	current, err := loadConversationTx(ctx, tx, conversationID)
	if err != nil {
		return Conversation{}, err
	}
	next, err := mutate(current)
	if err != nil {
		return Conversation{}, err
	}
	if len(next.Members) > 0 {
		if key == nil {
			return Conversation{}, ErrBadInput
		}
		if err := validateGroupKeyMembers(next, *key); err != nil {
			return Conversation{}, err
		}
	}
	if err := apply(ctx, tx, next); err != nil {
		return Conversation{}, err
	}
	if key != nil && len(next.Members) > 0 {
		if err := upsertConversationKeyTx(ctx, tx, *key); err != nil {
			return Conversation{}, err
		}
	}
	if err := tx.Commit(ctx); err != nil {
		return Conversation{}, err
	}
	return next, nil
}

func applyAddMembers(conversation Conversation, addedBy string, usernames []string) (Conversation, error) {
	if !conversation.HasMember(addedBy) {
		return Conversation{}, ErrConversationForbidden
	}
	next := cloneConversation(conversation)
	memberSet := make(map[string]struct{}, len(next.Members))
	var maxOrder int64
	for _, member := range next.Members {
		memberSet[member.Username] = struct{}{}
		if member.JoinedOrder > maxOrder {
			maxOrder = member.JoinedOrder
		}
	}
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
		next.Members = append(next.Members, ConversationMember{
			Username: username, Role: ConversationRoleMember, AddedBy: addedBy, JoinedOrder: maxOrder,
		})
	}
	return next, nil
}

func insertConversationTx(ctx context.Context, tx pgx.Tx, conversation Conversation) error {
	if _, err := tx.Exec(ctx, `
		INSERT INTO conversations (id, kind, title) VALUES ($1, $2, $3)
	`, conversation.ID, "group", conversation.Title); err != nil {
		return err
	}
	for _, member := range conversation.Members {
		if _, err := tx.Exec(ctx, `
			INSERT INTO conversation_members (conversation_id, username, role, added_by, joined_order)
			VALUES ($1, $2, $3, $4, $5)
		`, conversation.ID, member.Username, conversationRoleDB(member.Role), nullString(member.AddedBy), member.JoinedOrder); err != nil {
			return err
		}
	}
	return nil
}

func upsertConversationKeyTx(ctx context.Context, tx pgx.Tx, key ConversationKey) error {
	if _, err := tx.Exec(ctx, `
		INSERT INTO conversation_keys (conversation_id, version, algorithm, created_by)
		VALUES ($1, $2, $3, $4)
		ON CONFLICT (conversation_id, version) DO UPDATE
		SET algorithm = EXCLUDED.algorithm,
			created_by = EXCLUDED.created_by,
			created_at = NOW()
	`, key.ConversationID, key.Version, key.Algorithm, key.CreatedBy); err != nil {
		return err
	}
	if _, err := tx.Exec(ctx, `
		DELETE FROM conversation_key_envelopes WHERE conversation_id = $1 AND version = $2
	`, key.ConversationID, key.Version); err != nil {
		return err
	}
	for _, envelope := range key.Envelopes {
		if _, err := tx.Exec(ctx, `
			INSERT INTO conversation_key_envelopes (
				conversation_id, version, username, encrypted_key, nonce, sender_key_id, recipient_key_id
			) VALUES ($1, $2, $3, $4, $5, $6, $7)
		`, key.ConversationID, key.Version, envelope.Username, envelope.EncryptedKey, envelope.Nonce, envelope.SenderKeyID, envelope.RecipientKeyID); err != nil {
			return err
		}
	}
	return nil
}

func loadConversationTx(ctx context.Context, tx pgx.Tx, conversationID string) (Conversation, error) {
	var id, kind, title string
	err := tx.QueryRow(ctx, `
		SELECT id, kind, title FROM conversations WHERE id = $1
	`, conversationID).Scan(&id, &kind, &title)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Conversation{}, ErrConversationNotFound
		}
		return Conversation{}, err
	}

	rows, err := tx.Query(ctx, `
		SELECT username, role, COALESCE(added_by, ''), joined_order
		FROM conversation_members WHERE conversation_id = $1 ORDER BY joined_order, username
	`, conversationID)
	if err != nil {
		return Conversation{}, err
	}
	defer rows.Close()

	conversation := Conversation{ID: id, Title: title}
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
	return conversation, rows.Err()
}
