package com.example.messenger.crypto

import java.math.BigInteger
import java.security.KeyFactory
import javax.crypto.KeyAgreement
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import android.util.Base64

object E2EE {
    private const val EC = "EC"
    private const val ECDH = "ECDH"
    private const val AES_GCM = "AES/GCM/NoPadding"
    private const val HMAC = "HmacSHA256"
    private val P256 = ECGenParameterSpec("secp256r1")
    const val NONCE_SIZE = 12

    // X.509 SubjectPublicKeyInfo header for P-256 uncompressed point (65 bytes)
    private val P256_X509_HEADER = byteArrayOf(
        0x30, 0x59, 0x30, 0x13, 0x06, 0x07,
        0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x02, 0x01,
        0x06, 0x08, 0x2a, 0x86.toByte(), 0x48, 0xce.toByte(), 0x3d, 0x03, 0x01, 0x07,
        0x03, 0x42, 0x00
    )

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(EC)
        kpg.initialize(P256)
        return kpg.generateKeyPair()
    }

    fun exportPublicKey(pub: PublicKey): ByteArray {
        val ec = pub as ECPublicKey
        val x = ec.w.affineX.unsignedBytes(32)
        val y = ec.w.affineY.unsignedBytes(32)
        return byteArrayOf(0x04) + x + y
    }

    fun importPublicKey(raw: ByteArray): PublicKey {
        require(raw.size == 65 && raw[0] == 0x04.toByte()) { "Expected uncompressed P-256 point (65 bytes)" }
        return KeyFactory.getInstance(EC).generatePublic(X509EncodedKeySpec(P256_X509_HEADER + raw))
    }

    fun exportPrivateKey(priv: PrivateKey): ByteArray = priv.encoded // PKCS8

    fun importPrivateKey(pkcs8: ByteArray): PrivateKey =
        KeyFactory.getInstance(EC).generatePrivate(PKCS8EncodedKeySpec(pkcs8))

    fun ecdh(priv: PrivateKey, pub: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance(ECDH)
        ka.init(priv)
        ka.doPhase(pub, true)
        val raw = ka.generateSecret()
        // P-256 x-coordinate is 32 bytes; WebCrypto deriveBits(..., 256) always returns
        // exactly 32 bytes, so we must left-pad if the JVM returns fewer (leading-zero x).
        return if (raw.size < 32) ByteArray(32 - raw.size) + raw else raw
    }

    fun hkdf(ikm: ByteArray, info: String, salt: ByteArray = ByteArray(32), outLen: Int = 32): ByteArray {
        val mac = Mac.getInstance(HMAC)
        mac.init(SecretKeySpec(salt, HMAC))
        val prk = mac.doFinal(ikm)
        val infoBytes = info.encodeToByteArray()
        val result = ByteArray(outLen)
        var prev = ByteArray(0)
        var pos = 0; var ctr = 1
        while (pos < outLen) {
            mac.init(SecretKeySpec(prk, HMAC))
            mac.update(prev); mac.update(infoBytes); mac.update(ctr.toByte())
            prev = mac.doFinal()
            val n = minOf(prev.size, outLen - pos)
            prev.copyInto(result, pos, 0, n)
            pos += n; ctr++
        }
        return result
    }

    fun aesGcmEncrypt(keyBytes: ByteArray, plaintext: ByteArray): Pair<ByteArray, ByteArray> {
        val nonce = ByteArray(NONCE_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(plaintext) to nonce
    }

    fun aesGcmDecrypt(keyBytes: ByteArray, ciphertext: ByteArray, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(ciphertext)
    }

    // ── Direct message ───────────────────────────────────────────────────────

    private fun deriveDirectKey(priv: PrivateKey, spkPub: ByteArray, otpPub: ByteArray?): ByteArray {
        val secrets = mutableListOf(ecdh(priv, importPublicKey(spkPub)))
        if (otpPub != null) secrets += ecdh(priv, importPublicKey(otpPub))
        val ikm = secrets.reduce { a, b -> a + b }
        return hkdf(ikm, info = "messenger-direct-prekey-v1")
    }

    fun encryptDirectMessage(
        plaintext: String,
        senderIdentPriv: PrivateKey,
        recipientSpkPub: ByteArray,
        recipientOtpPub: ByteArray?
    ): Pair<ByteArray, ByteArray> {
        val key = deriveDirectKey(senderIdentPriv, recipientSpkPub, recipientOtpPub)
        return aesGcmEncrypt(key, plaintext.encodeToByteArray())
    }

    fun decryptDirectMessage(
        ciphertext: ByteArray,
        nonce: ByteArray,
        recipientSpkPriv: PrivateKey,
        recipientOtpPriv: PrivateKey?,
        senderIdentPub: ByteArray
    ): String {
        val senderPub = importPublicKey(senderIdentPub)
        val secrets = mutableListOf(ecdh(recipientSpkPriv, senderPub))
        if (recipientOtpPriv != null) secrets += ecdh(recipientOtpPriv, senderPub)
        val ikm = secrets.reduce { a, b -> a + b }
        val key = hkdf(ikm, info = "messenger-direct-prekey-v1")
        return String(aesGcmDecrypt(key, ciphertext, nonce), Charsets.UTF_8)
    }

    // ── Group envelope ───────────────────────────────────────────────────────

    fun encryptGroupEnvelope(groupKey: ByteArray, senderPriv: PrivateKey, recipientPub: ByteArray): Pair<ByteArray, ByteArray> {
        val key = hkdf(ecdh(senderPriv, importPublicKey(recipientPub)), info = "messenger-group-envelope-v1")
        return aesGcmEncrypt(key, groupKey)
    }

    fun decryptGroupEnvelope(encKey: ByteArray, nonce: ByteArray, recipientPriv: PrivateKey, senderPub: ByteArray): ByteArray {
        val key = hkdf(ecdh(recipientPriv, importPublicKey(senderPub)), info = "messenger-group-envelope-v1")
        return aesGcmDecrypt(key, encKey, nonce)
    }

    // ── Group message ─────────────────────────────────────────────────────────

    fun encryptGroupMessage(plaintext: String, groupKey: ByteArray): Pair<ByteArray, ByteArray> =
        aesGcmEncrypt(groupKey, plaintext.encodeToByteArray())

    fun decryptGroupMessage(ciphertext: ByteArray, nonce: ByteArray, groupKey: ByteArray): String =
        String(aesGcmDecrypt(groupKey, ciphertext, nonce), Charsets.UTF_8)

    data class EncryptedMedia(
        val ciphertext: ByteArray,
        val nonce: ByteArray,
        val sha256: ByteArray,
        val descriptorJson: String
    )

    fun encryptMedia(bytes: ByteArray, mimeType: String, filename: String, kind: String): EncryptedMedia {
        val mediaKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val (ciphertext, nonce) = aesGcmEncrypt(mediaKey, bytes)
        val sha256 = MessageDigest.getInstance("SHA-256").digest(bytes)
        val descriptorJson = """
            {
              "mediaKey":"${Base64.encodeToString(mediaKey, Base64.NO_WRAP)}",
              "mimeType":"$mimeType",
              "originalFilename":"$filename",
              "kind":"$kind",
              "sizeBytes":${bytes.size},
              "ciphertextSize":${ciphertext.size},
              "sha256":"${Base64.encodeToString(sha256, Base64.NO_WRAP)}"
            }
        """.trimIndent()
        return EncryptedMedia(ciphertext, nonce, sha256, descriptorJson)
    }

    fun decryptMedia(ciphertext: ByteArray, nonce: ByteArray, descriptorJson: String): ByteArray {
        val descriptor = org.json.JSONObject(descriptorJson)
        val mediaKey = Base64.decode(descriptor.getString("mediaKey"), Base64.NO_WRAP)
        val plaintext = aesGcmDecrypt(mediaKey, ciphertext, nonce)
        val actualHash = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(plaintext), Base64.NO_WRAP)
        require(actualHash == descriptor.getString("sha256")) { "attachment hash mismatch" }
        return plaintext
    }

    private fun BigInteger.unsignedBytes(size: Int): ByteArray {
        val b = toByteArray()
        return when {
            b.size == size + 1 && b[0] == 0.toByte() -> b.copyOfRange(1, b.size)
            b.size < size -> ByteArray(size - b.size) + b
            else -> b
        }
    }
}
