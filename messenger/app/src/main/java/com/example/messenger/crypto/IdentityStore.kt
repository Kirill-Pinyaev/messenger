package com.example.messenger.crypto

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.security.PrivateKey
import java.security.SecureRandom

private val Context.identityDataStore by preferencesDataStore("identity")

data class OtpKey(
    val keyId: String,
    val publicBytes: ByteArray,
    val privateBytes: ByteArray,
    val published: Boolean
)

data class IdentityState(
    val username: String,
    val deviceId: String,
    val keyId: String,
    val publicKeyBytes: ByteArray,
    val privateKeyBytes: ByteArray,
    val signedPrekeyId: String,
    val signedPrekeyPublicBytes: ByteArray,
    val signedPrekeyPrivateBytes: ByteArray,
    val oneTimePrekeys: List<OtpKey>,
    val published: Boolean
) {
    fun identityPrivateKey(): PrivateKey = E2EE.importPrivateKey(privateKeyBytes)
    fun signedPrekeyPrivateKey(): PrivateKey = E2EE.importPrivateKey(signedPrekeyPrivateBytes)
    fun findOtp(keyId: String): OtpKey? = oneTimePrekeys.find { it.keyId == keyId }
}

class IdentityStore(private val context: Context) {
    private val KEY = stringPreferencesKey("identity_v1")

    suspend fun load(): IdentityState? {
        val raw = context.identityDataStore.data.firstOrNull()?.get(KEY) ?: return null
        return fromJson(raw)
    }

    suspend fun save(state: IdentityState) {
        context.identityDataStore.edit { it[KEY] = state.toJson() }
    }

    suspend fun clear() {
        context.identityDataStore.edit { it.clear() }
    }

    fun generate(username: String, deviceId: String): IdentityState {
        val identKp = E2EE.generateKeyPair()
        val spkKp = E2EE.generateKeyPair()
        val otps = (1..10).map {
            val kp = E2EE.generateKeyPair()
            OtpKey(
                keyId = "${username}_otp_${System.currentTimeMillis()}_$it",
                publicBytes = E2EE.exportPublicKey(kp.public),
                privateBytes = E2EE.exportPrivateKey(kp.private),
                published = false
            )
        }
        return IdentityState(
            username = username,
            deviceId = deviceId,
            keyId = "${username}_identity_${System.currentTimeMillis()}",
            publicKeyBytes = E2EE.exportPublicKey(identKp.public),
            privateKeyBytes = E2EE.exportPrivateKey(identKp.private),
            signedPrekeyId = "${username}_spk_${System.currentTimeMillis()}",
            signedPrekeyPublicBytes = E2EE.exportPublicKey(spkKp.public),
            signedPrekeyPrivateBytes = E2EE.exportPrivateKey(spkKp.private),
            oneTimePrekeys = otps,
            published = false
        )
    }
}

private fun IdentityState.toJson(): String = JSONObject().apply {
    put("username", username)
    put("deviceId", deviceId)
    put("keyId", keyId)
    put("pub", publicKeyBytes.b64())
    put("priv", privateKeyBytes.b64())
    put("spkId", signedPrekeyId)
    put("spkPub", signedPrekeyPublicBytes.b64())
    put("spkPriv", signedPrekeyPrivateBytes.b64())
    put("published", published)
    put("otps", JSONArray().also { arr ->
        oneTimePrekeys.forEach { otp ->
            arr.put(JSONObject().apply {
                put("keyId", otp.keyId)
                put("pub", otp.publicBytes.b64())
                put("priv", otp.privateBytes.b64())
                put("published", otp.published)
            })
        }
    })
}.toString()

private fun fromJson(json: String): IdentityState {
    val o = JSONObject(json)
    val otpArr = o.getJSONArray("otps")
    val otps = (0 until otpArr.length()).map {
        val entry = otpArr.getJSONObject(it)
        OtpKey(
            keyId = entry.getString("keyId"),
            publicBytes = entry.getString("pub").fromB64(),
            privateBytes = entry.getString("priv").fromB64(),
            published = entry.getBoolean("published")
        )
    }
    return IdentityState(
        username = o.getString("username"),
        deviceId = o.optString("deviceId", ""),
        keyId = o.getString("keyId"),
        publicKeyBytes = o.getString("pub").fromB64(),
        privateKeyBytes = o.getString("priv").fromB64(),
        signedPrekeyId = o.getString("spkId"),
        signedPrekeyPublicBytes = o.getString("spkPub").fromB64(),
        signedPrekeyPrivateBytes = o.getString("spkPriv").fromB64(),
        oneTimePrekeys = otps,
        published = o.getBoolean("published")
    )
}

private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
