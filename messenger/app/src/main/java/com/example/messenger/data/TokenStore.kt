package com.example.messenger.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

private val Context.tokenDataStore by preferencesDataStore("auth")

data class SavedSession(
    val token: String,
    val username: String,
    val deviceId: String
)

class TokenStore(private val context: Context) {
    private val TOKEN = stringPreferencesKey("token")
    private val USERNAME = stringPreferencesKey("username")
    private val DEVICE_ID = stringPreferencesKey("device_id")

    suspend fun save(token: String, username: String, deviceId: String) {
        context.tokenDataStore.edit {
            it[TOKEN] = token
            it[USERNAME] = username
            it[DEVICE_ID] = deviceId
        }
    }

    suspend fun load(): SavedSession? {
        val prefs = context.tokenDataStore.data.firstOrNull() ?: return null
        val token = prefs[TOKEN] ?: return null
        val username = prefs[USERNAME] ?: return null
        val deviceId = prefs[DEVICE_ID] ?: return null
        return SavedSession(token = token, username = username, deviceId = deviceId)
    }

    suspend fun loadOrCreateDeviceId(): String {
        val prefs = context.tokenDataStore.data.firstOrNull()
        val existing = prefs?.get(DEVICE_ID)
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val deviceId = "android-${UUID.randomUUID()}"
        context.tokenDataStore.edit {
            it[DEVICE_ID] = deviceId
        }
        return deviceId
    }

    suspend fun clear() {
        context.tokenDataStore.edit {
            it.remove(TOKEN)
            it.remove(USERNAME)
        }
    }
}
