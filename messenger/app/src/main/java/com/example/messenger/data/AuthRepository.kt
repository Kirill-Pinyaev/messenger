package com.example.messenger.data

import com.example.messenger.proto.loginRequest
import com.example.messenger.proto.registerRequest

class AuthRepository(private val grpc: GrpcManager) {

    suspend fun login(username: String, password: String, deviceId: String): String {
        val resp = grpc.authStub().login(loginRequest {
            this.username = username
            this.password = password
            this.deviceId = deviceId
        })
        return resp.token
    }

    suspend fun register(username: String, password: String, firstName: String, lastName: String) {
        grpc.authStub().register(registerRequest {
            this.username = username
            this.password = password
            this.firstName = firstName
            this.lastName = lastName
        })
    }
}
