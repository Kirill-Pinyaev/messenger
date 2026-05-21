package com.example.messenger.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
