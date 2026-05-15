package com.example.messenger.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.App
import com.example.messenger.crypto.ArchiveE2EE
import com.example.messenger.data.AuthRepository
import com.example.messenger.data.MessengerRepository
import io.grpc.StatusException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    private val authRepo = AuthRepository(app.grpc)

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            try {
                val deviceId = app.tokenStore.loadOrCreateDeviceId()
                val token = authRepo.login(username.trim(), password, deviceId)
                app.tokenStore.save(token, username.trim(), deviceId)
                ensureIdentityPublished(token, username.trim(), deviceId)
                ensureArchiveInitialized(token, username.trim(), password)
                app.eventService.start(token)
                _state.value = AuthUiState(success = true)
            } catch (e: Exception) {
                _state.value = AuthUiState(error = e.message ?: "Ошибка входа")
            }
        }
    }

    fun register(username: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _state.value = AuthUiState(loading = true)
            try {
                authRepo.register(username.trim(), password, firstName.trim(), lastName.trim())
                _state.value = AuthUiState(success = true)
            } catch (e: Exception) {
                _state.value = AuthUiState(error = e.message ?: "Ошибка регистрации")
            }
        }
    }

    private suspend fun ensureIdentityPublished(token: String, username: String, deviceId: String) {
        val storedIdentity = app.identityStore.load()
        val identity = if (storedIdentity == null || storedIdentity.username != username || storedIdentity.deviceId != deviceId) {
            app.identityStore.generate(username, deviceId)
        } else {
            storedIdentity
        }
        val repo = MessengerRepository(app.grpc, token)
        try {
            repo.publishIdentityKey(identity.keyId, identity.publicKeyBytes)
            val unpublishedOtps = identity.oneTimePrekeys.filter { !it.published }
            repo.publishPrekeyBundle(
                spkId = identity.signedPrekeyId,
                spkPub = identity.signedPrekeyPublicBytes,
                otps = unpublishedOtps.map { it.keyId to it.publicBytes }
            )
            app.identityStore.save(identity.copy(
                published = true,
                oneTimePrekeys = identity.oneTimePrekeys.map { it.copy(published = true) }
            ))
            Log.i("Auth", "E2EE identity published: keyId=${identity.keyId} device=$deviceId")
        } catch (e: Exception) {
            Log.w("Auth", "E2EE key publish failed: ${e.message}")
        }
    }

    private suspend fun ensureArchiveInitialized(token: String, username: String, password: String) {
        val repo = MessengerRepository(app.grpc, token)
        try {
            val existingLocal = app.archiveStore.load()
            val usableLocal = existingLocal?.takeIf {
                runCatching { it.privateKey() }.isSuccess
            }
            if (existingLocal != null && existingLocal.username != username) {
                app.archiveStore.clear()
            }
            // Try to fetch existing server-side header
            val header = try {
                repo.getHistoryArchiveHeader()
            } catch (e: StatusException) {
                null
            }
            if (usableLocal != null && usableLocal.username == username && (header == null || header.publicKey.isEmpty)) {
                val bundle = ArchiveE2EE.wrapForServer(usableLocal, password)
                repo.initializeHistoryArchive(
                    publicKey = bundle.publicKeyBytes,
                    encryptedPrivateKey = bundle.encryptedPrivateKey,
                    kdfSalt = bundle.kdfSalt,
                    kdfParams = bundle.kdfParams,
                    version = bundle.version
                )
                app.archiveStore.save(usableLocal)
                return
            }
            if (usableLocal != null && usableLocal.username == username && header != null && !header.publicKey.isEmpty) {
                if (header.publicKey.toByteArray().contentEquals(usableLocal.publicKeyBytes)) {
                    app.archiveStore.save(usableLocal)
                    return
                }
            }
            if (header != null && !header.publicKey.isEmpty) {
                // Restore local identity from server using login password
                val restored = ArchiveE2EE.restoreFromServer(
                    publicKeyBytes = header.publicKey.toByteArray(),
                    encryptedPrivateKey = header.encryptedPrivateKey.toByteArray(),
                    kdfSalt = header.kdfSalt.toByteArray(),
                    kdfParams = header.kdfParams,
                    version = header.version,
                    password = password,
                    username = username
                )
                app.archiveStore.save(restored)
            } else {
                // First time — generate new archive identity and publish it
                val identity = ArchiveE2EE.generateArchiveIdentity(username)
                val bundle = ArchiveE2EE.wrapForServer(identity, password)
                repo.initializeHistoryArchive(
                    publicKey = bundle.publicKeyBytes,
                    encryptedPrivateKey = bundle.encryptedPrivateKey,
                    kdfSalt = bundle.kdfSalt,
                    kdfParams = bundle.kdfParams,
                    version = bundle.version
                )
                app.archiveStore.save(identity)
            }
        } catch (e: Exception) {
            Log.w("Archive", "Archive init failed: ${e.message}")
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
