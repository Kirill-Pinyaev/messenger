package grpcapi

import (
	"context"
	"errors"
	"io"
	"net"
	"testing"
	"time"

	messengerv1 "messenger/gen/messenger/v1"
	"messenger/internal/auth"
	"messenger/internal/store"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
	"google.golang.org/grpc/test/bufconn"
	"google.golang.org/protobuf/types/known/emptypb"
)

const bufSize = 1024 * 1024

func TestServerAuthAndProfileFlow(t *testing.T) {
	t.Parallel()

	authClient, userClient, _, cleanup := newTestClients(t)
	defer cleanup()

	ctx := context.Background()

	if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
		Username:  "alice",
		Password:  "secret",
		FirstName: "Alice",
		LastName:  "Doe",
	}); err != nil {
		t.Fatalf("Register(alice) error = %v", err)
	}
	if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
		Username:  "bob",
		Password:  "secret",
		FirstName: "Bob",
		LastName:  "Smith",
		AvatarHex: "123abc",
	}); err != nil {
		t.Fatalf("Register(bob) error = %v", err)
	}

	if _, err := userClient.GetProfile(ctx, &messengerv1.GetProfileRequest{}); status.Code(err) != codes.Unauthenticated {
		t.Fatalf("GetProfile() without auth code = %v, want %v", status.Code(err), codes.Unauthenticated)
	}

	if _, err := authClient.Login(ctx, &messengerv1.LoginRequest{
		Username: "ghost",
		Password: "secret",
		DeviceId: "web-ghost",
	}); status.Code(err) != codes.NotFound {
		t.Fatalf("Login(ghost) code = %v, want %v", status.Code(err), codes.NotFound)
	}

	if _, err := authClient.Login(ctx, &messengerv1.LoginRequest{
		Username: "alice",
		Password: "bad",
		DeviceId: "web-alice",
	}); status.Code(err) != codes.Unauthenticated {
		t.Fatalf("Login(alice,bad) code = %v, want %v", status.Code(err), codes.Unauthenticated)
	}

	loginResp, err := authClient.Login(ctx, &messengerv1.LoginRequest{
		Username: "alice",
		Password: "secret",
		DeviceId: "web-alice",
	})
	if err != nil {
		t.Fatalf("Login(alice) error = %v", err)
	}
	if loginResp.GetToken() == "" {
		t.Fatal("Login(alice) returned empty token")
	}

	aliceCtx := authContext(ctx, loginResp.GetToken())

	profile, err := userClient.GetProfile(aliceCtx, &messengerv1.GetProfileRequest{})
	if err != nil {
		t.Fatalf("GetProfile(self) error = %v", err)
	}
	if profile.GetUsername() != "alice" || profile.GetAvatarHex() == "" {
		t.Fatalf("GetProfile(self) = %+v", profile)
	}

	searchResp, err := userClient.SearchUsers(aliceCtx, &messengerv1.SearchUsersRequest{
		Query: "bo",
		Limit: 5,
	})
	if err != nil {
		t.Fatalf("SearchUsers() error = %v", err)
	}
	if len(searchResp.GetItems()) != 1 || searchResp.GetItems()[0].GetUsername() != "bob" {
		t.Fatalf("SearchUsers() = %+v", searchResp.GetItems())
	}

	updated, err := userClient.UpdateProfile(aliceCtx, &messengerv1.UpdateProfileRequest{
		FirstName: "Alicia",
		AvatarHex: "abcdef",
	})
	if err != nil {
		t.Fatalf("UpdateProfile() error = %v", err)
	}
	if updated.GetFirstName() != "Alicia" || updated.GetAvatarHex() != "abcdef" {
		t.Fatalf("UpdateProfile() = %+v", updated)
	}

	bobProfile, err := userClient.GetProfile(aliceCtx, &messengerv1.GetProfileRequest{Username: "bob"})
	if err != nil {
		t.Fatalf("GetProfile(bob) error = %v", err)
	}
	if bobProfile.GetUsername() != "bob" || bobProfile.GetAvatarHex() != "123abc" {
		t.Fatalf("GetProfile(bob) = %+v", bobProfile)
	}

	if _, err := authClient.DeleteAccount(aliceCtx, &emptypb.Empty{}); err != nil {
		t.Fatalf("DeleteAccount() error = %v", err)
	}
	if _, err := userClient.ListConversations(aliceCtx, &emptypb.Empty{}); status.Code(err) != codes.Unauthenticated {
		t.Fatalf("ListConversations() after delete code = %v, want %v", status.Code(err), codes.Unauthenticated)
	}
}

func TestServerMessageFlowAndStreaming(t *testing.T) {
	t.Parallel()

	authClient, userClient, messageClient, cleanup := newTestClients(t)
	defer cleanup()

	ctx := context.Background()
	registerAndLogin := func(username string) context.Context {
		t.Helper()

		if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
			Username:  username,
			Password:  "secret",
			FirstName: username,
		}); err != nil {
			t.Fatalf("Register(%s) error = %v", username, err)
		}
		loginResp, err := authClient.Login(ctx, &messengerv1.LoginRequest{
			Username: username,
			Password: "secret",
			DeviceId: username + "-device",
		})
		if err != nil {
			t.Fatalf("Login(%s) error = %v", username, err)
		}
		return authContext(ctx, loginResp.GetToken())
	}

	aliceCtx := registerAndLogin("alice")
	bobCtx := registerAndLogin("bob")
	_ = registerAndLogin("carol")

	bobStream, err := messageClient.StreamEvents(bobCtx, &messengerv1.StreamEventsRequest{})
	if err != nil {
		t.Fatalf("StreamEvents(bob) error = %v", err)
	}
	aliceStream, err := messageClient.StreamEvents(aliceCtx, &messengerv1.StreamEventsRequest{})
	if err != nil {
		t.Fatalf("StreamEvents(alice) error = %v", err)
	}

	if _, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		presence := event.GetPresence()
		return presence != nil && containsAll(presence.GetOnlineUsers(), "alice", "bob")
	}); err != nil {
		t.Fatalf("waiting for bob presence event: %v", err)
	}
	if _, err := waitForEvent(t, aliceStream, func(event *messengerv1.ServerEvent) bool {
		presence := event.GetPresence()
		return presence != nil && containsAll(presence.GetOnlineUsers(), "alice", "bob")
	}); err != nil {
		t.Fatalf("waiting for alice presence event: %v", err)
	}

	sent, err := messageClient.SendMessage(aliceCtx, &messengerv1.SendMessageRequest{
		To:   "bob",
		Text: "hello over grpc",
	})
	if err != nil {
		t.Fatalf("SendMessage() error = %v", err)
	}
	if sent.GetMessageId() == 0 || sent.GetConversationId() != "alice|bob" {
		t.Fatalf("SendMessage() = %+v", sent)
	}

	event, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		msg := event.GetMessage()
		return msg != nil && msg.GetMessageId() == sent.GetMessageId()
	})
	if err != nil {
		t.Fatalf("waiting for bob message event: %v", err)
	}
	if got := event.GetMessage(); got.GetText() != "hello over grpc" || got.GetFrom() != "alice" || got.GetTo() != "bob" {
		t.Fatalf("message event = %+v", got)
	}

	history, err := messageClient.GetMessages(aliceCtx, &messengerv1.GetMessagesRequest{
		WithUsername: "bob",
		Limit:        10,
	})
	if err != nil {
		t.Fatalf("GetMessages() error = %v", err)
	}
	if len(history.GetItems()) != 1 || history.GetItems()[0].GetMessageId() != sent.GetMessageId() {
		t.Fatalf("GetMessages() = %+v", history.GetItems())
	}

	conversations, err := userClient.ListConversations(aliceCtx, &emptypb.Empty{})
	if err != nil {
		t.Fatalf("ListConversations() error = %v", err)
	}
	if len(conversations.GetItems()) != 1 || conversations.GetItems()[0].GetPeerUsername() != "bob" {
		t.Fatalf("ListConversations() = %+v", conversations.GetItems())
	}

	found, err := messageClient.SearchMessages(aliceCtx, &messengerv1.SearchMessagesRequest{
		Query: "grpc",
		Limit: 10,
	})
	if err != nil {
		t.Fatalf("SearchMessages() error = %v", err)
	}
	if len(found.GetItems()) != 1 || found.GetItems()[0].GetMessageId() != sent.GetMessageId() {
		t.Fatalf("SearchMessages() = %+v", found.GetItems())
	}

	if _, err := messageClient.DeleteMessage(bobCtx, &messengerv1.DeleteMessageRequest{MessageId: sent.GetMessageId()}); status.Code(err) != codes.PermissionDenied {
		t.Fatalf("DeleteMessage(bob) code = %v, want %v", status.Code(err), codes.PermissionDenied)
	}

	if _, err := messageClient.DeleteMessage(aliceCtx, &messengerv1.DeleteMessageRequest{MessageId: sent.GetMessageId()}); err != nil {
		t.Fatalf("DeleteMessage(alice) error = %v", err)
	}

	deletedEvent, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		deleted := event.GetMessageDeleted()
		return deleted != nil && deleted.GetMessageId() == sent.GetMessageId()
	})
	if err != nil {
		t.Fatalf("waiting for delete event: %v", err)
	}
	if deletedEvent.GetMessageDeleted().GetConversationId() != "alice|bob" {
		t.Fatalf("delete event = %+v", deletedEvent.GetMessageDeleted())
	}

	history, err = messageClient.GetMessages(aliceCtx, &messengerv1.GetMessagesRequest{
		ConversationId: "alice|bob",
		Limit:          10,
	})
	if err != nil {
		t.Fatalf("GetMessages() after delete error = %v", err)
	}
	if len(history.GetItems()) != 0 {
		t.Fatalf("GetMessages() after delete len = %d, want 0", len(history.GetItems()))
	}
}

func TestServerMediaFlow(t *testing.T) {
	t.Setenv("MEDIA_DIR", t.TempDir())
	authClient, _, messageClient, cleanup := newTestClients(t)
	defer cleanup()

	ctx := context.Background()
	registerAndLogin := func(username string) context.Context {
		t.Helper()
		if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
			Username: username,
			Password: "secret",
		}); err != nil {
			t.Fatalf("Register(%s) error = %v", username, err)
		}
		loginResp, err := authClient.Login(ctx, &messengerv1.LoginRequest{
			Username: username,
			Password: "secret",
			DeviceId: username + "-device",
		})
		if err != nil {
			t.Fatalf("Login(%s) error = %v", username, err)
		}
		return authContext(ctx, loginResp.GetToken())
	}

	aliceCtx := registerAndLogin("alice")
	bobCtx := registerAndLogin("bob")

	prepared, err := messageClient.PrepareMediaUpload(aliceCtx, &messengerv1.PrepareMediaUploadRequest{
		Filename:  "cat.png",
		MimeType:  "image/png",
		SizeBytes: 128,
		Kind:      messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE,
	})
	if err != nil {
		t.Fatalf("PrepareMediaUpload() error = %v", err)
	}
	if prepared.GetMediaId() == "" || prepared.GetMaxSizeBytes() != store.MaxMediaSizeBytes {
		t.Fatalf("PrepareMediaUpload() = %+v", prepared)
	}

	if _, err := messageClient.UploadMedia(aliceCtx, &messengerv1.UploadMediaRequest{
		MediaId:    prepared.GetMediaId(),
		Ciphertext: []byte{1, 2, 3, 4},
		Nonce:      []byte{5, 6, 7},
		Sha256:     []byte{8, 9, 10},
		SizeBytes:  128,
		MimeType:   "image/png",
		Filename:   "cat.png",
		Kind:       messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE,
	}); err != nil {
		t.Fatalf("UploadMedia() error = %v", err)
	}

	sent, err := messageClient.SendMessage(aliceCtx, &messengerv1.SendMessageRequest{
		To: "bob",
		Attachments: []*messengerv1.Attachment{{
			AttachmentId:        "att-1",
			Kind:                messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE,
			Filename:            "cat.png",
			MimeType:            "image/png",
			SizeBytes:           128,
			MediaId:             prepared.GetMediaId(),
			EncryptedDescriptor: []byte{11, 12, 13},
			DescriptorNonce:     []byte{14, 15, 16},
			Sha256:              []byte{8, 9, 10},
			CiphertextSize:      4,
			Preview: &messengerv1.AttachmentPreview{
				Width:  320,
				Height: 240,
			},
		}},
	})
	if err != nil {
		t.Fatalf("SendMessage(attachment) error = %v", err)
	}
	if len(sent.GetAttachments()) != 1 || sent.GetAttachments()[0].GetMediaId() != prepared.GetMediaId() {
		t.Fatalf("SendMessage(attachment) = %+v", sent)
	}

	history, err := messageClient.GetMessages(bobCtx, &messengerv1.GetMessagesRequest{
		WithUsername: "alice",
		Limit:        10,
	})
	if err != nil {
		t.Fatalf("GetMessages() error = %v", err)
	}
	if len(history.GetItems()) != 1 || len(history.GetItems()[0].GetAttachments()) != 1 {
		t.Fatalf("GetMessages() = %+v", history.GetItems())
	}

	blob, err := messageClient.GetMedia(bobCtx, &messengerv1.GetMediaRequest{MediaId: prepared.GetMediaId()})
	if err != nil {
		t.Fatalf("GetMedia(bob) error = %v", err)
	}
	if len(blob.GetCiphertext()) != 4 || blob.GetFilename() != "cat.png" {
		t.Fatalf("GetMedia() = %+v", blob)
	}
}

func TestServerGroupConversationFlow(t *testing.T) {
	t.Parallel()

	authClient, userClient, messageClient, cleanup := newTestClients(t)
	defer cleanup()

	ctx := context.Background()
	registerAndLogin := func(username string) context.Context {
		t.Helper()

		if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
			Username: username,
			Password: "secret",
		}); err != nil {
			t.Fatalf("Register(%s) error = %v", username, err)
		}
		loginResp, err := authClient.Login(ctx, &messengerv1.LoginRequest{
			Username: username,
			Password: "secret",
			DeviceId: username + "-device",
		})
		if err != nil {
			t.Fatalf("Login(%s) error = %v", username, err)
		}
		return authContext(ctx, loginResp.GetToken())
	}

	aliceCtx := registerAndLogin("alice")
	bobCtx := registerAndLogin("bob")
	carolCtx := registerAndLogin("carol")
	daveCtx := registerAndLogin("dave")
	erinCtx := registerAndLogin("erin")

	bobStream, err := messageClient.StreamEvents(bobCtx, &messengerv1.StreamEventsRequest{})
	if err != nil {
		t.Fatalf("StreamEvents(bob) error = %v", err)
	}
	if _, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		return event.GetPresence() != nil
	}); err != nil {
		t.Fatalf("waiting for bob presence event: %v", err)
	}
	carolStream, err := messageClient.StreamEvents(carolCtx, &messengerv1.StreamEventsRequest{})
	if err != nil {
		t.Fatalf("StreamEvents(carol) error = %v", err)
	}
	if _, err := waitForEvent(t, carolStream, func(event *messengerv1.ServerEvent) bool {
		return event.GetPresence() != nil
	}); err != nil {
		t.Fatalf("waiting for carol presence event: %v", err)
	}
	daveStream, err := messageClient.StreamEvents(daveCtx, &messengerv1.StreamEventsRequest{})
	if err != nil {
		t.Fatalf("StreamEvents(dave) error = %v", err)
	}
	if _, err := waitForEvent(t, daveStream, func(event *messengerv1.ServerEvent) bool {
		return event.GetPresence() != nil
	}); err != nil {
		t.Fatalf("waiting for dave presence event: %v", err)
	}
	erinStream, err := messageClient.StreamEvents(erinCtx, &messengerv1.StreamEventsRequest{})
	if err != nil {
		t.Fatalf("StreamEvents(erin) error = %v", err)
	}
	if _, err := waitForEvent(t, erinStream, func(event *messengerv1.ServerEvent) bool {
		return event.GetPresence() != nil
	}); err != nil {
		t.Fatalf("waiting for erin presence event: %v", err)
	}

	group, err := userClient.CreateGroupConversation(aliceCtx, &messengerv1.CreateGroupConversationRequest{
		Title:           "Diploma team",
		MemberUsernames: []string{"bob", "carol"},
	})
	if err != nil {
		t.Fatalf("CreateGroupConversation() error = %v", err)
	}
	if group.GetKind() != messengerv1.ConversationKind_CONVERSATION_KIND_GROUP {
		t.Fatalf("CreateGroupConversation().Kind = %v", group.GetKind())
	}
	if group.GetTitle() != "Diploma team" {
		t.Fatalf("CreateGroupConversation().Title = %q", group.GetTitle())
	}
	if len(group.GetMemberUsernames()) != 3 {
		t.Fatalf("CreateGroupConversation().Members = %+v", group.GetMemberUsernames())
	}
	if len(group.GetMembers()) != 3 || group.GetMembers()[0].GetUsername() != "alice" || group.GetMembers()[0].GetRole() != messengerv1.ConversationRole_CONVERSATION_ROLE_ADMIN {
		t.Fatalf("CreateGroupConversation().DetailedMembers = %+v", group.GetMembers())
	}

	createdEvent, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		conversation := event.GetConversation()
		return conversation != nil && conversation.GetConversationId() == group.GetConversationId()
	})
	if err != nil {
		t.Fatalf("waiting for conversation created event: %v", err)
	}
	if createdEvent.GetConversation().GetTitle() != "Diploma team" {
		t.Fatalf("conversation event = %+v", createdEvent.GetConversation())
	}

	items, err := userClient.ListConversations(bobCtx, &emptypb.Empty{})
	if err != nil {
		t.Fatalf("ListConversations() error = %v", err)
	}
	if len(items.GetItems()) != 1 || items.GetItems()[0].GetKind() != messengerv1.ConversationKind_CONVERSATION_KIND_GROUP {
		t.Fatalf("ListConversations() = %+v", items.GetItems())
	}

	sent, err := messageClient.SendMessage(aliceCtx, &messengerv1.SendMessageRequest{
		ConversationId: group.GetConversationId(),
		Text:           "hello group",
	})
	if err != nil {
		t.Fatalf("SendMessage(group) error = %v", err)
	}
	if sent.GetConversationId() != group.GetConversationId() || sent.GetTo() != group.GetConversationId() {
		t.Fatalf("SendMessage(group) = %+v", sent)
	}

	event, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		msg := event.GetMessage()
		return msg != nil && msg.GetMessageId() == sent.GetMessageId()
	})
	if err != nil {
		t.Fatalf("waiting for group message event: %v", err)
	}
	if event.GetMessage().GetText() != "hello group" {
		t.Fatalf("group event = %+v", event.GetMessage())
	}

	history, err := messageClient.GetMessages(bobCtx, &messengerv1.GetMessagesRequest{
		ConversationId: group.GetConversationId(),
		Limit:          20,
	})
	if err != nil {
		t.Fatalf("GetMessages(group) error = %v", err)
	}
	if len(history.GetItems()) != 1 || history.GetItems()[0].GetConversationId() != group.GetConversationId() {
		t.Fatalf("GetMessages(group) = %+v", history.GetItems())
	}

	updatedGroup, err := userClient.AddGroupMembers(aliceCtx, &messengerv1.AddGroupMembersRequest{
		ConversationId:  group.GetConversationId(),
		MemberUsernames: []string{"dave"},
	})
	if err != nil {
		t.Fatalf("AddGroupMembers() error = %v", err)
	}
	if len(updatedGroup.GetMemberUsernames()) != 4 {
		t.Fatalf("AddGroupMembers() = %+v", updatedGroup.GetMemberUsernames())
	}
	if updatedGroup.GetMembers()[3].GetUsername() != "dave" || updatedGroup.GetMembers()[3].GetAddedBy() != "alice" {
		t.Fatalf("AddGroupMembers().Members = %+v", updatedGroup.GetMembers())
	}

	updatedEvent, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		conversation := event.GetConversation()
		return conversation != nil && len(conversation.GetMemberUsernames()) == 4
	})
	if err != nil {
		t.Fatalf("waiting for conversation updated event: %v", err)
	}
	if len(updatedEvent.GetConversation().GetMemberUsernames()) != 4 {
		t.Fatalf("updated conversation event = %+v", updatedEvent.GetConversation())
	}
	if _, err := waitForEvent(t, daveStream, func(event *messengerv1.ServerEvent) bool {
		conversation := event.GetConversation()
		return conversation != nil && conversation.GetConversationId() == group.GetConversationId()
	}); err != nil {
		t.Fatalf("waiting for dave added event: %v", err)
	}

	updatedGroup, err = userClient.RemoveGroupMember(aliceCtx, &messengerv1.RemoveGroupMemberRequest{
		ConversationId: group.GetConversationId(),
		Username:       "carol",
	})
	if err != nil {
		t.Fatalf("RemoveGroupMember() error = %v", err)
	}
	if len(updatedGroup.GetMemberUsernames()) != 3 {
		t.Fatalf("RemoveGroupMember() = %+v", updatedGroup.GetMemberUsernames())
	}
	if _, err := waitForEvent(t, carolStream, func(event *messengerv1.ServerEvent) bool {
		removed := event.GetConversationRemoved()
		return removed != nil && removed.GetConversationId() == group.GetConversationId()
	}); err != nil {
		t.Fatalf("waiting for carol removed event: %v", err)
	}

	carolConversations, err := userClient.ListConversations(carolCtx, &emptypb.Empty{})
	if err != nil {
		t.Fatalf("ListConversations(carol) error = %v", err)
	}
	if len(carolConversations.GetItems()) != 0 {
		t.Fatalf("ListConversations(carol) = %+v", carolConversations.GetItems())
	}
	if _, err := messageClient.GetMessages(carolCtx, &messengerv1.GetMessagesRequest{
		ConversationId: group.GetConversationId(),
		Limit:          20,
	}); status.Code(err) != codes.PermissionDenied {
		t.Fatalf("GetMessages(carol after remove) code = %v, want %v", status.Code(err), codes.PermissionDenied)
	}

	if _, err := userClient.AddGroupMembers(bobCtx, &messengerv1.AddGroupMembersRequest{
		ConversationId:  group.GetConversationId(),
		MemberUsernames: []string{"erin"},
	}); err != nil {
		t.Fatalf("AddGroupMembers(bob, erin) error = %v", err)
	}
	if _, err := waitForEvent(t, erinStream, func(event *messengerv1.ServerEvent) bool {
		conversation := event.GetConversation()
		return conversation != nil && conversation.GetConversationId() == group.GetConversationId()
	}); err != nil {
		t.Fatalf("waiting for erin added event: %v", err)
	}

	if _, err := userClient.RemoveGroupMember(bobCtx, &messengerv1.RemoveGroupMemberRequest{
		ConversationId: group.GetConversationId(),
		Username:       "dave",
	}); status.Code(err) != codes.PermissionDenied {
		t.Fatalf("RemoveGroupMember(bob,dave) code = %v, want %v", status.Code(err), codes.PermissionDenied)
	}

	if _, err := userClient.RemoveGroupMember(bobCtx, &messengerv1.RemoveGroupMemberRequest{
		ConversationId: group.GetConversationId(),
		Username:       "erin",
	}); err != nil {
		t.Fatalf("RemoveGroupMember(bob,erin) error = %v", err)
	}
	if _, err := waitForEvent(t, erinStream, func(event *messengerv1.ServerEvent) bool {
		removed := event.GetConversationRemoved()
		return removed != nil && removed.GetConversationId() == group.GetConversationId()
	}); err != nil {
		t.Fatalf("waiting for erin removed event: %v", err)
	}

	if _, err := userClient.TransferGroupAdmin(aliceCtx, &messengerv1.TransferGroupAdminRequest{
		ConversationId: group.GetConversationId(),
		Username:       "dave",
	}); err != nil {
		t.Fatalf("TransferGroupAdmin() error = %v", err)
	}

	transferredEvent, err := waitForEvent(t, daveStream, func(event *messengerv1.ServerEvent) bool {
		conversation := event.GetConversation()
		if conversation == nil || conversation.GetConversationId() != group.GetConversationId() {
			return false
		}
		for _, member := range conversation.GetMembers() {
			if member.GetUsername() == "dave" && member.GetRole() == messengerv1.ConversationRole_CONVERSATION_ROLE_ADMIN {
				return true
			}
		}
		return false
	})
	if err != nil {
		t.Fatalf("waiting for admin transfer event: %v", err)
	}
	if !hasRole(transferredEvent.GetConversation(), "dave", messengerv1.ConversationRole_CONVERSATION_ROLE_ADMIN) {
		t.Fatalf("TransferGroupAdmin() conversation = %+v", transferredEvent.GetConversation().GetMembers())
	}

	if _, err := userClient.LeaveGroupConversation(daveCtx, &messengerv1.LeaveGroupConversationRequest{
		ConversationId: group.GetConversationId(),
	}); err != nil {
		t.Fatalf("LeaveGroupConversation(dave) error = %v", err)
	}
	leaveEvent, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		conversation := event.GetConversation()
		if conversation == nil || conversation.GetConversationId() != group.GetConversationId() {
			return false
		}
		return hasRole(conversation, "alice", messengerv1.ConversationRole_CONVERSATION_ROLE_ADMIN)
	})
	if err != nil {
		t.Fatalf("waiting for admin leave update: %v", err)
	}
	if !hasRole(leaveEvent.GetConversation(), "alice", messengerv1.ConversationRole_CONVERSATION_ROLE_ADMIN) {
		t.Fatalf("LeaveGroupConversation() conversation = %+v", leaveEvent.GetConversation().GetMembers())
	}
}

func TestServerEncryptedMessageAndKeyFlow(t *testing.T) {
	t.Parallel()

	authClient, userClient, messageClient, cleanup := newTestClients(t)
	defer cleanup()

	ctx := context.Background()
	registerAndLogin := func(username string) context.Context {
		t.Helper()

		if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
			Username: username,
			Password: "secret",
		}); err != nil {
			t.Fatalf("Register(%s) error = %v", username, err)
		}
		loginResp, err := authClient.Login(ctx, &messengerv1.LoginRequest{
			Username: username,
			Password: "secret",
			DeviceId: username + "-device",
		})
		if err != nil {
			t.Fatalf("Login(%s) error = %v", username, err)
		}
		return authContext(ctx, loginResp.GetToken())
	}

	aliceCtx := registerAndLogin("alice")
	bobCtx := registerAndLogin("bob")
	_ = registerAndLogin("carol")

	aliceKey, err := userClient.PublishIdentityKey(aliceCtx, &messengerv1.PublishIdentityKeyRequest{
		KeyId:     "alice-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{1, 2, 3},
	})
	if err != nil {
		t.Fatalf("PublishIdentityKey(alice) error = %v", err)
	}
	if aliceKey.GetUsername() != "alice" || aliceKey.GetKeyId() != "alice-key-1" {
		t.Fatalf("PublishIdentityKey(alice) = %+v", aliceKey)
	}

	if _, err := userClient.PublishIdentityKey(bobCtx, &messengerv1.PublishIdentityKeyRequest{
		KeyId:     "bob-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{4, 5, 6},
	}); err != nil {
		t.Fatalf("PublishIdentityKey(bob) error = %v", err)
	}

	if _, err := userClient.PublishPrekeyBundle(bobCtx, &messengerv1.PublishPrekeyBundleRequest{
		SignedPrekeyId:        "bob-signed-1",
		SignedPrekeyAlgorithm: "P256-HKDF-AESGCM",
		SignedPrekeyPublicKey: []byte{6, 5, 4},
		OneTimePrekeys: []*messengerv1.OneTimePrekeyUpload{
			{KeyId: "bob-otp-1", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{9, 9, 1}},
			{KeyId: "bob-otp-2", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{9, 9, 2}},
		},
	}); err != nil {
		t.Fatalf("PublishPrekeyBundle(bob) error = %v", err)
	}

	acquiredFirst, err := userClient.AcquirePrekeyBundle(aliceCtx, &messengerv1.AcquirePrekeyBundleRequest{
		Username: "bob",
	})
	if err != nil {
		t.Fatalf("AcquirePrekeyBundle(first) error = %v", err)
	}
	if acquiredFirst.GetSignedPrekey().GetKeyId() != "bob-signed-1" || acquiredFirst.GetOneTimePrekey().GetKeyId() != "bob-otp-1" {
		t.Fatalf("AcquirePrekeyBundle(first) = %+v", acquiredFirst)
	}

	acquiredSecond, err := userClient.AcquirePrekeyBundle(aliceCtx, &messengerv1.AcquirePrekeyBundleRequest{
		Username: "bob",
	})
	if err != nil {
		t.Fatalf("AcquirePrekeyBundle(second) error = %v", err)
	}
	if acquiredSecond.GetOneTimePrekey().GetKeyId() != "bob-otp-2" {
		t.Fatalf("AcquirePrekeyBundle(second) = %+v", acquiredSecond)
	}

	keys, err := userClient.GetIdentityKeys(aliceCtx, &messengerv1.GetIdentityKeysRequest{
		Usernames: []string{"alice", "bob"},
	})
	if err != nil {
		t.Fatalf("GetIdentityKeys() error = %v", err)
	}
	if len(keys.GetItems()) != 2 {
		t.Fatalf("GetIdentityKeys() = %+v", keys.GetItems())
	}

	bobStream, err := messageClient.StreamEvents(bobCtx, &messengerv1.StreamEventsRequest{})
	if err != nil {
		t.Fatalf("StreamEvents(bob) error = %v", err)
	}
	if _, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		return event.GetPresence() != nil
	}); err != nil {
		t.Fatalf("waiting for bob presence event: %v", err)
	}

	sent, err := messageClient.SendMessage(aliceCtx, &messengerv1.SendMessageRequest{
		To:                       "bob",
		Ciphertext:               []byte{10, 11, 12},
		Nonce:                    []byte{1, 1, 1, 1},
		SenderKeyId:              "alice-key-1",
		ConversationKeyVersion: 1,
		Encrypted:                true,
		RecipientSignedPrekeyId:  acquiredFirst.GetSignedPrekey().GetKeyId(),
		RecipientSignedPrekeyPublic: acquiredFirst.GetSignedPrekey().GetPublicKey(),
		RecipientOneTimePrekeyId: acquiredFirst.GetOneTimePrekey().GetKeyId(),
		RecipientOneTimePrekeyPublic: acquiredFirst.GetOneTimePrekey().GetPublicKey(),
	})
	if err != nil {
		t.Fatalf("SendMessage(encrypted) error = %v", err)
	}
	if !sent.GetEncrypted() || len(sent.GetCiphertext()) != 3 || sent.GetText() != "" || sent.GetRecipientSignedPrekeyId() != "bob-signed-1" {
		t.Fatalf("SendMessage(encrypted) = %+v", sent)
	}

	event, err := waitForEvent(t, bobStream, func(event *messengerv1.ServerEvent) bool {
		msg := event.GetMessage()
		return msg != nil && msg.GetMessageId() == sent.GetMessageId()
	})
	if err != nil {
		t.Fatalf("waiting for encrypted message event: %v", err)
	}
	if !event.GetMessage().GetEncrypted() || event.GetMessage().GetSenderKeyId() != "alice-key-1" {
		t.Fatalf("encrypted event = %+v", event.GetMessage())
	}

	history, err := messageClient.GetMessages(bobCtx, &messengerv1.GetMessagesRequest{
		WithUsername: "alice",
		Limit:        10,
	})
	if err != nil {
		t.Fatalf("GetMessages(encrypted) error = %v", err)
	}
	if len(history.GetItems()) != 1 || history.GetItems()[0].GetText() != "" || !history.GetItems()[0].GetEncrypted() {
		t.Fatalf("GetMessages(encrypted) = %+v", history.GetItems())
	}

	group, err := userClient.CreateGroupConversation(aliceCtx, &messengerv1.CreateGroupConversationRequest{
		Title:           "Encrypted group",
		MemberUsernames: []string{"bob"},
		InitialKey: &messengerv1.GroupKeyUpdate{
			Version:   1,
			Algorithm: "AES-GCM",
			Envelopes: []*messengerv1.ConversationKeyEnvelope{
				{
					Username:       "alice",
					DeviceId:       "alice-device",
					EncryptedKey:   []byte{1, 2, 3},
					Nonce:          []byte{4, 5, 6},
					SenderKeyId:    "alice-key-1",
					RecipientKeyId: "alice-key-1",
				},
				{
					Username:       "bob",
					DeviceId:       "bob-device",
					EncryptedKey:   []byte{7, 8, 9},
					Nonce:          []byte{3, 2, 1},
					SenderKeyId:    "alice-key-1",
					RecipientKeyId: "bob-key-1",
				},
			},
		},
	})
	if err != nil {
		t.Fatalf("CreateGroupConversation() error = %v", err)
	}

	conversationKey, err := userClient.GetConversationKey(aliceCtx, &messengerv1.GetConversationKeyRequest{
		ConversationId: group.GetConversationId(),
		Version:        1,
	})
	if err != nil {
		t.Fatalf("GetConversationKey(create initial) error = %v", err)
	}
	if conversationKey.GetVersion() != 1 || len(conversationKey.GetEnvelopes()) != 2 {
		t.Fatalf("CreateGroupConversation(initial key) = %+v", conversationKey)
	}

	fetchedKey, err := userClient.GetConversationKey(bobCtx, &messengerv1.GetConversationKeyRequest{
		ConversationId: group.GetConversationId(),
		Version:        1,
	})
	if err != nil {
		t.Fatalf("GetConversationKey() error = %v", err)
	}
	if fetchedKey.GetConversationId() != group.GetConversationId() || fetchedKey.GetEnvelopes()[1].GetRecipientKeyId() != "bob-key-1" {
		t.Fatalf("GetConversationKey() = %+v", fetchedKey)
	}

	updatedGroup, err := userClient.AddGroupMembers(aliceCtx, &messengerv1.AddGroupMembersRequest{
		ConversationId:  group.GetConversationId(),
		MemberUsernames: []string{"carol"},
		NextKey: &messengerv1.GroupKeyUpdate{
			Version:   2,
			Algorithm: "AES-GCM",
			Envelopes: []*messengerv1.ConversationKeyEnvelope{
				{Username: "alice", DeviceId: "alice-device", EncryptedKey: []byte{1}, Nonce: []byte{1}, SenderKeyId: "alice-key-1", RecipientKeyId: "alice-key-1"},
				{Username: "bob", DeviceId: "bob-device", EncryptedKey: []byte{2}, Nonce: []byte{2}, SenderKeyId: "alice-key-1", RecipientKeyId: "bob-key-1"},
				{Username: "carol", DeviceId: "carol-device", EncryptedKey: []byte{3}, Nonce: []byte{3}, SenderKeyId: "alice-key-1", RecipientKeyId: "carol-key-1"},
			},
		},
	})
	if err != nil {
		t.Fatalf("AddGroupMembers(next key) error = %v", err)
	}
	if len(updatedGroup.GetMemberUsernames()) != 3 {
		t.Fatalf("AddGroupMembers(next key) = %+v", updatedGroup)
	}

	latestKey, err := userClient.GetConversationKey(aliceCtx, &messengerv1.GetConversationKeyRequest{
		ConversationId: group.GetConversationId(),
		Version:        0,
	})
	if err != nil {
		t.Fatalf("GetConversationKey(latest after add) error = %v", err)
	}
	if latestKey.GetVersion() != 2 || len(latestKey.GetEnvelopes()) != 3 {
		t.Fatalf("GetConversationKey(latest after add) = %+v", latestKey)
	}
}

func TestServerDirectMessagesProjectPerDevice(t *testing.T) {
	t.Parallel()

	authClient, userClient, messageClient, cleanup := newTestClients(t)
	defer cleanup()

	ctx := context.Background()

	registerUser := func(username string) {
		t.Helper()
		if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
			Username: username,
			Password: "secret",
		}); err != nil {
			t.Fatalf("Register(%s) error = %v", username, err)
		}
	}

	loginDevice := func(username, deviceID string) context.Context {
		t.Helper()
		loginResp, err := authClient.Login(ctx, &messengerv1.LoginRequest{
			Username: username,
			Password: "secret",
			DeviceId: deviceID,
		})
		if err != nil {
			t.Fatalf("Login(%s,%s) error = %v", username, deviceID, err)
		}
		return authContext(ctx, loginResp.GetToken())
	}

	registerUser("alice")
	registerUser("bob")

	aliceCtx := loginDevice("alice", "alice-web")
	bobWebCtx := loginDevice("bob", "bob-web")
	bobPhoneCtx := loginDevice("bob", "bob-phone")

	if _, err := userClient.PublishIdentityKey(aliceCtx, &messengerv1.PublishIdentityKeyRequest{
		KeyId:     "alice-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{1, 2, 3},
	}); err != nil {
		t.Fatalf("PublishIdentityKey(alice) error = %v", err)
	}
	if _, err := userClient.PublishIdentityKey(bobWebCtx, &messengerv1.PublishIdentityKeyRequest{
		KeyId:     "bob-web-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{4, 5, 6},
	}); err != nil {
		t.Fatalf("PublishIdentityKey(bob-web) error = %v", err)
	}
	if _, err := userClient.PublishIdentityKey(bobPhoneCtx, &messengerv1.PublishIdentityKeyRequest{
		KeyId:     "bob-phone-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{7, 8, 9},
	}); err != nil {
		t.Fatalf("PublishIdentityKey(bob-phone) error = %v", err)
	}

	if _, err := userClient.PublishPrekeyBundle(bobWebCtx, &messengerv1.PublishPrekeyBundleRequest{
		SignedPrekeyId:        "bob-web-spk-1",
		SignedPrekeyAlgorithm: "P256-HKDF-AESGCM",
		SignedPrekeyPublicKey: []byte{11, 12, 13},
		OneTimePrekeys: []*messengerv1.OneTimePrekeyUpload{
			{KeyId: "bob-web-otp-1", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{21, 22, 23}},
		},
	}); err != nil {
		t.Fatalf("PublishPrekeyBundle(bob-web) error = %v", err)
	}
	if _, err := userClient.PublishPrekeyBundle(bobPhoneCtx, &messengerv1.PublishPrekeyBundleRequest{
		SignedPrekeyId:        "bob-phone-spk-1",
		SignedPrekeyAlgorithm: "P256-HKDF-AESGCM",
		SignedPrekeyPublicKey: []byte{31, 32, 33},
		OneTimePrekeys: []*messengerv1.OneTimePrekeyUpload{
			{KeyId: "bob-phone-otp-1", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{41, 42, 43}},
		},
	}); err != nil {
		t.Fatalf("PublishPrekeyBundle(bob-phone) error = %v", err)
	}

	bundlesResp, err := userClient.AcquirePrekeyBundles(aliceCtx, &messengerv1.AcquirePrekeyBundlesRequest{
		Username: "bob",
	})
	if err != nil {
		t.Fatalf("AcquirePrekeyBundles() error = %v", err)
	}
	if len(bundlesResp.GetItems()) != 2 {
		t.Fatalf("AcquirePrekeyBundles() = %+v", bundlesResp.GetItems())
	}

	if _, err := messageClient.SendMessage(aliceCtx, &messengerv1.SendMessageRequest{
		To:          "bob",
		Encrypted:   true,
		SenderKeyId: "alice-key-1",
		DirectEnvelopes: []*messengerv1.DirectMessageEnvelope{
			{
				TargetUsername:               "bob",
				TargetDeviceId:               "bob-web",
				Ciphertext:                   []byte{1, 1, 1},
				Nonce:                        []byte{9, 9, 0},
				RecipientSignedPrekeyId:      "bob-web-spk-1",
				RecipientSignedPrekeyPublic:  []byte{11, 12, 13},
				RecipientOneTimePrekeyId:     "bob-web-otp-1",
				RecipientOneTimePrekeyPublic: []byte{21, 22, 23},
			},
			{
				TargetUsername:               "bob",
				TargetDeviceId:               "bob-phone",
				Ciphertext:                   []byte{2, 2, 2},
				Nonce:                        []byte{9, 9, 1},
				RecipientSignedPrekeyId:      "bob-phone-spk-1",
				RecipientSignedPrekeyPublic:  []byte{31, 32, 33},
				RecipientOneTimePrekeyId:     "bob-phone-otp-1",
				RecipientOneTimePrekeyPublic: []byte{41, 42, 43},
			},
		},
	}); err != nil {
		t.Fatalf("SendMessage(direct multi-device) error = %v", err)
	}

	webHistory, err := messageClient.GetMessages(bobWebCtx, &messengerv1.GetMessagesRequest{
		WithUsername: "alice",
		Limit:        10,
	})
	if err != nil {
		t.Fatalf("GetMessages(bob-web) error = %v", err)
	}
	if len(webHistory.GetItems()) != 1 {
		t.Fatalf("GetMessages(bob-web) = %+v", webHistory.GetItems())
	}
	if got := webHistory.GetItems()[0]; got.GetRecipientSignedPrekeyId() != "bob-web-spk-1" || string(got.GetCiphertext()) != string([]byte{1, 1, 1}) {
		t.Fatalf("GetMessages(bob-web) projected = %+v", got)
	}

	phoneHistory, err := messageClient.GetMessages(bobPhoneCtx, &messengerv1.GetMessagesRequest{
		WithUsername: "alice",
		Limit:        10,
	})
	if err != nil {
		t.Fatalf("GetMessages(bob-phone) error = %v", err)
	}
	if len(phoneHistory.GetItems()) != 1 {
		t.Fatalf("GetMessages(bob-phone) = %+v", phoneHistory.GetItems())
	}
	if got := phoneHistory.GetItems()[0]; got.GetRecipientSignedPrekeyId() != "bob-phone-spk-1" || string(got.GetCiphertext()) != string([]byte{2, 2, 2}) {
		t.Fatalf("GetMessages(bob-phone) projected = %+v", got)
	}

	aliceHistory, err := messageClient.GetMessages(aliceCtx, &messengerv1.GetMessagesRequest{
		WithUsername: "bob",
		Limit:        10,
	})
	if err != nil {
		t.Fatalf("GetMessages(alice-web) error = %v", err)
	}
	if len(aliceHistory.GetItems()) != 1 {
		t.Fatalf("GetMessages(alice-web) = %+v", aliceHistory.GetItems())
	}
	if got := aliceHistory.GetItems()[0]; got.GetRecipientSignedPrekeyId() != "bob-web-spk-1" || string(got.GetCiphertext()) != string([]byte{1, 1, 1}) {
		t.Fatalf("GetMessages(alice-web) sender fallback = %+v", got)
	}
}

func TestServerGetMessagesReturnsPlaceholderForDirectHistoryUnavailableOnNewDevice(t *testing.T) {
	t.Parallel()

	authClient, userClient, messageClient, cleanup := newTestClients(t)
	defer cleanup()

	ctx := context.Background()

	registerUser := func(username string) {
		t.Helper()
		if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
			Username: username,
			Password: "secret",
		}); err != nil {
			t.Fatalf("Register(%s) error = %v", username, err)
		}
	}

	loginDevice := func(username, deviceID string) context.Context {
		t.Helper()
		loginResp, err := authClient.Login(ctx, &messengerv1.LoginRequest{
			Username: username,
			Password: "secret",
			DeviceId: deviceID,
		})
		if err != nil {
			t.Fatalf("Login(%s,%s) error = %v", username, deviceID, err)
		}
		return authContext(ctx, loginResp.GetToken())
	}

	registerUser("alice")
	registerUser("bob")

	aliceWebCtx := loginDevice("alice", "alice-web")
	aliceAndroidCtx := loginDevice("alice", "alice-android")
	bobWebCtx := loginDevice("bob", "bob-web")

	if _, err := userClient.PublishIdentityKey(aliceWebCtx, &messengerv1.PublishIdentityKeyRequest{
		KeyId:     "alice-web-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{1, 2, 3},
	}); err != nil {
		t.Fatalf("PublishIdentityKey(alice-web) error = %v", err)
	}
	if _, err := userClient.PublishIdentityKey(bobWebCtx, &messengerv1.PublishIdentityKeyRequest{
		KeyId:     "bob-web-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{4, 5, 6},
	}); err != nil {
		t.Fatalf("PublishIdentityKey(bob-web) error = %v", err)
	}
	if _, err := userClient.PublishPrekeyBundle(aliceWebCtx, &messengerv1.PublishPrekeyBundleRequest{
		SignedPrekeyId:        "alice-web-spk-1",
		SignedPrekeyAlgorithm: "P256-HKDF-AESGCM",
		SignedPrekeyPublicKey: []byte{11, 12, 13},
		OneTimePrekeys: []*messengerv1.OneTimePrekeyUpload{
			{KeyId: "alice-web-otp-1", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{21, 22, 23}},
		},
	}); err != nil {
		t.Fatalf("PublishPrekeyBundle(alice-web) error = %v", err)
	}
	if _, err := userClient.PublishPrekeyBundle(bobWebCtx, &messengerv1.PublishPrekeyBundleRequest{
		SignedPrekeyId:        "bob-web-spk-1",
		SignedPrekeyAlgorithm: "P256-HKDF-AESGCM",
		SignedPrekeyPublicKey: []byte{31, 32, 33},
		OneTimePrekeys: []*messengerv1.OneTimePrekeyUpload{
			{KeyId: "bob-web-otp-1", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{41, 42, 43}},
		},
	}); err != nil {
		t.Fatalf("PublishPrekeyBundle(bob-web) error = %v", err)
	}

	if _, err := messageClient.SendMessage(bobWebCtx, &messengerv1.SendMessageRequest{
		To:          "alice",
		Encrypted:   true,
		SenderKeyId: "bob-web-key-1",
		DirectEnvelopes: []*messengerv1.DirectMessageEnvelope{
			{
				TargetUsername:               "alice",
				TargetDeviceId:               "alice-web",
				Ciphertext:                   []byte{1, 1, 1},
				Nonce:                        []byte{9, 9, 0},
				RecipientSignedPrekeyId:      "alice-web-spk-1",
				RecipientSignedPrekeyPublic:  []byte{11, 12, 13},
				RecipientOneTimePrekeyId:     "alice-web-otp-1",
				RecipientOneTimePrekeyPublic: []byte{21, 22, 23},
			},
		},
	}); err != nil {
		t.Fatalf("SendMessage(old direct history) error = %v", err)
	}

	if _, err := userClient.PublishIdentityKey(aliceAndroidCtx, &messengerv1.PublishIdentityKeyRequest{
		KeyId:     "alice-android-key-1",
		Algorithm: "P256-HKDF-AESGCM",
		PublicKey: []byte{7, 8, 9},
	}); err != nil {
		t.Fatalf("PublishIdentityKey(alice-android) error = %v", err)
	}
	if _, err := userClient.PublishPrekeyBundle(aliceAndroidCtx, &messengerv1.PublishPrekeyBundleRequest{
		SignedPrekeyId:        "alice-android-spk-1",
		SignedPrekeyAlgorithm: "P256-HKDF-AESGCM",
		SignedPrekeyPublicKey: []byte{51, 52, 53},
		OneTimePrekeys: []*messengerv1.OneTimePrekeyUpload{
			{KeyId: "alice-android-otp-1", Algorithm: "P256-HKDF-AESGCM", PublicKey: []byte{61, 62, 63}},
		},
	}); err != nil {
		t.Fatalf("PublishPrekeyBundle(alice-android) error = %v", err)
	}

	history, err := messageClient.GetMessages(aliceAndroidCtx, &messengerv1.GetMessagesRequest{
		WithUsername: "bob",
		Limit:        10,
	})
	if err != nil {
		t.Fatalf("GetMessages(alice-android) error = %v", err)
	}
	if len(history.GetItems()) != 1 {
		t.Fatalf("GetMessages(alice-android) len = %d, want 1", len(history.GetItems()))
	}
	if got := history.GetItems()[0]; got.GetText() != "[Сообщение недоступно на этом устройстве]" || got.GetEncrypted() {
		t.Fatalf("GetMessages(alice-android) placeholder = %+v", got)
	}
}

func TestServerDirectAttachmentOnlyMessageProjectsDescriptorPerDevice(t *testing.T) {
	t.Parallel()

	authClient, _, messageClient, cleanup := newTestClients(t)
	defer cleanup()

	ctx := context.Background()

	registerUser := func(username string) {
		t.Helper()
		if _, err := authClient.Register(ctx, &messengerv1.RegisterRequest{
			Username: username,
			Password: "secret",
		}); err != nil {
			t.Fatalf("Register(%s) error = %v", username, err)
		}
	}

	loginDevice := func(username, deviceID string) context.Context {
		t.Helper()
		loginResp, err := authClient.Login(ctx, &messengerv1.LoginRequest{
			Username: username,
			Password: "secret",
			DeviceId: deviceID,
		})
		if err != nil {
			t.Fatalf("Login(%s,%s) error = %v", username, deviceID, err)
		}
		return authContext(ctx, loginResp.GetToken())
	}

	registerUser("alice")
	registerUser("bob")

	aliceCtx := loginDevice("alice", "alice-web")
	bobCtx := loginDevice("bob", "bob-web")

	if _, err := messageClient.PrepareMediaUpload(aliceCtx, &messengerv1.PrepareMediaUploadRequest{
		Filename:  "image.png",
		MimeType:  "image/png",
		SizeBytes: 3,
		Kind:      messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE,
	}); err != nil {
		t.Fatalf("PrepareMediaUpload() error = %v", err)
	}

	prepared, err := messageClient.PrepareMediaUpload(aliceCtx, &messengerv1.PrepareMediaUploadRequest{
		Filename:  "image.png",
		MimeType:  "image/png",
		SizeBytes: 3,
		Kind:      messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE,
	})
	if err != nil {
		t.Fatalf("PrepareMediaUpload(image) error = %v", err)
	}
	if _, err := messageClient.UploadMedia(aliceCtx, &messengerv1.UploadMediaRequest{
		MediaId:   prepared.GetMediaId(),
		Ciphertext: []byte{1, 2, 3},
		Nonce:     []byte{4, 5, 6},
		Sha256:    []byte{7, 8, 9},
		SizeBytes: 3,
		MimeType:  "image/png",
		Filename:  "image.png",
		Kind:      messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE,
	}); err != nil {
		t.Fatalf("UploadMedia(image) error = %v", err)
	}

	if _, err := messageClient.SendMessage(aliceCtx, &messengerv1.SendMessageRequest{
		To:          "bob",
		Encrypted:   true,
		SenderKeyId: "alice-key-1",
		Attachments: []*messengerv1.Attachment{
			{
				AttachmentId: "att-1",
				Kind:         messengerv1.AttachmentKind_ATTACHMENT_KIND_IMAGE,
				Filename:     "image.png",
				MimeType:     "image/png",
				SizeBytes:    3,
				MediaId:      prepared.GetMediaId(),
				Sha256:       []byte{7, 8, 9},
				CiphertextSize: 3,
				DirectEnvelopes: []*messengerv1.AttachmentDirectEnvelope{
					{
						TargetUsername:      "bob",
						TargetDeviceId:      "bob-web",
						EncryptedDescriptor: []byte{9, 9, 9},
						DescriptorNonce:     []byte{8, 8, 8},
						RecipientSignedPrekeyId:     "bob-web-spk-1",
						RecipientSignedPrekeyPublic: []byte{11, 12, 13},
						RecipientOneTimePrekeyId:    "bob-web-otp-1",
						RecipientOneTimePrekeyPublic: []byte{21, 22, 23},
					},
				},
			},
		},
	}); err != nil {
		t.Fatalf("SendMessage(attachment-only direct) error = %v", err)
	}

	history, err := messageClient.GetMessages(bobCtx, &messengerv1.GetMessagesRequest{
		WithUsername: "alice",
		Limit:        10,
	})
	if err != nil {
		t.Fatalf("GetMessages(bob attachment-only) error = %v", err)
	}
	if len(history.GetItems()) != 1 || len(history.GetItems()[0].GetAttachments()) != 1 {
		t.Fatalf("GetMessages(bob attachment-only) = %+v", history.GetItems())
	}
	got := history.GetItems()[0].GetAttachments()[0]
	if string(got.GetEncryptedDescriptor()) != string([]byte{9, 9, 9}) || string(got.GetDescriptorNonce()) != string([]byte{8, 8, 8}) {
		t.Fatalf("GetMessages(bob attachment-only) projected attachment = %+v", got)
	}
	if history.GetItems()[0].GetRecipientSignedPrekeyId() != "bob-web-spk-1" || string(history.GetItems()[0].GetRecipientSignedPrekeyPublic()) != string([]byte{11, 12, 13}) {
		t.Fatalf("GetMessages(bob attachment-only) projected prekey material = %+v", history.GetItems()[0])
	}
}

func newTestClients(t *testing.T) (messengerv1.AuthServiceClient, messengerv1.UserServiceClient, messengerv1.MessageServiceClient, func()) {
	t.Helper()

	userStore := store.NewMemoryUserStore()
	msgStore := store.NewMemoryMessageStore()
	convStore := store.NewMemoryConversationStore()
	keyStore := store.NewMemoryKeyStore()
	archiveStore := store.NewMemoryArchiveStore()
	authSvc := auth.NewService(userStore)
	apiServer := NewServer(authSvc, userStore, msgStore, convStore, keyStore, archiveStore)

	listener := bufconn.Listen(bufSize)
	server := grpc.NewServer()
	messengerv1.RegisterAuthServiceServer(server, apiServer)
	messengerv1.RegisterUserServiceServer(server, apiServer)
	messengerv1.RegisterMessageServiceServer(server, apiServer)

	go func() {
		if err := server.Serve(listener); err != nil && !errors.Is(err, grpc.ErrServerStopped) {
			panic(err)
		}
	}()

	conn, err := grpc.NewClient("passthrough:///bufnet",
		grpc.WithContextDialer(func(context.Context, string) (net.Conn, error) {
			return listener.Dial()
		}),
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		t.Fatalf("grpc.NewClient() error = %v", err)
	}

	cleanup := func() {
		_ = conn.Close()
		server.Stop()
		_ = listener.Close()
	}

	return messengerv1.NewAuthServiceClient(conn), messengerv1.NewUserServiceClient(conn), messengerv1.NewMessageServiceClient(conn), cleanup
}

func authContext(ctx context.Context, token string) context.Context {
	return metadata.AppendToOutgoingContext(ctx, "authorization", "Bearer "+token)
}

func waitForEvent(t *testing.T, stream messengerv1.MessageService_StreamEventsClient, match func(*messengerv1.ServerEvent) bool) (*messengerv1.ServerEvent, error) {
	t.Helper()

	deadline := time.Now().Add(5 * time.Second)
	for time.Now().Before(deadline) {
		ctx, cancel := context.WithTimeout(context.Background(), time.Until(deadline))
		eventCh := make(chan *messengerv1.ServerEvent, 1)
		errCh := make(chan error, 1)

		go func() {
			event, err := stream.Recv()
			if err != nil {
				errCh <- err
				return
			}
			eventCh <- event
		}()

		select {
		case <-ctx.Done():
			cancel()
			return nil, ctx.Err()
		case err := <-errCh:
			cancel()
			if errors.Is(err, io.EOF) {
				return nil, err
			}
			return nil, err
		case event := <-eventCh:
			cancel()
			if match(event) {
				return event, nil
			}
		}
	}
	return nil, context.DeadlineExceeded
}

func containsAll(values []string, want ...string) bool {
	set := make(map[string]struct{}, len(values))
	for _, value := range values {
		set[value] = struct{}{}
	}
	for _, item := range want {
		if _, ok := set[item]; !ok {
			return false
		}
	}
	return true
}

func hasRole(conversation *messengerv1.Conversation, username string, role messengerv1.ConversationRole) bool {
	for _, member := range conversation.GetMembers() {
		if member.GetUsername() == username && member.GetRole() == role {
			return true
		}
	}
	return false
}
