package com.example.messenger.crypto

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject

private val Context.archiveDataStore by preferencesDataStore("archive_identity")

class ArchiveStore(private val context: Context) {
    private val KEY = stringPreferencesKey("archive_identity_v1")

    suspend fun load(): ArchiveIdentityState? {
        val raw = context.archiveDataStore.data.firstOrNull()?.get(KEY) ?: return null
        return runCatching { fromJson(raw) }.getOrNull()
    }

    suspend fun save(state: ArchiveIdentityState) {
        context.archiveDataStore.edit { it[KEY] = state.toJson() }
    }

    suspend fun clear() {
        context.archiveDataStore.edit { it.clear() }
    }
}

private fun ArchiveIdentityState.toJson(): String = JSONObject().apply {
    put("username", username)
    put("pub", Base64.encodeToString(publicKeyBytes, Base64.NO_WRAP))
    put("priv", Base64.encodeToString(privateKeyBytes, Base64.NO_WRAP))
    put("version", version)
}.toString()

private fun fromJson(json: String): ArchiveIdentityState {
    val o = JSONObject(json)
    return ArchiveIdentityState(
        username = o.getString("username"),
        publicKeyBytes = Base64.decode(o.getString("pub"), Base64.NO_WRAP),
        privateKeyBytes = Base64.decode(o.getString("priv"), Base64.NO_WRAP),
        version = o.optInt("version", 1)
    )
}
