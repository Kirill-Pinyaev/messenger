package com.example.messenger.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: messenger/v1/messenger.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class MessageServiceGrpc {

  private MessageServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "messenger.v1.MessageService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.SendMessageRequest,
      com.example.messenger.proto.Message> getSendMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendMessage",
      requestType = com.example.messenger.proto.SendMessageRequest.class,
      responseType = com.example.messenger.proto.Message.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.SendMessageRequest,
      com.example.messenger.proto.Message> getSendMessageMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.SendMessageRequest, com.example.messenger.proto.Message> getSendMessageMethod;
    if ((getSendMessageMethod = MessageServiceGrpc.getSendMessageMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getSendMessageMethod = MessageServiceGrpc.getSendMessageMethod) == null) {
          MessageServiceGrpc.getSendMessageMethod = getSendMessageMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.SendMessageRequest, com.example.messenger.proto.Message>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.SendMessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.Message.getDefaultInstance()))
              .build();
        }
      }
    }
    return getSendMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.GetMessagesRequest,
      com.example.messenger.proto.GetMessagesResponse> getGetMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMessages",
      requestType = com.example.messenger.proto.GetMessagesRequest.class,
      responseType = com.example.messenger.proto.GetMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.GetMessagesRequest,
      com.example.messenger.proto.GetMessagesResponse> getGetMessagesMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.GetMessagesRequest, com.example.messenger.proto.GetMessagesResponse> getGetMessagesMethod;
    if ((getGetMessagesMethod = MessageServiceGrpc.getGetMessagesMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getGetMessagesMethod = MessageServiceGrpc.getGetMessagesMethod) == null) {
          MessageServiceGrpc.getGetMessagesMethod = getGetMessagesMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.GetMessagesRequest, com.example.messenger.proto.GetMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetMessagesResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetMessagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.SearchMessagesRequest,
      com.example.messenger.proto.SearchMessagesResponse> getSearchMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchMessages",
      requestType = com.example.messenger.proto.SearchMessagesRequest.class,
      responseType = com.example.messenger.proto.SearchMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.SearchMessagesRequest,
      com.example.messenger.proto.SearchMessagesResponse> getSearchMessagesMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.SearchMessagesRequest, com.example.messenger.proto.SearchMessagesResponse> getSearchMessagesMethod;
    if ((getSearchMessagesMethod = MessageServiceGrpc.getSearchMessagesMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getSearchMessagesMethod = MessageServiceGrpc.getSearchMessagesMethod) == null) {
          MessageServiceGrpc.getSearchMessagesMethod = getSearchMessagesMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.SearchMessagesRequest, com.example.messenger.proto.SearchMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.SearchMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.SearchMessagesResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getSearchMessagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.DeleteMessageRequest,
      com.google.protobuf.Empty> getDeleteMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteMessage",
      requestType = com.example.messenger.proto.DeleteMessageRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.DeleteMessageRequest,
      com.google.protobuf.Empty> getDeleteMessageMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.DeleteMessageRequest, com.google.protobuf.Empty> getDeleteMessageMethod;
    if ((getDeleteMessageMethod = MessageServiceGrpc.getDeleteMessageMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getDeleteMessageMethod = MessageServiceGrpc.getDeleteMessageMethod) == null) {
          MessageServiceGrpc.getDeleteMessageMethod = getDeleteMessageMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.DeleteMessageRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.DeleteMessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .build();
        }
      }
    }
    return getDeleteMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.AppendHistoryArchiveRecordsRequest,
      com.google.protobuf.Empty> getAppendHistoryArchiveRecordsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "AppendHistoryArchiveRecords",
      requestType = com.example.messenger.proto.AppendHistoryArchiveRecordsRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.AppendHistoryArchiveRecordsRequest,
      com.google.protobuf.Empty> getAppendHistoryArchiveRecordsMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.AppendHistoryArchiveRecordsRequest, com.google.protobuf.Empty> getAppendHistoryArchiveRecordsMethod;
    if ((getAppendHistoryArchiveRecordsMethod = MessageServiceGrpc.getAppendHistoryArchiveRecordsMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getAppendHistoryArchiveRecordsMethod = MessageServiceGrpc.getAppendHistoryArchiveRecordsMethod) == null) {
          MessageServiceGrpc.getAppendHistoryArchiveRecordsMethod = getAppendHistoryArchiveRecordsMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.AppendHistoryArchiveRecordsRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AppendHistoryArchiveRecords"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.AppendHistoryArchiveRecordsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .build();
        }
      }
    }
    return getAppendHistoryArchiveRecordsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.ListHistoryArchiveRecordsRequest,
      com.example.messenger.proto.ListHistoryArchiveRecordsResponse> getListHistoryArchiveRecordsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListHistoryArchiveRecords",
      requestType = com.example.messenger.proto.ListHistoryArchiveRecordsRequest.class,
      responseType = com.example.messenger.proto.ListHistoryArchiveRecordsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.ListHistoryArchiveRecordsRequest,
      com.example.messenger.proto.ListHistoryArchiveRecordsResponse> getListHistoryArchiveRecordsMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.ListHistoryArchiveRecordsRequest, com.example.messenger.proto.ListHistoryArchiveRecordsResponse> getListHistoryArchiveRecordsMethod;
    if ((getListHistoryArchiveRecordsMethod = MessageServiceGrpc.getListHistoryArchiveRecordsMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getListHistoryArchiveRecordsMethod = MessageServiceGrpc.getListHistoryArchiveRecordsMethod) == null) {
          MessageServiceGrpc.getListHistoryArchiveRecordsMethod = getListHistoryArchiveRecordsMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.ListHistoryArchiveRecordsRequest, com.example.messenger.proto.ListHistoryArchiveRecordsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListHistoryArchiveRecords"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.ListHistoryArchiveRecordsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.ListHistoryArchiveRecordsResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getListHistoryArchiveRecordsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.PrepareMediaUploadRequest,
      com.example.messenger.proto.PrepareMediaUploadResponse> getPrepareMediaUploadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PrepareMediaUpload",
      requestType = com.example.messenger.proto.PrepareMediaUploadRequest.class,
      responseType = com.example.messenger.proto.PrepareMediaUploadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.PrepareMediaUploadRequest,
      com.example.messenger.proto.PrepareMediaUploadResponse> getPrepareMediaUploadMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.PrepareMediaUploadRequest, com.example.messenger.proto.PrepareMediaUploadResponse> getPrepareMediaUploadMethod;
    if ((getPrepareMediaUploadMethod = MessageServiceGrpc.getPrepareMediaUploadMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getPrepareMediaUploadMethod = MessageServiceGrpc.getPrepareMediaUploadMethod) == null) {
          MessageServiceGrpc.getPrepareMediaUploadMethod = getPrepareMediaUploadMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.PrepareMediaUploadRequest, com.example.messenger.proto.PrepareMediaUploadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PrepareMediaUpload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.PrepareMediaUploadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.PrepareMediaUploadResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getPrepareMediaUploadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.UploadMediaRequest,
      com.example.messenger.proto.UploadMediaResponse> getUploadMediaMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UploadMedia",
      requestType = com.example.messenger.proto.UploadMediaRequest.class,
      responseType = com.example.messenger.proto.UploadMediaResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.UploadMediaRequest,
      com.example.messenger.proto.UploadMediaResponse> getUploadMediaMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.UploadMediaRequest, com.example.messenger.proto.UploadMediaResponse> getUploadMediaMethod;
    if ((getUploadMediaMethod = MessageServiceGrpc.getUploadMediaMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getUploadMediaMethod = MessageServiceGrpc.getUploadMediaMethod) == null) {
          MessageServiceGrpc.getUploadMediaMethod = getUploadMediaMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.UploadMediaRequest, com.example.messenger.proto.UploadMediaResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UploadMedia"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.UploadMediaRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.UploadMediaResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getUploadMediaMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.GetMediaRequest,
      com.example.messenger.proto.GetMediaResponse> getGetMediaMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMedia",
      requestType = com.example.messenger.proto.GetMediaRequest.class,
      responseType = com.example.messenger.proto.GetMediaResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.GetMediaRequest,
      com.example.messenger.proto.GetMediaResponse> getGetMediaMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.GetMediaRequest, com.example.messenger.proto.GetMediaResponse> getGetMediaMethod;
    if ((getGetMediaMethod = MessageServiceGrpc.getGetMediaMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getGetMediaMethod = MessageServiceGrpc.getGetMediaMethod) == null) {
          MessageServiceGrpc.getGetMediaMethod = getGetMediaMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.GetMediaRequest, com.example.messenger.proto.GetMediaResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMedia"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetMediaRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.GetMediaResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetMediaMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.example.messenger.proto.StreamEventsRequest,
      com.example.messenger.proto.ServerEvent> getStreamEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamEvents",
      requestType = com.example.messenger.proto.StreamEventsRequest.class,
      responseType = com.example.messenger.proto.ServerEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.example.messenger.proto.StreamEventsRequest,
      com.example.messenger.proto.ServerEvent> getStreamEventsMethod() {
    io.grpc.MethodDescriptor<com.example.messenger.proto.StreamEventsRequest, com.example.messenger.proto.ServerEvent> getStreamEventsMethod;
    if ((getStreamEventsMethod = MessageServiceGrpc.getStreamEventsMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getStreamEventsMethod = MessageServiceGrpc.getStreamEventsMethod) == null) {
          MessageServiceGrpc.getStreamEventsMethod = getStreamEventsMethod =
              io.grpc.MethodDescriptor.<com.example.messenger.proto.StreamEventsRequest, com.example.messenger.proto.ServerEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.StreamEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.example.messenger.proto.ServerEvent.getDefaultInstance()))
              .build();
        }
      }
    }
    return getStreamEventsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static MessageServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessageServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessageServiceStub>() {
        @java.lang.Override
        public MessageServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessageServiceStub(channel, callOptions);
        }
      };
    return MessageServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static MessageServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessageServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessageServiceBlockingStub>() {
        @java.lang.Override
        public MessageServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessageServiceBlockingStub(channel, callOptions);
        }
      };
    return MessageServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static MessageServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<MessageServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<MessageServiceFutureStub>() {
        @java.lang.Override
        public MessageServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new MessageServiceFutureStub(channel, callOptions);
        }
      };
    return MessageServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void sendMessage(com.example.messenger.proto.SendMessageRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Message> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendMessageMethod(), responseObserver);
    }

    /**
     */
    default void getMessages(com.example.messenger.proto.GetMessagesRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.GetMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMessagesMethod(), responseObserver);
    }

    /**
     */
    default void searchMessages(com.example.messenger.proto.SearchMessagesRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.SearchMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchMessagesMethod(), responseObserver);
    }

    /**
     */
    default void deleteMessage(com.example.messenger.proto.DeleteMessageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMessageMethod(), responseObserver);
    }

    /**
     */
    default void appendHistoryArchiveRecords(com.example.messenger.proto.AppendHistoryArchiveRecordsRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getAppendHistoryArchiveRecordsMethod(), responseObserver);
    }

    /**
     */
    default void listHistoryArchiveRecords(com.example.messenger.proto.ListHistoryArchiveRecordsRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ListHistoryArchiveRecordsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListHistoryArchiveRecordsMethod(), responseObserver);
    }

    /**
     */
    default void prepareMediaUpload(com.example.messenger.proto.PrepareMediaUploadRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.PrepareMediaUploadResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPrepareMediaUploadMethod(), responseObserver);
    }

    /**
     */
    default void uploadMedia(com.example.messenger.proto.UploadMediaRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.UploadMediaResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUploadMediaMethod(), responseObserver);
    }

    /**
     */
    default void getMedia(com.example.messenger.proto.GetMediaRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.GetMediaResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMediaMethod(), responseObserver);
    }

    /**
     */
    default void streamEvents(com.example.messenger.proto.StreamEventsRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ServerEvent> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getStreamEventsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service MessageService.
   */
  public static abstract class MessageServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return MessageServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service MessageService.
   */
  public static final class MessageServiceStub
      extends io.grpc.stub.AbstractAsyncStub<MessageServiceStub> {
    private MessageServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MessageServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessageServiceStub(channel, callOptions);
    }

    /**
     */
    public void sendMessage(com.example.messenger.proto.SendMessageRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.Message> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getMessages(com.example.messenger.proto.GetMessagesRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.GetMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMessagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void searchMessages(com.example.messenger.proto.SearchMessagesRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.SearchMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchMessagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteMessage(com.example.messenger.proto.DeleteMessageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void appendHistoryArchiveRecords(com.example.messenger.proto.AppendHistoryArchiveRecordsRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getAppendHistoryArchiveRecordsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listHistoryArchiveRecords(com.example.messenger.proto.ListHistoryArchiveRecordsRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ListHistoryArchiveRecordsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListHistoryArchiveRecordsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void prepareMediaUpload(com.example.messenger.proto.PrepareMediaUploadRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.PrepareMediaUploadResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPrepareMediaUploadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void uploadMedia(com.example.messenger.proto.UploadMediaRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.UploadMediaResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUploadMediaMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getMedia(com.example.messenger.proto.GetMediaRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.GetMediaResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMediaMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void streamEvents(com.example.messenger.proto.StreamEventsRequest request,
        io.grpc.stub.StreamObserver<com.example.messenger.proto.ServerEvent> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getStreamEventsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service MessageService.
   */
  public static final class MessageServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<MessageServiceBlockingStub> {
    private MessageServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MessageServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessageServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.example.messenger.proto.Message sendMessage(com.example.messenger.proto.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendMessageMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.GetMessagesResponse getMessages(com.example.messenger.proto.GetMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMessagesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.SearchMessagesResponse searchMessages(com.example.messenger.proto.SearchMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchMessagesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty deleteMessage(com.example.messenger.proto.DeleteMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMessageMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty appendHistoryArchiveRecords(com.example.messenger.proto.AppendHistoryArchiveRecordsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getAppendHistoryArchiveRecordsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.ListHistoryArchiveRecordsResponse listHistoryArchiveRecords(com.example.messenger.proto.ListHistoryArchiveRecordsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListHistoryArchiveRecordsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.PrepareMediaUploadResponse prepareMediaUpload(com.example.messenger.proto.PrepareMediaUploadRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPrepareMediaUploadMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.UploadMediaResponse uploadMedia(com.example.messenger.proto.UploadMediaRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUploadMediaMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.example.messenger.proto.GetMediaResponse getMedia(com.example.messenger.proto.GetMediaRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMediaMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<com.example.messenger.proto.ServerEvent> streamEvents(
        com.example.messenger.proto.StreamEventsRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getStreamEventsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service MessageService.
   */
  public static final class MessageServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<MessageServiceFutureStub> {
    private MessageServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected MessageServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new MessageServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.Message> sendMessage(
        com.example.messenger.proto.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.GetMessagesResponse> getMessages(
        com.example.messenger.proto.GetMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMessagesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.SearchMessagesResponse> searchMessages(
        com.example.messenger.proto.SearchMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchMessagesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> deleteMessage(
        com.example.messenger.proto.DeleteMessageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMessageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> appendHistoryArchiveRecords(
        com.example.messenger.proto.AppendHistoryArchiveRecordsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getAppendHistoryArchiveRecordsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.ListHistoryArchiveRecordsResponse> listHistoryArchiveRecords(
        com.example.messenger.proto.ListHistoryArchiveRecordsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListHistoryArchiveRecordsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.PrepareMediaUploadResponse> prepareMediaUpload(
        com.example.messenger.proto.PrepareMediaUploadRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPrepareMediaUploadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.UploadMediaResponse> uploadMedia(
        com.example.messenger.proto.UploadMediaRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUploadMediaMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.example.messenger.proto.GetMediaResponse> getMedia(
        com.example.messenger.proto.GetMediaRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMediaMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_MESSAGE = 0;
  private static final int METHODID_GET_MESSAGES = 1;
  private static final int METHODID_SEARCH_MESSAGES = 2;
  private static final int METHODID_DELETE_MESSAGE = 3;
  private static final int METHODID_APPEND_HISTORY_ARCHIVE_RECORDS = 4;
  private static final int METHODID_LIST_HISTORY_ARCHIVE_RECORDS = 5;
  private static final int METHODID_PREPARE_MEDIA_UPLOAD = 6;
  private static final int METHODID_UPLOAD_MEDIA = 7;
  private static final int METHODID_GET_MEDIA = 8;
  private static final int METHODID_STREAM_EVENTS = 9;

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
        case METHODID_SEND_MESSAGE:
          serviceImpl.sendMessage((com.example.messenger.proto.SendMessageRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.Message>) responseObserver);
          break;
        case METHODID_GET_MESSAGES:
          serviceImpl.getMessages((com.example.messenger.proto.GetMessagesRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.GetMessagesResponse>) responseObserver);
          break;
        case METHODID_SEARCH_MESSAGES:
          serviceImpl.searchMessages((com.example.messenger.proto.SearchMessagesRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.SearchMessagesResponse>) responseObserver);
          break;
        case METHODID_DELETE_MESSAGE:
          serviceImpl.deleteMessage((com.example.messenger.proto.DeleteMessageRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_APPEND_HISTORY_ARCHIVE_RECORDS:
          serviceImpl.appendHistoryArchiveRecords((com.example.messenger.proto.AppendHistoryArchiveRecordsRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_LIST_HISTORY_ARCHIVE_RECORDS:
          serviceImpl.listHistoryArchiveRecords((com.example.messenger.proto.ListHistoryArchiveRecordsRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.ListHistoryArchiveRecordsResponse>) responseObserver);
          break;
        case METHODID_PREPARE_MEDIA_UPLOAD:
          serviceImpl.prepareMediaUpload((com.example.messenger.proto.PrepareMediaUploadRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.PrepareMediaUploadResponse>) responseObserver);
          break;
        case METHODID_UPLOAD_MEDIA:
          serviceImpl.uploadMedia((com.example.messenger.proto.UploadMediaRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.UploadMediaResponse>) responseObserver);
          break;
        case METHODID_GET_MEDIA:
          serviceImpl.getMedia((com.example.messenger.proto.GetMediaRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.GetMediaResponse>) responseObserver);
          break;
        case METHODID_STREAM_EVENTS:
          serviceImpl.streamEvents((com.example.messenger.proto.StreamEventsRequest) request,
              (io.grpc.stub.StreamObserver<com.example.messenger.proto.ServerEvent>) responseObserver);
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
          getSendMessageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.SendMessageRequest,
              com.example.messenger.proto.Message>(
                service, METHODID_SEND_MESSAGE)))
        .addMethod(
          getGetMessagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.GetMessagesRequest,
              com.example.messenger.proto.GetMessagesResponse>(
                service, METHODID_GET_MESSAGES)))
        .addMethod(
          getSearchMessagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.SearchMessagesRequest,
              com.example.messenger.proto.SearchMessagesResponse>(
                service, METHODID_SEARCH_MESSAGES)))
        .addMethod(
          getDeleteMessageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.DeleteMessageRequest,
              com.google.protobuf.Empty>(
                service, METHODID_DELETE_MESSAGE)))
        .addMethod(
          getAppendHistoryArchiveRecordsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.AppendHistoryArchiveRecordsRequest,
              com.google.protobuf.Empty>(
                service, METHODID_APPEND_HISTORY_ARCHIVE_RECORDS)))
        .addMethod(
          getListHistoryArchiveRecordsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.ListHistoryArchiveRecordsRequest,
              com.example.messenger.proto.ListHistoryArchiveRecordsResponse>(
                service, METHODID_LIST_HISTORY_ARCHIVE_RECORDS)))
        .addMethod(
          getPrepareMediaUploadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.PrepareMediaUploadRequest,
              com.example.messenger.proto.PrepareMediaUploadResponse>(
                service, METHODID_PREPARE_MEDIA_UPLOAD)))
        .addMethod(
          getUploadMediaMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.UploadMediaRequest,
              com.example.messenger.proto.UploadMediaResponse>(
                service, METHODID_UPLOAD_MEDIA)))
        .addMethod(
          getGetMediaMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.example.messenger.proto.GetMediaRequest,
              com.example.messenger.proto.GetMediaResponse>(
                service, METHODID_GET_MEDIA)))
        .addMethod(
          getStreamEventsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.example.messenger.proto.StreamEventsRequest,
              com.example.messenger.proto.ServerEvent>(
                service, METHODID_STREAM_EVENTS)))
        .build();
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (MessageServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .addMethod(getSendMessageMethod())
              .addMethod(getGetMessagesMethod())
              .addMethod(getSearchMessagesMethod())
              .addMethod(getDeleteMessageMethod())
              .addMethod(getAppendHistoryArchiveRecordsMethod())
              .addMethod(getListHistoryArchiveRecordsMethod())
              .addMethod(getPrepareMediaUploadMethod())
              .addMethod(getUploadMediaMethod())
              .addMethod(getGetMediaMethod())
              .addMethod(getStreamEventsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
