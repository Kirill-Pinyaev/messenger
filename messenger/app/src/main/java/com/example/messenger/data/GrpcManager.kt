package com.example.messenger.data

import com.example.messenger.proto.AuthServiceGrpcKt
import com.example.messenger.proto.MessageServiceGrpcKt
import com.example.messenger.proto.UserServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

// Change to your server's IP when using a physical device
const val SERVER_HOST = "192.168.1.116"
const val SERVER_PORT = 9090

class GrpcManager {
    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(SERVER_HOST, SERVER_PORT)
        .usePlaintext()
        .build()

    fun authStub(): AuthServiceGrpcKt.AuthServiceCoroutineStub =
        AuthServiceGrpcKt.AuthServiceCoroutineStub(channel)

    fun userStub(token: String): UserServiceGrpcKt.UserServiceCoroutineStub =
        UserServiceGrpcKt.UserServiceCoroutineStub(channel)
            .withInterceptors(AuthInterceptor(token))

    fun messageStub(token: String): MessageServiceGrpcKt.MessageServiceCoroutineStub =
        MessageServiceGrpcKt.MessageServiceCoroutineStub(channel)
            .withInterceptors(AuthInterceptor(token))

    fun shutdown() = channel.shutdown()
}
