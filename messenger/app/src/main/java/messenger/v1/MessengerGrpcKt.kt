package messenger.v1

import com.google.protobuf.Empty
import io.grpc.CallOptions
import io.grpc.CallOptions.DEFAULT
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServerServiceDefinition.builder
import io.grpc.ServiceDescriptor
import io.grpc.Status.UNIMPLEMENTED
import io.grpc.StatusException
import io.grpc.kotlin.AbstractCoroutineServerImpl
import io.grpc.kotlin.AbstractCoroutineStub
import io.grpc.kotlin.ClientCalls.serverStreamingRpc
import io.grpc.kotlin.ClientCalls.unaryRpc
import io.grpc.kotlin.ServerCalls.serverStreamingServerMethodDefinition
import io.grpc.kotlin.ServerCalls.unaryServerMethodDefinition
import io.grpc.kotlin.StubFor
import kotlin.String
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.Flow
import messenger.v1.AuthServiceGrpc.getServiceDescriptor as authServiceGrpcGetServiceDescriptor
import messenger.v1.MessageServiceGrpc.getServiceDescriptor as messageServiceGrpcGetServiceDescriptor
import messenger.v1.UserServiceGrpc.getServiceDescriptor as userServiceGrpcGetServiceDescriptor

/**
 * Holder for Kotlin coroutine-based client and server APIs for messenger.v1.AuthService.
 */
public object AuthServiceGrpcKt {
  public const val SERVICE_NAME: String = AuthServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = authServiceGrpcGetServiceDescriptor()

  public val registerMethod: MethodDescriptor<Messenger.RegisterRequest, Empty>
    @JvmStatic
    get() = AuthServiceGrpc.getRegisterMethod()

  public val loginMethod: MethodDescriptor<Messenger.LoginRequest, Messenger.LoginResponse>
    @JvmStatic
    get() = AuthServiceGrpc.getLoginMethod()

  public val deleteAccountMethod: MethodDescriptor<Empty, Empty>
    @JvmStatic
    get() = AuthServiceGrpc.getDeleteAccountMethod()

  /**
   * A stub for issuing RPCs to a(n) messenger.v1.AuthService service as suspending coroutines.
   */
  @StubFor(AuthServiceGrpc::class)
  public class AuthServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<AuthServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): AuthServiceCoroutineStub =
        AuthServiceCoroutineStub(channel, callOptions)

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun register(request: Messenger.RegisterRequest, headers: Metadata = Metadata()):
        Empty = unaryRpc(
      channel,
      AuthServiceGrpc.getRegisterMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun login(request: Messenger.LoginRequest, headers: Metadata = Metadata()):
        Messenger.LoginResponse = unaryRpc(
      channel,
      AuthServiceGrpc.getLoginMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun deleteAccount(request: Empty, headers: Metadata = Metadata()): Empty =
        unaryRpc(
      channel,
      AuthServiceGrpc.getDeleteAccountMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the messenger.v1.AuthService service based on Kotlin coroutines.
   */
  public abstract class AuthServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for messenger.v1.AuthService.Register.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun register(request: Messenger.RegisterRequest): Empty = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.AuthService.Register is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.AuthService.Login.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun login(request: Messenger.LoginRequest): Messenger.LoginResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.AuthService.Login is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.AuthService.DeleteAccount.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun deleteAccount(request: Empty): Empty = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.AuthService.DeleteAccount is unimplemented"))

    final override fun bindService(): ServerServiceDefinition =
        builder(authServiceGrpcGetServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getRegisterMethod(),
      implementation = ::register
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getLoginMethod(),
      implementation = ::login
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = AuthServiceGrpc.getDeleteAccountMethod(),
      implementation = ::deleteAccount
    )).build()
  }
}

/**
 * Holder for Kotlin coroutine-based client and server APIs for messenger.v1.UserService.
 */
public object UserServiceGrpcKt {
  public const val SERVICE_NAME: String = UserServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = userServiceGrpcGetServiceDescriptor()

  public val getProfileMethod: MethodDescriptor<Messenger.GetProfileRequest, Messenger.Profile>
    @JvmStatic
    get() = UserServiceGrpc.getGetProfileMethod()

  public val updateProfileMethod:
      MethodDescriptor<Messenger.UpdateProfileRequest, Messenger.Profile>
    @JvmStatic
    get() = UserServiceGrpc.getUpdateProfileMethod()

  public val searchUsersMethod:
      MethodDescriptor<Messenger.SearchUsersRequest, Messenger.SearchUsersResponse>
    @JvmStatic
    get() = UserServiceGrpc.getSearchUsersMethod()

  public val listConversationsMethod: MethodDescriptor<Empty, Messenger.ListConversationsResponse>
    @JvmStatic
    get() = UserServiceGrpc.getListConversationsMethod()

  public val publishIdentityKeyMethod:
      MethodDescriptor<Messenger.PublishIdentityKeyRequest, Messenger.IdentityKey>
    @JvmStatic
    get() = UserServiceGrpc.getPublishIdentityKeyMethod()

  public val publishPrekeyBundleMethod:
      MethodDescriptor<Messenger.PublishPrekeyBundleRequest, Messenger.PrekeyBundle>
    @JvmStatic
    get() = UserServiceGrpc.getPublishPrekeyBundleMethod()

  public val getIdentityKeyMethod:
      MethodDescriptor<Messenger.GetIdentityKeyRequest, Messenger.IdentityKey>
    @JvmStatic
    get() = UserServiceGrpc.getGetIdentityKeyMethod()

  public val getIdentityKeysMethod:
      MethodDescriptor<Messenger.GetIdentityKeysRequest, Messenger.GetIdentityKeysResponse>
    @JvmStatic
    get() = UserServiceGrpc.getGetIdentityKeysMethod()

  public val acquirePrekeyBundleMethod:
      MethodDescriptor<Messenger.AcquirePrekeyBundleRequest, Messenger.PrekeyBundle>
    @JvmStatic
    get() = UserServiceGrpc.getAcquirePrekeyBundleMethod()

  public val acquirePrekeyBundlesMethod:
      MethodDescriptor<Messenger.AcquirePrekeyBundlesRequest, Messenger.AcquirePrekeyBundlesResponse>
    @JvmStatic
    get() = UserServiceGrpc.getAcquirePrekeyBundlesMethod()

  public val createGroupConversationMethod:
      MethodDescriptor<Messenger.CreateGroupConversationRequest, Messenger.Conversation>
    @JvmStatic
    get() = UserServiceGrpc.getCreateGroupConversationMethod()

  public val addGroupMembersMethod:
      MethodDescriptor<Messenger.AddGroupMembersRequest, Messenger.Conversation>
    @JvmStatic
    get() = UserServiceGrpc.getAddGroupMembersMethod()

  public val removeGroupMemberMethod:
      MethodDescriptor<Messenger.RemoveGroupMemberRequest, Messenger.Conversation>
    @JvmStatic
    get() = UserServiceGrpc.getRemoveGroupMemberMethod()

  public val leaveGroupConversationMethod:
      MethodDescriptor<Messenger.LeaveGroupConversationRequest, Empty>
    @JvmStatic
    get() = UserServiceGrpc.getLeaveGroupConversationMethod()

  public val transferGroupAdminMethod:
      MethodDescriptor<Messenger.TransferGroupAdminRequest, Messenger.Conversation>
    @JvmStatic
    get() = UserServiceGrpc.getTransferGroupAdminMethod()

  public val upsertConversationKeyMethod:
      MethodDescriptor<Messenger.UpsertConversationKeyRequest, Messenger.ConversationKey>
    @JvmStatic
    get() = UserServiceGrpc.getUpsertConversationKeyMethod()

  public val getConversationKeyMethod:
      MethodDescriptor<Messenger.GetConversationKeyRequest, Messenger.ConversationKey>
    @JvmStatic
    get() = UserServiceGrpc.getGetConversationKeyMethod()

  /**
   * A stub for issuing RPCs to a(n) messenger.v1.UserService service as suspending coroutines.
   */
  @StubFor(UserServiceGrpc::class)
  public class UserServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<UserServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): UserServiceCoroutineStub =
        UserServiceCoroutineStub(channel, callOptions)

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getProfile(request: Messenger.GetProfileRequest, headers: Metadata =
        Metadata()): Messenger.Profile = unaryRpc(
      channel,
      UserServiceGrpc.getGetProfileMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun updateProfile(request: Messenger.UpdateProfileRequest, headers: Metadata =
        Metadata()): Messenger.Profile = unaryRpc(
      channel,
      UserServiceGrpc.getUpdateProfileMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun searchUsers(request: Messenger.SearchUsersRequest, headers: Metadata =
        Metadata()): Messenger.SearchUsersResponse = unaryRpc(
      channel,
      UserServiceGrpc.getSearchUsersMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun listConversations(request: Empty, headers: Metadata = Metadata()):
        Messenger.ListConversationsResponse = unaryRpc(
      channel,
      UserServiceGrpc.getListConversationsMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun publishIdentityKey(request: Messenger.PublishIdentityKeyRequest,
        headers: Metadata = Metadata()): Messenger.IdentityKey = unaryRpc(
      channel,
      UserServiceGrpc.getPublishIdentityKeyMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun publishPrekeyBundle(request: Messenger.PublishPrekeyBundleRequest,
        headers: Metadata = Metadata()): Messenger.PrekeyBundle = unaryRpc(
      channel,
      UserServiceGrpc.getPublishPrekeyBundleMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getIdentityKey(request: Messenger.GetIdentityKeyRequest, headers: Metadata =
        Metadata()): Messenger.IdentityKey = unaryRpc(
      channel,
      UserServiceGrpc.getGetIdentityKeyMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getIdentityKeys(request: Messenger.GetIdentityKeysRequest, headers: Metadata
        = Metadata()): Messenger.GetIdentityKeysResponse = unaryRpc(
      channel,
      UserServiceGrpc.getGetIdentityKeysMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun acquirePrekeyBundle(request: Messenger.AcquirePrekeyBundleRequest,
        headers: Metadata = Metadata()): Messenger.PrekeyBundle = unaryRpc(
      channel,
      UserServiceGrpc.getAcquirePrekeyBundleMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun acquirePrekeyBundles(request: Messenger.AcquirePrekeyBundlesRequest,
        headers: Metadata = Metadata()): Messenger.AcquirePrekeyBundlesResponse = unaryRpc(
      channel,
      UserServiceGrpc.getAcquirePrekeyBundlesMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun createGroupConversation(request: Messenger.CreateGroupConversationRequest,
        headers: Metadata = Metadata()): Messenger.Conversation = unaryRpc(
      channel,
      UserServiceGrpc.getCreateGroupConversationMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun addGroupMembers(request: Messenger.AddGroupMembersRequest, headers: Metadata
        = Metadata()): Messenger.Conversation = unaryRpc(
      channel,
      UserServiceGrpc.getAddGroupMembersMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun removeGroupMember(request: Messenger.RemoveGroupMemberRequest,
        headers: Metadata = Metadata()): Messenger.Conversation = unaryRpc(
      channel,
      UserServiceGrpc.getRemoveGroupMemberMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun leaveGroupConversation(request: Messenger.LeaveGroupConversationRequest,
        headers: Metadata = Metadata()): Empty = unaryRpc(
      channel,
      UserServiceGrpc.getLeaveGroupConversationMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun transferGroupAdmin(request: Messenger.TransferGroupAdminRequest,
        headers: Metadata = Metadata()): Messenger.Conversation = unaryRpc(
      channel,
      UserServiceGrpc.getTransferGroupAdminMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun upsertConversationKey(request: Messenger.UpsertConversationKeyRequest,
        headers: Metadata = Metadata()): Messenger.ConversationKey = unaryRpc(
      channel,
      UserServiceGrpc.getUpsertConversationKeyMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getConversationKey(request: Messenger.GetConversationKeyRequest,
        headers: Metadata = Metadata()): Messenger.ConversationKey = unaryRpc(
      channel,
      UserServiceGrpc.getGetConversationKeyMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the messenger.v1.UserService service based on Kotlin coroutines.
   */
  public abstract class UserServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for messenger.v1.UserService.GetProfile.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getProfile(request: Messenger.GetProfileRequest): Messenger.Profile =
        throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.GetProfile is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.UpdateProfile.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun updateProfile(request: Messenger.UpdateProfileRequest):
        Messenger.Profile = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.UpdateProfile is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.SearchUsers.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun searchUsers(request: Messenger.SearchUsersRequest):
        Messenger.SearchUsersResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.SearchUsers is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.ListConversations.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun listConversations(request: Empty): Messenger.ListConversationsResponse =
        throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.ListConversations is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.PublishIdentityKey.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun publishIdentityKey(request: Messenger.PublishIdentityKeyRequest):
        Messenger.IdentityKey = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.PublishIdentityKey is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.PublishPrekeyBundle.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun publishPrekeyBundle(request: Messenger.PublishPrekeyBundleRequest):
        Messenger.PrekeyBundle = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.PublishPrekeyBundle is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.GetIdentityKey.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getIdentityKey(request: Messenger.GetIdentityKeyRequest):
        Messenger.IdentityKey = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.GetIdentityKey is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.GetIdentityKeys.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getIdentityKeys(request: Messenger.GetIdentityKeysRequest):
        Messenger.GetIdentityKeysResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.GetIdentityKeys is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.AcquirePrekeyBundle.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun acquirePrekeyBundle(request: Messenger.AcquirePrekeyBundleRequest):
        Messenger.PrekeyBundle = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.AcquirePrekeyBundle is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.AcquirePrekeyBundles.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun acquirePrekeyBundles(request: Messenger.AcquirePrekeyBundlesRequest):
        Messenger.AcquirePrekeyBundlesResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.AcquirePrekeyBundles is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.CreateGroupConversation.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend
        fun createGroupConversation(request: Messenger.CreateGroupConversationRequest):
        Messenger.Conversation = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.CreateGroupConversation is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.AddGroupMembers.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun addGroupMembers(request: Messenger.AddGroupMembersRequest):
        Messenger.Conversation = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.AddGroupMembers is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.RemoveGroupMember.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun removeGroupMember(request: Messenger.RemoveGroupMemberRequest):
        Messenger.Conversation = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.RemoveGroupMember is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.LeaveGroupConversation.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend
        fun leaveGroupConversation(request: Messenger.LeaveGroupConversationRequest): Empty = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.LeaveGroupConversation is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.TransferGroupAdmin.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun transferGroupAdmin(request: Messenger.TransferGroupAdminRequest):
        Messenger.Conversation = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.TransferGroupAdmin is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.UpsertConversationKey.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun upsertConversationKey(request: Messenger.UpsertConversationKeyRequest):
        Messenger.ConversationKey = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.UpsertConversationKey is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.UserService.GetConversationKey.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getConversationKey(request: Messenger.GetConversationKeyRequest):
        Messenger.ConversationKey = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.UserService.GetConversationKey is unimplemented"))

    final override fun bindService(): ServerServiceDefinition =
        builder(userServiceGrpcGetServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getGetProfileMethod(),
      implementation = ::getProfile
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getUpdateProfileMethod(),
      implementation = ::updateProfile
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getSearchUsersMethod(),
      implementation = ::searchUsers
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getListConversationsMethod(),
      implementation = ::listConversations
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getPublishIdentityKeyMethod(),
      implementation = ::publishIdentityKey
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getPublishPrekeyBundleMethod(),
      implementation = ::publishPrekeyBundle
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getGetIdentityKeyMethod(),
      implementation = ::getIdentityKey
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getGetIdentityKeysMethod(),
      implementation = ::getIdentityKeys
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getAcquirePrekeyBundleMethod(),
      implementation = ::acquirePrekeyBundle
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getAcquirePrekeyBundlesMethod(),
      implementation = ::acquirePrekeyBundles
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getCreateGroupConversationMethod(),
      implementation = ::createGroupConversation
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getAddGroupMembersMethod(),
      implementation = ::addGroupMembers
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getRemoveGroupMemberMethod(),
      implementation = ::removeGroupMember
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getLeaveGroupConversationMethod(),
      implementation = ::leaveGroupConversation
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getTransferGroupAdminMethod(),
      implementation = ::transferGroupAdmin
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getUpsertConversationKeyMethod(),
      implementation = ::upsertConversationKey
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = UserServiceGrpc.getGetConversationKeyMethod(),
      implementation = ::getConversationKey
    )).build()
  }
}

/**
 * Holder for Kotlin coroutine-based client and server APIs for messenger.v1.MessageService.
 */
public object MessageServiceGrpcKt {
  public const val SERVICE_NAME: String = MessageServiceGrpc.SERVICE_NAME

  @JvmStatic
  public val serviceDescriptor: ServiceDescriptor
    get() = messageServiceGrpcGetServiceDescriptor()

  public val sendMessageMethod: MethodDescriptor<Messenger.SendMessageRequest, Messenger.Message>
    @JvmStatic
    get() = MessageServiceGrpc.getSendMessageMethod()

  public val getMessagesMethod:
      MethodDescriptor<Messenger.GetMessagesRequest, Messenger.GetMessagesResponse>
    @JvmStatic
    get() = MessageServiceGrpc.getGetMessagesMethod()

  public val searchMessagesMethod:
      MethodDescriptor<Messenger.SearchMessagesRequest, Messenger.SearchMessagesResponse>
    @JvmStatic
    get() = MessageServiceGrpc.getSearchMessagesMethod()

  public val deleteMessageMethod: MethodDescriptor<Messenger.DeleteMessageRequest, Empty>
    @JvmStatic
    get() = MessageServiceGrpc.getDeleteMessageMethod()

  public val prepareMediaUploadMethod:
      MethodDescriptor<Messenger.PrepareMediaUploadRequest, Messenger.PrepareMediaUploadResponse>
    @JvmStatic
    get() = MessageServiceGrpc.getPrepareMediaUploadMethod()

  public val uploadMediaMethod:
      MethodDescriptor<Messenger.UploadMediaRequest, Messenger.UploadMediaResponse>
    @JvmStatic
    get() = MessageServiceGrpc.getUploadMediaMethod()

  public val getMediaMethod: MethodDescriptor<Messenger.GetMediaRequest, Messenger.GetMediaResponse>
    @JvmStatic
    get() = MessageServiceGrpc.getGetMediaMethod()

  public val streamEventsMethod:
      MethodDescriptor<Messenger.StreamEventsRequest, Messenger.ServerEvent>
    @JvmStatic
    get() = MessageServiceGrpc.getStreamEventsMethod()

  /**
   * A stub for issuing RPCs to a(n) messenger.v1.MessageService service as suspending coroutines.
   */
  @StubFor(MessageServiceGrpc::class)
  public class MessageServiceCoroutineStub @JvmOverloads constructor(
    channel: Channel,
    callOptions: CallOptions = DEFAULT,
  ) : AbstractCoroutineStub<MessageServiceCoroutineStub>(channel, callOptions) {
    override fun build(channel: Channel, callOptions: CallOptions): MessageServiceCoroutineStub =
        MessageServiceCoroutineStub(channel, callOptions)

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun sendMessage(request: Messenger.SendMessageRequest, headers: Metadata =
        Metadata()): Messenger.Message = unaryRpc(
      channel,
      MessageServiceGrpc.getSendMessageMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getMessages(request: Messenger.GetMessagesRequest, headers: Metadata =
        Metadata()): Messenger.GetMessagesResponse = unaryRpc(
      channel,
      MessageServiceGrpc.getGetMessagesMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun searchMessages(request: Messenger.SearchMessagesRequest, headers: Metadata =
        Metadata()): Messenger.SearchMessagesResponse = unaryRpc(
      channel,
      MessageServiceGrpc.getSearchMessagesMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun deleteMessage(request: Messenger.DeleteMessageRequest, headers: Metadata =
        Metadata()): Empty = unaryRpc(
      channel,
      MessageServiceGrpc.getDeleteMessageMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun prepareMediaUpload(request: Messenger.PrepareMediaUploadRequest,
        headers: Metadata = Metadata()): Messenger.PrepareMediaUploadResponse = unaryRpc(
      channel,
      MessageServiceGrpc.getPrepareMediaUploadMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun uploadMedia(request: Messenger.UploadMediaRequest, headers: Metadata =
        Metadata()): Messenger.UploadMediaResponse = unaryRpc(
      channel,
      MessageServiceGrpc.getUploadMediaMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Executes this RPC and returns the response message, suspending until the RPC completes
     * with [`Status.OK`][io.grpc.Status].  If the RPC completes with another status, a
     * corresponding
     * [StatusException] is thrown.  If this coroutine is cancelled, the RPC is also cancelled
     * with the corresponding exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return The single response from the server.
     */
    public suspend fun getMedia(request: Messenger.GetMediaRequest, headers: Metadata = Metadata()):
        Messenger.GetMediaResponse = unaryRpc(
      channel,
      MessageServiceGrpc.getGetMediaMethod(),
      request,
      callOptions,
      headers
    )

    /**
     * Returns a [Flow] that, when collected, executes this RPC and emits responses from the
     * server as they arrive.  That flow finishes normally if the server closes its response with
     * [`Status.OK`][io.grpc.Status], and fails by throwing a [StatusException] otherwise.  If
     * collecting the flow downstream fails exceptionally (including via cancellation), the RPC
     * is cancelled with that exception as a cause.
     *
     * @param request The request message to send to the server.
     *
     * @param headers Metadata to attach to the request.  Most users will not need this.
     *
     * @return A flow that, when collected, emits the responses from the server.
     */
    public fun streamEvents(request: Messenger.StreamEventsRequest, headers: Metadata = Metadata()):
        Flow<Messenger.ServerEvent> = serverStreamingRpc(
      channel,
      MessageServiceGrpc.getStreamEventsMethod(),
      request,
      callOptions,
      headers
    )
  }

  /**
   * Skeletal implementation of the messenger.v1.MessageService service based on Kotlin coroutines.
   */
  public abstract class MessageServiceCoroutineImplBase(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
  ) : AbstractCoroutineServerImpl(coroutineContext) {
    /**
     * Returns the response to an RPC for messenger.v1.MessageService.SendMessage.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun sendMessage(request: Messenger.SendMessageRequest): Messenger.Message =
        throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.MessageService.SendMessage is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.MessageService.GetMessages.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getMessages(request: Messenger.GetMessagesRequest):
        Messenger.GetMessagesResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.MessageService.GetMessages is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.MessageService.SearchMessages.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun searchMessages(request: Messenger.SearchMessagesRequest):
        Messenger.SearchMessagesResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.MessageService.SearchMessages is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.MessageService.DeleteMessage.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun deleteMessage(request: Messenger.DeleteMessageRequest): Empty = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.MessageService.DeleteMessage is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.MessageService.PrepareMediaUpload.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun prepareMediaUpload(request: Messenger.PrepareMediaUploadRequest):
        Messenger.PrepareMediaUploadResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.MessageService.PrepareMediaUpload is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.MessageService.UploadMedia.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun uploadMedia(request: Messenger.UploadMediaRequest):
        Messenger.UploadMediaResponse = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.MessageService.UploadMedia is unimplemented"))

    /**
     * Returns the response to an RPC for messenger.v1.MessageService.GetMedia.
     *
     * If this method fails with a [StatusException], the RPC will fail with the corresponding
     * [io.grpc.Status].  If this method fails with a [java.util.concurrent.CancellationException],
     * the RPC will fail
     * with status `Status.CANCELLED`.  If this method fails for any other reason, the RPC will
     * fail with `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open suspend fun getMedia(request: Messenger.GetMediaRequest): Messenger.GetMediaResponse
        = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.MessageService.GetMedia is unimplemented"))

    /**
     * Returns a [Flow] of responses to an RPC for messenger.v1.MessageService.StreamEvents.
     *
     * If creating or collecting the returned flow fails with a [StatusException], the RPC
     * will fail with the corresponding [io.grpc.Status].  If it fails with a
     * [java.util.concurrent.CancellationException], the RPC will fail with status
     * `Status.CANCELLED`.  If creating
     * or collecting the returned flow fails for any other reason, the RPC will fail with
     * `Status.UNKNOWN` with the exception as a cause.
     *
     * @param request The request from the client.
     */
    public open fun streamEvents(request: Messenger.StreamEventsRequest):
        Flow<Messenger.ServerEvent> = throw
        StatusException(UNIMPLEMENTED.withDescription("Method messenger.v1.MessageService.StreamEvents is unimplemented"))

    final override fun bindService(): ServerServiceDefinition =
        builder(messageServiceGrpcGetServiceDescriptor())
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MessageServiceGrpc.getSendMessageMethod(),
      implementation = ::sendMessage
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MessageServiceGrpc.getGetMessagesMethod(),
      implementation = ::getMessages
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MessageServiceGrpc.getSearchMessagesMethod(),
      implementation = ::searchMessages
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MessageServiceGrpc.getDeleteMessageMethod(),
      implementation = ::deleteMessage
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MessageServiceGrpc.getPrepareMediaUploadMethod(),
      implementation = ::prepareMediaUpload
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MessageServiceGrpc.getUploadMediaMethod(),
      implementation = ::uploadMedia
    ))
      .addMethod(unaryServerMethodDefinition(
      context = this.context,
      descriptor = MessageServiceGrpc.getGetMediaMethod(),
      implementation = ::getMedia
    ))
      .addMethod(serverStreamingServerMethodDefinition(
      context = this.context,
      descriptor = MessageServiceGrpc.getStreamEventsMethod(),
      implementation = ::streamEvents
    )).build()
  }
}
