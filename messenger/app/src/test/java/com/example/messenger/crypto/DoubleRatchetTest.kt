package com.example.messenger.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class DoubleRatchetTest {
    @Test
    fun signedPrekeyVerifiesAndTamperingFails() {
        val bob = DoubleRatchet.createIdentity("bob", "phone")
        val bundle = bob.toPrekeyBundle()

        assertTrue(DoubleRatchet.verifySignedPrekey(bundle))

        val tampered = bundle.copy(signedPrekeyPublic = bundle.signedPrekeyPublic.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() })
        assertFalse(DoubleRatchet.verifySignedPrekey(tampered))
    }

    @Test
    fun encryptsAndDecryptsSequentialMessages() {
        val alice = DoubleRatchet.createIdentity("alice", "web")
        val bob = DoubleRatchet.createIdentity("bob", "phone")

        val first = DoubleRatchet.encrypt("hello", alice, bob.toPrekeyBundle(), "alice|bob")
        val firstPlain = DoubleRatchet.decrypt(first, bob, alice.toIdentityKey(), "alice|bob")
        val second = DoubleRatchet.encrypt("again", alice, bob.toPrekeyBundle(), "alice|bob")
        val secondPlain = DoubleRatchet.decrypt(second, bob, alice.toIdentityKey(), "alice|bob")

        assertEquals("hello", firstPlain)
        assertEquals("again", secondPlain)
        assertEquals(DoubleRatchet.ALGORITHM, first.e2eeAlgorithm)
        assertEquals(first.messageNumber + 1, second.messageNumber)
    }

    @Test
    fun ratchetAdvancesAfterBothSidesSend() {
        val alice = DoubleRatchet.createIdentity("alice", "web")
        val bob = DoubleRatchet.createIdentity("bob", "phone")

        val a1 = DoubleRatchet.encrypt("a1", alice, bob.toPrekeyBundle(), "alice|bob")
        assertEquals("a1", DoubleRatchet.decrypt(a1, bob, alice.toIdentityKey(), "alice|bob"))

        val b1 = DoubleRatchet.encrypt("b1", bob, alice.toPrekeyBundle(), "alice|bob")
        assertEquals("b1", DoubleRatchet.decrypt(b1, alice, bob.toIdentityKey(), "alice|bob"))

        val a2 = DoubleRatchet.encrypt("a2", alice, bob.toPrekeyBundle(), "alice|bob")
        assertEquals("a2", DoubleRatchet.decrypt(a2, bob, alice.toIdentityKey(), "alice|bob"))
        assertNotEquals(a1.ratchetPublicKey.toList(), a2.ratchetPublicKey.toList())
    }

    @Test
    fun exportImportPreservesSessionState() {
        val alice = DoubleRatchet.createIdentity("alice", "web")
        val bob = DoubleRatchet.createIdentity("bob", "phone")

        val first = DoubleRatchet.encrypt("persist me", alice, bob.toPrekeyBundle(), "alice|bob")
        assertEquals("persist me", DoubleRatchet.decrypt(first, bob, alice.toIdentityKey(), "alice|bob"))

        val restored = DoubleRatchet.importIdentity(DoubleRatchet.exportIdentity(bob))
        val second = DoubleRatchet.encrypt("after restore", alice, bob.toPrekeyBundle(), "alice|bob")

        assertEquals("after restore", DoubleRatchet.decrypt(second, restored, alice.toIdentityKey(), "alice|bob"))
    }

    @Test
    fun exportImportPreservesSenderMessageKeysForOwnDecrypt() {
        val alice = DoubleRatchet.createIdentity("alice", "android")
        val bob = DoubleRatchet.createIdentity("bob", "web")

        val outbound = DoubleRatchet.encrypt("own copy", alice, bob.toPrekeyBundle(), "alice|bob")
        val restoredAlice = DoubleRatchet.importIdentity(DoubleRatchet.exportIdentity(alice))

        assertEquals("own copy", DoubleRatchet.decrypt(outbound, restoredAlice, bob.toIdentityKey(), "alice|bob"))
    }

    @Test
    fun decryptsOlderSkippedMessageAfterLaterMessage() {
        val alice = DoubleRatchet.createIdentity("alice", "web")
        val bob = DoubleRatchet.createIdentity("bob", "android")

        val first = DoubleRatchet.encrypt("first", alice, bob.toPrekeyBundle(), "alice|bob")
        val second = DoubleRatchet.encrypt("second", alice, bob.toPrekeyBundle(), "alice|bob")

        assertEquals("second", DoubleRatchet.decrypt(second, bob, alice.toIdentityKey(), "alice|bob"))
        assertEquals("first", DoubleRatchet.decrypt(first, bob, alice.toIdentityKey(), "alice|bob"))
    }

    @Test
    fun decryptsIndependentInitialMessagesAfterBothSidesSendBeforeReceiving() {
        val alice = DoubleRatchet.createIdentity("alice", "web")
        val bob = DoubleRatchet.createIdentity("bob", "android")

        val aliceFirst = DoubleRatchet.encrypt("alice first", alice, bob.toPrekeyBundle(), "alice|bob")
        val bobFirst = DoubleRatchet.encrypt("bob first", bob, alice.toPrekeyBundle(), "alice|bob")

        assertEquals("alice first", DoubleRatchet.decrypt(aliceFirst, bob, alice.toIdentityKey(), "alice|bob"))
        assertEquals("bob first", DoubleRatchet.decrypt(bobFirst, alice, bob.toIdentityKey(), "alice|bob"))
    }

    @Test
    fun rejectsInvalidSignedPrekeyBeforeEncrypting() {
        val alice = DoubleRatchet.createIdentity("alice", "web")
        val bob = DoubleRatchet.createIdentity("bob", "phone")
        val bundle = bob.toPrekeyBundle()
        val invalid = bundle.copy(signedPrekeySignature = bundle.signedPrekeySignature.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() })

        try {
            DoubleRatchet.encrypt("nope", alice, invalid, "alice|bob")
            throw AssertionError("encrypt should have failed")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message!!.contains("invalid signed prekey signature", ignoreCase = true))
        }
    }

    @Test
    fun fixedVectorRoundTripKeepsBytesStableInsideJvm() {
        val alice = DoubleRatchet.createIdentity("alice", "web")
        val exported = DoubleRatchet.exportIdentity(alice)
        val restored = DoubleRatchet.importIdentity(exported)

        assertArrayEquals(alice.identityPublic, restored.identityPublic)
        assertArrayEquals(alice.signedPrekeySignature, restored.signedPrekeySignature)
    }

    @Test
    fun decryptsWebGeneratedMessageVector() {
        val bob = RatchetIdentity(
            username = "bob",
            deviceId = "android",
            keyId = "bob-identity-mpgmqel8-38g1fujk",
            identityPrivate = b64("ulRJy4aCCGre5tehpFqMqLGOGul0Qa0jKzKEkiLbsRc="),
            identityPublic = b64("rpL9jfX3gCYm4m8wCnRf6DjMCX5XM5rvT1mbNRMFrIw="),
            signedPrekeyId = "bob-spk-mpgmqel8-vadir4dx",
            signedPrekeyPrivate = b64("A2ON/Bj87DrwLbN78YdfBGhN6Ps9tBMWWZ9pa2p3WoI="),
            signedPrekeyPublic = b64("MYnbz6nN7fkQaU3wTEuyFIyyamVZXrO5eUGjxbBp4ic="),
            signedPrekeySignature = b64("4iDXby9ab9buSCQDo7LN2lU3vPeOIK5I5yoTEDWvoWtGVL14Qgp3Y2+ICEx1k5QsBqYC2wXwhZyohL+HJWixDQ=="),
        )
        val alice = RatchetIdentityKey(
            username = "alice",
            deviceId = "web",
            keyId = "alice-identity-mpgmqel6-j2pqqifa",
            publicKey = b64("75iCTjpdoNwMgwGUj/3lxOgpSBdpzY1DdZvge8LVCCg="),
        )
        val message = RatchetMessage(
            ciphertext = b64("Bj04vcxtq/bR07V7DuUtKbc3lAfvek2HX2uYptGq"),
            nonce = b64("gAx4abjbc+cSxcY7"),
            senderKeyId = alice.keyId,
            recipientSignedPrekeyId = bob.signedPrekeyId,
            recipientSignedPrekeyPublic = bob.signedPrekeyPublic,
            recipientOneTimePrekeyId = "",
            recipientOneTimePrekeyPublic = null,
            e2eeAlgorithm = DoubleRatchet.ALGORITHM,
            ratchetPublicKey = b64("k4Ntp0QarO37bcZcNfp4N3aknM1lbjPmciRjBngcURc="),
            previousChainLength = 0,
            messageNumber = 0,
        )

        assertEquals("web to android", DoubleRatchet.decrypt(message, bob, alice, "alice|bob"))
    }

    private fun b64(value: String): ByteArray = Base64.getDecoder().decode(value)
}
