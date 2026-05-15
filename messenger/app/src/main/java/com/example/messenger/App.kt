package com.example.messenger

import android.app.Application
import com.example.messenger.crypto.ArchiveStore
import com.example.messenger.crypto.IdentityStore
import com.example.messenger.data.EventService
import com.example.messenger.data.GrpcManager
import com.example.messenger.data.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class App : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val grpc by lazy { GrpcManager() }
    val tokenStore by lazy { TokenStore(this) }
    val identityStore by lazy { IdentityStore(this) }
    val archiveStore by lazy { ArchiveStore(this) }
    val eventService by lazy { EventService(this) }
}
