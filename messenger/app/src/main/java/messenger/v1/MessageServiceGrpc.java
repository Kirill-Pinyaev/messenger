package messenger.v1;

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
  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.SendMessageRequest,
      messenger.v1.Messenger.Message> getSendMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SendMessage",
      requestType = messenger.v1.Messenger.SendMessageRequest.class,
      responseType = messenger.v1.Messenger.Message.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.SendMessageRequest,
      messenger.v1.Messenger.Message> getSendMessageMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.SendMessageRequest, messenger.v1.Messenger.Message> getSendMessageMethod;
    if ((getSendMessageMethod = MessageServiceGrpc.getSendMessageMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getSendMessageMethod = MessageServiceGrpc.getSendMessageMethod) == null) {
          MessageServiceGrpc.getSendMessageMethod = getSendMessageMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.SendMessageRequest, messenger.v1.Messenger.Message>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.SendMessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.Message.getDefaultInstance()))
              .build();
        }
      }
    }
    return getSendMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.GetMessagesRequest,
      messenger.v1.Messenger.GetMessagesResponse> getGetMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMessages",
      requestType = messenger.v1.Messenger.GetMessagesRequest.class,
      responseType = messenger.v1.Messenger.GetMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.GetMessagesRequest,
      messenger.v1.Messenger.GetMessagesResponse> getGetMessagesMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.GetMessagesRequest, messenger.v1.Messenger.GetMessagesResponse> getGetMessagesMethod;
    if ((getGetMessagesMethod = MessageServiceGrpc.getGetMessagesMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getGetMessagesMethod = MessageServiceGrpc.getGetMessagesMethod) == null) {
          MessageServiceGrpc.getGetMessagesMethod = getGetMessagesMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.GetMessagesRequest, messenger.v1.Messenger.GetMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetMessagesResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetMessagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.SearchMessagesRequest,
      messenger.v1.Messenger.SearchMessagesResponse> getSearchMessagesMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "SearchMessages",
      requestType = messenger.v1.Messenger.SearchMessagesRequest.class,
      responseType = messenger.v1.Messenger.SearchMessagesResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.SearchMessagesRequest,
      messenger.v1.Messenger.SearchMessagesResponse> getSearchMessagesMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.SearchMessagesRequest, messenger.v1.Messenger.SearchMessagesResponse> getSearchMessagesMethod;
    if ((getSearchMessagesMethod = MessageServiceGrpc.getSearchMessagesMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getSearchMessagesMethod = MessageServiceGrpc.getSearchMessagesMethod) == null) {
          MessageServiceGrpc.getSearchMessagesMethod = getSearchMessagesMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.SearchMessagesRequest, messenger.v1.Messenger.SearchMessagesResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SearchMessages"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.SearchMessagesRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.SearchMessagesResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getSearchMessagesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.DeleteMessageRequest,
      com.google.protobuf.Empty> getDeleteMessageMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteMessage",
      requestType = messenger.v1.Messenger.DeleteMessageRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.DeleteMessageRequest,
      com.google.protobuf.Empty> getDeleteMessageMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.DeleteMessageRequest, com.google.protobuf.Empty> getDeleteMessageMethod;
    if ((getDeleteMessageMethod = MessageServiceGrpc.getDeleteMessageMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getDeleteMessageMethod = MessageServiceGrpc.getDeleteMessageMethod) == null) {
          MessageServiceGrpc.getDeleteMessageMethod = getDeleteMessageMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.DeleteMessageRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteMessage"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.DeleteMessageRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .build();
        }
      }
    }
    return getDeleteMessageMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.PrepareMediaUploadRequest,
      messenger.v1.Messenger.PrepareMediaUploadResponse> getPrepareMediaUploadMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PrepareMediaUpload",
      requestType = messenger.v1.Messenger.PrepareMediaUploadRequest.class,
      responseType = messenger.v1.Messenger.PrepareMediaUploadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.PrepareMediaUploadRequest,
      messenger.v1.Messenger.PrepareMediaUploadResponse> getPrepareMediaUploadMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.PrepareMediaUploadRequest, messenger.v1.Messenger.PrepareMediaUploadResponse> getPrepareMediaUploadMethod;
    if ((getPrepareMediaUploadMethod = MessageServiceGrpc.getPrepareMediaUploadMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getPrepareMediaUploadMethod = MessageServiceGrpc.getPrepareMediaUploadMethod) == null) {
          MessageServiceGrpc.getPrepareMediaUploadMethod = getPrepareMediaUploadMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.PrepareMediaUploadRequest, messenger.v1.Messenger.PrepareMediaUploadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PrepareMediaUpload"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.PrepareMediaUploadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.PrepareMediaUploadResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getPrepareMediaUploadMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.UploadMediaRequest,
      messenger.v1.Messenger.UploadMediaResponse> getUploadMediaMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "UploadMedia",
      requestType = messenger.v1.Messenger.UploadMediaRequest.class,
      responseType = messenger.v1.Messenger.UploadMediaResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.UploadMediaRequest,
      messenger.v1.Messenger.UploadMediaResponse> getUploadMediaMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.UploadMediaRequest, messenger.v1.Messenger.UploadMediaResponse> getUploadMediaMethod;
    if ((getUploadMediaMethod = MessageServiceGrpc.getUploadMediaMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getUploadMediaMethod = MessageServiceGrpc.getUploadMediaMethod) == null) {
          MessageServiceGrpc.getUploadMediaMethod = getUploadMediaMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.UploadMediaRequest, messenger.v1.Messenger.UploadMediaResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "UploadMedia"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.UploadMediaRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.UploadMediaResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getUploadMediaMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.GetMediaRequest,
      messenger.v1.Messenger.GetMediaResponse> getGetMediaMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetMedia",
      requestType = messenger.v1.Messenger.GetMediaRequest.class,
      responseType = messenger.v1.Messenger.GetMediaResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.GetMediaRequest,
      messenger.v1.Messenger.GetMediaResponse> getGetMediaMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.GetMediaRequest, messenger.v1.Messenger.GetMediaResponse> getGetMediaMethod;
    if ((getGetMediaMethod = MessageServiceGrpc.getGetMediaMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getGetMediaMethod = MessageServiceGrpc.getGetMediaMethod) == null) {
          MessageServiceGrpc.getGetMediaMethod = getGetMediaMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.GetMediaRequest, messenger.v1.Messenger.GetMediaResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetMedia"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetMediaRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.GetMediaResponse.getDefaultInstance()))
              .build();
        }
      }
    }
    return getGetMediaMethod;
  }

  private static volatile io.grpc.MethodDescriptor<messenger.v1.Messenger.StreamEventsRequest,
      messenger.v1.Messenger.ServerEvent> getStreamEventsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "StreamEvents",
      requestType = messenger.v1.Messenger.StreamEventsRequest.class,
      responseType = messenger.v1.Messenger.ServerEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<messenger.v1.Messenger.StreamEventsRequest,
      messenger.v1.Messenger.ServerEvent> getStreamEventsMethod() {
    io.grpc.MethodDescriptor<messenger.v1.Messenger.StreamEventsRequest, messenger.v1.Messenger.ServerEvent> getStreamEventsMethod;
    if ((getStreamEventsMethod = MessageServiceGrpc.getStreamEventsMethod) == null) {
      synchronized (MessageServiceGrpc.class) {
        if ((getStreamEventsMethod = MessageServiceGrpc.getStreamEventsMethod) == null) {
          MessageServiceGrpc.getStreamEventsMethod = getStreamEventsMethod =
              io.grpc.MethodDescriptor.<messenger.v1.Messenger.StreamEventsRequest, messenger.v1.Messenger.ServerEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "StreamEvents"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.StreamEventsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.lite.ProtoLiteUtils.marshaller(
                  messenger.v1.Messenger.ServerEvent.getDefaultInstance()))
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
    default void sendMessage(messenger.v1.Messenger.SendMessageRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Message> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendMessageMethod(), responseObserver);
    }

    /**
     */
    default void getMessages(messenger.v1.Messenger.GetMessagesRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMessagesMethod(), responseObserver);
    }

    /**
     */
    default void searchMessages(messenger.v1.Messenger.SearchMessagesRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.SearchMessagesResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSearchMessagesMethod(), responseObserver);
    }

    /**
     */
    default void deleteMessage(messenger.v1.Messenger.DeleteMessageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMessageMethod(), responseObserver);
    }

    /**
     */
    default void prepareMediaUpload(messenger.v1.Messenger.PrepareMediaUploadRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrepareMediaUploadResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPrepareMediaUploadMethod(), responseObserver);
    }

    /**
     */
    default void uploadMedia(messenger.v1.Messenger.UploadMediaRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.UploadMediaResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getUploadMediaMethod(), responseObserver);
    }

    /**
     */
    default void getMedia(messenger.v1.Messenger.GetMediaRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetMediaResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMediaMethod(), responseObserver);
    }

    /**
     */
    default void streamEvents(messenger.v1.Messenger.StreamEventsRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.ServerEvent> responseObserver) {
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
    public void sendMessage(messenger.v1.Messenger.SendMessageRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.Message> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getMessages(messenger.v1.Messenger.GetMessagesRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMessagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void searchMessages(messenger.v1.Messenger.SearchMessagesRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.SearchMessagesResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSearchMessagesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteMessage(messenger.v1.Messenger.DeleteMessageRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMessageMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void prepareMediaUpload(messenger.v1.Messenger.PrepareMediaUploadRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrepareMediaUploadResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPrepareMediaUploadMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void uploadMedia(messenger.v1.Messenger.UploadMediaRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.UploadMediaResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getUploadMediaMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getMedia(messenger.v1.Messenger.GetMediaRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetMediaResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMediaMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void streamEvents(messenger.v1.Messenger.StreamEventsRequest request,
        io.grpc.stub.StreamObserver<messenger.v1.Messenger.ServerEvent> responseObserver) {
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
    public messenger.v1.Messenger.Message sendMessage(messenger.v1.Messenger.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendMessageMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.GetMessagesResponse getMessages(messenger.v1.Messenger.GetMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMessagesMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.SearchMessagesResponse searchMessages(messenger.v1.Messenger.SearchMessagesRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSearchMessagesMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.google.protobuf.Empty deleteMessage(messenger.v1.Messenger.DeleteMessageRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMessageMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.PrepareMediaUploadResponse prepareMediaUpload(messenger.v1.Messenger.PrepareMediaUploadRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPrepareMediaUploadMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.UploadMediaResponse uploadMedia(messenger.v1.Messenger.UploadMediaRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getUploadMediaMethod(), getCallOptions(), request);
    }

    /**
     */
    public messenger.v1.Messenger.GetMediaResponse getMedia(messenger.v1.Messenger.GetMediaRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMediaMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<messenger.v1.Messenger.ServerEvent> streamEvents(
        messenger.v1.Messenger.StreamEventsRequest request) {
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
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.Message> sendMessage(
        messenger.v1.Messenger.SendMessageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendMessageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.GetMessagesResponse> getMessages(
        messenger.v1.Messenger.GetMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMessagesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.SearchMessagesResponse> searchMessages(
        messenger.v1.Messenger.SearchMessagesRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSearchMessagesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> deleteMessage(
        messenger.v1.Messenger.DeleteMessageRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMessageMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.PrepareMediaUploadResponse> prepareMediaUpload(
        messenger.v1.Messenger.PrepareMediaUploadRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPrepareMediaUploadMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.UploadMediaResponse> uploadMedia(
        messenger.v1.Messenger.UploadMediaRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getUploadMediaMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<messenger.v1.Messenger.GetMediaResponse> getMedia(
        messenger.v1.Messenger.GetMediaRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMediaMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_MESSAGE = 0;
  private static final int METHODID_GET_MESSAGES = 1;
  private static final int METHODID_SEARCH_MESSAGES = 2;
  private static final int METHODID_DELETE_MESSAGE = 3;
  private static final int METHODID_PREPARE_MEDIA_UPLOAD = 4;
  private static final int METHODID_UPLOAD_MEDIA = 5;
  private static final int METHODID_GET_MEDIA = 6;
  private static final int METHODID_STREAM_EVENTS = 7;

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
          serviceImpl.sendMessage((messenger.v1.Messenger.SendMessageRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.Message>) responseObserver);
          break;
        case METHODID_GET_MESSAGES:
          serviceImpl.getMessages((messenger.v1.Messenger.GetMessagesRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetMessagesResponse>) responseObserver);
          break;
        case METHODID_SEARCH_MESSAGES:
          serviceImpl.searchMessages((messenger.v1.Messenger.SearchMessagesRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.SearchMessagesResponse>) responseObserver);
          break;
        case METHODID_DELETE_MESSAGE:
          serviceImpl.deleteMessage((messenger.v1.Messenger.DeleteMessageRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_PREPARE_MEDIA_UPLOAD:
          serviceImpl.prepareMediaUpload((messenger.v1.Messenger.PrepareMediaUploadRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.PrepareMediaUploadResponse>) responseObserver);
          break;
        case METHODID_UPLOAD_MEDIA:
          serviceImpl.uploadMedia((messenger.v1.Messenger.UploadMediaRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.UploadMediaResponse>) responseObserver);
          break;
        case METHODID_GET_MEDIA:
          serviceImpl.getMedia((messenger.v1.Messenger.GetMediaRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.GetMediaResponse>) responseObserver);
          break;
        case METHODID_STREAM_EVENTS:
          serviceImpl.streamEvents((messenger.v1.Messenger.StreamEventsRequest) request,
              (io.grpc.stub.StreamObserver<messenger.v1.Messenger.ServerEvent>) responseObserver);
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
              messenger.v1.Messenger.SendMessageRequest,
              messenger.v1.Messenger.Message>(
                service, METHODID_SEND_MESSAGE)))
        .addMethod(
          getGetMessagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.GetMessagesRequest,
              messenger.v1.Messenger.GetMessagesResponse>(
                service, METHODID_GET_MESSAGES)))
        .addMethod(
          getSearchMessagesMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.SearchMessagesRequest,
              messenger.v1.Messenger.SearchMessagesResponse>(
                service, METHODID_SEARCH_MESSAGES)))
        .addMethod(
          getDeleteMessageMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.DeleteMessageRequest,
              com.google.protobuf.Empty>(
                service, METHODID_DELETE_MESSAGE)))
        .addMethod(
          getPrepareMediaUploadMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.PrepareMediaUploadRequest,
              messenger.v1.Messenger.PrepareMediaUploadResponse>(
                service, METHODID_PREPARE_MEDIA_UPLOAD)))
        .addMethod(
          getUploadMediaMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.UploadMediaRequest,
              messenger.v1.Messenger.UploadMediaResponse>(
                service, METHODID_UPLOAD_MEDIA)))
        .addMethod(
          getGetMediaMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              messenger.v1.Messenger.GetMediaRequest,
              messenger.v1.Messenger.GetMediaResponse>(
                service, METHODID_GET_MEDIA)))
        .addMethod(
          getStreamEventsMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              messenger.v1.Messenger.StreamEventsRequest,
              messenger.v1.Messenger.ServerEvent>(
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
