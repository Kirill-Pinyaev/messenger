package grpcapi

import (
	"context"
	"crypto/rand"
	"errors"
	"io"
	"slices"
	"strings"
	"sync"
	"time"

	messengerv1 "messenger/gen/messenger/v1"
	"messenger/internal/auth"
	"messenger/internal/store"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/emptypb"
	"google.golang.org/protobuf/types/known/timestamppb"
)

type Server struct {
	messengerv1.UnimplementedAuthServiceServer
	messengerv1.UnimplementedUserServiceServer
	messengerv1.UnimplementedMessageServiceServer

	authSvc   *auth.Service
	userStore store.UserStore
	msgStore  store.MessageStore
	convStore store.ConversationStore
	hub       *eventHub
}

func NewServer(authSvc *auth.Service, userStore store.UserStore, msgStore store.MessageStore, convStore store.ConversationStore) *Server {
	return &Server{
		authSvc:   authSvc,
		userStore: userStore,
		msgStore:  msgStore,
		convStore: convStore,
		hub:       newEventHub(),
	}
}

func (s *Server) Register(ctx context.Context, req *messengerv1.RegisterRequest) (*emptypb.Empty, error) {
	if req.GetAvatarHex() == "" {
		req.AvatarHex = randomAvatarHex()
	}
	if err := s.authSvc.Register(
		req.GetUsername(),
		req.GetPassword(),
		req.GetFirstName(),
		req.GetLastName(),
		req.GetAvatarHex(),
		req.GetAvatarData(),
	); err != nil {
		return nil, mapAuthErr(err)
	}
	return &emptypb.Empty{}, nil
}

func (s *Server) Login(ctx context.Context, req *messengerv1.LoginRequest) (*messengerv1.LoginResponse, error) {
	token, err := s.authSvc.Login(req.GetUsername(), req.GetPassword())
	if err != nil {
		return nil, mapAuthErr(err)
	}
	return &messengerv1.LoginResponse{Token: token}, nil
}

func (s *Server) DeleteAccount(ctx context.Context, _ *emptypb.Empty) (*emptypb.Empty, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	if err := s.msgStore.DeleteUser(ctx, username); err != nil {
		return nil, status.Error(codes.Internal, "failed to delete messages")
	}
	if err := s.convStore.DeleteUser(ctx, username); err != nil {
		return nil, status.Error(codes.Internal, "failed to delete conversations")
	}
	if err := s.authSvc.DeleteAccount(ctx, username); err != nil {
		return nil, status.Error(codes.Internal, "failed to delete user")
	}
	s.hub.disconnectUser(username)
	return &emptypb.Empty{}, nil
}

func (s *Server) GetProfile(ctx context.Context, req *messengerv1.GetProfileRequest) (*messengerv1.Profile, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	target := strings.TrimSpace(req.GetUsername())
	if target == "" {
		target = username
	}

	user, err := s.userStore.Get(ctx, target)
	if err != nil {
		if errors.Is(err, store.ErrUserNotFound) {
			return nil, status.Error(codes.NotFound, "profile not found")
		}
		return nil, status.Error(codes.Internal, "failed to load profile")
	}
	return profileFromUser(user), nil
}

func (s *Server) UpdateProfile(ctx context.Context, req *messengerv1.UpdateProfileRequest) (*messengerv1.Profile, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	user, err := s.userStore.UpdateProfile(
		ctx,
		username,
		req.GetFirstName(),
		req.GetLastName(),
		req.GetAvatarHex(),
		req.GetAvatarData(),
	)
	if err != nil {
		if errors.Is(err, store.ErrUserNotFound) {
			return nil, status.Error(codes.NotFound, "profile not found")
		}
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to update profile")
	}

	profile := profileFromUser(user)
	s.hub.publishAll(newProfileEvent(profile))
	return profile, nil
}

func (s *Server) SearchUsers(ctx context.Context, req *messengerv1.SearchUsersRequest) (*messengerv1.SearchUsersResponse, error) {
	if _, _, err := s.requireAuth(ctx); err != nil {
		return nil, err
	}

	users, err := s.userStore.Search(ctx, req.GetQuery(), int(req.GetLimit()))
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "query is required")
		}
		return nil, status.Error(codes.Internal, "failed to search users")
	}

	items := make([]*messengerv1.Profile, 0, len(users))
	for _, user := range users {
		items = append(items, profileFromUser(user))
	}
	return &messengerv1.SearchUsersResponse{Items: items}, nil
}

func (s *Server) ListConversations(ctx context.Context, _ *emptypb.Empty) (*messengerv1.ListConversationsResponse, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	items, err := s.msgStore.Conversations(ctx, username)
	if err != nil {
		return nil, status.Error(codes.Internal, "failed to load conversations")
	}

	peersSet := make(map[string]struct{})
	convPeers := make(map[string]string, len(items))
	for _, convID := range items {
		peer, ok := peerFromConversationID(convID, username)
		if !ok {
			continue
		}
		convPeers[convID] = peer
		peersSet[peer] = struct{}{}
	}

	peers := make([]string, 0, len(peersSet))
	for peer := range peersSet {
		peers = append(peers, peer)
	}
	peersInfo, err := s.userStore.GetMany(ctx, peers)
	if err != nil {
		return nil, status.Error(codes.Internal, "failed to load users")
	}

	out := make([]*messengerv1.Conversation, 0, len(items))
	for _, convID := range items {
		peer, ok := convPeers[convID]
		if !ok {
			continue
		}
		out = append(out, &messengerv1.Conversation{
			ConversationId: convID,
			PeerUsername:   peer,
			PeerProfile:    profileFromUser(peersInfo[peer]),
			Kind:           messengerv1.ConversationKind_CONVERSATION_KIND_DIRECT,
			Title:          peer,
		})
	}

	groupConversations, err := s.convStore.ListGroupConversationsForUser(ctx, username)
	if err != nil {
		return nil, status.Error(codes.Internal, "failed to load group conversations")
	}
	for _, conversation := range groupConversations {
		out = append(out, conversationToProto(conversation))
	}
	return &messengerv1.ListConversationsResponse{Items: out}, nil
}

func (s *Server) CreateGroupConversation(ctx context.Context, req *messengerv1.CreateGroupConversationRequest) (*messengerv1.Conversation, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	memberSet := map[string]struct{}{username: {}}
	members := []store.ConversationMember{{
		Username:    username,
		Role:        store.ConversationRoleAdmin,
		JoinedOrder: 1,
	}}
	for _, member := range req.GetMemberUsernames() {
		member = strings.TrimSpace(member)
		if member == "" {
			return nil, status.Error(codes.InvalidArgument, "member username is required")
		}
		if _, ok := memberSet[member]; ok {
			continue
		}
		if _, err := s.userStore.Get(ctx, member); err != nil {
			if errors.Is(err, store.ErrUserNotFound) {
				return nil, status.Error(codes.NotFound, "member not found")
			}
			return nil, status.Error(codes.Internal, "failed to load member")
		}
		memberSet[member] = struct{}{}
		members = append(members, store.ConversationMember{
			Username:    member,
			Role:        store.ConversationRoleMember,
			AddedBy:     username,
			JoinedOrder: int64(len(members) + 1),
		})
	}
	if len(members) < 2 || strings.TrimSpace(req.GetTitle()) == "" {
		return nil, status.Error(codes.InvalidArgument, "title and at least one member are required")
	}

	conversation, err := s.convStore.CreateGroupConversation(ctx, store.Conversation{
		ID:      newGroupConversationID(),
		Kind:    store.ConversationKindGroup,
		Title:   req.GetTitle(),
		Members: members,
	})
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to create group")
	}
	s.hub.publishUsers(conversation.MemberUsernames(), newConversationEvent(conversationToProto(conversation)))
	return conversationToProto(conversation), nil
}

func (s *Server) AddGroupMembers(ctx context.Context, req *messengerv1.AddGroupMembersRequest) (*messengerv1.Conversation, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	conversation, err := s.convStore.GetConversation(ctx, req.GetConversationId())
	if err != nil {
		if errors.Is(err, store.ErrConversationNotFound) {
			return nil, status.Error(codes.NotFound, "conversation not found")
		}
		return nil, status.Error(codes.Internal, "failed to load conversation")
	}
	if !conversation.HasMember(username) {
		return nil, status.Error(codes.PermissionDenied, "not a conversation member")
	}

	var members []string
	for _, member := range req.GetMemberUsernames() {
		member = strings.TrimSpace(member)
		if member == "" {
			return nil, status.Error(codes.InvalidArgument, "member username is required")
		}
		if _, err := s.userStore.Get(ctx, member); err != nil {
			if errors.Is(err, store.ErrUserNotFound) {
				return nil, status.Error(codes.NotFound, "member not found")
			}
			return nil, status.Error(codes.Internal, "failed to load member")
		}
		members = append(members, member)
	}

	updated, err := s.convStore.AddMembers(ctx, req.GetConversationId(), username, members)
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		if errors.Is(err, store.ErrConversationForbidden) {
			return nil, status.Error(codes.PermissionDenied, "not allowed")
		}
		return nil, status.Error(codes.Internal, "failed to add members")
	}
	s.hub.publishUsers(updated.MemberUsernames(), newConversationEvent(conversationToProto(updated)))
	return conversationToProto(updated), nil
}

func (s *Server) RemoveGroupMember(ctx context.Context, req *messengerv1.RemoveGroupMemberRequest) (*messengerv1.Conversation, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	conversation, err := s.convStore.GetConversation(ctx, req.GetConversationId())
	if err != nil {
		if errors.Is(err, store.ErrConversationNotFound) {
			return nil, status.Error(codes.NotFound, "conversation not found")
		}
		return nil, status.Error(codes.Internal, "failed to load conversation")
	}
	if !conversation.HasMember(username) {
		return nil, status.Error(codes.PermissionDenied, "not a conversation member")
	}

	updated, err := s.convStore.RemoveMember(ctx, req.GetConversationId(), username, req.GetUsername())
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		if errors.Is(err, store.ErrConversationNotFound) {
			return nil, status.Error(codes.NotFound, "conversation not found")
		}
		if errors.Is(err, store.ErrConversationForbidden) {
			return nil, status.Error(codes.PermissionDenied, "not allowed")
		}
		return nil, status.Error(codes.Internal, "failed to remove member")
	}
	s.publishConversationSync(conversation.MemberUsernames(), updated)
	return conversationToProto(updated), nil
}

func (s *Server) LeaveGroupConversation(ctx context.Context, req *messengerv1.LeaveGroupConversationRequest) (*emptypb.Empty, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	conversation, err := s.convStore.GetConversation(ctx, req.GetConversationId())
	if err != nil {
		if errors.Is(err, store.ErrConversationNotFound) {
			return nil, status.Error(codes.NotFound, "conversation not found")
		}
		return nil, status.Error(codes.Internal, "failed to load conversation")
	}
	if !conversation.HasMember(username) {
		return nil, status.Error(codes.PermissionDenied, "not a conversation member")
	}

	updated, err := s.convStore.LeaveConversation(ctx, req.GetConversationId(), username)
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		if errors.Is(err, store.ErrConversationForbidden) {
			return nil, status.Error(codes.PermissionDenied, "not allowed")
		}
		return nil, status.Error(codes.Internal, "failed to leave conversation")
	}

	s.publishConversationSync(conversation.MemberUsernames(), updated)
	return &emptypb.Empty{}, nil
}

func (s *Server) TransferGroupAdmin(ctx context.Context, req *messengerv1.TransferGroupAdminRequest) (*messengerv1.Conversation, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	conversation, err := s.convStore.GetConversation(ctx, req.GetConversationId())
	if err != nil {
		if errors.Is(err, store.ErrConversationNotFound) {
			return nil, status.Error(codes.NotFound, "conversation not found")
		}
		return nil, status.Error(codes.Internal, "failed to load conversation")
	}
	member, ok := conversation.Member(username)
	if !ok {
		return nil, status.Error(codes.PermissionDenied, "not a conversation member")
	}
	if member.Role != store.ConversationRoleAdmin {
		return nil, status.Error(codes.PermissionDenied, "only admin can transfer admin role")
	}

	updated, err := s.convStore.TransferAdmin(ctx, req.GetConversationId(), req.GetUsername())
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		if errors.Is(err, store.ErrConversationNotFound) {
			return nil, status.Error(codes.NotFound, "conversation not found")
		}
		return nil, status.Error(codes.Internal, "failed to transfer admin")
	}
	s.hub.publishUsers(updated.MemberUsernames(), newConversationEvent(conversationToProto(updated)))
	return conversationToProto(updated), nil
}

func (s *Server) SendMessage(ctx context.Context, req *messengerv1.SendMessageRequest) (*messengerv1.Message, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	if req.GetText() == "" {
		return nil, status.Error(codes.InvalidArgument, "text is required")
	}

	now := time.Now().UTC()
	convID := strings.TrimSpace(req.GetConversationId())
	recipient := strings.TrimSpace(req.GetTo())
	participants := []string{username}

	if convID != "" {
		conversation, err := s.convStore.GetConversation(ctx, convID)
		if err != nil {
			if errors.Is(err, store.ErrConversationNotFound) {
				return nil, status.Error(codes.NotFound, "conversation not found")
			}
			return nil, status.Error(codes.Internal, "failed to load conversation")
		}
		if !conversation.HasMember(username) {
			return nil, status.Error(codes.PermissionDenied, "not a conversation member")
		}
		recipient = convID
		participants = conversation.MemberUsernames()
	} else {
		if recipient == "" {
			return nil, status.Error(codes.InvalidArgument, "to or conversation_id is required")
		}
		convID = conversationID(username, recipient)
		participants = append(participants, recipient)
	}

	saved, err := s.msgStore.Save(ctx, store.Message{
		ConversationID: convID,
		From:           username,
		To:             recipient,
		Text:           req.GetText(),
		TS:             now,
	})
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad message")
		}
		return nil, status.Error(codes.Internal, "failed to save message")
	}

	msg := messageFromStore(saved)
	event := newMessageEvent(msg)
	s.hub.publishUsers(participants, event)
	return msg, nil
}

func (s *Server) GetMessages(ctx context.Context, req *messengerv1.GetMessagesRequest) (*messengerv1.GetMessagesResponse, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	convID := strings.TrimSpace(req.GetConversationId())
	if convID == "" && strings.TrimSpace(req.GetWithUsername()) != "" {
		convID = conversationID(username, strings.TrimSpace(req.GetWithUsername()))
	}
	if convID == "" {
		return nil, status.Error(codes.InvalidArgument, "conversation_id or with_username is required")
	}
	if conversation, err := s.convStore.GetConversation(ctx, convID); err == nil {
		if !conversation.HasMember(username) {
			return nil, status.Error(codes.PermissionDenied, "not a conversation member")
		}
	} else if !errors.Is(err, store.ErrConversationNotFound) {
		return nil, status.Error(codes.Internal, "failed to load conversation")
	}

	msgs, err := s.msgStore.History(ctx, convID, int(req.GetLimit()))
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to load history")
	}

	items := make([]*messengerv1.Message, 0, len(msgs))
	for _, msg := range msgs {
		items = append(items, messageFromStore(msg))
	}
	return &messengerv1.GetMessagesResponse{Items: items}, nil
}

func (s *Server) SearchMessages(ctx context.Context, req *messengerv1.SearchMessagesRequest) (*messengerv1.SearchMessagesResponse, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	msgs, err := s.msgStore.SearchMessages(ctx, username, req.GetQuery(), int(req.GetLimit()))
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "query is required")
		}
		return nil, status.Error(codes.Internal, "failed to search messages")
	}

	items := make([]*messengerv1.Message, 0, len(msgs))
	for _, msg := range msgs {
		items = append(items, messageFromStore(msg))
	}
	return &messengerv1.SearchMessagesResponse{Items: items}, nil
}

func (s *Server) DeleteMessage(ctx context.Context, req *messengerv1.DeleteMessageRequest) (*emptypb.Empty, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	if req.GetMessageId() <= 0 {
		return nil, status.Error(codes.InvalidArgument, "message_id is required")
	}

	msg, err := s.msgStore.GetByID(ctx, req.GetMessageId())
	if err != nil {
		if errors.Is(err, store.ErrMessageNotFound) {
			return nil, status.Error(codes.NotFound, "message not found")
		}
		return nil, status.Error(codes.Internal, "failed to load message")
	}
	if msg.From != username {
		return nil, status.Error(codes.PermissionDenied, "not allowed")
	}

	if err := s.msgStore.DeleteMessage(ctx, req.GetMessageId()); err != nil {
		if errors.Is(err, store.ErrMessageNotFound) {
			return nil, status.Error(codes.NotFound, "message not found")
		}
		return nil, status.Error(codes.Internal, "failed to delete message")
	}

	recipients := []string{msg.From, msg.To}
	if conversation, err := s.convStore.GetConversation(ctx, msg.ConversationID); err == nil {
		recipients = conversation.MemberUsernames()
	} else if err != nil && !errors.Is(err, store.ErrConversationNotFound) {
		return nil, status.Error(codes.Internal, "failed to load conversation")
	}
	s.hub.publishUsers(recipients, newMessageDeletedEvent(req.GetMessageId(), msg.ConversationID))
	return &emptypb.Empty{}, nil
}

func (s *Server) StreamEvents(req *messengerv1.StreamEventsRequest, stream messengerv1.MessageService_StreamEventsServer) error {
	username, _, err := s.requireAuth(stream.Context())
	if err != nil {
		return err
	}
	if req == nil {
		req = &messengerv1.StreamEventsRequest{}
	}

	events, unsubscribe := s.hub.subscribe(username)
	defer unsubscribe()

	for {
		select {
		case <-stream.Context().Done():
			if err := stream.Context().Err(); err != nil && !errors.Is(err, context.Canceled) {
				return err
			}
			return nil
		case evt := <-events:
			if evt == nil {
				continue
			}
			if err := stream.Send(evt); err != nil {
				if errors.Is(err, io.EOF) {
					return nil
				}
				return err
			}
		}
	}
}

func (s *Server) requireAuth(ctx context.Context) (username string, token string, err error) {
	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		return "", "", status.Error(codes.Unauthenticated, "missing metadata")
	}

	token = firstMetadata(md, "authorization")
	if strings.HasPrefix(strings.ToLower(token), "bearer ") {
		token = strings.TrimSpace(token[7:])
	}
	if token == "" {
		token = firstMetadata(md, "token")
	}
	if token == "" {
		return "", "", status.Error(codes.Unauthenticated, "missing token")
	}

	username, ok = s.authSvc.UsernameForToken(token)
	if !ok {
		return "", "", status.Error(codes.Unauthenticated, "unauthorized")
	}
	return username, token, nil
}

func firstMetadata(md metadata.MD, key string) string {
	values := md.Get(key)
	if len(values) == 0 {
		return ""
	}
	return strings.TrimSpace(values[0])
}

func mapAuthErr(err error) error {
	switch {
	case errors.Is(err, auth.ErrBadInput), errors.Is(err, store.ErrBadInput):
		return status.Error(codes.InvalidArgument, "bad request")
	case errors.Is(err, auth.ErrUserExists):
		return status.Error(codes.AlreadyExists, "user already exists")
	case errors.Is(err, auth.ErrUserNotFound):
		return status.Error(codes.NotFound, "user not found")
	case errors.Is(err, auth.ErrWrongPassword):
		return status.Error(codes.Unauthenticated, "wrong password")
	case errors.Is(err, auth.ErrInvalidCredentials):
		return status.Error(codes.Unauthenticated, "invalid credentials")
	default:
		return status.Error(codes.Internal, "internal error")
	}
}

func profileFromUser(user store.User) *messengerv1.Profile {
	return &messengerv1.Profile{
		Username:   user.Username,
		FirstName:  user.FirstName,
		LastName:   user.LastName,
		AvatarHex:  user.AvatarHex,
		AvatarData: user.AvatarData,
	}
}

func conversationToProto(conversation store.Conversation) *messengerv1.Conversation {
	memberUsernames := conversation.MemberUsernames()
	members := make([]*messengerv1.ConversationMember, 0, len(conversation.Members))
	for _, member := range conversation.Members {
		members = append(members, conversationMemberToProto(member))
	}

	return &messengerv1.Conversation{
		ConversationId:  conversation.ID,
		Kind:            messengerv1.ConversationKind_CONVERSATION_KIND_GROUP,
		Title:           conversation.Title,
		MemberUsernames: memberUsernames,
		Members:         members,
	}
}

func conversationMemberToProto(member store.ConversationMember) *messengerv1.ConversationMember {
	role := messengerv1.ConversationRole_CONVERSATION_ROLE_MEMBER
	if member.Role == store.ConversationRoleAdmin {
		role = messengerv1.ConversationRole_CONVERSATION_ROLE_ADMIN
	}
	return &messengerv1.ConversationMember{
		Username: member.Username,
		Role:     role,
		AddedBy:  member.AddedBy,
	}
}

func messageFromStore(msg store.Message) *messengerv1.Message {
	return &messengerv1.Message{
		MessageId:      msg.ID,
		ConversationId: msg.ConversationID,
		From:           msg.From,
		To:             msg.To,
		Text:           msg.Text,
		CreatedAt:      timestamppb.New(msg.TS),
	}
}

func newMessageEvent(msg *messengerv1.Message) *messengerv1.ServerEvent {
	return &messengerv1.ServerEvent{
		CreatedAt: timestamppb.Now(),
		Payload: &messengerv1.ServerEvent_Message{
			Message: msg,
		},
	}
}

func newProfileEvent(profile *messengerv1.Profile) *messengerv1.ServerEvent {
	return &messengerv1.ServerEvent{
		CreatedAt: timestamppb.Now(),
		Payload: &messengerv1.ServerEvent_Profile{
			Profile: &messengerv1.ProfileEvent{Profile: profile},
		},
	}
}

func newPresenceEvent(users []string) *messengerv1.ServerEvent {
	slices.Sort(users)
	return &messengerv1.ServerEvent{
		CreatedAt: timestamppb.Now(),
		Payload: &messengerv1.ServerEvent_Presence{
			Presence: &messengerv1.PresenceEvent{OnlineUsers: users},
		},
	}
}

func newMessageDeletedEvent(messageID int64, conversationID string) *messengerv1.ServerEvent {
	return &messengerv1.ServerEvent{
		CreatedAt: timestamppb.Now(),
		Payload: &messengerv1.ServerEvent_MessageDeleted{
			MessageDeleted: &messengerv1.MessageDeletedEvent{
				MessageId:      messageID,
				ConversationId: conversationID,
			},
		},
	}
}

func newConversationEvent(conversation *messengerv1.Conversation) *messengerv1.ServerEvent {
	return &messengerv1.ServerEvent{
		CreatedAt: timestamppb.Now(),
		Payload: &messengerv1.ServerEvent_Conversation{
			Conversation: conversation,
		},
	}
}

func newConversationRemovedEvent(conversationID string) *messengerv1.ServerEvent {
	return &messengerv1.ServerEvent{
		CreatedAt: timestamppb.Now(),
		Payload: &messengerv1.ServerEvent_ConversationRemoved{
			ConversationRemoved: &messengerv1.ConversationRemovedEvent{
				ConversationId: conversationID,
			},
		},
	}
}

func (s *Server) publishConversationSync(previousMembers []string, updated store.Conversation) {
	currentMembers := updated.MemberUsernames()
	currentSet := make(map[string]struct{}, len(currentMembers))
	for _, member := range currentMembers {
		currentSet[member] = struct{}{}
	}
	for _, member := range previousMembers {
		if _, ok := currentSet[member]; ok {
			continue
		}
		s.hub.publishUsers([]string{member}, newConversationRemovedEvent(updated.ID))
	}
	if len(currentMembers) > 0 {
		s.hub.publishUsers(currentMembers, newConversationEvent(conversationToProto(updated)))
	}
}

func conversationID(a, b string) string {
	if strings.Compare(a, b) <= 0 {
		return a + "|" + b
	}
	return b + "|" + a
}

func peerFromConversationID(conversationID, username string) (string, bool) {
	parts := strings.Split(conversationID, "|")
	if len(parts) != 2 {
		return "", false
	}
	if parts[0] == username {
		return parts[1], true
	}
	if parts[1] == username {
		return parts[0], true
	}
	return "", false
}

func randomAvatarHex() string {
	const letters = "0123456789abcdef"
	b := make([]byte, 6)
	if _, err := rand.Read(b); err != nil {
		return "7a7a7a"
	}
	for i := range b {
		b[i] = letters[int(b[i])%len(letters)]
	}
	return string(b)
}

func newGroupConversationID() string {
	const letters = "0123456789abcdefghijklmnopqrstuvwxyz"
	b := make([]byte, 10)
	if _, err := rand.Read(b); err != nil {
		return "group-fallback"
	}
	for i := range b {
		b[i] = letters[int(b[i])%len(letters)]
	}
	return "group-" + string(b)
}

type eventHub struct {
	mu          sync.RWMutex
	subscribers map[string]map[chan *messengerv1.ServerEvent]struct{}
}

func newEventHub() *eventHub {
	return &eventHub{
		subscribers: make(map[string]map[chan *messengerv1.ServerEvent]struct{}),
	}
}

func (h *eventHub) subscribe(username string) (<-chan *messengerv1.ServerEvent, func()) {
	ch := make(chan *messengerv1.ServerEvent, 32)

	h.mu.Lock()
	if _, ok := h.subscribers[username]; !ok {
		h.subscribers[username] = make(map[chan *messengerv1.ServerEvent]struct{})
	}
	h.subscribers[username][ch] = struct{}{}
	presence := h.presenceLocked()
	h.mu.Unlock()

	h.publishAll(newPresenceEvent(presence))

	return ch, func() {
		h.mu.Lock()
		if userSubs, ok := h.subscribers[username]; ok {
			delete(userSubs, ch)
			if len(userSubs) == 0 {
				delete(h.subscribers, username)
			}
		}
		presence := h.presenceLocked()
		h.mu.Unlock()

		h.publishAll(newPresenceEvent(presence))
	}
}

func (h *eventHub) disconnectUser(username string) {
	h.mu.Lock()
	delete(h.subscribers, username)
	presence := h.presenceLocked()
	h.mu.Unlock()

	h.publishAll(newPresenceEvent(presence))
}

func (h *eventHub) publishUsers(usernames []string, evt *messengerv1.ServerEvent) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	for _, username := range usernames {
		for ch := range h.subscribers[username] {
			select {
			case ch <- evt:
			default:
			}
		}
	}
}

func (h *eventHub) publishAll(evt *messengerv1.ServerEvent) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	for _, userSubs := range h.subscribers {
		for ch := range userSubs {
			select {
			case ch <- evt:
			default:
			}
		}
	}
}

func (h *eventHub) presenceLocked() []string {
	users := make([]string, 0, len(h.subscribers))
	for username := range h.subscribers {
		users = append(users, username)
	}
	return users
}
