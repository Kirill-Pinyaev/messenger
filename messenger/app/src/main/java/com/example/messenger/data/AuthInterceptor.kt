package com.example.messenger.data

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor

class AuthInterceptor(private val token: String) : ClientInterceptor {
    override fun <Q, R> interceptCall(
        method: MethodDescriptor<Q, R>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<Q, R> = object : ForwardingClientCall.SimpleForwardingClientCall<Q, R>(
        next.newCall(method, callOptions)
    ) {
        override fun start(responseListener: Listener<R>, headers: Metadata) {
            headers.put(
                Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
                "Bearer $token"
            )
            super.start(responseListener, headers)
        }
    }
}
