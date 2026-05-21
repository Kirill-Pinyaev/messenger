package com.example.messenger.crypto

import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class RatchetPrekeyBundle(
    val username: String,
    val deviceId: String,
    val identityKeyId: String,
    val identityPublic: ByteArray,
    val signedPrekeyId: String,
    val signedPrekeyPublic: ByteArray,
    val signedPrekeySignature: ByteArray,
    val oneTimePrekeyId: String = "",
    val oneTimePrekeyPublic: ByteArray? = null,
)

data class RatchetIdentityKey(
    val username: String,
    val deviceId: String,
    val keyId: String,
    val publicKey: ByteArray,
)

data class RatchetMessage(
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val senderKeyId: String,
    val recipientSignedPrekeyId: String,
    val recipientSignedPrekeyPublic: ByteArray,
    val recipientOneTimePrekeyId: String,
    val recipientOneTimePrekeyPublic: ByteArray?,
    val e2eeAlgorithm: String,
    val ratchetPublicKey: ByteArray,
    val previousChainLength: Int,
    val messageNumber: Int,
)

data class RatchetSession(
    val rootKey: ByteArray,
    val sendingChainKey: ByteArray?,
    val receivingChainKey: ByteArray?,
    val localRatchetPrivateKey: ByteArray,
    val localRatchetPublicKey: ByteArray,
    val remoteRatchetPublicKey: ByteArray,
    val previousChainLength: Int,
    val sendingMessageNumber: Int,
    val receivingMessageNumber: Int,
)

data class RatchetIdentity(
    val username: String,
    val deviceId: String,
    val keyId: String,
    val identityPrivate: ByteArray,
    val identityPublic: ByteArray,
    val signedPrekeyId: String,
    val signedPrekeyPrivate: ByteArray,
    val signedPrekeyPublic: ByteArray,
    val signedPrekeySignature: ByteArray,
    val sessions: MutableMap<String, RatchetSession> = mutableMapOf(),
    val sentMessageKeys: MutableMap<String, ByteArray> = mutableMapOf(),
) {
    fun toPrekeyBundle(): RatchetPrekeyBundle = RatchetPrekeyBundle(
        username = username,
        deviceId = deviceId,
        identityKeyId = keyId,
        identityPublic = identityPublic,
        signedPrekeyId = signedPrekeyId,
        signedPrekeyPublic = signedPrekeyPublic,
        signedPrekeySignature = signedPrekeySignature,
    )

    fun toIdentityKey(): RatchetIdentityKey = RatchetIdentityKey(username, deviceId, keyId, identityPublic)
}

object DoubleRatchet {
    const val ALGORITHM = "DR-X25519-HKDF-SHA256-AESGCM-Ed25519-v1"
    private const val HMAC = "HmacSHA256"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private val random = SecureRandom()

    fun createIdentity(username: String, deviceId: String): RatchetIdentity {
        val identityPriv = ByteArray(32).also(random::nextBytes)
        val identity = Ed25519PrivateKeyParameters(identityPriv, 0)
        val identityPub = identity.generatePublicKey().encoded
        val spkPriv = ByteArray(32).also(random::nextBytes)
        val spk = X25519PrivateKeyParameters(spkPriv, 0)
        val spkPub = spk.generatePublicKey().encoded
        val keyId = "${username}_identity_${System.currentTimeMillis()}"
        val spkId = "${username}_spk_${System.currentTimeMillis()}"
        val sig = signSpk(identityPriv, username, deviceId, spkId, spkPub)
        return RatchetIdentity(username, deviceId, keyId, identityPriv, identityPub, spkId, spkPriv, spkPub, sig)
    }

    fun verifySignedPrekey(bundle: RatchetPrekeyBundle): Boolean {
        val signer = Ed25519Signer()
        signer.init(false, Ed25519PublicKeyParameters(bundle.identityPublic, 0))
        val msg = canonicalSpk(bundle.username, bundle.deviceId, bundle.signedPrekeyId, bundle.signedPrekeyPublic)
        signer.update(msg, 0, msg.size)
        return signer.verifySignature(bundle.signedPrekeySignature)
    }

    fun encrypt(plaintext: String, sender: RatchetIdentity, recipient: RatchetPrekeyBundle, conversationId: String): RatchetMessage {
        require(verifySignedPrekey(recipient)) { "invalid signed prekey signature" }
        val id = sessionKey(recipient.username, recipient.deviceId.ifBlank { conversationId })
        var session = sender.sessions[id]
        if (session == null) {
            val ratchet = x25519KeyPair()
            val initialRoot = hkdf(x25519(ratchet.first, recipient.signedPrekeyPublic), "messenger-dr-root-v1")
            val sending = kdfRoot(initialRoot, x25519(ratchet.first, recipient.signedPrekeyPublic))
            session = RatchetSession(
                rootKey = sending.first,
                sendingChainKey = sending.second,
                receivingChainKey = null,
                localRatchetPrivateKey = ratchet.first,
                localRatchetPublicKey = ratchet.second,
                remoteRatchetPublicKey = recipient.signedPrekeyPublic,
                previousChainLength = 0,
                sendingMessageNumber = 0,
                receivingMessageNumber = 0,
            )
        } else if (session.sendingChainKey == null) {
            val ratchet = x25519KeyPair()
            val send = kdfRoot(session.rootKey, x25519(ratchet.first, session.remoteRatchetPublicKey))
            session = session.copy(
                rootKey = send.first,
                sendingChainKey = send.second,
                localRatchetPrivateKey = ratchet.first,
                localRatchetPublicKey = ratchet.second,
                previousChainLength = session.receivingMessageNumber,
                sendingMessageNumber = 0,
            )
        }
        val chain = kdfChain(session.sendingChainKey ?: error("missing sending chain"))
        val encrypted = aesGcmEncrypt(chain.second, plaintext.encodeToByteArray())
        val number = session.sendingMessageNumber
        sender.sentMessageKeys[sentKey(session.localRatchetPublicKey, number)] = chain.second
        sender.sessions[id] = session.copy(
            sendingChainKey = chain.first,
            sendingMessageNumber = number + 1,
        )
        return RatchetMessage(
            ciphertext = encrypted.first,
            nonce = encrypted.second,
            senderKeyId = sender.keyId,
            recipientSignedPrekeyId = recipient.signedPrekeyId,
            recipientSignedPrekeyPublic = recipient.signedPrekeyPublic,
            recipientOneTimePrekeyId = recipient.oneTimePrekeyId,
            recipientOneTimePrekeyPublic = recipient.oneTimePrekeyPublic,
            e2eeAlgorithm = ALGORITHM,
            ratchetPublicKey = session.localRatchetPublicKey,
            previousChainLength = session.previousChainLength,
            messageNumber = number,
        )
    }

    fun decrypt(message: RatchetMessage, recipient: RatchetIdentity, senderIdentity: RatchetIdentityKey, conversationId: String): String {
        require(message.e2eeAlgorithm == ALGORITHM) { "unsupported direct E2EE algorithm" }
        val sentKey = recipient.sentMessageKeys[sentKey(message.ratchetPublicKey, message.messageNumber)]
        val messageKey = sentKey ?: nextReceivingKey(message, recipient, senderIdentity, conversationId)
        return aesGcmDecrypt(messageKey, message.ciphertext, message.nonce).decodeToString()
    }

    private fun nextReceivingKey(message: RatchetMessage, recipient: RatchetIdentity, senderIdentity: RatchetIdentityKey, conversationId: String): ByteArray {
        val id = sessionKey(senderIdentity.username, senderIdentity.deviceId.ifBlank { conversationId })
        var session = recipient.sessions[id]
        if (session == null) {
            val initialRoot = hkdf(x25519(recipient.signedPrekeyPrivate, message.ratchetPublicKey), "messenger-dr-root-v1")
            val receiving = kdfRoot(initialRoot, x25519(recipient.signedPrekeyPrivate, message.ratchetPublicKey))
            session = RatchetSession(
                rootKey = receiving.first,
                sendingChainKey = null,
                receivingChainKey = receiving.second,
                localRatchetPrivateKey = recipient.signedPrekeyPrivate,
                localRatchetPublicKey = recipient.signedPrekeyPublic,
                remoteRatchetPublicKey = message.ratchetPublicKey,
                previousChainLength = 0,
                sendingMessageNumber = 0,
                receivingMessageNumber = 0,
            )
        } else if (!session.remoteRatchetPublicKey.contentEquals(message.ratchetPublicKey)) {
            val recv = kdfRoot(session.rootKey, x25519(session.localRatchetPrivateKey, message.ratchetPublicKey))
            val ratchet = x25519KeyPair()
            val send = kdfRoot(recv.first, x25519(ratchet.first, message.ratchetPublicKey))
            session = session.copy(
                rootKey = send.first,
                receivingChainKey = recv.second,
                sendingChainKey = send.second,
                localRatchetPrivateKey = ratchet.first,
                localRatchetPublicKey = ratchet.second,
                remoteRatchetPublicKey = message.ratchetPublicKey,
                previousChainLength = session.sendingMessageNumber,
                sendingMessageNumber = 0,
                receivingMessageNumber = 0,
            )
        }
        var current = session ?: error("missing ratchet session")
        var messageKey = ByteArray(0)
        while (current.receivingMessageNumber <= message.messageNumber) {
            val chain = kdfChain(current.receivingChainKey ?: error("missing receiving chain"))
            messageKey = chain.second
            current = current.copy(
                receivingChainKey = chain.first,
                receivingMessageNumber = current.receivingMessageNumber + 1,
            )
        }
        recipient.sessions[id] = current
        return messageKey
    }

    fun exportIdentity(identity: RatchetIdentity): String = JSONObject().apply {
        put("username", identity.username)
        put("deviceId", identity.deviceId)
        put("keyId", identity.keyId)
        put("identityPrivate", identity.identityPrivate.b64())
        put("identityPublic", identity.identityPublic.b64())
        put("signedPrekeyId", identity.signedPrekeyId)
        put("signedPrekeyPrivate", identity.signedPrekeyPrivate.b64())
        put("signedPrekeyPublic", identity.signedPrekeyPublic.b64())
        put("signedPrekeySignature", identity.signedPrekeySignature.b64())
        put("sessions", JSONObject().also { sessions ->
            identity.sessions.forEach { (key, value) -> sessions.put(key, value.toJson()) }
        })
        put("sentMessageKeys", JSONObject().also { sent ->
            identity.sentMessageKeys.forEach { (key, value) -> sent.put(key, value.b64()) }
        })
    }.toString()

    fun importIdentity(json: String): RatchetIdentity {
        val o = JSONObject(json)
        val sessions = mutableMapOf<String, RatchetSession>()
        val sessionsJson = o.optJSONObject("sessions") ?: JSONObject()
        sessionsJson.keys().forEach { key -> sessions[key] = sessionFromJson(sessionsJson.getJSONObject(key)) }
        val sent = mutableMapOf<String, ByteArray>()
        val sentJson = o.optJSONObject("sentMessageKeys") ?: JSONObject()
        sentJson.keys().forEach { key -> sent[key] = sentJson.getString(key).fromB64() }
        return RatchetIdentity(
            username = o.getString("username"),
            deviceId = o.optString("deviceId", ""),
            keyId = o.getString("keyId"),
            identityPrivate = o.getString("identityPrivate").fromB64(),
            identityPublic = o.getString("identityPublic").fromB64(),
            signedPrekeyId = o.getString("signedPrekeyId"),
            signedPrekeyPrivate = o.getString("signedPrekeyPrivate").fromB64(),
            signedPrekeyPublic = o.getString("signedPrekeyPublic").fromB64(),
            signedPrekeySignature = o.getString("signedPrekeySignature").fromB64(),
            sessions = sessions,
            sentMessageKeys = sent,
        )
    }

    private fun signSpk(identityPrivate: ByteArray, username: String, deviceId: String, spkId: String, spkPublic: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(identityPrivate, 0))
        val msg = canonicalSpk(username, deviceId, spkId, spkPublic)
        signer.update(msg, 0, msg.size)
        return signer.generateSignature()
    }

    private fun canonicalSpk(username: String, deviceId: String, spkId: String, spkPublic: ByteArray): ByteArray =
        "messenger-spk-v1".encodeToByteArray() + username.encodeToByteArray() + byteArrayOf(0) +
            deviceId.encodeToByteArray() + byteArrayOf(0) + spkId.encodeToByteArray() + byteArrayOf(0) + spkPublic

    private fun x25519KeyPair(): Pair<ByteArray, ByteArray> {
        val priv = ByteArray(32).also(random::nextBytes)
        return priv to X25519PrivateKeyParameters(priv, 0).generatePublicKey().encoded
    }

    private fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        val out = ByteArray(32)
        X25519PrivateKeyParameters(privateKey, 0).generateSecret(X25519PublicKeyParameters(publicKey, 0), out, 0)
        return out
    }

    private fun kdfRoot(rootKey: ByteArray, dhOut: ByteArray): Pair<ByteArray, ByteArray> {
        val out = hkdf(rootKey + dhOut, "messenger-dr-root-v1", 64)
        return out.copyOfRange(0, 32) to out.copyOfRange(32, 64)
    }

    private fun kdfChain(chainKey: ByteArray): Pair<ByteArray, ByteArray> {
        val out = hkdf(chainKey, "messenger-dr-chain-v1", 64)
        return out.copyOfRange(0, 32) to hkdf(out.copyOfRange(32, 64), "messenger-dr-message-v1")
    }

    private fun hkdf(ikm: ByteArray, info: String, outLen: Int = 32): ByteArray {
        val mac = Mac.getInstance(HMAC)
        mac.init(SecretKeySpec(ByteArray(32), HMAC))
        val prk = mac.doFinal(ikm)
        val result = ByteArray(outLen)
        var prev = ByteArray(0)
        var offset = 0
        var ctr = 1
        while (offset < outLen) {
            mac.init(SecretKeySpec(prk, HMAC))
            mac.update(prev)
            mac.update(info.encodeToByteArray())
            mac.update(ctr.toByte())
            prev = mac.doFinal()
            val n = minOf(prev.size, outLen - offset)
            prev.copyInto(result, offset, 0, n)
            offset += n
            ctr++
        }
        return result
    }

    private fun aesGcmEncrypt(key: ByteArray, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(plaintext) to nonce
    }

    private fun aesGcmDecrypt(key: ByteArray, ciphertext: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(ciphertext)
    }

    private fun sessionKey(username: String, deviceId: String): String = "$username|$deviceId"
    private fun sentKey(ratchetPublicKey: ByteArray, number: Int): String = "${ratchetPublicKey.b64()}|$number"
}

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

private fun ByteArray.b64(): String = Base64.getEncoder().encodeToString(this)
private fun String.fromB64(): ByteArray = Base64.getDecoder().decode(this)
