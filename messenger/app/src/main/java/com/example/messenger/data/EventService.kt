package com.example.messenger.data

import com.example.messenger.App
import com.example.messenger.proto.ServerEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EventService(private val app: App) {
    private val _events = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ServerEvent> = _events

    private var started = false

    fun start(token: String) {
        if (started) return
        started = true
        app.appScope.launch {
            while (isActive) {
                try {
                    val repo = MessengerRepository(app.grpc, token)
                    repo.streamEvents().collect { event -> _events.emit(event) }
                } catch (_: Exception) {
                    delay(3_000)
                }
            }
        }
    }

    fun reset() { started = false }
}
