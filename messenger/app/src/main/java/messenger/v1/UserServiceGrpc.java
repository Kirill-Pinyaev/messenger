package messenger.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: messenger/v1/messenger.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class UserServiceGrpc {

  private UserServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "messenger.v1.UserService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.GetProfileRequest,
      messenger.v1.Messenger.Profile> getGetProfileMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetProfile",
      requestType = messenger.v1.Messenger.GetProfileRequest.class,
      responseType = messenger.v1.Messenger.Profile.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.GetProfileRequest,
      messenger.v1.Messenger.Profile> getGetProfileMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.GetProfileRequest, messenger.v1.Messenger.Profile> getGetProfileMethod;
    if ((getGetProfileMethod = UserServiceGrpc.getGetProfileMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetProfileMethod = UserServiceGrpc.getGetProfileMethod) == null) {
          UserServiceGrpc.getGetProfileMethod = getGetProfileMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.GetProfileRequest, messenger.v1.Messenger.Profile>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetProfile"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetProfileRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.Profile.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetProfileMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.UpdateProfileRequest,
      messenger.v1.Messenger.Profile> getUpdateProfileMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateProfile",
      requestType = messenger.v1.Messenger.UpdateProfileRequest.class,
      responseType = messenger.v1.Messenger.Profile.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.UpdateProfileRequest,
      messenger.v1.Messenger.Profile> getUpdateProfileMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.UpdateProfileRequest, messenger.v1.Messenger.Profile> getUpdateProfileMethod;
    if ((getUpdateProfileMethod = UserServiceGrpc.getUpdateProfileMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getUpdateProfileMethod = UserServiceGrpc.getUpdateProfileMethod) == null) {
          UserServiceGrpc.getUpdateProfileMethod = getUpdateProfileMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.UpdateProfileRequest, messenger.v1.Messenger.Profile>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateProfile"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.UpdateProfileRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.Profile.getDefaultInstance()))
              .build();
        }
      }
    }
    return getUpdateProfileMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.SearchUsersRequest,
      messenger.v1.Messenger.SearchUsersResponse> getSearchUsersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchUsers",
      requestType = messenger.v1.Messenger.SearchUsersRequest.class,
      responseType = messenger.v1.Messenger.SearchUsersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.SearchUsersRequest,
      messenger.v1.Messenger.SearchUsersResponse> getSearchUsersMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.SearchUsersRequest, messenger.v1.Messenger.SearchUsersResponse> getSearchUsersMethod;
    if ((getSearchUsersMethod = UserServiceGrpc.getSearchUsersMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getSearchUsersMethod = UserServiceGrpc.getSearchUsersMethod) == null) {
          UserServiceGrpc.getSearchUsersMethod = getSearchUsersMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.SearchUsersRequest, messenger.v1.Messenger.SearchUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchUsers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.SearchUsersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.SearchUsersResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getSearchUsersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      messenger.v1.Messenger.ListConversationsResponse> getListConversationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListConversations",
      requestType = com.google.protobuf.Empty.class,
      responseType = messenger.v1.Messenger.ListConversationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      messenger.v1.Messenger.ListConversationsResponse> getListConversationsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, messenger.v1.Messenger.ListConversationsResponse> getListConversationsMethod;
    if ((getListConversationsMethod = UserServiceGrpc.getListConversationsMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getListConversationsMethod = UserServiceGrpc.getListConversationsMethod) == null) {
          UserServiceGrpc.getListConversationsMethod = getListConversationsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, messenger.v1.Messenger.ListConversationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListConversations"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.ListConversationsResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getListConversationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.PublishIdentityKeyRequest,
      messenger.v1.Messenger.IdentityKey> getPublishIdentityKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishIdentityKey",
      requestType = messenger.v1.Messenger.PublishIdentityKeyRequest.class,
      responseType = messenger.v1.Messenger.IdentityKey.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.PublishIdentityKeyRequest,
      messenger.v1.Messenger.IdentityKey> getPublishIdentityKeyMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.PublishIdentityKeyRequest, messenger.v1.Messenger.IdentityKey> getPublishIdentityKeyMethod;
    if ((getPublishIdentityKeyMethod = UserServiceGrpc.getPublishIdentityKeyMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getPublishIdentityKeyMethod = UserServiceGrpc.getPublishIdentityKeyMethod) == null) {
          UserServiceGrpc.getPublishIdentityKeyMethod = getPublishIdentityKeyMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.PublishIdentityKeyRequest, messenger.v1.Messenger.IdentityKey>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishIdentityKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.PublishIdentityKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.IdentityKey.getDefaultInstance()))
              .build();
        }
      }
    }
    return getPublishIdentityKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.PublishPrekeyBundleRequest,
      messenger.v1.Messenger.PrekeyBundle> getPublishPrekeyBundleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishPrekeyBundle",
      requestType = messenger.v1.Messenger.PublishPrekeyBundleRequest.class,
      responseType = messenger.v1.Messenger.PrekeyBundle.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.PublishPrekeyBundleRequest,
      messenger.v1.Messenger.PrekeyBundle> getPublishPrekeyBundleMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.PublishPrekeyBundleRequest, messenger.v1.Messenger.PrekeyBundle> getPublishPrekeyBundleMethod;
    if ((getPublishPrekeyBundleMethod = UserServiceGrpc.getPublishPrekeyBundleMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getPublishPrekeyBundleMethod = UserServiceGrpc.getPublishPrekeyBundleMethod) == null) {
          UserServiceGrpc.getPublishPrekeyBundleMethod = getPublishPrekeyBundleMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.PublishPrekeyBundleRequest, messenger.v1.Messenger.PrekeyBundle>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishPrekeyBundle"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.PublishPrekeyBundleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.PrekeyBundle.getDefaultInstance()))
              .build();
        }
      }
    }
    return getPublishPrekeyBundleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.GetIdentityKeyRequest,
      messenger.v1.Messenger.IdentityKey> getGetIdentityKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetIdentityKey",
      requestType = messenger.v1.Messenger.GetIdentityKeyRequest.class,
      responseType = messenger.v1.Messenger.IdentityKey.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.GetIdentityKeyRequest,
      messenger.v1.Messenger.IdentityKey> getGetIdentityKeyMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.GetIdentityKeyRequest, messenger.v1.Messenger.IdentityKey> getGetIdentityKeyMethod;
    if ((getGetIdentityKeyMethod = UserServiceGrpc.getGetIdentityKeyMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetIdentityKeyMethod = UserServiceGrpc.getGetIdentityKeyMethod) == null) {
          UserServiceGrpc.getGetIdentityKeyMethod = getGetIdentityKeyMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.GetIdentityKeyRequest, messenger.v1.Messenger.IdentityKey>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetIdentityKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetIdentityKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.IdentityKey.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetIdentityKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.GetIdentityKeysRequest,
      messenger.v1.Messenger.GetIdentityKeysResponse> getGetIdentityKeysMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetIdentityKeys",
      requestType = messenger.v1.Messenger.GetIdentityKeysRequest.class,
      responseType = messenger.v1.Messenger.GetIdentityKeysResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.GetIdentityKeysRequest,
      messenger.v1.Messenger.GetIdentityKeysResponse> getGetIdentityKeysMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.GetIdentityKeysRequest, messenger.v1.Messenger.GetIdentityKeysResponse> getGetIdentityKeysMethod;
    if ((getGetIdentityKeysMethod = UserServiceGrpc.getGetIdentityKeysMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetIdentityKeysMethod = UserServiceGrpc.getGetIdentityKeysMethod) == null) {
          UserServiceGrpc.getGetIdentityKeysMethod = getGetIdentityKeysMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.GetIdentityKeysRequest, messenger.v1.Messenger.GetIdentityKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetIdentityKeys"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetIdentityKeysRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetIdentityKeysResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetIdentityKeysMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.AcquirePrekeyBundleRequest,
      messenger.v1.Messenger.PrekeyBundle> getAcquirePrekeyBundleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AcquirePrekeyBundle",
      requestType = messenger.v1.Messenger.AcquirePrekeyBundleRequest.class,
      responseType = messenger.v1.Messenger.PrekeyBundle.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.AcquirePrekeyBundleRequest,
      messenger.v1.Messenger.PrekeyBundle> getAcquirePrekeyBundleMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.AcquirePrekeyBundleRequest, messenger.v1.Messenger.PrekeyBundle> getAcquirePrekeyBundleMethod;
    if ((getAcquirePrekeyBundleMethod = UserServiceGrpc.getAcquirePrekeyBundleMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getAcquirePrekeyBundleMethod = UserServiceGrpc.getAcquirePrekeyBundleMethod) == null) {
          UserServiceGrpc.getAcquirePrekeyBundleMethod = getAcquirePrekeyBundleMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.AcquirePrekeyBundleRequest, messenger.v1.Messenger.PrekeyBundle>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AcquirePrekeyBundle"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.AcquirePrekeyBundleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.PrekeyBundle.getDefaultInstance()))
              .build();
        }
      }
    }
    return getAcquirePrekeyBundleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.AcquirePrekeyBundlesRequest,
      messenger.v1.Messenger.AcquirePrekeyBundlesResponse> getAcquirePrekeyBundlesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AcquirePrekeyBundles",
      requestType = messenger.v1.Messenger.AcquirePrekeyBundlesRequest.class,
      responseType = messenger.v1.Messenger.AcquirePrekeyBundlesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.AcquirePrekeyBundlesRequest,
      messenger.v1.Messenger.AcquirePrekeyBundlesResponse> getAcquirePrekeyBundlesMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.AcquirePrekeyBundlesRequest, messenger.v1.Messenger.AcquirePrekeyBundlesResponse> getAcquirePrekeyBundlesMethod;
    if ((getAcquirePrekeyBundlesMethod = UserServiceGrpc.getAcquirePrekeyBundlesMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getAcquirePrekeyBundlesMethod = UserServiceGrpc.getAcquirePrekeyBundlesMethod) == null) {
          UserServiceGrpc.getAcquirePrekeyBundlesMethod = getAcquirePrekeyBundlesMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.AcquirePrekeyBundlesRequest, messenger.v1.Messenger.AcquirePrekeyBundlesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AcquirePrekeyBundles"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.AcquirePrekeyBundlesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.AcquirePrekeyBundlesResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getAcquirePrekeyBundlesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.CreateGroupConversationRequest,
      messenger.v1.Messenger.Conversation> getCreateGroupConversationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateGroupConversation",
      requestType = messenger.v1.Messenger.CreateGroupConversationRequest.class,
      responseType = messenger.v1.Messenger.Conversation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.CreateGroupConversationRequest,
      messenger.v1.Messenger.Conversation> getCreateGroupConversationMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.CreateGroupConversationRequest, messenger.v1.Messenger.Conversation> getCreateGroupConversationMethod;
    if ((getCreateGroupConversationMethod = UserServiceGrpc.getCreateGroupConversationMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getCreateGroupConversationMethod = UserServiceGrpc.getCreateGroupConversationMethod) == null) {
          UserServiceGrpc.getCreateGroupConversationMethod = getCreateGroupConversationMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.CreateGroupConversationRequest, messenger.v1.Messenger.Conversation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateGroupConversation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.CreateGroupConversationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.Conversation.getDefaultInstance()))
              .build();
        }
      }
    }
    return getCreateGroupConversationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.AddGroupMembersRequest,
      messenger.v1.Messenger.Conversation> getAddGroupMembersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddGroupMembers",
      requestType = messenger.v1.Messenger.AddGroupMembersRequest.class,
      responseType = messenger.v1.Messenger.Conversation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.AddGroupMembersRequest,
      messenger.v1.Messenger.Conversation> getAddGroupMembersMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.AddGroupMembersRequest, messenger.v1.Messenger.Conversation> getAddGroupMembersMethod;
    if ((getAddGroupMembersMethod = UserServiceGrpc.getAddGroupMembersMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getAddGroupMembersMethod = UserServiceGrpc.getAddGroupMembersMethod) == null) {
          UserServiceGrpc.getAddGroupMembersMethod = getAddGroupMembersMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.AddGroupMembersRequest, messenger.v1.Messenger.Conversation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddGroupMembers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.AddGroupMembersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.Conversation.getDefaultInstance()))
              .build();
        }
      }
    }
    return getAddGroupMembersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.RemoveGroupMemberRequest,
      messenger.v1.Messenger.Conversation> getRemoveGroupMemberMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveGroupMember",
      requestType = messenger.v1.Messenger.RemoveGroupMemberRequest.class,
      responseType = messenger.v1.Messenger.Conversation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.RemoveGroupMemberRequest,
      messenger.v1.Messenger.Conversation> getRemoveGroupMemberMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.RemoveGroupMemberRequest, messenger.v1.Messenger.Conversation> getRemoveGroupMemberMethod;
    if ((getRemoveGroupMemberMethod = UserServiceGrpc.getRemoveGroupMemberMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getRemoveGroupMemberMethod = UserServiceGrpc.getRemoveGroupMemberMethod) == null) {
          UserServiceGrpc.getRemoveGroupMemberMethod = getRemoveGroupMemberMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.RemoveGroupMemberRequest, messenger.v1.Messenger.Conversation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveGroupMember"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.RemoveGroupMemberRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.Conversation.getDefaultInstance()))
              .build();
        }
      }
    }
    return getRemoveGroupMemberMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.LeaveGroupConversationRequest,
      com.google.protobuf.Empty> getLeaveGroupConversationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "LeaveGroupConversation",
      requestType = messenger.v1.Messenger.LeaveGroupConversationRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.LeaveGroupConversationRequest,
      com.google.protobuf.Empty> getLeaveGroupConversationMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.LeaveGroupConversationRequest, com.google.protobuf.Empty> getLeaveGroupConversationMethod;
    if ((getLeaveGroupConversationMethod = UserServiceGrpc.getLeaveGroupConversationMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getLeaveGroupConversationMethod = UserServiceGrpc.getLeaveGroupConversationMethod) == null) {
          UserServiceGrpc.getLeaveGroupConversationMethod = getLeaveGroupConversationMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.LeaveGroupConversationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "LeaveGroupConversation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.LeaveGroupConversationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .build();
        }
      }
    }
    return getLeaveGroupConversationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.TransferGroupAdminRequest,
      messenger.v1.Messenger.Conversation> getTransferGroupAdminMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "TransferGroupAdmin",
      requestType = messenger.v1.Messenger.TransferGroupAdminRequest.class,
      responseType = messenger.v1.Messenger.Conversation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.TransferGroupAdminRequest,
      messenger.v1.Messenger.Conversation> getTransferGroupAdminMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.TransferGroupAdminRequest, messenger.v1.Messenger.Conversation> getTransferGroupAdminMethod;
    if ((getTransferGroupAdminMethod = UserServiceGrpc.getTransferGroupAdminMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getTransferGroupAdminMethod = UserServiceGrpc.getTransferGroupAdminMethod) == null) {
          UserServiceGrpc.getTransferGroupAdminMethod = getTransferGroupAdminMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.TransferGroupAdminRequest, messenger.v1.Messenger.Conversation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "TransferGroupAdmin"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.TransferGroupAdminRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.Conversation.getDefaultInstance()))
              .build();
        }
      }
    }
    return getTransferGroupAdminMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.UpsertConversationKeyRequest,
      messenger.v1.Messenger.ConversationKey> getUpsertConversationKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpsertConversationKey",
      requestType = messenger.v1.Messenger.UpsertConversationKeyRequest.class,
      responseType = messenger.v1.Messenger.ConversationKey.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.UpsertConversationKeyRequest,
      messenger.v1.Messenger.ConversationKey> getUpsertConversationKeyMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.UpsertConversationKeyRequest, messenger.v1.Messenger.ConversationKey> getUpsertConversationKeyMethod;
    if ((getUpsertConversationKeyMethod = UserServiceGrpc.getUpsertConversationKeyMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getUpsertConversationKeyMethod = UserServiceGrpc.getUpsertConversationKeyMethod) == null) {
          UserServiceGrpc.getUpsertConversationKeyMethod = getUpsertConversationKeyMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.UpsertConversationKeyRequest, messenger.v1.Messenger.ConversationKey>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpsertConversationKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.UpsertConversationKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.ConversationKey.getDefaultInstance()))
              .build();
        }
      }
    }
    return getUpsertConversationKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.GetConversationKeyRequest,
      messenger.v1.Messenger.ConversationKey> getGetConversationKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetConversationKey",
      requestType = messenger.v1.Messenger.GetConversationKeyRequest.class,
      responseType = messenger.v1.Messenger.ConversationKey.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.GetConversationKeyRequest,
      messenger.v1.Messenger.ConversationKey> getGetConversationKeyMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.GetConversationKeyRequest, messenger.v1.Messenger.ConversationKey> getGetConversationKeyMethod;
    if ((getGetConversationKeyMethod = UserServiceGrpc.getGetConversationKeyMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetConversationKeyMethod = UserServiceGrpc.getGetConversationKeyMethod) == null) {
          UserServiceGrpc.getGetConversationKeyMethod = getGetConversationKeyMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.GetConversationKeyRequest, messenger.v1.Messenger.ConversationKey>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetConversationKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetConversationKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.ConversationKey.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetConversationKeyMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static UserServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserServiceStub>() {
        @java.lang.Override
        public UserServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserServiceStub(channel, callOptions);
        }
      };
    return UserServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static UserServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserServiceBlockingStub>() {
        @java.lang.Override
        public UserServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserServiceBlockingStub(channel, callOptions);
        }
      };
    return UserServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static UserServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<UserServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<UserServiceFutureStub>() {
        @java.lang.Override
        public UserServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new UserServiceFutureStub(channel, callOptions);
        }
      };
    return UserServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void getProfile(messenger.v1.Messenger.GetProfileRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Profile> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetProfileMethod(), responseObserver);
    }

    /**
     */
    default void updateProfile(messenger.v1.Messenger.UpdateProfileRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Profile> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateProfileMethod(), responseObserver);
    }

    /**
     */
    default void searchUsers(messenger.v1.Messenger.SearchUsersRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.SearchUsersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchUsersMethod(), responseObserver);
    }

    /**
     */
    default void listConversations(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.ListConversationsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListConversationsMethod(), responseObserver);
    }

    /**
     */
    default void publishIdentityKey(messenger.v1.Messenger.PublishIdentityKeyRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.IdentityKey> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishIdentityKeyMethod(), responseObserver);
    }

    /**
     */
    default void publishPrekeyBundle(messenger.v1.Messenger.PublishPrekeyBundleRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrekeyBundle> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishPrekeyBundleMethod(), responseObserver);
    }

    /**
     */
    default void getIdentityKey(messenger.v1.Messenger.GetIdentityKeyRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.IdentityKey> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetIdentityKeyMethod(), responseObserver);
    }

    /**
     */
    default void getIdentityKeys(messenger.v1.Messenger.GetIdentityKeysRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetIdentityKeysResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetIdentityKeysMethod(), responseObserver);
    }

    /**
     */
    default void acquirePrekeyBundle(messenger.v1.Messenger.AcquirePrekeyBundleRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrekeyBundle> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAcquirePrekeyBundleMethod(), responseObserver);
    }

    /**
     */
    default void acquirePrekeyBundles(messenger.v1.Messenger.AcquirePrekeyBundlesRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.AcquirePrekeyBundlesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAcquirePrekeyBundlesMethod(), responseObserver);
    }

    /**
     */
    default void createGroupConversation(messenger.v1.Messenger.CreateGroupConversationRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateGroupConversationMethod(), responseObserver);
    }

    /**
     */
    default void addGroupMembers(messenger.v1.Messenger.AddGroupMembersRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddGroupMembersMethod(), responseObserver);
    }

    /**
     */
    default void removeGroupMember(messenger.v1.Messenger.RemoveGroupMemberRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemoveGroupMemberMethod(), responseObserver);
    }

    /**
     */
    default void leaveGroupConversation(messenger.v1.Messenger.LeaveGroupConversationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLeaveGroupConversationMethod(), responseObserver);
    }

    /**
     */
    default void transferGroupAdmin(messenger.v1.Messenger.TransferGroupAdminRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getTransferGroupAdminMethod(), responseObserver);
    }

    /**
     */
    default void upsertConversationKey(messenger.v1.Messenger.UpsertConversationKeyRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.ConversationKey> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpsertConversationKeyMethod(), responseObserver);
    }

    /**
     */
    default void getConversationKey(messenger.v1.Messenger.GetConversationKeyRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.ConversationKey> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetConversationKeyMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service UserService.
   */
  public static abstract class UserServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return UserServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service UserService.
   */
  public static final class UserServiceStub
      extends io.grpc.stub.AbstractAsyncStub<UserServiceStub> {
    private UserServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserServiceStub(channel, callOptions);
    }

    /**
     */
    public void getProfile(messenger.v1.Messenger.GetProfileRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Profile> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetProfileMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void updateProfile(messenger.v1.Messenger.UpdateProfileRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Profile> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateProfileMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void searchUsers(messenger.v1.Messenger.SearchUsersRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.SearchUsersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchUsersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listConversations(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.ListConversationsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListConversationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void publishIdentityKey(messenger.v1.Messenger.PublishIdentityKeyRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.IdentityKey> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishIdentityKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void publishPrekeyBundle(messenger.v1.Messenger.PublishPrekeyBundleRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrekeyBundle> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishPrekeyBundleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getIdentityKey(messenger.v1.Messenger.GetIdentityKeyRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.IdentityKey> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetIdentityKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getIdentityKeys(messenger.v1.Messenger.GetIdentityKeysRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetIdentityKeysResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetIdentityKeysMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void acquirePrekeyBundle(messenger.v1.Messenger.AcquirePrekeyBundleRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrekeyBundle> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAcquirePrekeyBundleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void acquirePrekeyBundles(messenger.v1.Messenger.AcquirePrekeyBundlesRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.AcquirePrekeyBundlesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAcquirePrekeyBundlesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void createGroupConversation(messenger.v1.Messenger.CreateGroupConversationRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateGroupConversationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void addGroupMembers(messenger.v1.Messenger.AddGroupMembersRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddGroupMembersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void removeGroupMember(messenger.v1.Messenger.RemoveGroupMemberRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemoveGroupMemberMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void leaveGroupConversation(messenger.v1.Messenger.LeaveGroupConversationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLeaveGroupConversationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void transferGroupAdmin(messenger.v1.Messenger.TransferGroupAdminRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getTransferGroupAdminMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void upsertConversationKey(messenger.v1.Messenger.UpsertConversationKeyRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.ConversationKey> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpsertConversationKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getConversationKey(messenger.v1.Messenger.GetConversationKeyRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.ConversationKey> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetConversationKeyMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service UserService.
   */
  public static final class UserServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<UserServiceBlockingStub> {
    private UserServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public messenger.v1.Messenger.Profile getProfile(messenger.v1.Messenger.GetProfileRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetProfileMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.Profile updateProfile(messenger.v1.Messenger.UpdateProfileRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateProfileMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.SearchUsersResponse searchUsers(messenger.v1.Messenger.SearchUsersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchUsersMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.ListConversationsResponse listConversations(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListConversationsMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.IdentityKey publishIdentityKey(messenger.v1.Messenger.PublishIdentityKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishIdentityKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.PrekeyBundle publishPrekeyBundle(messenger.v1.Messenger.PublishPrekeyBundleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishPrekeyBundleMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.IdentityKey getIdentityKey(messenger.v1.Messenger.GetIdentityKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetIdentityKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.GetIdentityKeysResponse getIdentityKeys(messenger.v1.Messenger.GetIdentityKeysRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetIdentityKeysMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.PrekeyBundle acquirePrekeyBundle(messenger.v1.Messenger.AcquirePrekeyBundleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAcquirePrekeyBundleMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.AcquirePrekeyBundlesResponse acquirePrekeyBundles(messenger.v1.Messenger.AcquirePrekeyBundlesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAcquirePrekeyBundlesMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.Conversation createGroupConversation(messenger.v1.Messenger.CreateGroupConversationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateGroupConversationMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.Conversation addGroupMembers(messenger.v1.Messenger.AddGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddGroupMembersMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.Conversation removeGroupMember(messenger.v1.Messenger.RemoveGroupMemberRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemoveGroupMemberMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty leaveGroupConversation(messenger.v1.Messenger.LeaveGroupConversationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLeaveGroupConversationMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.Conversation transferGroupAdmin(messenger.v1.Messenger.TransferGroupAdminRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getTransferGroupAdminMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.ConversationKey upsertConversationKey(messenger.v1.Messenger.UpsertConversationKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpsertConversationKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.ConversationKey getConversationKey(messenger.v1.Messenger.GetConversationKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetConversationKeyMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service UserService.
   */
  public static final class UserServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<UserServiceFutureStub> {
    private UserServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected UserServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new UserServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.Profile> getProfile(
        messenger.v1.Messenger.GetProfileRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetProfileMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.Profile> updateProfile(
        messenger.v1.Messenger.UpdateProfileRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateProfileMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.SearchUsersResponse> searchUsers(
        messenger.v1.Messenger.SearchUsersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchUsersMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.ListConversationsResponse> listConversations(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListConversationsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.IdentityKey> publishIdentityKey(
        messenger.v1.Messenger.PublishIdentityKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishIdentityKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.PrekeyBundle> publishPrekeyBundle(
        messenger.v1.Messenger.PublishPrekeyBundleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishPrekeyBundleMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.IdentityKey> getIdentityKey(
        messenger.v1.Messenger.GetIdentityKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetIdentityKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.GetIdentityKeysResponse> getIdentityKeys(
        messenger.v1.Messenger.GetIdentityKeysRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetIdentityKeysMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.PrekeyBundle> acquirePrekeyBundle(
        messenger.v1.Messenger.AcquirePrekeyBundleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAcquirePrekeyBundleMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.AcquirePrekeyBundlesResponse> acquirePrekeyBundles(
        messenger.v1.Messenger.AcquirePrekeyBundlesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAcquirePrekeyBundlesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.Conversation> createGroupConversation(
        messenger.v1.Messenger.CreateGroupConversationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateGroupConversationMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.Conversation> addGroupMembers(
        messenger.v1.Messenger.AddGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddGroupMembersMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.Conversation> removeGroupMember(
        messenger.v1.Messenger.RemoveGroupMemberRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemoveGroupMemberMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> leaveGroupConversation(
        messenger.v1.Messenger.LeaveGroupConversationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLeaveGroupConversationMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.Conversation> transferGroupAdmin(
        messenger.v1.Messenger.TransferGroupAdminRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getTransferGroupAdminMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.ConversationKey> upsertConversationKey(
        messenger.v1.Messenger.UpsertConversationKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpsertConversationKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.ConversationKey> getConversationKey(
        messenger.v1.Messenger.GetConversationKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetConversationKeyMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_PROFILE = 0;
  private static final int METHODID_UPDATE_PROFILE = 1;
  private static final int METHODID_SEARCH_USERS = 2;
  private static final int METHODID_LIST_CONVERSATIONS = 3;
  private static final int METHODID_PUBLISH_IDENTITY_KEY = 4;
  private static final int METHODID_PUBLISH_PREKEY_BUNDLE = 5;
  private static final int METHODID_GET_IDENTITY_KEY = 6;
  private static final int METHODID_GET_IDENTITY_KEYS = 7;
  private static final int METHODID_ACQUIRE_PREKEY_BUNDLE = 8;
  private static final int METHODID_ACQUIRE_PREKEY_BUNDLES = 9;
  private static final int METHODID_CREATE_GROUP_CONVERSATION = 10;
  private static final int METHODID_ADD_GROUP_MEMBERS = 11;
  private static final int METHODID_REMOVE_GROUP_MEMBER = 12;
  private static final int METHODID_LEAVE_GROUP_CONVERSATION = 13;
  private static final int METHODID_TRANSFER_GROUP_ADMIN = 14;
  private static final int METHODID_UPSERT_CONVERSATION_KEY = 15;
  private static final int METHODID_GET_CONVERSATION_KEY = 16;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GET_PROFILE:
          serviceImpl.getProfile((messenger.v1.Messenger.GetProfileRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.Profile>) responseObserver);
          break;
        case METHODID_UPDATE_PROFILE:
          serviceImpl.updateProfile((messenger.v1.Messenger.UpdateProfileRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.Profile>) responseObserver);
          break;
        case METHODID_SEARCH_USERS:
          serviceImpl.searchUsers((messenger.v1.Messenger.SearchUsersRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.SearchUsersResponse>) responseObserver);
          break;
        case METHODID_LIST_CONVERSATIONS:
          serviceImpl.listConversations((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.ListConversationsResponse>) responseObserver);
          break;
        case METHODID_PUBLISH_IDENTITY_KEY:
          serviceImpl.publishIdentityKey((messenger.v1.Messenger.PublishIdentityKeyRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.IdentityKey>) responseObserver);
          break;
        case METHODID_PUBLISH_PREKEY_BUNDLE:
          serviceImpl.publishPrekeyBundle((messenger.v1.Messenger.PublishPrekeyBundleRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrekeyBundle>) responseObserver);
          break;
        case METHODID_GET_IDENTITY_KEY:
          serviceImpl.getIdentityKey((messenger.v1.Messenger.GetIdentityKeyRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.IdentityKey>) responseObserver);
          break;
        case METHODID_GET_IDENTITY_KEYS:
          serviceImpl.getIdentityKeys((messenger.v1.Messenger.GetIdentityKeysRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetIdentityKeysResponse>) responseObserver);
          break;
        case METHODID_ACQUIRE_PREKEY_BUNDLE:
          serviceImpl.acquirePrekeyBundle((messenger.v1.Messenger.AcquirePrekeyBundleRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrekeyBundle>) responseObserver);
          break;
        case METHODID_ACQUIRE_PREKEY_BUNDLES:
          serviceImpl.acquirePrekeyBundles((messenger.v1.Messenger.AcquirePrekeyBundlesRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.AcquirePrekeyBundlesResponse>) responseObserver);
          break;
        case METHODID_CREATE_GROUP_CONVERSATION:
          serviceImpl.createGroupConversation((messenger.v1.Messenger.CreateGroupConversationRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation>) responseObserver);
          break;
        case METHODID_ADD_GROUP_MEMBERS:
          serviceImpl.addGroupMembers((messenger.v1.Messenger.AddGroupMembersRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation>) responseObserver);
          break;
        case METHODID_REMOVE_GROUP_MEMBER:
          serviceImpl.removeGroupMember((messenger.v1.Messenger.RemoveGroupMemberRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation>) responseObserver);
          break;
        case METHODID_LEAVE_GROUP_CONVERSATION:
          serviceImpl.leaveGroupConversation((messenger.v1.Messenger.LeaveGroupConversationRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_TRANSFER_GROUP_ADMIN:
          serviceImpl.transferGroupAdmin((messenger.v1.Messenger.TransferGroupAdminRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.Conversation>) responseObserver);
          break;
        case METHODID_UPSERT_CONVERSATION_KEY:
          serviceImpl.upsertConversationKey((messenger.v1.Messenger.UpsertConversationKeyRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.ConversationKey>) responseObserver);
          break;
        case METHODID_GET_CONVERSATION_KEY:
          serviceImpl.getConversationKey((messenger.v1.Messenger.GetConversationKeyRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.ConversationKey>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getGetProfileMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.GetProfileRequest,
              messenger.v1.Messenger.Profile>(
                service, METHODID_GET_PROFILE)))
        .addMethod(
          getUpdateProfileMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.UpdateProfileRequest,
              messenger.v1.Messenger.Profile>(
                service, METHODID_UPDATE_PROFILE)))
        .addMethod(
          getSearchUsersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.SearchUsersRequest,
              messenger.v1.Messenger.SearchUsersResponse>(
                service, METHODID_SEARCH_USERS)))
        .addMethod(
          getListConversationsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              messenger.v1.Messenger.ListConversationsResponse>(
                service, METHODID_LIST_CONVERSATIONS)))
        .addMethod(
          getPublishIdentityKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.PublishIdentityKeyRequest,
              messenger.v1.Messenger.IdentityKey>(
                service, METHODID_PUBLISH_IDENTITY_KEY)))
        .addMethod(
          getPublishPrekeyBundleMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.PublishPrekeyBundleRequest,
              messenger.v1.Messenger.PrekeyBundle>(
                service, METHODID_PUBLISH_PREKEY_BUNDLE)))
        .addMethod(
          getGetIdentityKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.GetIdentityKeyRequest,
              messenger.v1.Messenger.IdentityKey>(
                service, METHODID_GET_IDENTITY_KEY)))
        .addMethod(
          getGetIdentityKeysMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.GetIdentityKeysRequest,
              messenger.v1.Messenger.GetIdentityKeysResponse>(
                service, METHODID_GET_IDENTITY_KEYS)))
        .addMethod(
          getAcquirePrekeyBundleMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.AcquirePrekeyBundleRequest,
              messenger.v1.Messenger.PrekeyBundle>(
                service, METHODID_ACQUIRE_PREKEY_BUNDLE)))
        .addMethod(
          getAcquirePrekeyBundlesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.AcquirePrekeyBundlesRequest,
              messenger.v1.Messenger.AcquirePrekeyBundlesResponse>(
                service, METHODID_ACQUIRE_PREKEY_BUNDLES)))
        .addMethod(
          getCreateGroupConversationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.CreateGroupConversationRequest,
              messenger.v1.Messenger.Conversation>(
                service, METHODID_CREATE_GROUP_CONVERSATION)))
        .addMethod(
          getAddGroupMembersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.AddGroupMembersRequest,
              messenger.v1.Messenger.Conversation>(
                service, METHODID_ADD_GROUP_MEMBERS)))
        .addMethod(
          getRemoveGroupMemberMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.RemoveGroupMemberRequest,
              messenger.v1.Messenger.Conversation>(
                service, METHODID_REMOVE_GROUP_MEMBER)))
        .addMethod(
          getLeaveGroupConversationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.LeaveGroupConversationRequest,
              com.google.protobuf.Empty>(
                service, METHODID_LEAVE_GROUP_CONVERSATION)))
        .addMethod(
          getTransferGroupAdminMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.TransferGroupAdminRequest,
              messenger.v1.Messenger.Conversation>(
                service, METHODID_TRANSFER_GROUP_ADMIN)))
        .addMethod(
          getUpsertConversationKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.UpsertConversationKeyRequest,
              messenger.v1.Messenger.ConversationKey>(
                service, METHODID_UPSERT_CONVERSATION_KEY)))
        .addMethod(
          getGetConversationKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.GetConversationKeyRequest,
              messenger.v1.Messenger.ConversationKey>(
                service, METHODID_GET_CONVERSATION_KEY)))
        .build();
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (UserServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .addMethod(getGetProfileMethod())
              .addMethod(getUpdateProfileMethod())
              .addMethod(getSearchUsersMethod())
              .addMethod(getListConversationsMethod())
              .addMethod(getPublishIdentityKeyMethod())
              .addMethod(getPublishPrekeyBundleMethod())
              .addMethod(getGetIdentityKeyMethod())
              .addMethod(getGetIdentityKeysMethod())
              .addMethod(getAcquirePrekeyBundleMethod())
              .addMethod(getAcquirePrekeyBundlesMethod())
              .addMethod(getCreateGroupConversationMethod())
              .addMethod(getAddGroupMembersMethod())
              .addMethod(getRemoveGroupMemberMethod())
              .addMethod(getLeaveGroupConversationMethod())
              .addMethod(getTransferGroupAdminMethod())
              .addMethod(getUpsertConversationKeyMethod())
              .addMethod(getGetConversationKeyMethod())
              .build();
        }
      }
    }
    return result;
  }
}
