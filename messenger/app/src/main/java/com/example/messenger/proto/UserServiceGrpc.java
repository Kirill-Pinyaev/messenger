package com.example.messenger.proto;

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
  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.GetProfileRequest,
      com.example.messenger.proto.Profile> getGetProfileMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetProfile",
      requestType = com.example.messenger.proto.GetProfileRequest.class,
      responseType = com.example.messenger.proto.Profile.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.GetProfileRequest,
      com.example.messenger.proto.Profile> getGetProfileMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.GetProfileRequest, com.example.messenger.proto.Profile> getGetProfileMethod;
    if ((getGetProfileMethod = UserServiceGrpc.getGetProfileMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetProfileMethod = UserServiceGrpc.getGetProfileMethod) == null) {
          UserServiceGrpc.getGetProfileMethod = getGetProfileMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.GetProfileRequest, com.example.messenger.proto.Profile>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetProfile"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetProfileRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.Profile.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetProfileMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.UpdateProfileRequest,
      com.example.messenger.proto.Profile> getUpdateProfileMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpdateProfile",
      requestType = com.example.messenger.proto.UpdateProfileRequest.class,
      responseType = com.example.messenger.proto.Profile.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.UpdateProfileRequest,
      com.example.messenger.proto.Profile> getUpdateProfileMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.UpdateProfileRequest, com.example.messenger.proto.Profile> getUpdateProfileMethod;
    if ((getUpdateProfileMethod = UserServiceGrpc.getUpdateProfileMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getUpdateProfileMethod = UserServiceGrpc.getUpdateProfileMethod) == null) {
          UserServiceGrpc.getUpdateProfileMethod = getUpdateProfileMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.UpdateProfileRequest, com.example.messenger.proto.Profile>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpdateProfile"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.UpdateProfileRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.Profile.getDefaultInstance()))
              .build();
        }
      }
    }
    return getUpdateProfileMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.SearchUsersRequest,
      com.example.messenger.proto.SearchUsersResponse> getSearchUsersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchUsers",
      requestType = com.example.messenger.proto.SearchUsersRequest.class,
      responseType = com.example.messenger.proto.SearchUsersResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.SearchUsersRequest,
      com.example.messenger.proto.SearchUsersResponse> getSearchUsersMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.SearchUsersRequest, com.example.messenger.proto.SearchUsersResponse> getSearchUsersMethod;
    if ((getSearchUsersMethod = UserServiceGrpc.getSearchUsersMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getSearchUsersMethod = UserServiceGrpc.getSearchUsersMethod) == null) {
          UserServiceGrpc.getSearchUsersMethod = getSearchUsersMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.SearchUsersRequest, com.example.messenger.proto.SearchUsersResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchUsers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.SearchUsersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.SearchUsersResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getSearchUsersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.example.messenger.proto.ListConversationsResponse> getListConversationsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListConversations",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.example.messenger.proto.ListConversationsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.example.messenger.proto.ListConversationsResponse> getListConversationsMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.example.messenger.proto.ListConversationsResponse> getListConversationsMethod;
    if ((getListConversationsMethod = UserServiceGrpc.getListConversationsMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getListConversationsMethod = UserServiceGrpc.getListConversationsMethod) == null) {
          UserServiceGrpc.getListConversationsMethod = getListConversationsMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.example.messenger.proto.ListConversationsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListConversations"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.ListConversationsResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getListConversationsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.PublishIdentityKeyRequest,
      com.example.messenger.proto.IdentityKey> getPublishIdentityKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishIdentityKey",
      requestType = com.example.messenger.proto.PublishIdentityKeyRequest.class,
      responseType = com.example.messenger.proto.IdentityKey.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.PublishIdentityKeyRequest,
      com.example.messenger.proto.IdentityKey> getPublishIdentityKeyMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.PublishIdentityKeyRequest, com.example.messenger.proto.IdentityKey> getPublishIdentityKeyMethod;
    if ((getPublishIdentityKeyMethod = UserServiceGrpc.getPublishIdentityKeyMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getPublishIdentityKeyMethod = UserServiceGrpc.getPublishIdentityKeyMethod) == null) {
          UserServiceGrpc.getPublishIdentityKeyMethod = getPublishIdentityKeyMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.PublishIdentityKeyRequest, com.example.messenger.proto.IdentityKey>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishIdentityKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.PublishIdentityKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.IdentityKey.getDefaultInstance()))
              .build();
        }
      }
    }
    return getPublishIdentityKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.PublishPrekeyBundleRequest,
      com.example.messenger.proto.PrekeyBundle> getPublishPrekeyBundleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PublishPrekeyBundle",
      requestType = com.example.messenger.proto.PublishPrekeyBundleRequest.class,
      responseType = com.example.messenger.proto.PrekeyBundle.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.PublishPrekeyBundleRequest,
      com.example.messenger.proto.PrekeyBundle> getPublishPrekeyBundleMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.PublishPrekeyBundleRequest, com.example.messenger.proto.PrekeyBundle> getPublishPrekeyBundleMethod;
    if ((getPublishPrekeyBundleMethod = UserServiceGrpc.getPublishPrekeyBundleMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getPublishPrekeyBundleMethod = UserServiceGrpc.getPublishPrekeyBundleMethod) == null) {
          UserServiceGrpc.getPublishPrekeyBundleMethod = getPublishPrekeyBundleMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.PublishPrekeyBundleRequest, com.example.messenger.proto.PrekeyBundle>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PublishPrekeyBundle"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.PublishPrekeyBundleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.PrekeyBundle.getDefaultInstance()))
              .build();
        }
      }
    }
    return getPublishPrekeyBundleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.InitializeHistoryArchiveRequest,
      com.example.messenger.proto.HistoryArchiveHeader> getInitializeHistoryArchiveMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "InitializeHistoryArchive",
      requestType = com.example.messenger.proto.InitializeHistoryArchiveRequest.class,
      responseType = com.example.messenger.proto.HistoryArchiveHeader.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.InitializeHistoryArchiveRequest,
      com.example.messenger.proto.HistoryArchiveHeader> getInitializeHistoryArchiveMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.InitializeHistoryArchiveRequest, com.example.messenger.proto.HistoryArchiveHeader> getInitializeHistoryArchiveMethod;
    if ((getInitializeHistoryArchiveMethod = UserServiceGrpc.getInitializeHistoryArchiveMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getInitializeHistoryArchiveMethod = UserServiceGrpc.getInitializeHistoryArchiveMethod) == null) {
          UserServiceGrpc.getInitializeHistoryArchiveMethod = getInitializeHistoryArchiveMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.InitializeHistoryArchiveRequest, com.example.messenger.proto.HistoryArchiveHeader>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "InitializeHistoryArchive"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.InitializeHistoryArchiveRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.HistoryArchiveHeader.getDefaultInstance()))
              .build();
        }
      }
    }
    return getInitializeHistoryArchiveMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.example.messenger.proto.HistoryArchiveHeader> getGetHistoryArchiveHeaderMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetHistoryArchiveHeader",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.example.messenger.proto.HistoryArchiveHeader.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.example.messenger.proto.HistoryArchiveHeader> getGetHistoryArchiveHeaderMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.example.messenger.proto.HistoryArchiveHeader> getGetHistoryArchiveHeaderMethod;
    if ((getGetHistoryArchiveHeaderMethod = UserServiceGrpc.getGetHistoryArchiveHeaderMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetHistoryArchiveHeaderMethod = UserServiceGrpc.getGetHistoryArchiveHeaderMethod) == null) {
          UserServiceGrpc.getGetHistoryArchiveHeaderMethod = getGetHistoryArchiveHeaderMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.example.messenger.proto.HistoryArchiveHeader>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetHistoryArchiveHeader"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.HistoryArchiveHeader.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetHistoryArchiveHeaderMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.GetArchivePublicKeysRequest,
      com.example.messenger.proto.GetArchivePublicKeysResponse> getGetArchivePublicKeysMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetArchivePublicKeys",
      requestType = com.example.messenger.proto.GetArchivePublicKeysRequest.class,
      responseType = com.example.messenger.proto.GetArchivePublicKeysResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.GetArchivePublicKeysRequest,
      com.example.messenger.proto.GetArchivePublicKeysResponse> getGetArchivePublicKeysMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.GetArchivePublicKeysRequest, com.example.messenger.proto.GetArchivePublicKeysResponse> getGetArchivePublicKeysMethod;
    if ((getGetArchivePublicKeysMethod = UserServiceGrpc.getGetArchivePublicKeysMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetArchivePublicKeysMethod = UserServiceGrpc.getGetArchivePublicKeysMethod) == null) {
          UserServiceGrpc.getGetArchivePublicKeysMethod = getGetArchivePublicKeysMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.GetArchivePublicKeysRequest, com.example.messenger.proto.GetArchivePublicKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetArchivePublicKeys"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetArchivePublicKeysRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetArchivePublicKeysResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetArchivePublicKeysMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.GetIdentityKeyRequest,
      com.example.messenger.proto.IdentityKey> getGetIdentityKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetIdentityKey",
      requestType = com.example.messenger.proto.GetIdentityKeyRequest.class,
      responseType = com.example.messenger.proto.IdentityKey.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.GetIdentityKeyRequest,
      com.example.messenger.proto.IdentityKey> getGetIdentityKeyMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.GetIdentityKeyRequest, com.example.messenger.proto.IdentityKey> getGetIdentityKeyMethod;
    if ((getGetIdentityKeyMethod = UserServiceGrpc.getGetIdentityKeyMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetIdentityKeyMethod = UserServiceGrpc.getGetIdentityKeyMethod) == null) {
          UserServiceGrpc.getGetIdentityKeyMethod = getGetIdentityKeyMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.GetIdentityKeyRequest, com.example.messenger.proto.IdentityKey>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetIdentityKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetIdentityKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.IdentityKey.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetIdentityKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.GetIdentityKeysRequest,
      com.example.messenger.proto.GetIdentityKeysResponse> getGetIdentityKeysMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetIdentityKeys",
      requestType = com.example.messenger.proto.GetIdentityKeysRequest.class,
      responseType = com.example.messenger.proto.GetIdentityKeysResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.GetIdentityKeysRequest,
      com.example.messenger.proto.GetIdentityKeysResponse> getGetIdentityKeysMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.GetIdentityKeysRequest, com.example.messenger.proto.GetIdentityKeysResponse> getGetIdentityKeysMethod;
    if ((getGetIdentityKeysMethod = UserServiceGrpc.getGetIdentityKeysMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetIdentityKeysMethod = UserServiceGrpc.getGetIdentityKeysMethod) == null) {
          UserServiceGrpc.getGetIdentityKeysMethod = getGetIdentityKeysMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.GetIdentityKeysRequest, com.example.messenger.proto.GetIdentityKeysResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetIdentityKeys"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetIdentityKeysRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetIdentityKeysResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetIdentityKeysMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.AcquirePrekeyBundleRequest,
      com.example.messenger.proto.PrekeyBundle> getAcquirePrekeyBundleMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AcquirePrekeyBundle",
      requestType = com.example.messenger.proto.AcquirePrekeyBundleRequest.class,
      responseType = com.example.messenger.proto.PrekeyBundle.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.AcquirePrekeyBundleRequest,
      com.example.messenger.proto.PrekeyBundle> getAcquirePrekeyBundleMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.AcquirePrekeyBundleRequest, com.example.messenger.proto.PrekeyBundle> getAcquirePrekeyBundleMethod;
    if ((getAcquirePrekeyBundleMethod = UserServiceGrpc.getAcquirePrekeyBundleMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getAcquirePrekeyBundleMethod = UserServiceGrpc.getAcquirePrekeyBundleMethod) == null) {
          UserServiceGrpc.getAcquirePrekeyBundleMethod = getAcquirePrekeyBundleMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.AcquirePrekeyBundleRequest, com.example.messenger.proto.PrekeyBundle>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AcquirePrekeyBundle"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.AcquirePrekeyBundleRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.PrekeyBundle.getDefaultInstance()))
              .build();
        }
      }
    }
    return getAcquirePrekeyBundleMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.AcquirePrekeyBundlesRequest,
      com.example.messenger.proto.AcquirePrekeyBundlesResponse> getAcquirePrekeyBundlesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AcquirePrekeyBundles",
      requestType = com.example.messenger.proto.AcquirePrekeyBundlesRequest.class,
      responseType = com.example.messenger.proto.AcquirePrekeyBundlesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.AcquirePrekeyBundlesRequest,
      com.example.messenger.proto.AcquirePrekeyBundlesResponse> getAcquirePrekeyBundlesMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.AcquirePrekeyBundlesRequest, com.example.messenger.proto.AcquirePrekeyBundlesResponse> getAcquirePrekeyBundlesMethod;
    if ((getAcquirePrekeyBundlesMethod = UserServiceGrpc.getAcquirePrekeyBundlesMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getAcquirePrekeyBundlesMethod = UserServiceGrpc.getAcquirePrekeyBundlesMethod) == null) {
          UserServiceGrpc.getAcquirePrekeyBundlesMethod = getAcquirePrekeyBundlesMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.AcquirePrekeyBundlesRequest, com.example.messenger.proto.AcquirePrekeyBundlesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AcquirePrekeyBundles"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.AcquirePrekeyBundlesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.AcquirePrekeyBundlesResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getAcquirePrekeyBundlesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.CreateGroupConversationRequest,
      com.example.messenger.proto.Conversation> getCreateGroupConversationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateGroupConversation",
      requestType = com.example.messenger.proto.CreateGroupConversationRequest.class,
      responseType = com.example.messenger.proto.Conversation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.CreateGroupConversationRequest,
      com.example.messenger.proto.Conversation> getCreateGroupConversationMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.CreateGroupConversationRequest, com.example.messenger.proto.Conversation> getCreateGroupConversationMethod;
    if ((getCreateGroupConversationMethod = UserServiceGrpc.getCreateGroupConversationMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getCreateGroupConversationMethod = UserServiceGrpc.getCreateGroupConversationMethod) == null) {
          UserServiceGrpc.getCreateGroupConversationMethod = getCreateGroupConversationMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.CreateGroupConversationRequest, com.example.messenger.proto.Conversation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateGroupConversation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.CreateGroupConversationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.Conversation.getDefaultInstance()))
              .build();
        }
      }
    }
    return getCreateGroupConversationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.AddGroupMembersRequest,
      com.example.messenger.proto.Conversation> getAddGroupMembersMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AddGroupMembers",
      requestType = com.example.messenger.proto.AddGroupMembersRequest.class,
      responseType = com.example.messenger.proto.Conversation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.AddGroupMembersRequest,
      com.example.messenger.proto.Conversation> getAddGroupMembersMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.AddGroupMembersRequest, com.example.messenger.proto.Conversation> getAddGroupMembersMethod;
    if ((getAddGroupMembersMethod = UserServiceGrpc.getAddGroupMembersMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getAddGroupMembersMethod = UserServiceGrpc.getAddGroupMembersMethod) == null) {
          UserServiceGrpc.getAddGroupMembersMethod = getAddGroupMembersMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.AddGroupMembersRequest, com.example.messenger.proto.Conversation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AddGroupMembers"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.AddGroupMembersRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.Conversation.getDefaultInstance()))
              .build();
        }
      }
    }
    return getAddGroupMembersMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.RemoveGroupMemberRequest,
      com.example.messenger.proto.Conversation> getRemoveGroupMemberMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RemoveGroupMember",
      requestType = com.example.messenger.proto.RemoveGroupMemberRequest.class,
      responseType = com.example.messenger.proto.Conversation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.RemoveGroupMemberRequest,
      com.example.messenger.proto.Conversation> getRemoveGroupMemberMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.RemoveGroupMemberRequest, com.example.messenger.proto.Conversation> getRemoveGroupMemberMethod;
    if ((getRemoveGroupMemberMethod = UserServiceGrpc.getRemoveGroupMemberMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getRemoveGroupMemberMethod = UserServiceGrpc.getRemoveGroupMemberMethod) == null) {
          UserServiceGrpc.getRemoveGroupMemberMethod = getRemoveGroupMemberMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.RemoveGroupMemberRequest, com.example.messenger.proto.Conversation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RemoveGroupMember"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.RemoveGroupMemberRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.Conversation.getDefaultInstance()))
              .build();
        }
      }
    }
    return getRemoveGroupMemberMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.LeaveGroupConversationRequest,
      com.google.protobuf.Empty> getLeaveGroupConversationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "LeaveGroupConversation",
      requestType = com.example.messenger.proto.LeaveGroupConversationRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.LeaveGroupConversationRequest,
      com.google.protobuf.Empty> getLeaveGroupConversationMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.LeaveGroupConversationRequest, com.google.protobuf.Empty> getLeaveGroupConversationMethod;
    if ((getLeaveGroupConversationMethod = UserServiceGrpc.getLeaveGroupConversationMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getLeaveGroupConversationMethod = UserServiceGrpc.getLeaveGroupConversationMethod) == null) {
          UserServiceGrpc.getLeaveGroupConversationMethod = getLeaveGroupConversationMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.LeaveGroupConversationRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "LeaveGroupConversation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.LeaveGroupConversationRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .build();
        }
      }
    }
    return getLeaveGroupConversationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.TransferGroupAdminRequest,
      com.example.messenger.proto.Conversation> getTransferGroupAdminMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "TransferGroupAdmin",
      requestType = com.example.messenger.proto.TransferGroupAdminRequest.class,
      responseType = com.example.messenger.proto.Conversation.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.TransferGroupAdminRequest,
      com.example.messenger.proto.Conversation> getTransferGroupAdminMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.TransferGroupAdminRequest, com.example.messenger.proto.Conversation> getTransferGroupAdminMethod;
    if ((getTransferGroupAdminMethod = UserServiceGrpc.getTransferGroupAdminMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getTransferGroupAdminMethod = UserServiceGrpc.getTransferGroupAdminMethod) == null) {
          UserServiceGrpc.getTransferGroupAdminMethod = getTransferGroupAdminMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.TransferGroupAdminRequest, com.example.messenger.proto.Conversation>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "TransferGroupAdmin"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.TransferGroupAdminRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.Conversation.getDefaultInstance()))
              .build();
        }
      }
    }
    return getTransferGroupAdminMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.UpsertConversationKeyRequest,
      com.example.messenger.proto.ConversationKey> getUpsertConversationKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UpsertConversationKey",
      requestType = com.example.messenger.proto.UpsertConversationKeyRequest.class,
      responseType = com.example.messenger.proto.ConversationKey.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.UpsertConversationKeyRequest,
      com.example.messenger.proto.ConversationKey> getUpsertConversationKeyMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.UpsertConversationKeyRequest, com.example.messenger.proto.ConversationKey> getUpsertConversationKeyMethod;
    if ((getUpsertConversationKeyMethod = UserServiceGrpc.getUpsertConversationKeyMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getUpsertConversationKeyMethod = UserServiceGrpc.getUpsertConversationKeyMethod) == null) {
          UserServiceGrpc.getUpsertConversationKeyMethod = getUpsertConversationKeyMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.UpsertConversationKeyRequest, com.example.messenger.proto.ConversationKey>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UpsertConversationKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.UpsertConversationKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.ConversationKey.getDefaultInstance()))
              .build();
        }
      }
    }
    return getUpsertConversationKeyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.GetConversationKeyRequest,
      com.example.messenger.proto.ConversationKey> getGetConversationKeyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetConversationKey",
      requestType = com.example.messenger.proto.GetConversationKeyRequest.class,
      responseType = com.example.messenger.proto.ConversationKey.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.GetConversationKeyRequest,
      com.example.messenger.proto.ConversationKey> getGetConversationKeyMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.GetConversationKeyRequest, com.example.messenger.proto.ConversationKey> getGetConversationKeyMethod;
    if ((getGetConversationKeyMethod = UserServiceGrpc.getGetConversationKeyMethod) == null) {
      synchronized (UserServiceGrpc.class) {
        if ((getGetConversationKeyMethod = UserServiceGrpc.getGetConversationKeyMethod) == null) {
          UserServiceGrpc.getGetConversationKeyMethod = getGetConversationKeyMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.GetConversationKeyRequest, com.example.messenger.proto.ConversationKey>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetConversationKey"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetConversationKeyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.ConversationKey.getDefaultInstance()))
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
    default void getProfile(com.example.messenger.proto.GetProfileRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Profile> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetProfileMethod(), responseObserver);
    }

    /**
     */
    default void updateProfile(com.example.messenger.proto.UpdateProfileRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Profile> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpdateProfileMethod(), responseObserver);
    }

    /**
     */
    default void searchUsers(com.example.messenger.proto.SearchUsersRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.SearchUsersResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchUsersMethod(), responseObserver);
    }

    /**
     */
    default void listConversations(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ListConversationsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListConversationsMethod(), responseObserver);
    }

    /**
     */
    default void publishIdentityKey(com.example.messenger.proto.PublishIdentityKeyRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.IdentityKey> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishIdentityKeyMethod(), responseObserver);
    }

    /**
     */
    default void publishPrekeyBundle(com.example.messenger.proto.PublishPrekeyBundleRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.PrekeyBundle> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPublishPrekeyBundleMethod(), responseObserver);
    }

    /**
     */
    default void initializeHistoryArchive(com.example.messenger.proto.InitializeHistoryArchiveRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.HistoryArchiveHeader> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getInitializeHistoryArchiveMethod(), responseObserver);
    }

    /**
     */
    default void getHistoryArchiveHeader(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.HistoryArchiveHeader> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetHistoryArchiveHeaderMethod(), responseObserver);
    }

    /**
     */
    default void getArchivePublicKeys(com.example.messenger.proto.GetArchivePublicKeysRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.GetArchivePublicKeysResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetArchivePublicKeysMethod(), responseObserver);
    }

    /**
     */
    default void getIdentityKey(com.example.messenger.proto.GetIdentityKeyRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.IdentityKey> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetIdentityKeyMethod(), responseObserver);
    }

    /**
     */
    default void getIdentityKeys(com.example.messenger.proto.GetIdentityKeysRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.GetIdentityKeysResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetIdentityKeysMethod(), responseObserver);
    }

    /**
     */
    default void acquirePrekeyBundle(com.example.messenger.proto.AcquirePrekeyBundleRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.PrekeyBundle> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAcquirePrekeyBundleMethod(), responseObserver);
    }

    /**
     */
    default void acquirePrekeyBundles(com.example.messenger.proto.AcquirePrekeyBundlesRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.AcquirePrekeyBundlesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAcquirePrekeyBundlesMethod(), responseObserver);
    }

    /**
     */
    default void createGroupConversation(com.example.messenger.proto.CreateGroupConversationRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateGroupConversationMethod(), responseObserver);
    }

    /**
     */
    default void addGroupMembers(com.example.messenger.proto.AddGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAddGroupMembersMethod(), responseObserver);
    }

    /**
     */
    default void removeGroupMember(com.example.messenger.proto.RemoveGroupMemberRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRemoveGroupMemberMethod(), responseObserver);
    }

    /**
     */
    default void leaveGroupConversation(com.example.messenger.proto.LeaveGroupConversationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getLeaveGroupConversationMethod(), responseObserver);
    }

    /**
     */
    default void transferGroupAdmin(com.example.messenger.proto.TransferGroupAdminRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getTransferGroupAdminMethod(), responseObserver);
    }

    /**
     */
    default void upsertConversationKey(com.example.messenger.proto.UpsertConversationKeyRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ConversationKey> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUpsertConversationKeyMethod(), responseObserver);
    }

    /**
     */
    default void getConversationKey(com.example.messenger.proto.GetConversationKeyRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ConversationKey> responseObserver) {
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
    public void getProfile(com.example.messenger.proto.GetProfileRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Profile> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetProfileMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void updateProfile(com.example.messenger.proto.UpdateProfileRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Profile> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpdateProfileMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void searchUsers(com.example.messenger.proto.SearchUsersRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.SearchUsersResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchUsersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listConversations(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ListConversationsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListConversationsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void publishIdentityKey(com.example.messenger.proto.PublishIdentityKeyRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.IdentityKey> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishIdentityKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void publishPrekeyBundle(com.example.messenger.proto.PublishPrekeyBundleRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.PrekeyBundle> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPublishPrekeyBundleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void initializeHistoryArchive(com.example.messenger.proto.InitializeHistoryArchiveRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.HistoryArchiveHeader> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getInitializeHistoryArchiveMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getHistoryArchiveHeader(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.HistoryArchiveHeader> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetHistoryArchiveHeaderMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getArchivePublicKeys(com.example.messenger.proto.GetArchivePublicKeysRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.GetArchivePublicKeysResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetArchivePublicKeysMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getIdentityKey(com.example.messenger.proto.GetIdentityKeyRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.IdentityKey> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetIdentityKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getIdentityKeys(com.example.messenger.proto.GetIdentityKeysRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.GetIdentityKeysResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetIdentityKeysMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void acquirePrekeyBundle(com.example.messenger.proto.AcquirePrekeyBundleRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.PrekeyBundle> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAcquirePrekeyBundleMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void acquirePrekeyBundles(com.example.messenger.proto.AcquirePrekeyBundlesRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.AcquirePrekeyBundlesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAcquirePrekeyBundlesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void createGroupConversation(com.example.messenger.proto.CreateGroupConversationRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateGroupConversationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void addGroupMembers(com.example.messenger.proto.AddGroupMembersRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAddGroupMembersMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void removeGroupMember(com.example.messenger.proto.RemoveGroupMemberRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRemoveGroupMemberMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void leaveGroupConversation(com.example.messenger.proto.LeaveGroupConversationRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getLeaveGroupConversationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void transferGroupAdmin(com.example.messenger.proto.TransferGroupAdminRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getTransferGroupAdminMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void upsertConversationKey(com.example.messenger.proto.UpsertConversationKeyRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ConversationKey> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUpsertConversationKeyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getConversationKey(com.example.messenger.proto.GetConversationKeyRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ConversationKey> responseObserver) {
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
    public com.example.messenger.proto.Profile getProfile(com.example.messenger.proto.GetProfileRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetProfileMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.Profile updateProfile(com.example.messenger.proto.UpdateProfileRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpdateProfileMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.SearchUsersResponse searchUsers(com.example.messenger.proto.SearchUsersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchUsersMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.ListConversationsResponse listConversations(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListConversationsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.IdentityKey publishIdentityKey(com.example.messenger.proto.PublishIdentityKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishIdentityKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.PrekeyBundle publishPrekeyBundle(com.example.messenger.proto.PublishPrekeyBundleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPublishPrekeyBundleMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.HistoryArchiveHeader initializeHistoryArchive(com.example.messenger.proto.InitializeHistoryArchiveRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getInitializeHistoryArchiveMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.HistoryArchiveHeader getHistoryArchiveHeader(com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetHistoryArchiveHeaderMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.GetArchivePublicKeysResponse getArchivePublicKeys(com.example.messenger.proto.GetArchivePublicKeysRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetArchivePublicKeysMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.IdentityKey getIdentityKey(com.example.messenger.proto.GetIdentityKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetIdentityKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.GetIdentityKeysResponse getIdentityKeys(com.example.messenger.proto.GetIdentityKeysRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetIdentityKeysMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.PrekeyBundle acquirePrekeyBundle(com.example.messenger.proto.AcquirePrekeyBundleRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAcquirePrekeyBundleMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.AcquirePrekeyBundlesResponse acquirePrekeyBundles(com.example.messenger.proto.AcquirePrekeyBundlesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAcquirePrekeyBundlesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.Conversation createGroupConversation(com.example.messenger.proto.CreateGroupConversationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateGroupConversationMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.Conversation addGroupMembers(com.example.messenger.proto.AddGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAddGroupMembersMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.Conversation removeGroupMember(com.example.messenger.proto.RemoveGroupMemberRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRemoveGroupMemberMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty leaveGroupConversation(com.example.messenger.proto.LeaveGroupConversationRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getLeaveGroupConversationMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.Conversation transferGroupAdmin(com.example.messenger.proto.TransferGroupAdminRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getTransferGroupAdminMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.ConversationKey upsertConversationKey(com.example.messenger.proto.UpsertConversationKeyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUpsertConversationKeyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.ConversationKey getConversationKey(com.example.messenger.proto.GetConversationKeyRequest request) {
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
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.Profile> getProfile(
        com.example.messenger.proto.GetProfileRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetProfileMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.Profile> updateProfile(
        com.example.messenger.proto.UpdateProfileRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpdateProfileMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.SearchUsersResponse> searchUsers(
        com.example.messenger.proto.SearchUsersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchUsersMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.ListConversationsResponse> listConversations(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListConversationsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.IdentityKey> publishIdentityKey(
        com.example.messenger.proto.PublishIdentityKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishIdentityKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.PrekeyBundle> publishPrekeyBundle(
        com.example.messenger.proto.PublishPrekeyBundleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPublishPrekeyBundleMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.HistoryArchiveHeader> initializeHistoryArchive(
        com.example.messenger.proto.InitializeHistoryArchiveRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getInitializeHistoryArchiveMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.HistoryArchiveHeader> getHistoryArchiveHeader(
        com.google.protobuf.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetHistoryArchiveHeaderMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.GetArchivePublicKeysResponse> getArchivePublicKeys(
        com.example.messenger.proto.GetArchivePublicKeysRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetArchivePublicKeysMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.IdentityKey> getIdentityKey(
        com.example.messenger.proto.GetIdentityKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetIdentityKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.GetIdentityKeysResponse> getIdentityKeys(
        com.example.messenger.proto.GetIdentityKeysRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetIdentityKeysMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.PrekeyBundle> acquirePrekeyBundle(
        com.example.messenger.proto.AcquirePrekeyBundleRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAcquirePrekeyBundleMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.AcquirePrekeyBundlesResponse> acquirePrekeyBundles(
        com.example.messenger.proto.AcquirePrekeyBundlesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAcquirePrekeyBundlesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.Conversation> createGroupConversation(
        com.example.messenger.proto.CreateGroupConversationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateGroupConversationMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.Conversation> addGroupMembers(
        com.example.messenger.proto.AddGroupMembersRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAddGroupMembersMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.Conversation> removeGroupMember(
        com.example.messenger.proto.RemoveGroupMemberRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRemoveGroupMemberMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> leaveGroupConversation(
        com.example.messenger.proto.LeaveGroupConversationRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getLeaveGroupConversationMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.Conversation> transferGroupAdmin(
        com.example.messenger.proto.TransferGroupAdminRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getTransferGroupAdminMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.ConversationKey> upsertConversationKey(
        com.example.messenger.proto.UpsertConversationKeyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUpsertConversationKeyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.ConversationKey> getConversationKey(
        com.example.messenger.proto.GetConversationKeyRequest request) {
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
  private static final int METHODID_INITIALIZE_HISTORY_ARCHIVE = 6;
  private static final int METHODID_GET_HISTORY_ARCHIVE_HEADER = 7;
  private static final int METHODID_GET_ARCHIVE_PUBLIC_KEYS = 8;
  private static final int METHODID_GET_IDENTITY_KEY = 9;
  private static final int METHODID_GET_IDENTITY_KEYS = 10;
  private static final int METHODID_ACQUIRE_PREKEY_BUNDLE = 11;
  private static final int METHODID_ACQUIRE_PREKEY_BUNDLES = 12;
  private static final int METHODID_CREATE_GROUP_CONVERSATION = 13;
  private static final int METHODID_ADD_GROUP_MEMBERS = 14;
  private static final int METHODID_REMOVE_GROUP_MEMBER = 15;
  private static final int METHODID_LEAVE_GROUP_CONVERSATION = 16;
  private static final int METHODID_TRANSFER_GROUP_ADMIN = 17;
  private static final int METHODID_UPSERT_CONVERSATION_KEY = 18;
  private static final int METHODID_GET_CONVERSATION_KEY = 19;

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
          serviceImpl.getProfile((com.example.messenger.proto.GetProfileRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.Profile>) responseObserver);
          break;
        case METHODID_UPDATE_PROFILE:
          serviceImpl.updateProfile((com.example.messenger.proto.UpdateProfileRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.Profile>) responseObserver);
          break;
        case METHODID_SEARCH_USERS:
          serviceImpl.searchUsers((com.example.messenger.proto.SearchUsersRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.SearchUsersResponse>) responseObserver);
          break;
        case METHODID_LIST_CONVERSATIONS:
          serviceImpl.listConversations((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.ListConversationsResponse>) responseObserver);
          break;
        case METHODID_PUBLISH_IDENTITY_KEY:
          serviceImpl.publishIdentityKey((com.example.messenger.proto.PublishIdentityKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.IdentityKey>) responseObserver);
          break;
        case METHODID_PUBLISH_PREKEY_BUNDLE:
          serviceImpl.publishPrekeyBundle((com.example.messenger.proto.PublishPrekeyBundleRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.PrekeyBundle>) responseObserver);
          break;
        case METHODID_INITIALIZE_HISTORY_ARCHIVE:
          serviceImpl.initializeHistoryArchive((com.example.messenger.proto.InitializeHistoryArchiveRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.HistoryArchiveHeader>) responseObserver);
          break;
        case METHODID_GET_HISTORY_ARCHIVE_HEADER:
          serviceImpl.getHistoryArchiveHeader((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.HistoryArchiveHeader>) responseObserver);
          break;
        case METHODID_GET_ARCHIVE_PUBLIC_KEYS:
          serviceImpl.getArchivePublicKeys((com.example.messenger.proto.GetArchivePublicKeysRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.GetArchivePublicKeysResponse>) responseObserver);
          break;
        case METHODID_GET_IDENTITY_KEY:
          serviceImpl.getIdentityKey((com.example.messenger.proto.GetIdentityKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.IdentityKey>) responseObserver);
          break;
        case METHODID_GET_IDENTITY_KEYS:
          serviceImpl.getIdentityKeys((com.example.messenger.proto.GetIdentityKeysRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.GetIdentityKeysResponse>) responseObserver);
          break;
        case METHODID_ACQUIRE_PREKEY_BUNDLE:
          serviceImpl.acquirePrekeyBundle((com.example.messenger.proto.AcquirePrekeyBundleRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.PrekeyBundle>) responseObserver);
          break;
        case METHODID_ACQUIRE_PREKEY_BUNDLES:
          serviceImpl.acquirePrekeyBundles((com.example.messenger.proto.AcquirePrekeyBundlesRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.AcquirePrekeyBundlesResponse>) responseObserver);
          break;
        case METHODID_CREATE_GROUP_CONVERSATION:
          serviceImpl.createGroupConversation((com.example.messenger.proto.CreateGroupConversationRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation>) responseObserver);
          break;
        case METHODID_ADD_GROUP_MEMBERS:
          serviceImpl.addGroupMembers((com.example.messenger.proto.AddGroupMembersRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation>) responseObserver);
          break;
        case METHODID_REMOVE_GROUP_MEMBER:
          serviceImpl.removeGroupMember((com.example.messenger.proto.RemoveGroupMemberRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation>) responseObserver);
          break;
        case METHODID_LEAVE_GROUP_CONVERSATION:
          serviceImpl.leaveGroupConversation((com.example.messenger.proto.LeaveGroupConversationRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_TRANSFER_GROUP_ADMIN:
          serviceImpl.transferGroupAdmin((com.example.messenger.proto.TransferGroupAdminRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.Conversation>) responseObserver);
          break;
        case METHODID_UPSERT_CONVERSATION_KEY:
          serviceImpl.upsertConversationKey((com.example.messenger.proto.UpsertConversationKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.ConversationKey>) responseObserver);
          break;
        case METHODID_GET_CONVERSATION_KEY:
          serviceImpl.getConversationKey((com.example.messenger.proto.GetConversationKeyRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.ConversationKey>) responseObserver);
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
              com.example.messenger.proto.GetProfileRequest,
              com.example.messenger.proto.Profile>(
                service, METHODID_GET_PROFILE)))
        .addMethod(
          getUpdateProfileMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.UpdateProfileRequest,
              com.example.messenger.proto.Profile>(
                service, METHODID_UPDATE_PROFILE)))
        .addMethod(
          getSearchUsersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.SearchUsersRequest,
              com.example.messenger.proto.SearchUsersResponse>(
                service, METHODID_SEARCH_USERS)))
        .addMethod(
          getListConversationsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.example.messenger.proto.ListConversationsResponse>(
                service, METHODID_LIST_CONVERSATIONS)))
        .addMethod(
          getPublishIdentityKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.PublishIdentityKeyRequest,
              com.example.messenger.proto.IdentityKey>(
                service, METHODID_PUBLISH_IDENTITY_KEY)))
        .addMethod(
          getPublishPrekeyBundleMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.PublishPrekeyBundleRequest,
              com.example.messenger.proto.PrekeyBundle>(
                service, METHODID_PUBLISH_PREKEY_BUNDLE)))
        .addMethod(
          getInitializeHistoryArchiveMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.InitializeHistoryArchiveRequest,
              com.example.messenger.proto.HistoryArchiveHeader>(
                service, METHODID_INITIALIZE_HISTORY_ARCHIVE)))
        .addMethod(
          getGetHistoryArchiveHeaderMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.google.protobuf.Empty,
              com.example.messenger.proto.HistoryArchiveHeader>(
                service, METHODID_GET_HISTORY_ARCHIVE_HEADER)))
        .addMethod(
          getGetArchivePublicKeysMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.GetArchivePublicKeysRequest,
              com.example.messenger.proto.GetArchivePublicKeysResponse>(
                service, METHODID_GET_ARCHIVE_PUBLIC_KEYS)))
        .addMethod(
          getGetIdentityKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.GetIdentityKeyRequest,
              com.example.messenger.proto.IdentityKey>(
                service, METHODID_GET_IDENTITY_KEY)))
        .addMethod(
          getGetIdentityKeysMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.GetIdentityKeysRequest,
              com.example.messenger.proto.GetIdentityKeysResponse>(
                service, METHODID_GET_IDENTITY_KEYS)))
        .addMethod(
          getAcquirePrekeyBundleMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.AcquirePrekeyBundleRequest,
              com.example.messenger.proto.PrekeyBundle>(
                service, METHODID_ACQUIRE_PREKEY_BUNDLE)))
        .addMethod(
          getAcquirePrekeyBundlesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.AcquirePrekeyBundlesRequest,
              com.example.messenger.proto.AcquirePrekeyBundlesResponse>(
                service, METHODID_ACQUIRE_PREKEY_BUNDLES)))
        .addMethod(
          getCreateGroupConversationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.CreateGroupConversationRequest,
              com.example.messenger.proto.Conversation>(
                service, METHODID_CREATE_GROUP_CONVERSATION)))
        .addMethod(
          getAddGroupMembersMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.AddGroupMembersRequest,
              com.example.messenger.proto.Conversation>(
                service, METHODID_ADD_GROUP_MEMBERS)))
        .addMethod(
          getRemoveGroupMemberMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.RemoveGroupMemberRequest,
              com.example.messenger.proto.Conversation>(
                service, METHODID_REMOVE_GROUP_MEMBER)))
        .addMethod(
          getLeaveGroupConversationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.LeaveGroupConversationRequest,
              com.google.protobuf.Empty>(
                service, METHODID_LEAVE_GROUP_CONVERSATION)))
        .addMethod(
          getTransferGroupAdminMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.TransferGroupAdminRequest,
              com.example.messenger.proto.Conversation>(
                service, METHODID_TRANSFER_GROUP_ADMIN)))
        .addMethod(
          getUpsertConversationKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.UpsertConversationKeyRequest,
              com.example.messenger.proto.ConversationKey>(
                service, METHODID_UPSERT_CONVERSATION_KEY)))
        .addMethod(
          getGetConversationKeyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.GetConversationKeyRequest,
              com.example.messenger.proto.ConversationKey>(
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
              .addMethod(getInitializeHistoryArchiveMethod())
              .addMethod(getGetHistoryArchiveHeaderMethod())
              .addMethod(getGetArchivePublicKeysMethod())
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
