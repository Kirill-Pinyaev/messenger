package grpcapi

import (
	"context"
	"crypto/rand"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
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

var multiDeviceMarker = []byte("__multi_device__")
var errDirectMessageUnavailableForDevice = errors.New("direct message unavailable for device")

const doubleRatchetAlgorithm = "DR-X25519-HKDF-SHA256-AESGCM-Ed25519-v1"

type storedDirectEnvelope struct {
	TargetUsername               string `json:"target_username"`
	TargetDeviceID               string `json:"target_device_id"`
	Ciphertext                   []byte `json:"ciphertext"`
	Nonce                        []byte `json:"nonce"`
	RecipientSignedPrekeyID      string `json:"recipient_signed_prekey_id"`
	RecipientSignedPrekeyPublic  []byte `json:"recipient_signed_prekey_public"`
	RecipientOneTimePrekeyID     string `json:"recipient_one_time_prekey_id"`
	RecipientOneTimePrekeyPublic []byte `json:"recipient_one_time_prekey_public"`
	E2EEAlgorithm                string `json:"e2ee_algorithm"`
	RatchetPublicKey             []byte `json:"ratchet_public_key"`
	PreviousChainLength          int32  `json:"previous_chain_length"`
	MessageNumber                int32  `json:"message_number"`
}

type storedAttachmentDirectEnvelope struct {
	TargetUsername               string `json:"target_username"`
	TargetDeviceID               string `json:"target_device_id"`
	EncryptedDescriptor          []byte `json:"encrypted_descriptor"`
	DescriptorNonce              []byte `json:"descriptor_nonce"`
	RecipientSignedPrekeyID      string `json:"recipient_signed_prekey_id"`
	RecipientSignedPrekeyPublic  []byte `json:"recipient_signed_prekey_public"`
	RecipientOneTimePrekeyID     string `json:"recipient_one_time_prekey_id"`
	RecipientOneTimePrekeyPublic []byte `json:"recipient_one_time_prekey_public"`
	E2EEAlgorithm                string `json:"e2ee_algorithm"`
	RatchetPublicKey             []byte `json:"ratchet_public_key"`
	PreviousChainLength          int32  `json:"previous_chain_length"`
	MessageNumber                int32  `json:"message_number"`
}

type Server struct {
	messengerv1.UnimplementedAuthServiceServer
	messengerv1.UnimplementedUserServiceServer
	messengerv1.UnimplementedMessageServiceServer

	authSvc      *auth.Service
	userStore    store.UserStore
	msgStore     store.MessageStore
	convStore    store.ConversationStore
	keyStore     store.KeyStore
	archiveStore store.ArchiveStore
	groupState   store.GroupStateStore
	mediaRoot    string
	hub          *eventHub
}

func NewServer(authSvc *auth.Service, userStore store.UserStore, msgStore store.MessageStore, convStore store.ConversationStore, keyStore store.KeyStore, archiveStore store.ArchiveStore) *Server {
	return &Server{
		authSvc:      authSvc,
		userStore:    userStore,
		msgStore:     msgStore,
		convStore:    convStore,
		keyStore:     keyStore,
		archiveStore: archiveStore,
		groupState:   store.NewGroupStateStore(convStore, keyStore),
		mediaRoot:    defaultMediaRoot(),
		hub:          newEventHub(),
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
	token, err := s.authSvc.Login(req.GetUsername(), req.GetPassword(), req.GetDeviceId())
	if err != nil {
		return nil, mapAuthErr(err)
	}
	return &messengerv1.LoginResponse{Token: token, DeviceId: req.GetDeviceId()}, nil
}

func (s *Server) DeleteAccount(ctx context.Context, _ *emptypb.Empty) (*emptypb.Empty, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	ownedMedia, err := s.msgStore.ListMediaByOwner(ctx, username)
	if err != nil {
		return nil, status.Error(codes.Internal, "failed to load media")
	}
	if err := s.msgStore.DeleteUser(ctx, username); err != nil {
		return nil, status.Error(codes.Internal, "failed to delete messages")
	}
	s.deleteMediaFiles(ownedMedia)
	if err := s.convStore.DeleteUser(ctx, username); err != nil {
		return nil, status.Error(codes.Internal, "failed to delete conversations")
	}
	if err := s.keyStore.DeleteUser(ctx, username); err != nil {
		return nil, status.Error(codes.Internal, "failed to delete keys")
	}
	if s.archiveStore != nil {
		if err := s.archiveStore.DeleteUser(ctx, username); err != nil {
			return nil, status.Error(codes.Internal, "failed to delete history archive")
		}
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

func (s *Server) PublishIdentityKey(ctx context.Context, req *messengerv1.PublishIdentityKeyRequest) (*messengerv1.IdentityKey, error) {
	username, deviceID, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	key, err := s.keyStore.UpsertIdentityKey(ctx, store.IdentityKey{
		Username:  username,
		DeviceID:  deviceID,
		KeyID:     req.GetKeyId(),
		Algorithm: req.GetAlgorithm(),
		PublicKey: req.GetPublicKey(),
	})
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to publish identity key")
	}
	return identityKeyToProto(key), nil
}

func (s *Server) GetIdentityKey(ctx context.Context, req *messengerv1.GetIdentityKeyRequest) (*messengerv1.IdentityKey, error) {
	sessionUsername, sessionDeviceID, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	deviceID := strings.TrimSpace(req.GetDeviceId())
	if deviceID == "" && req.GetUsername() == sessionUsername {
		deviceID = sessionDeviceID
	}
	key, err := s.keyStore.GetIdentityKey(ctx, req.GetUsername(), deviceID)
	if err != nil {
		if errors.Is(err, store.ErrIdentityKeyNotFound) {
			return nil, status.Error(codes.NotFound, "identity key not found")
		}
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to load identity key")
	}
	return identityKeyToProto(key), nil
}

func (s *Server) GetIdentityKeys(ctx context.Context, req *messengerv1.GetIdentityKeysRequest) (*messengerv1.GetIdentityKeysResponse, error) {
	if _, _, err := s.requireAuth(ctx); err != nil {
		return nil, err
	}

	items, err := s.keyStore.GetIdentityKeys(ctx, req.GetUsernames())
	if err != nil {
		return nil, status.Error(codes.Internal, "failed to load identity keys")
	}
	out := make([]*messengerv1.IdentityKey, 0, len(items))
	for _, item := range items {
		out = append(out, identityKeyToProto(item))
	}
	return &messengerv1.GetIdentityKeysResponse{Items: out}, nil
}

func (s *Server) PublishPrekeyBundle(ctx context.Context, req *messengerv1.PublishPrekeyBundleRequest) (*messengerv1.PrekeyBundle, error) {
	username, deviceID, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}

	identity, err := s.keyStore.GetIdentityKey(ctx, username, deviceID)
	if err != nil {
		if errors.Is(err, store.ErrIdentityKeyNotFound) {
			return nil, status.Error(codes.FailedPrecondition, "identity key must be published first")
		}
		return nil, status.Error(codes.Internal, "failed to load identity key")
	}
	existingSignedPrekey, err := s.keyStore.GetSignedPrekey(ctx, username, deviceID)
	if err != nil && !errors.Is(err, store.ErrSignedPrekeyNotFound) && !errors.Is(err, store.ErrBadInput) {
		return nil, status.Error(codes.Internal, "failed to load signed prekey")
	}
	resetOTPQueue := err == nil && (existingSignedPrekey.KeyID != req.GetSignedPrekeyId() || !slices.Equal(existingSignedPrekey.PublicKey, req.GetSignedPrekeyPublicKey()))
	if req.GetSignedPrekeyAlgorithm() == "X25519" && (len(req.GetSignedPrekeySignature()) == 0 || req.GetSignedPrekeySignatureAlgorithm() == "") {
		return nil, status.Error(codes.InvalidArgument, "signed prekey signature required")
	}

	signedPrekey, err := s.keyStore.UpsertSignedPrekey(ctx, store.SignedPrekey{
		Username:           username,
		DeviceID:           deviceID,
		KeyID:              req.GetSignedPrekeyId(),
		Algorithm:          req.GetSignedPrekeyAlgorithm(),
		PublicKey:          req.GetSignedPrekeyPublicKey(),
		Signature:          req.GetSignedPrekeySignature(),
		SignatureAlgorithm: req.GetSignedPrekeySignatureAlgorithm(),
	})
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to publish signed prekey")
	}

	oneTimePrekeys := make([]store.OneTimePrekey, 0, len(req.GetOneTimePrekeys()))
	for _, item := range req.GetOneTimePrekeys() {
		oneTimePrekeys = append(oneTimePrekeys, store.OneTimePrekey{
			Username:  username,
			DeviceID:  deviceID,
			KeyID:     item.GetKeyId(),
			Algorithm: item.GetAlgorithm(),
			PublicKey: item.GetPublicKey(),
		})
	}
	if resetOTPQueue {
		if err := s.keyStore.DeleteOneTimePrekeys(ctx, username, deviceID); err != nil {
			return nil, status.Error(codes.Internal, "failed to reset one-time prekeys")
		}
	}
	if err := s.keyStore.PutOneTimePrekeys(ctx, username, deviceID, oneTimePrekeys); err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to publish one-time prekeys")
	}

	return prekeyBundleToProto(store.PrekeyBundle{
		Username:     username,
		DeviceID:     deviceID,
		IdentityKey:  identity,
		SignedPrekey: signedPrekey,
	}), nil
}

func (s *Server) InitializeHistoryArchive(ctx context.Context, req *messengerv1.InitializeHistoryArchiveRequest) (*messengerv1.HistoryArchiveHeader, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	if s.archiveStore == nil {
		return nil, status.Error(codes.Unimplemented, "history archive is not configured")
	}
	header, err := s.archiveStore.InitializeHeader(ctx, store.ArchiveKeyBundle{
		Username:            username,
		PublicKey:           req.GetPublicKey(),
		EncryptedPrivateKey: req.GetEncryptedPrivateKey(),
		KDFSalt:             req.GetKdfSalt(),
		KDFParams:           req.GetKdfParams(),
		Version:             req.GetVersion(),
	})
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad archive header")
		}
		return nil, status.Error(codes.Internal, "failed to initialize history archive")
	}
	return archiveHeaderToProto(header), nil
}

func (s *Server) GetHistoryArchiveHeader(ctx context.Context, _ *emptypb.Empty) (*messengerv1.HistoryArchiveHeader, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	if s.archiveStore == nil {
		return nil, status.Error(codes.Unimplemented, "history archive is not configured")
	}
	header, err := s.archiveStore.GetHeader(ctx, username)
	if err != nil {
		if errors.Is(err, store.ErrArchiveHeaderNotFound) {
			return nil, status.Error(codes.NotFound, "history archive header not found")
		}
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to load history archive header")
	}
	return archiveHeaderToProto(header), nil
}

func (s *Server) GetArchivePublicKeys(ctx context.Context, req *messengerv1.GetArchivePublicKeysRequest) (*messengerv1.GetArchivePublicKeysResponse, error) {
	if _, _, err := s.requireAuth(ctx); err != nil {
		return nil, err
	}
	if s.archiveStore == nil {
		return nil, status.Error(codes.Unimplemented, "history archive is not configured")
	}
	keys, err := s.archiveStore.GetPublicKeys(ctx, req.GetUsernames())
	if err != nil {
		return nil, status.Error(codes.Internal, "failed to load archive public keys")
	}
	items := make([]*messengerv1.ArchiveKeyBundle, 0, len(keys))
	for _, item := range keys {
		items = append(items, archiveKeyBundleToProto(item, false))
	}
	return &messengerv1.GetArchivePublicKeysResponse{Items: items}, nil
}

func (s *Server) AcquirePrekeyBundle(ctx context.Context, req *messengerv1.AcquirePrekeyBundleRequest) (*messengerv1.PrekeyBundle, error) {
	if _, _, err := s.requireAuth(ctx); err != nil {
		return nil, err
	}

	bundles, err := s.keyStore.AcquirePrekeyBundles(ctx, req.GetUsername())
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to acquire prekey bundle")
	}
	if len(bundles) == 0 {
		return nil, status.Error(codes.NotFound, "prekey bundle not found")
	}
	bundle, err := s.keyStore.AcquirePrekeyBundle(ctx, req.GetUsername(), bundles[0].DeviceID)
	if err != nil {
		if errors.Is(err, store.ErrIdentityKeyNotFound) || errors.Is(err, store.ErrSignedPrekeyNotFound) {
			return nil, status.Error(codes.NotFound, "prekey bundle not found")
		}
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to acquire prekey bundle")
	}
	return prekeyBundleToProto(bundle), nil
}

func (s *Server) AcquirePrekeyBundles(ctx context.Context, req *messengerv1.AcquirePrekeyBundlesRequest) (*messengerv1.AcquirePrekeyBundlesResponse, error) {
	if _, _, err := s.requireAuth(ctx); err != nil {
		return nil, err
	}

	bundles, err := s.keyStore.AcquirePrekeyBundles(ctx, req.GetUsername())
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to acquire prekey bundles")
	}
	out := make([]*messengerv1.PrekeyBundle, 0, len(bundles))
	for _, bundle := range bundles {
		out = append(out, prekeyBundleToProto(bundle))
	}
	return &messengerv1.AcquirePrekeyBundlesResponse{Items: out}, nil
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

	baseConversation := store.Conversation{
		ID:      newGroupConversationID(),
		Kind:    store.ConversationKindGroup,
		Title:   req.GetTitle(),
		Members: members,
	}
	var conversation store.Conversation
	if req.GetInitialKey() != nil && s.groupState != nil {
		key := groupKeyUpdateFromProto(req.GetInitialKey(), baseConversation.ID, username)
		conversation, _, err = s.groupState.CreateConversationWithKey(ctx, baseConversation, key)
	} else {
		conversation, err = s.convStore.CreateGroupConversation(ctx, baseConversation)
	}
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to create group")
	}
	if req.GetInitialKey() != nil && len(req.GetInitialKey().GetArchiveRecords()) > 0 && s.archiveStore != nil {
		archiveRecords, err := s.archiveRecordsFromProto(req.GetInitialKey().GetArchiveRecords(), conversation.ID, username, conversation.MemberUsernames())
		if err != nil {
			return nil, err
		}
		if err := s.archiveStore.AppendRecords(ctx, archiveRecords); err != nil {
			if errors.Is(err, store.ErrBadInput) {
				return nil, status.Error(codes.InvalidArgument, "bad archive record")
			}
			return nil, status.Error(codes.Internal, "failed to save history archive")
		}
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

	var updated store.Conversation
	if req.GetNextKey() != nil && s.groupState != nil {
		key := groupKeyUpdateFromProto(req.GetNextKey(), req.GetConversationId(), username)
		updated, _, err = s.groupState.AddMembersWithKey(ctx, req.GetConversationId(), username, members, key)
	} else {
		updated, err = s.convStore.AddMembers(ctx, req.GetConversationId(), username, members)
	}
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		if errors.Is(err, store.ErrConversationForbidden) {
			return nil, status.Error(codes.PermissionDenied, "not allowed")
		}
		return nil, status.Error(codes.Internal, "failed to add members")
	}
	if req.GetNextKey() != nil && len(req.GetNextKey().GetArchiveRecords()) > 0 && s.archiveStore != nil {
		archiveRecords, err := s.archiveRecordsFromProto(req.GetNextKey().GetArchiveRecords(), updated.ID, username, updated.MemberUsernames())
		if err != nil {
			return nil, err
		}
		if err := s.archiveStore.AppendRecords(ctx, archiveRecords); err != nil {
			if errors.Is(err, store.ErrBadInput) {
				return nil, status.Error(codes.InvalidArgument, "bad archive record")
			}
			return nil, status.Error(codes.Internal, "failed to save history archive")
		}
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

	var updated store.Conversation
	if req.GetNextKey() != nil && s.groupState != nil {
		key := groupKeyUpdateFromProto(req.GetNextKey(), req.GetConversationId(), username)
		updated, _, err = s.groupState.RemoveMemberWithKey(ctx, req.GetConversationId(), username, req.GetUsername(), key)
	} else {
		updated, err = s.convStore.RemoveMember(ctx, req.GetConversationId(), username, req.GetUsername())
	}
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
	if req.GetNextKey() != nil && len(req.GetNextKey().GetArchiveRecords()) > 0 && s.archiveStore != nil {
		archiveRecords, err := s.archiveRecordsFromProto(req.GetNextKey().GetArchiveRecords(), updated.ID, username, updated.MemberUsernames())
		if err != nil {
			return nil, err
		}
		if err := s.archiveStore.AppendRecords(ctx, archiveRecords); err != nil {
			if errors.Is(err, store.ErrBadInput) {
				return nil, status.Error(codes.InvalidArgument, "bad archive record")
			}
			return nil, status.Error(codes.Internal, "failed to save history archive")
		}
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

	var updated store.Conversation
	if req.GetNextKey() != nil && s.groupState != nil {
		key := groupKeyUpdatePtrFromProto(req.GetNextKey(), req.GetConversationId(), username)
		updated, _, err = s.groupState.LeaveConversationWithKey(ctx, req.GetConversationId(), username, key)
	} else {
		updated, err = s.convStore.LeaveConversation(ctx, req.GetConversationId(), username)
	}
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		if errors.Is(err, store.ErrConversationForbidden) {
			return nil, status.Error(codes.PermissionDenied, "not allowed")
		}
		return nil, status.Error(codes.Internal, "failed to leave conversation")
	}
	if req.GetNextKey() != nil && len(req.GetNextKey().GetArchiveRecords()) > 0 && s.archiveStore != nil {
		archiveRecords, err := s.archiveRecordsFromProto(req.GetNextKey().GetArchiveRecords(), updated.ID, username, updated.MemberUsernames())
		if err != nil {
			return nil, err
		}
		if err := s.archiveStore.AppendRecords(ctx, archiveRecords); err != nil {
			if errors.Is(err, store.ErrBadInput) {
				return nil, status.Error(codes.InvalidArgument, "bad archive record")
			}
			return nil, status.Error(codes.Internal, "failed to save history archive")
		}
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

func (s *Server) UpsertConversationKey(ctx context.Context, req *messengerv1.UpsertConversationKeyRequest) (*messengerv1.ConversationKey, error) {
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

	envelopes := make([]store.ConversationKeyEnvelope, 0, len(req.GetEnvelopes()))
	for _, envelope := range req.GetEnvelopes() {
		envelopes = append(envelopes, store.ConversationKeyEnvelope{
			Username:       envelope.GetUsername(),
			DeviceID:       envelope.GetDeviceId(),
			EncryptedKey:   envelope.GetEncryptedKey(),
			Nonce:          envelope.GetNonce(),
			SenderKeyID:    envelope.GetSenderKeyId(),
			RecipientKeyID: envelope.GetRecipientKeyId(),
		})
	}

	key, err := s.keyStore.UpsertConversationKey(ctx, store.ConversationKey{
		ConversationID: req.GetConversationId(),
		Version:        req.GetVersion(),
		Algorithm:      req.GetAlgorithm(),
		CreatedBy:      username,
		Envelopes:      envelopes,
	})
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to upsert conversation key")
	}
	if len(req.GetArchiveRecords()) > 0 && s.archiveStore != nil {
		archiveRecords, err := s.archiveRecordsFromProto(req.GetArchiveRecords(), req.GetConversationId(), username, conversation.MemberUsernames())
		if err != nil {
			return nil, err
		}
		if err := s.archiveStore.AppendRecords(ctx, archiveRecords); err != nil {
			if errors.Is(err, store.ErrBadInput) {
				return nil, status.Error(codes.InvalidArgument, "bad archive record")
			}
			return nil, status.Error(codes.Internal, "failed to save history archive")
		}
	}
	return conversationKeyToProto(key), nil
}

func (s *Server) GetConversationKey(ctx context.Context, req *messengerv1.GetConversationKeyRequest) (*messengerv1.ConversationKey, error) {
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

	key, err := s.keyStore.GetConversationKey(ctx, req.GetConversationId(), req.GetVersion())
	if err != nil {
		if errors.Is(err, store.ErrConversationKeyNotFound) {
			return nil, status.Error(codes.NotFound, "conversation key not found")
		}
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to load conversation key")
	}
	return conversationKeyToProto(key), nil
}

func (s *Server) SendMessage(ctx context.Context, req *messengerv1.SendMessageRequest) (*messengerv1.Message, error) {
	username, deviceID, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	if !req.GetEncrypted() && req.GetText() == "" && len(req.GetAttachments()) == 0 {
		return nil, status.Error(codes.InvalidArgument, "text or attachments are required")
	}
	if req.GetEncrypted() && len(req.GetCiphertext()) == 0 && req.GetText() == "" && len(req.GetAttachments()) == 0 && len(req.GetDirectEnvelopes()) == 0 {
		return nil, status.Error(codes.InvalidArgument, "ciphertext, direct envelopes, text or attachments are required")
	}
	if req.GetEncrypted() && len(req.GetCiphertext()) > 0 && (len(req.GetNonce()) == 0 || req.GetSenderKeyId() == "") {
		return nil, status.Error(codes.InvalidArgument, "ciphertext, nonce and sender_key_id are required")
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

	ciphertextToStore := req.GetCiphertext()
	nonceToStore := req.GetNonce()

	attachments := make([]store.Attachment, 0, len(req.GetAttachments()))
	for _, item := range req.GetAttachments() {
		kind, err := attachmentKindFromProto(item.GetKind())
		if err != nil {
			return nil, status.Error(codes.InvalidArgument, "bad attachment kind")
		}
		encryptedDescriptor := item.GetEncryptedDescriptor()
		descriptorNonce := item.GetDescriptorNonce()
		directEnvelopes := make([]store.AttachmentDirectEnvelope, 0, len(item.GetDirectEnvelopes()))
		for _, envelope := range item.GetDirectEnvelopes() {
			directEnvelopes = append(directEnvelopes, store.AttachmentDirectEnvelope{
				TargetUsername:               envelope.GetTargetUsername(),
				TargetDeviceID:               envelope.GetTargetDeviceId(),
				EncryptedDescriptor:          envelope.GetEncryptedDescriptor(),
				DescriptorNonce:              envelope.GetDescriptorNonce(),
				RecipientSignedPrekeyID:      envelope.GetRecipientSignedPrekeyId(),
				RecipientSignedPrekeyPublic:  envelope.GetRecipientSignedPrekeyPublic(),
				RecipientOneTimePrekeyID:     envelope.GetRecipientOneTimePrekeyId(),
				RecipientOneTimePrekeyPublic: envelope.GetRecipientOneTimePrekeyPublic(),
				E2EEAlgorithm:                envelope.GetE2EeAlgorithm(),
				RatchetPublicKey:             envelope.GetRatchetPublicKey(),
				PreviousChainLength:          envelope.GetPreviousChainLength(),
				MessageNumber:                envelope.GetMessageNumber(),
			})
		}
		attachments = append(attachments, store.Attachment{
			AttachmentID:        item.GetAttachmentId(),
			Kind:                kind,
			Filename:            item.GetFilename(),
			MimeType:            item.GetMimeType(),
			SizeBytes:           item.GetSizeBytes(),
			MediaID:             item.GetMediaId(),
			EncryptedDescriptor: encryptedDescriptor,
			DescriptorNonce:     descriptorNonce,
			SHA256:              item.GetSha256(),
			CiphertextSize:      item.GetCiphertextSize(),
			PreviewWidth:        item.GetPreview().GetWidth(),
			PreviewHeight:       item.GetPreview().GetHeight(),
			DirectEnvelopes:     directEnvelopes,
		})
	}

	directEnvelopes := make([]store.DirectEnvelope, 0, len(req.GetDirectEnvelopes()))
	for _, envelope := range req.GetDirectEnvelopes() {
		if req.GetEncrypted() && req.GetConversationId() == "" && !validDirectEnvelopeV2(envelope.GetE2EeAlgorithm(), envelope.GetRatchetPublicKey(), envelope.GetNonce(), envelope.GetCiphertext()) {
			return nil, status.Error(codes.InvalidArgument, "bad direct ratchet envelope")
		}
		directEnvelopes = append(directEnvelopes, store.DirectEnvelope{
			TargetUsername:               envelope.GetTargetUsername(),
			TargetDeviceID:               envelope.GetTargetDeviceId(),
			Ciphertext:                   envelope.GetCiphertext(),
			Nonce:                        envelope.GetNonce(),
			RecipientSignedPrekeyID:      envelope.GetRecipientSignedPrekeyId(),
			RecipientSignedPrekeyPublic:  envelope.GetRecipientSignedPrekeyPublic(),
			RecipientOneTimePrekeyID:     envelope.GetRecipientOneTimePrekeyId(),
			RecipientOneTimePrekeyPublic: envelope.GetRecipientOneTimePrekeyPublic(),
			E2EEAlgorithm:                envelope.GetE2EeAlgorithm(),
			RatchetPublicKey:             envelope.GetRatchetPublicKey(),
			PreviousChainLength:          envelope.GetPreviousChainLength(),
			MessageNumber:                envelope.GetMessageNumber(),
		})
	}

	saved, err := s.msgStore.Save(ctx, store.Message{
		ConversationID:               convID,
		From:                         username,
		To:                           recipient,
		SenderDeviceID:               deviceID,
		Text:                         req.GetText(),
		Ciphertext:                   ciphertextToStore,
		Nonce:                        nonceToStore,
		SenderKeyID:                  req.GetSenderKeyId(),
		KeyVersion:                   req.GetConversationKeyVersion(),
		Encrypted:                    req.GetEncrypted(),
		RecipientSignedPrekeyID:      req.GetRecipientSignedPrekeyId(),
		RecipientSignedPrekeyPublic:  req.GetRecipientSignedPrekeyPublic(),
		RecipientOneTimePrekeyID:     req.GetRecipientOneTimePrekeyId(),
		RecipientOneTimePrekeyPublic: req.GetRecipientOneTimePrekeyPublic(),
		DirectEnvelopes:              directEnvelopes,
		Attachments:                  attachments,
		TS:                           now,
	})
	if err != nil {
		log.Printf("failed to save message: conv=%s from=%s to=%s encrypted=%t direct_envelopes=%d attachments=%d err=%v", convID, username, recipient, req.GetEncrypted(), len(directEnvelopes), len(attachments), err)
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad message")
		}
		return nil, status.Error(codes.Internal, "failed to save message")
	}

	msg, err := s.projectMessageForDevice(saved, username, deviceID)
	if err != nil {
		return nil, status.Error(codes.Internal, "failed to project message")
	}
	if len(req.GetArchiveRecords()) > 0 && s.archiveStore != nil {
		archiveRecords, err := s.archiveRecordsFromProto(req.GetArchiveRecords(), convID, username, participants)
		if err != nil {
			return nil, err
		}
		for i := range archiveRecords {
			if archiveRecords[i].MessageID == 0 {
				archiveRecords[i].MessageID = saved.ID
			}
		}
		if err := s.archiveStore.AppendRecords(ctx, archiveRecords); err != nil {
			if errors.Is(err, store.ErrBadInput) {
				return nil, status.Error(codes.InvalidArgument, "bad archive record")
			}
			return nil, status.Error(codes.Internal, "failed to save history archive")
		}
	}
	s.hub.publishMessageUsers(participants, saved, s.projectMessageForDevice)
	return msg, nil
}

func (s *Server) GetMessages(ctx context.Context, req *messengerv1.GetMessagesRequest) (*messengerv1.GetMessagesResponse, error) {
	username, deviceID, err := s.requireAuth(ctx)
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
		projected, err := s.projectMessageForDevice(msg, username, deviceID)
		if err != nil {
			if errors.Is(err, errDirectMessageUnavailableForDevice) {
				items = append(items, unavailableDirectMessage(msg))
				continue
			}
			return nil, status.Error(codes.Internal, "failed to project messages")
		}
		items = append(items, projected)
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

func (s *Server) AppendHistoryArchiveRecords(ctx context.Context, req *messengerv1.AppendHistoryArchiveRecordsRequest) (*emptypb.Empty, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	if s.archiveStore == nil {
		return nil, status.Error(codes.Unimplemented, "history archive is not configured")
	}
	records := make([]store.HistoryArchiveRecord, 0, len(req.GetItems()))
	for _, item := range req.GetItems() {
		if item.GetOwnerUsername() != username || item.GetSender() != username {
			return nil, status.Error(codes.PermissionDenied, "not allowed to append archive records for another user")
		}
		records = append(records, historyArchiveRecordFromProto(item))
	}
	if err := s.archiveStore.AppendRecords(ctx, records); err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad archive record")
		}
		return nil, status.Error(codes.Internal, "failed to append archive records")
	}
	return &emptypb.Empty{}, nil
}

func (s *Server) ListHistoryArchiveRecords(ctx context.Context, req *messengerv1.ListHistoryArchiveRecordsRequest) (*messengerv1.ListHistoryArchiveRecordsResponse, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	if s.archiveStore == nil {
		return nil, status.Error(codes.Unimplemented, "history archive is not configured")
	}
	records, err := s.archiveStore.ListRecords(ctx, username, req.GetAfterSequence(), int(req.GetLimit()))
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad request")
		}
		return nil, status.Error(codes.Internal, "failed to list archive records")
	}
	items := make([]*messengerv1.HistoryArchiveRecord, 0, len(records))
	for _, item := range records {
		items = append(items, historyArchiveRecordToProto(item))
	}
	return &messengerv1.ListHistoryArchiveRecordsResponse{Items: items}, nil
}

func (s *Server) PrepareMediaUpload(ctx context.Context, req *messengerv1.PrepareMediaUploadRequest) (*messengerv1.PrepareMediaUploadResponse, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	kind, err := attachmentKindFromProto(req.GetKind())
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "bad attachment kind")
	}
	if strings.TrimSpace(req.GetFilename()) == "" || strings.TrimSpace(req.GetMimeType()) == "" {
		return nil, status.Error(codes.InvalidArgument, "filename and mime_type are required")
	}
	if req.GetSizeBytes() <= 0 || req.GetSizeBytes() > store.MaxMediaSizeBytes {
		return nil, status.Error(codes.InvalidArgument, "invalid file size")
	}

	mediaID := newMediaID()
	media := store.MediaObject{
		MediaID:       mediaID,
		OwnerUsername: username,
		StorageKey:    mediaStorageKey(mediaID),
		Filename:      req.GetFilename(),
		MimeType:      req.GetMimeType(),
		Kind:          kind,
		SizeBytes:     req.GetSizeBytes(),
		CreatedAt:     time.Now().UTC(),
	}
	if _, err := s.msgStore.PrepareMedia(ctx, media); err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad media metadata")
		}
		return nil, status.Error(codes.Internal, "failed to prepare media")
	}
	return &messengerv1.PrepareMediaUploadResponse{
		MediaId:      mediaID,
		MaxSizeBytes: store.MaxMediaSizeBytes,
	}, nil
}

func (s *Server) UploadMedia(ctx context.Context, req *messengerv1.UploadMediaRequest) (*messengerv1.UploadMediaResponse, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	if strings.TrimSpace(req.GetMediaId()) == "" || len(req.GetCiphertext()) == 0 || len(req.GetNonce()) == 0 || len(req.GetSha256()) == 0 {
		return nil, status.Error(codes.InvalidArgument, "media_id, ciphertext, nonce and sha256 are required")
	}

	media, err := s.msgStore.GetMedia(ctx, req.GetMediaId())
	if err != nil {
		if errors.Is(err, store.ErrMediaNotFound) {
			return nil, status.Error(codes.NotFound, "media not found")
		}
		return nil, status.Error(codes.Internal, "failed to load media")
	}
	if media.OwnerUsername != username {
		return nil, status.Error(codes.PermissionDenied, "not allowed")
	}
	if media.Uploaded {
		return nil, status.Error(codes.AlreadyExists, "media already uploaded")
	}

	kind, err := attachmentKindFromProto(req.GetKind())
	if err != nil {
		return nil, status.Error(codes.InvalidArgument, "bad attachment kind")
	}
	if media.SizeBytes != req.GetSizeBytes() || media.MimeType != req.GetMimeType() || media.Filename != req.GetFilename() || media.Kind != kind {
		return nil, status.Error(codes.InvalidArgument, "media metadata mismatch")
	}
	if req.GetSizeBytes() <= 0 || req.GetSizeBytes() > store.MaxMediaSizeBytes {
		return nil, status.Error(codes.InvalidArgument, "invalid file size")
	}
	if err := s.writeMediaFile(media.StorageKey, req.GetCiphertext()); err != nil {
		return nil, status.Error(codes.Internal, "failed to store media")
	}

	media.Nonce = req.GetNonce()
	media.SHA256 = req.GetSha256()
	media.CiphertextSize = int64(len(req.GetCiphertext()))
	media.Uploaded = true
	media.UploadedAt = time.Now().UTC()
	if _, err := s.msgStore.CompleteMedia(ctx, media); err != nil {
		_ = s.removeMediaFile(media.StorageKey)
		if errors.Is(err, store.ErrMediaNotFound) {
			return nil, status.Error(codes.NotFound, "media not found")
		}
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "bad media")
		}
		return nil, status.Error(codes.Internal, "failed to finalize media")
	}
	return &messengerv1.UploadMediaResponse{MediaId: media.MediaID}, nil
}

func (s *Server) GetMedia(ctx context.Context, req *messengerv1.GetMediaRequest) (*messengerv1.GetMediaResponse, error) {
	username, _, err := s.requireAuth(ctx)
	if err != nil {
		return nil, err
	}
	allowed, err := s.msgStore.CanAccessMedia(ctx, username, req.GetMediaId())
	if err != nil {
		if errors.Is(err, store.ErrBadInput) {
			return nil, status.Error(codes.InvalidArgument, "media_id is required")
		}
		return nil, status.Error(codes.Internal, "failed to authorize media access")
	}
	if !allowed {
		return nil, status.Error(codes.PermissionDenied, "not allowed")
	}

	media, err := s.msgStore.GetMedia(ctx, req.GetMediaId())
	if err != nil {
		if errors.Is(err, store.ErrMediaNotFound) {
			return nil, status.Error(codes.NotFound, "media not found")
		}
		return nil, status.Error(codes.Internal, "failed to load media")
	}
	ciphertext, err := s.readMediaFile(media.StorageKey)
	if err != nil {
		if errors.Is(err, os.ErrNotExist) {
			return nil, status.Error(codes.NotFound, "media blob not found")
		}
		return nil, status.Error(codes.Internal, "failed to read media")
	}
	return &messengerv1.GetMediaResponse{
		MediaId:    media.MediaID,
		Ciphertext: ciphertext,
		Nonce:      media.Nonce,
		Sha256:     media.SHA256,
		SizeBytes:  media.SizeBytes,
		MimeType:   media.MimeType,
		Filename:   media.Filename,
		Kind:       attachmentKindToProto(media.Kind),
	}, nil
}

func (s *Server) StreamEvents(req *messengerv1.StreamEventsRequest, stream messengerv1.MessageService_StreamEventsServer) error {
	username, deviceID, err := s.requireAuth(stream.Context())
	if err != nil {
		return err
	}
	if req == nil {
		req = &messengerv1.StreamEventsRequest{}
	}

	events, unsubscribe := s.hub.subscribe(username, deviceID)
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

func (s *Server) requireAuth(ctx context.Context) (username string, deviceID string, err error) {
	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		return "", "", status.Error(codes.Unauthenticated, "missing metadata")
	}

	token := firstMetadata(md, "authorization")
	if strings.HasPrefix(strings.ToLower(token), "bearer ") {
		token = strings.TrimSpace(token[7:])
	}
	if token == "" {
		token = firstMetadata(md, "token")
	}
	if token == "" {
		return "", "", status.Error(codes.Unauthenticated, "missing token")
	}

	session, ok := s.authSvc.SessionForToken(token)
	if !ok {
		return "", "", status.Error(codes.Unauthenticated, "unauthorized")
	}
	return session.Username, session.DeviceID, nil
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
	attachments := make([]*messengerv1.Attachment, 0, len(msg.Attachments))
	for _, item := range msg.Attachments {
		attachments = append(attachments, attachmentToProto(item))
	}
	return &messengerv1.Message{
		MessageId:                    msg.ID,
		ConversationId:               msg.ConversationID,
		From:                         msg.From,
		To:                           msg.To,
		SenderDeviceId:               msg.SenderDeviceID,
		Text:                         msg.Text,
		CreatedAt:                    timestamppb.New(msg.TS),
		Ciphertext:                   msg.Ciphertext,
		Nonce:                        msg.Nonce,
		SenderKeyId:                  msg.SenderKeyID,
		ConversationKeyVersion:       msg.KeyVersion,
		Encrypted:                    msg.Encrypted,
		RecipientSignedPrekeyId:      msg.RecipientSignedPrekeyID,
		RecipientSignedPrekeyPublic:  msg.RecipientSignedPrekeyPublic,
		RecipientOneTimePrekeyId:     msg.RecipientOneTimePrekeyID,
		RecipientOneTimePrekeyPublic: msg.RecipientOneTimePrekeyPublic,
		E2EeAlgorithm:                msg.E2EEAlgorithm,
		RatchetPublicKey:             msg.RatchetPublicKey,
		PreviousChainLength:          msg.PreviousChainLength,
		MessageNumber:                msg.MessageNumber,
		Attachments:                  attachments,
	}
}

func (s *Server) projectMessageForDevice(msg store.Message, username, deviceID string) (*messengerv1.Message, error) {
	projected := msg
	projected.Attachments = cloneStoreAttachments(msg.Attachments)

	isDirectEncrypted := msg.Encrypted && msg.To != msg.ConversationID

	if isDirectEncrypted && (len(msg.DirectEnvelopes) > 0 || slices.Equal(msg.Nonce, multiDeviceMarker)) {
		envelopes := msg.DirectEnvelopes
		if len(envelopes) == 0 {
			legacy, err := decodeDirectEnvelopes(msg.Ciphertext)
			if err != nil {
				return nil, err
			}
			envelopes = make([]store.DirectEnvelope, 0, len(legacy))
			for _, item := range legacy {
				envelopes = append(envelopes, store.DirectEnvelope{
					TargetUsername:               item.TargetUsername,
					TargetDeviceID:               item.TargetDeviceID,
					Ciphertext:                   item.Ciphertext,
					Nonce:                        item.Nonce,
					RecipientSignedPrekeyID:      item.RecipientSignedPrekeyID,
					RecipientSignedPrekeyPublic:  item.RecipientSignedPrekeyPublic,
					RecipientOneTimePrekeyID:     item.RecipientOneTimePrekeyID,
					RecipientOneTimePrekeyPublic: item.RecipientOneTimePrekeyPublic,
					E2EEAlgorithm:                item.E2EEAlgorithm,
					RatchetPublicKey:             item.RatchetPublicKey,
					PreviousChainLength:          item.PreviousChainLength,
					MessageNumber:                item.MessageNumber,
				})
			}
		}
		envelope, ok := pickDirectEnvelope(envelopes, msg, username, deviceID)
		if !ok {
			log.Printf("direct projection unavailable: msg_id=%d conv=%s viewer=%s viewer_device=%s sender=%s sender_device=%s recipient=%s direct_envelopes=%d",
				msg.ID, msg.ConversationID, username, deviceID, msg.From, msg.SenderDeviceID, msg.To, len(envelopes))
			return nil, errDirectMessageUnavailableForDevice
		}
		projected.Ciphertext = envelope.Ciphertext
		projected.Nonce = envelope.Nonce
		projected.RecipientSignedPrekeyID = envelope.RecipientSignedPrekeyID
		projected.RecipientSignedPrekeyPublic = envelope.RecipientSignedPrekeyPublic
		projected.RecipientOneTimePrekeyID = envelope.RecipientOneTimePrekeyID
		projected.RecipientOneTimePrekeyPublic = envelope.RecipientOneTimePrekeyPublic
		projected.E2EEAlgorithm = envelope.E2EEAlgorithm
		projected.RatchetPublicKey = envelope.RatchetPublicKey
		projected.PreviousChainLength = envelope.PreviousChainLength
		projected.MessageNumber = envelope.MessageNumber
	}

	if isDirectEncrypted {
		for i := range projected.Attachments {
			if len(projected.Attachments[i].DirectEnvelopes) == 0 && !slices.Equal(projected.Attachments[i].DescriptorNonce, multiDeviceMarker) {
				continue
			}
			attachmentEnvelopes := projected.Attachments[i].DirectEnvelopes
			if len(attachmentEnvelopes) == 0 {
				legacy, err := decodeAttachmentDirectEnvelopes(projected.Attachments[i].EncryptedDescriptor)
				if err != nil {
					return nil, err
				}
				attachmentEnvelopes = make([]store.AttachmentDirectEnvelope, 0, len(legacy))
				for _, item := range legacy {
					attachmentEnvelopes = append(attachmentEnvelopes, store.AttachmentDirectEnvelope{
						TargetUsername:               item.TargetUsername,
						TargetDeviceID:               item.TargetDeviceID,
						EncryptedDescriptor:          item.EncryptedDescriptor,
						DescriptorNonce:              item.DescriptorNonce,
						RecipientSignedPrekeyID:      item.RecipientSignedPrekeyID,
						RecipientSignedPrekeyPublic:  item.RecipientSignedPrekeyPublic,
						RecipientOneTimePrekeyID:     item.RecipientOneTimePrekeyID,
						RecipientOneTimePrekeyPublic: item.RecipientOneTimePrekeyPublic,
						E2EEAlgorithm:                item.E2EEAlgorithm,
						RatchetPublicKey:             item.RatchetPublicKey,
						PreviousChainLength:          item.PreviousChainLength,
						MessageNumber:                item.MessageNumber,
					})
				}
			}
			attachmentEnvelope, ok := pickAttachmentDirectEnvelope(attachmentEnvelopes, msg, username, deviceID)
			if !ok {
				log.Printf("attachment projection unavailable: msg_id=%d conv=%s viewer=%s viewer_device=%s sender=%s sender_device=%s recipient=%s attachment_id=%s attachment_envelopes=%d",
					msg.ID, msg.ConversationID, username, deviceID, msg.From, msg.SenderDeviceID, msg.To, projected.Attachments[i].AttachmentID, len(attachmentEnvelopes))
				return nil, errDirectMessageUnavailableForDevice
			}
			projected.Attachments[i].EncryptedDescriptor = attachmentEnvelope.EncryptedDescriptor
			projected.Attachments[i].DescriptorNonce = attachmentEnvelope.DescriptorNonce
			if projected.RecipientSignedPrekeyID == "" {
				projected.RecipientSignedPrekeyID = attachmentEnvelope.RecipientSignedPrekeyID
				projected.RecipientSignedPrekeyPublic = attachmentEnvelope.RecipientSignedPrekeyPublic
				projected.RecipientOneTimePrekeyID = attachmentEnvelope.RecipientOneTimePrekeyID
				projected.RecipientOneTimePrekeyPublic = attachmentEnvelope.RecipientOneTimePrekeyPublic
				projected.E2EEAlgorithm = attachmentEnvelope.E2EEAlgorithm
				projected.RatchetPublicKey = attachmentEnvelope.RatchetPublicKey
				projected.PreviousChainLength = attachmentEnvelope.PreviousChainLength
				projected.MessageNumber = attachmentEnvelope.MessageNumber
			}
		}
	}
	return messageFromStore(projected), nil
}

func unavailableDirectMessage(msg store.Message) *messengerv1.Message {
	return &messengerv1.Message{
		MessageId:      msg.ID,
		ConversationId: msg.ConversationID,
		From:           msg.From,
		To:             msg.To,
		Text:           "[Сообщение недоступно на этом устройстве]",
		CreatedAt:      timestamppb.New(msg.TS),
		Encrypted:      false,
	}
}

func cloneStoreAttachments(items []store.Attachment) []store.Attachment {
	out := make([]store.Attachment, len(items))
	copy(out, items)
	for i := range out {
		out[i].EncryptedDescriptor = append([]byte(nil), out[i].EncryptedDescriptor...)
		out[i].DescriptorNonce = append([]byte(nil), out[i].DescriptorNonce...)
		out[i].SHA256 = append([]byte(nil), out[i].SHA256...)
	}
	return out
}

func encodeDirectEnvelopes(items []*messengerv1.DirectMessageEnvelope) ([]byte, error) {
	envelopes := make([]storedDirectEnvelope, 0, len(items))
	for _, item := range items {
		envelopes = append(envelopes, storedDirectEnvelope{
			TargetUsername:               item.GetTargetUsername(),
			TargetDeviceID:               item.GetTargetDeviceId(),
			Ciphertext:                   item.GetCiphertext(),
			Nonce:                        item.GetNonce(),
			RecipientSignedPrekeyID:      item.GetRecipientSignedPrekeyId(),
			RecipientSignedPrekeyPublic:  item.GetRecipientSignedPrekeyPublic(),
			RecipientOneTimePrekeyID:     item.GetRecipientOneTimePrekeyId(),
			RecipientOneTimePrekeyPublic: item.GetRecipientOneTimePrekeyPublic(),
			E2EEAlgorithm:                item.GetE2EeAlgorithm(),
			RatchetPublicKey:             item.GetRatchetPublicKey(),
			PreviousChainLength:          item.GetPreviousChainLength(),
			MessageNumber:                item.GetMessageNumber(),
		})
	}
	return json.Marshal(envelopes)
}

func validDirectEnvelopeV2(algorithm string, ratchetPublicKey, nonce, ciphertext []byte) bool {
	return algorithm == doubleRatchetAlgorithm && len(ratchetPublicKey) > 0 && len(nonce) > 0 && len(ciphertext) > 0
}

func decodeDirectEnvelopes(raw []byte) ([]storedDirectEnvelope, error) {
	var envelopes []storedDirectEnvelope
	if err := json.Unmarshal(raw, &envelopes); err != nil {
		return nil, err
	}
	return envelopes, nil
}

func pickDirectEnvelope(items []store.DirectEnvelope, msg store.Message, username, deviceID string) (store.DirectEnvelope, bool) {
	for _, item := range items {
		if item.TargetUsername == username && item.TargetDeviceID == deviceID {
			return item, true
		}
	}
	if username == msg.From && msg.SenderDeviceID == deviceID {
		for _, item := range items {
			if item.TargetUsername == msg.To {
				return item, true
			}
		}
	}
	return store.DirectEnvelope{}, false
}

func encodeAttachmentDirectEnvelopes(items []*messengerv1.AttachmentDirectEnvelope) ([]byte, error) {
	envelopes := make([]storedAttachmentDirectEnvelope, 0, len(items))
	for _, item := range items {
		envelopes = append(envelopes, storedAttachmentDirectEnvelope{
			TargetUsername:               item.GetTargetUsername(),
			TargetDeviceID:               item.GetTargetDeviceId(),
			EncryptedDescriptor:          item.GetEncryptedDescriptor(),
			DescriptorNonce:              item.GetDescriptorNonce(),
			RecipientSignedPrekeyID:      item.GetRecipientSignedPrekeyId(),
			RecipientSignedPrekeyPublic:  item.GetRecipientSignedPrekeyPublic(),
			RecipientOneTimePrekeyID:     item.GetRecipientOneTimePrekeyId(),
			RecipientOneTimePrekeyPublic: item.GetRecipientOneTimePrekeyPublic(),
			E2EEAlgorithm:                item.GetE2EeAlgorithm(),
			RatchetPublicKey:             item.GetRatchetPublicKey(),
			PreviousChainLength:          item.GetPreviousChainLength(),
			MessageNumber:                item.GetMessageNumber(),
		})
	}
	return json.Marshal(envelopes)
}

func decodeAttachmentDirectEnvelopes(raw []byte) ([]storedAttachmentDirectEnvelope, error) {
	var envelopes []storedAttachmentDirectEnvelope
	if err := json.Unmarshal(raw, &envelopes); err != nil {
		return nil, err
	}
	return envelopes, nil
}

func pickAttachmentDirectEnvelope(items []store.AttachmentDirectEnvelope, msg store.Message, username, deviceID string) (store.AttachmentDirectEnvelope, bool) {
	for _, item := range items {
		if item.TargetUsername == username && item.TargetDeviceID == deviceID {
			return item, true
		}
	}
	if username == msg.From && msg.SenderDeviceID == deviceID {
		for _, item := range items {
			if item.TargetUsername == msg.To {
				return item, true
			}
		}
	}
	return store.AttachmentDirectEnvelope{}, false
}

func attachmentToProto(item store.Attachment) *messengerv1.Attachment {
	attachment := &messengerv1.Attachment{
		AttachmentId:        item.AttachmentID,
		Kind:                attachmentKindToProto(item.Kind),
		Filename:            item.Filename,
		MimeType:            item.MimeType,
		SizeBytes:           item.SizeBytes,
		MediaId:             item.MediaID,
		EncryptedDescriptor: item.EncryptedDescriptor,
		DescriptorNonce:     item.DescriptorNonce,
		Sha256:              item.SHA256,
		CiphertextSize:      item.CiphertextSize,
	}
	if item.PreviewWidth > 0 || item.PreviewHeight > 0 {
		attachment.Preview = &messengerv1.AttachmentPreview{
			Width:  item.PreviewWidth,
			Height: item.PreviewHeight,
		}
	}
	return attachment
}

func attachmentKindFromProto(kind messengerv1.AttachmentKind) (store.AttachmentKind, error) {
	switch kind {
	case messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE:
		return store.AttachmentKindImage, nil
	case messengerv1.AttachmentKind_ATTACHMENT_KIND_VIDEO:
		return store.AttachmentKindVideo, nil
	case messengerv1.AttachmentKind_ATTACHMENT_KIND_FILE:
		return store.AttachmentKindFile, nil
	default:
		return "", store.ErrBadInput
	}
}

func attachmentKindToProto(kind store.AttachmentKind) messengerv1.AttachmentKind {
	switch kind {
	case store.AttachmentKindImage:
		return messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE
	case store.AttachmentKindVideo:
		return messengerv1.AttachmentKind_ATTACHMENT_KIND_VIDEO
	default:
		return messengerv1.AttachmentKind_ATTACHMENT_KIND_FILE
	}
}

func identityKeyToProto(key store.IdentityKey) *messengerv1.IdentityKey {
	return &messengerv1.IdentityKey{
		Username:    key.Username,
		DeviceId:    key.DeviceID,
		KeyId:       key.KeyID,
		Algorithm:   key.Algorithm,
		PublicKey:   key.PublicKey,
		PublishedAt: timestamppb.New(key.PublishedAt),
	}
}

func archiveKeyBundleToProto(bundle store.ArchiveKeyBundle, includePrivate bool) *messengerv1.ArchiveKeyBundle {
	out := &messengerv1.ArchiveKeyBundle{
		Username:  bundle.Username,
		PublicKey: bundle.PublicKey,
		Version:   bundle.Version,
		UpdatedAt: timestamppb.New(bundle.UpdatedAt),
	}
	if includePrivate {
		out.EncryptedPrivateKey = bundle.EncryptedPrivateKey
		out.KdfSalt = bundle.KDFSalt
		out.KdfParams = bundle.KDFParams
	}
	return out
}

func archiveHeaderToProto(bundle store.ArchiveKeyBundle) *messengerv1.HistoryArchiveHeader {
	return &messengerv1.HistoryArchiveHeader{
		Username:            bundle.Username,
		PublicKey:           bundle.PublicKey,
		EncryptedPrivateKey: bundle.EncryptedPrivateKey,
		KdfSalt:             bundle.KDFSalt,
		KdfParams:           bundle.KDFParams,
		Version:             bundle.Version,
		UpdatedAt:           timestamppb.New(bundle.UpdatedAt),
	}
}

func historyArchiveRecordFromProto(item *messengerv1.HistoryArchiveRecord) store.HistoryArchiveRecord {
	record := store.HistoryArchiveRecord{
		Sequence:           item.GetSequence(),
		RecordID:           item.GetRecordId(),
		OwnerUsername:      item.GetOwnerUsername(),
		ConversationID:     item.GetConversationId(),
		RecordType:         historyArchiveRecordTypeFromProto(item.GetRecordType()),
		MessageID:          item.GetMessageId(),
		AttachmentID:       item.GetAttachmentId(),
		GroupKeyVersion:    item.GetGroupKeyVersion(),
		Sender:             item.GetSender(),
		Ciphertext:         item.GetCiphertext(),
		Nonce:              item.GetNonce(),
		EphemeralPublicKey: item.GetEphemeralPublicKey(),
		ArchiveKeyVersion:  item.GetArchiveKeyVersion(),
	}
	if createdAt := item.GetCreatedAt(); createdAt != nil {
		record.CreatedAt = createdAt.AsTime()
	}
	return record
}

func historyArchiveRecordToProto(item store.HistoryArchiveRecord) *messengerv1.HistoryArchiveRecord {
	return &messengerv1.HistoryArchiveRecord{
		Sequence:           item.Sequence,
		RecordId:           item.RecordID,
		OwnerUsername:      item.OwnerUsername,
		ConversationId:     item.ConversationID,
		RecordType:         historyArchiveRecordTypeToProto(item.RecordType),
		MessageId:          item.MessageID,
		AttachmentId:       item.AttachmentID,
		GroupKeyVersion:    item.GroupKeyVersion,
		Sender:             item.Sender,
		CreatedAt:          timestamppb.New(item.CreatedAt),
		Ciphertext:         item.Ciphertext,
		Nonce:              item.Nonce,
		EphemeralPublicKey: item.EphemeralPublicKey,
		ArchiveKeyVersion:  item.ArchiveKeyVersion,
	}
}

func historyArchiveRecordTypeFromProto(value messengerv1.HistoryArchiveRecordType) store.HistoryArchiveRecordType {
	switch value {
	case messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_GROUP_MESSAGE:
		return store.HistoryArchiveRecordTypeGroupMessage
	case messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_DIRECT_ATTACHMENT_DESCRIPTOR:
		return store.HistoryArchiveRecordTypeDirectAttachmentDescriptor
	case messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_GROUP_ATTACHMENT_DESCRIPTOR:
		return store.HistoryArchiveRecordTypeGroupAttachmentDescriptor
	case messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_GROUP_KEY_VERSION:
		return store.HistoryArchiveRecordTypeGroupKeyVersion
	default:
		return store.HistoryArchiveRecordTypeDirectMessage
	}
}

func historyArchiveRecordTypeToProto(value store.HistoryArchiveRecordType) messengerv1.HistoryArchiveRecordType {
	switch value {
	case store.HistoryArchiveRecordTypeGroupMessage:
		return messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_GROUP_MESSAGE
	case store.HistoryArchiveRecordTypeDirectAttachmentDescriptor:
		return messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_DIRECT_ATTACHMENT_DESCRIPTOR
	case store.HistoryArchiveRecordTypeGroupAttachmentDescriptor:
		return messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_GROUP_ATTACHMENT_DESCRIPTOR
	case store.HistoryArchiveRecordTypeGroupKeyVersion:
		return messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_GROUP_KEY_VERSION
	default:
		return messengerv1.HistoryArchiveRecordType_HISTORY_ARCHIVE_RECORD_TYPE_DIRECT_MESSAGE
	}
}

func conversationKeyToProto(key store.ConversationKey) *messengerv1.ConversationKey {
	envelopes := make([]*messengerv1.ConversationKeyEnvelope, 0, len(key.Envelopes))
	for _, envelope := range key.Envelopes {
		envelopes = append(envelopes, &messengerv1.ConversationKeyEnvelope{
			Username:       envelope.Username,
			DeviceId:       envelope.DeviceID,
			EncryptedKey:   envelope.EncryptedKey,
			Nonce:          envelope.Nonce,
			SenderKeyId:    envelope.SenderKeyID,
			RecipientKeyId: envelope.RecipientKeyID,
		})
	}
	return &messengerv1.ConversationKey{
		ConversationId: key.ConversationID,
		Version:        key.Version,
		Algorithm:      key.Algorithm,
		CreatedBy:      key.CreatedBy,
		Envelopes:      envelopes,
		CreatedAt:      timestamppb.New(key.CreatedAt),
	}
}

func prekeyBundleToProto(bundle store.PrekeyBundle) *messengerv1.PrekeyBundle {
	return &messengerv1.PrekeyBundle{
		Username:      bundle.Username,
		DeviceId:      bundle.DeviceID,
		IdentityKey:   identityKeyToProto(bundle.IdentityKey),
		SignedPrekey:  signedPrekeyToProto(bundle.SignedPrekey),
		OneTimePrekey: oneTimePrekeyToProto(bundle.OneTimePrekey),
	}
}

func signedPrekeyToProto(key store.SignedPrekey) *messengerv1.SignedPrekey {
	if key.Username == "" {
		return nil
	}
	return &messengerv1.SignedPrekey{
		Username:           key.Username,
		DeviceId:           key.DeviceID,
		KeyId:              key.KeyID,
		Algorithm:          key.Algorithm,
		PublicKey:          key.PublicKey,
		Signature:          key.Signature,
		SignatureAlgorithm: key.SignatureAlgorithm,
		PublishedAt:        timestamppb.New(key.PublishedAt),
	}
}

func oneTimePrekeyToProto(key store.OneTimePrekey) *messengerv1.OneTimePrekey {
	if key.Username == "" {
		return nil
	}
	return &messengerv1.OneTimePrekey{
		Username:    key.Username,
		DeviceId:    key.DeviceID,
		KeyId:       key.KeyID,
		Algorithm:   key.Algorithm,
		PublicKey:   key.PublicKey,
		PublishedAt: timestamppb.New(key.PublishedAt),
	}
}

func groupKeyUpdateFromProto(req *messengerv1.GroupKeyUpdate, conversationID, createdBy string) store.ConversationKey {
	envelopes := make([]store.ConversationKeyEnvelope, 0, len(req.GetEnvelopes()))
	for _, envelope := range req.GetEnvelopes() {
		envelopes = append(envelopes, store.ConversationKeyEnvelope{
			Username:       envelope.GetUsername(),
			DeviceID:       envelope.GetDeviceId(),
			EncryptedKey:   envelope.GetEncryptedKey(),
			Nonce:          envelope.GetNonce(),
			SenderKeyID:    envelope.GetSenderKeyId(),
			RecipientKeyID: envelope.GetRecipientKeyId(),
		})
	}
	return store.ConversationKey{
		ConversationID: conversationID,
		Version:        req.GetVersion(),
		Algorithm:      req.GetAlgorithm(),
		CreatedBy:      createdBy,
		Envelopes:      envelopes,
	}
}

func (s *Server) archiveRecordsFromProto(items []*messengerv1.HistoryArchiveRecord, conversationID, sender string, allowedOwners []string) ([]store.HistoryArchiveRecord, error) {
	allowed := make(map[string]struct{}, len(allowedOwners))
	for _, owner := range allowedOwners {
		allowed[owner] = struct{}{}
	}
	records := make([]store.HistoryArchiveRecord, 0, len(items))
	for _, item := range items {
		record := historyArchiveRecordFromProto(item)
		if record.ConversationID != conversationID || record.Sender != sender {
			return nil, status.Error(codes.InvalidArgument, "archive record metadata mismatch")
		}
		if _, ok := allowed[record.OwnerUsername]; !ok {
			return nil, status.Error(codes.PermissionDenied, "archive record owner is not a conversation participant")
		}
		records = append(records, record)
	}
	return records, nil
}

func groupKeyUpdatePtrFromProto(req *messengerv1.GroupKeyUpdate, conversationID, createdBy string) *store.ConversationKey {
	if req == nil {
		return nil
	}
	key := groupKeyUpdateFromProto(req, conversationID, createdBy)
	return &key
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

func defaultMediaRoot() string {
	if value := strings.TrimSpace(os.Getenv("MEDIA_DIR")); value != "" {
		return value
	}
	return filepath.Join(".", "data", "media")
}

func newMediaID() string {
	token := make([]byte, 12)
	if _, err := rand.Read(token); err != nil {
		return fmt.Sprintf("media-%d", time.Now().UnixNano())
	}
	return fmt.Sprintf("media-%x", token)
}

func mediaStorageKey(mediaID string) string {
	prefix := "xx"
	if len(mediaID) >= 2 {
		prefix = mediaID[len(mediaID)-2:]
	}
	return filepath.Join(prefix, mediaID+".bin")
}

func (s *Server) mediaPath(storageKey string) string {
	return filepath.Join(s.mediaRoot, storageKey)
}

func (s *Server) writeMediaFile(storageKey string, ciphertext []byte) error {
	path := s.mediaPath(storageKey)
	if err := os.MkdirAll(filepath.Dir(path), 0o755); err != nil {
		return err
	}
	tmpPath := path + ".tmp"
	if err := os.WriteFile(tmpPath, ciphertext, 0o600); err != nil {
		return err
	}
	return os.Rename(tmpPath, path)
}

func (s *Server) readMediaFile(storageKey string) ([]byte, error) {
	return os.ReadFile(s.mediaPath(storageKey))
}

func (s *Server) removeMediaFile(storageKey string) error {
	err := os.Remove(s.mediaPath(storageKey))
	if err != nil && !errors.Is(err, os.ErrNotExist) {
		return err
	}
	return nil
}

func (s *Server) deleteMediaFiles(items []store.MediaObject) {
	for _, item := range items {
		_ = s.removeMediaFile(item.StorageKey)
	}
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
	subscribers map[string]map[chan *messengerv1.ServerEvent]string
}

func newEventHub() *eventHub {
	return &eventHub{
		subscribers: make(map[string]map[chan *messengerv1.ServerEvent]string),
	}
}

func (h *eventHub) subscribe(username, deviceID string) (<-chan *messengerv1.ServerEvent, func()) {
	ch := make(chan *messengerv1.ServerEvent, 32)

	h.mu.Lock()
	if _, ok := h.subscribers[username]; !ok {
		h.subscribers[username] = make(map[chan *messengerv1.ServerEvent]string)
	}
	h.subscribers[username][ch] = deviceID
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

func (h *eventHub) publishMessageUsers(usernames []string, msg store.Message, projector func(store.Message, string, string) (*messengerv1.Message, error)) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	for _, username := range usernames {
		for ch, deviceID := range h.subscribers[username] {
			projected, err := projector(msg, username, deviceID)
			if err != nil {
				continue
			}
			select {
			case ch <- newMessageEvent(projected):
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
