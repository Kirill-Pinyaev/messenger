package com.example.messenger.crypto

import android.util.Base64
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyPair
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

data class ArchiveIdentityState(
    val username: String,
    val publicKeyBytes: ByteArray,
    val privateKeyBytes: ByteArray,
    val version: Int = 1
) {
    fun publicKey(): PublicKey = E2EE.importPublicKey(publicKeyBytes)
    fun privateKey(): PrivateKey = E2EE.importPrivateKey(privateKeyBytes)
}

data class ArchiveKeyBundle(
    val username: String,
    val publicKeyBytes: ByteArray,
    val encryptedPrivateKey: ByteArray,
    val kdfSalt: ByteArray,
    val kdfParams: String,
    val version: Int
)

data class ArchiveEncrypted(
    val ciphertext: ByteArray,
    val nonce: ByteArray,
    val ephemeralPublicKey: ByteArray
)

object ArchiveE2EE {
    private const val KDF_ITERATIONS = 120000
    private const val NONCE_SIZE = 12
    private const val EC = "EC"
    private val P256 = ECGenParameterSpec("secp256r1")

    fun generateArchiveIdentity(username: String): ArchiveIdentityState {
        val kp: KeyPair = E2EE.generateKeyPair()
        return ArchiveIdentityState(
            username = username,
            publicKeyBytes = E2EE.exportPublicKey(kp.public),
            privateKeyBytes = E2EE.exportPrivateKey(kp.private),
            version = 1
        )
    }

    fun wrapForServer(identity: ArchiveIdentityState, password: String): ArchiveKeyBundle {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val kek = derivePasswordKey(password, salt, KDF_ITERATIONS)
        val nonce = ByteArray(NONCE_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kek, GCMParameterSpec(128, nonce))
        val privB64 = Base64.encodeToString(identity.privateKeyBytes, Base64.NO_WRAP)
        val ciphertext = cipher.doFinal(privB64.toByteArray(Charsets.UTF_8))
        val combined = nonce + ciphertext
        val params = JSONObject().apply {
            put("name", "PBKDF2")
            put("hash", "SHA-256")
            put("iterations", KDF_ITERATIONS)
        }.toString()
        return ArchiveKeyBundle(
            username = identity.username,
            publicKeyBytes = identity.publicKeyBytes,
            encryptedPrivateKey = combined,
            kdfSalt = salt,
            kdfParams = params,
            version = identity.version
        )
    }

    fun restoreFromServer(
        publicKeyBytes: ByteArray,
        encryptedPrivateKey: ByteArray,
        kdfSalt: ByteArray,
        kdfParams: String,
        version: Int,
        password: String,
        username: String
    ): ArchiveIdentityState {
        val params = JSONObject(kdfParams)
        val iterations = params.optInt("iterations", KDF_ITERATIONS)
        val kek = derivePasswordKey(password, kdfSalt, iterations)
        val nonce = encryptedPrivateKey.copyOfRange(0, NONCE_SIZE)
        val ciphertext = encryptedPrivateKey.copyOfRange(NONCE_SIZE, encryptedPrivateKey.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(128, nonce))
        val plaintext = cipher.doFinal(ciphertext)
        val decoded = String(plaintext, Charsets.UTF_8)
        val privateKeyBytes = if (decoded.trim().startsWith("{")) {
            importLegacyWebJwkPrivateKey(decoded).encoded
        } else {
            Base64.decode(decoded, Base64.NO_WRAP)
        }
        return ArchiveIdentityState(
            username = username,
            publicKeyBytes = publicKeyBytes,
            privateKeyBytes = privateKeyBytes,
            version = version
        )
    }

    fun encryptPayload(plaintext: ByteArray, recipientPublicKeyBytes: ByteArray): ArchiveEncrypted {
        val ephemeralKp = E2EE.generateKeyPair()
        val ephemeralPub = ephemeralKp.public
        val ephemeralPriv = ephemeralKp.private
        val ephemeralPubBytes = E2EE.exportPublicKey(ephemeralPub)
        val recipientPub = E2EE.importPublicKey(recipientPublicKeyBytes)
        val sharedSecret = E2EE.ecdh(ephemeralPriv, recipientPub)
        val aesKey = E2EE.hkdf(sharedSecret, info = "messenger-history-archive-v1")
        val (ciphertext, nonce) = E2EE.aesGcmEncrypt(aesKey, plaintext)
        return ArchiveEncrypted(ciphertext, nonce, ephemeralPubBytes)
    }

    fun decryptPayload(encrypted: ArchiveEncrypted, identity: ArchiveIdentityState): ByteArray {
        val ephemeralPub = E2EE.importPublicKey(encrypted.ephemeralPublicKey)
        val sharedSecret = E2EE.ecdh(identity.privateKey(), ephemeralPub)
        val aesKey = E2EE.hkdf(sharedSecret, info = "messenger-history-archive-v1")
        return E2EE.aesGcmDecrypt(aesKey, encrypted.ciphertext, encrypted.nonce)
    }

    private fun derivePasswordKey(password: String, salt: ByteArray, iterations: Int): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun importLegacyWebJwkPrivateKey(json: String): PrivateKey {
        val jwk = JSONObject(json)
        require(jwk.optString("kty") == "EC") { "unsupported archive JWK type" }
        require(jwk.optString("crv") == "P-256") { "unsupported archive curve" }
        val d = base64UrlDecode(jwk.getString("d"))
        val params = AlgorithmParameters.getInstance(EC).apply { init(P256) }
            .getParameterSpec(ECParameterSpec::class.java)
        val spec = ECPrivateKeySpec(BigInteger(1, d), params)
        return KeyFactory.getInstance(EC).generatePrivate(spec)
    }

    private fun base64UrlDecode(value: String): ByteArray {
        val normalized = value
            .replace('-', '+')
            .replace('_', '/')
            .let {
                val pad = (4 - (it.length % 4)) % 4
                it + "=".repeat(pad)
            }
        return Base64.decode(normalized, Base64.DEFAULT)
    }
}
