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
    val signedPrekeySignature: ByteArray,
    val oneTimePrekeys: List<OtpKey>,
    val published: Boolean,
    val ratchetSessions: Map<String, RatchetSession> = emptyMap(),
    val sentMessageKeys: Map<String, ByteArray> = emptyMap(),
    val skippedMessageKeys: Map<String, ByteArray> = emptyMap(),
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
        val ratchet = DoubleRatchet.createIdentity(username, deviceId)
        val otps = emptyList<OtpKey>()
        return IdentityState(
            username = username,
            deviceId = deviceId,
            keyId = ratchet.keyId,
            publicKeyBytes = ratchet.identityPublic,
            privateKeyBytes = ratchet.identityPrivate,
            signedPrekeyId = ratchet.signedPrekeyId,
            signedPrekeyPublicBytes = ratchet.signedPrekeyPublic,
            signedPrekeyPrivateBytes = ratchet.signedPrekeyPrivate,
            signedPrekeySignature = ratchet.signedPrekeySignature,
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
    put("spkSig", signedPrekeySignature.b64())
    put("published", published)
    put("ratchetSessions", JSONObject().also { sessions ->
        ratchetSessions.forEach { (key, value) -> sessions.put(key, value.toJson()) }
    })
    put("sentMessageKeys", JSONObject().also { sent ->
        sentMessageKeys.forEach { (key, value) -> sent.put(key, value.b64()) }
    })
    put("skippedMessageKeys", JSONObject().also { skipped ->
        skippedMessageKeys.forEach { (key, value) -> skipped.put(key, value.b64()) }
    })
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
    val sessions = mutableMapOf<String, RatchetSession>()
    val sessionsJson = o.optJSONObject("ratchetSessions") ?: JSONObject()
    sessionsJson.keys().forEach { key -> sessions[key] = sessionFromJson(sessionsJson.getJSONObject(key)) }
    val sent = mutableMapOf<String, ByteArray>()
    val sentJson = o.optJSONObject("sentMessageKeys") ?: JSONObject()
    sentJson.keys().forEach { key -> sent[key] = sentJson.getString(key).fromB64() }
    val skipped = mutableMapOf<String, ByteArray>()
    val skippedJson = o.optJSONObject("skippedMessageKeys") ?: JSONObject()
    skippedJson.keys().forEach { key -> skipped[key] = skippedJson.getString(key).fromB64() }
    return IdentityState(
        username = o.getString("username"),
        deviceId = o.optString("deviceId", ""),
        keyId = o.getString("keyId"),
        publicKeyBytes = o.getString("pub").fromB64(),
        privateKeyBytes = o.getString("priv").fromB64(),
        signedPrekeyId = o.getString("spkId"),
        signedPrekeyPublicBytes = o.getString("spkPub").fromB64(),
        signedPrekeyPrivateBytes = o.getString("spkPriv").fromB64(),
        signedPrekeySignature = o.optString("spkSig").takeIf { it.isNotBlank() }?.fromB64() ?: ByteArray(0),
        oneTimePrekeys = otps,
        published = o.getBoolean("published"),
        ratchetSessions = sessions,
        sentMessageKeys = sent,
        skippedMessageKeys = skipped,
    )
}

private fun ByteArray.b64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

private fun RatchetSession.toJson(): JSONObject = JSONObject().apply {
    put("rootKey", rootKey.b64())
    put("sendingChainKey", sendingChainKey?.b64() ?: JSONObject.NULL)
    put("receivingChainKey", receivingChainKey?.b64() ?: JSONObject.NULL)
    put("localRatchetPrivateKey", localRatchetPrivateKey.b64())
    put("localRatchetPublicKey", localRatchetPublicKey.b64())
    put("remoteRatchetPublicKey", remoteRatchetPublicKey.b64())
    put("previousChainLength", previousChainLength)
    put("sendingMessageNumber", sendingMessageNumber)
    put("receivingMessageNumber", receivingMessageNumber)
}

private fun sessionFromJson(o: JSONObject): RatchetSession = RatchetSession(
    rootKey = o.getString("rootKey").fromB64(),
    sendingChainKey = o.optString("sendingChainKey").takeIf { it.isNotBlank() && it != "null" }?.fromB64(),
    receivingChainKey = o.optString("receivingChainKey").takeIf { it.isNotBlank() && it != "null" }?.fromB64(),
    localRatchetPrivateKey = o.getString("localRatchetPrivateKey").fromB64(),
    localRatchetPublicKey = o.getString("localRatchetPublicKey").fromB64(),
    remoteRatchetPublicKey = o.getString("remoteRatchetPublicKey").fromB64(),
    previousChainLength = o.getInt("previousChainLength"),
    sendingMessageNumber = o.getInt("sendingMessageNumber"),
    receivingMessageNumber = o.getInt("receivingMessageNumber"),
)
