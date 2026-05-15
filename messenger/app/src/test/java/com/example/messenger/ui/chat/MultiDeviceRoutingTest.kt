package com.example.messenger.ui.chat

import com.example.messenger.proto.ConversationKeyEnvelope
import com.example.messenger.proto.IdentityKey
import com.example.messenger.proto.OneTimePrekey
import com.example.messenger.proto.PrekeyBundle
import com.example.messenger.proto.SignedPrekey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MultiDeviceRoutingTest {

    @Test
    fun buildDirectBundleTargets_includesRecipientDevicesAndAllOwnDevices() {
        val targets = buildDirectBundleTargets(
            currentUsername = "alice",
            currentDeviceId = "alice-phone",
            localSignedPrekeyId = "alice-local-spk",
            localSignedPrekeyPublic = byteArrayOf(1, 1, 1),
            recipientBundles = listOf(
                prekeyBundle("bob", "bob-web", "bob-web-spk", "bob-web-otp"),
                prekeyBundle("bob", "bob-phone", "bob-phone-spk", "bob-phone-otp"),
            ),
            ownBundles = listOf(
                prekeyBundle("alice", "alice-phone", "alice-remote-phone", "alice-phone-otp"),
                prekeyBundle("alice", "alice-tablet", "alice-tablet-spk", "alice-tablet-otp"),
            ),
        )

        assertEquals(
            listOf(
                "bob|bob-web|bob-web-spk|bob-web-otp",
                "bob|bob-phone|bob-phone-spk|bob-phone-otp",
                "alice|alice-phone|alice-local-spk|",
                "alice|alice-tablet|alice-tablet-spk|alice-tablet-otp",
            ),
            targets.map { "${it.username}|${it.deviceId}|${it.signedPrekeyId}|${it.oneTimePrekeyId}" },
        )
    }

    @Test
    fun buildDirectBundleTargets_prefersLocalCurrentDeviceOverServerCopy() {
        val targets = buildDirectBundleTargets(
            currentUsername = "alice",
            currentDeviceId = "alice-phone",
            localSignedPrekeyId = "alice-local-spk",
            localSignedPrekeyPublic = byteArrayOf(7, 7, 7),
            recipientBundles = emptyList(),
            ownBundles = listOf(
                prekeyBundle("alice", "alice-phone", "alice-stale-spk", "alice-stale-otp"),
            ),
        )

        assertEquals(1, targets.size)
        assertEquals("alice-local-spk", targets.single().signedPrekeyId)
        assertEquals("", targets.single().oneTimePrekeyId)
    }

    @Test
    fun findConversationEnvelopeForDevice_matchesExactDevice() {
        val envelope = findConversationEnvelopeForDevice(
            listOf(
                ConversationKeyEnvelope.newBuilder().setUsername("alice").setDeviceId("alice-web").build(),
                ConversationKeyEnvelope.newBuilder().setUsername("alice").setDeviceId("alice-phone").build(),
            ),
            username = "alice",
            deviceId = "alice-phone",
        )

        assertNotNull(envelope)
        assertEquals("alice-phone", envelope!!.deviceId)
    }

    @Test
    fun findConversationEnvelopeForDevice_returnsNullWhenMissing() {
        val envelope = findConversationEnvelopeForDevice(
            listOf(
                ConversationKeyEnvelope.newBuilder().setUsername("alice").setDeviceId("alice-web").build(),
            ),
            username = "alice",
            deviceId = "alice-phone",
        )

        assertNull(envelope)
    }

    private fun prekeyBundle(
        username: String,
        deviceId: String,
        signedPrekeyId: String,
        oneTimePrekeyId: String,
    ): PrekeyBundle =
        PrekeyBundle.newBuilder()
            .setUsername(username)
            .setDeviceId(deviceId)
            .setIdentityKey(
                IdentityKey.newBuilder()
                    .setUsername(username)
                    .setDeviceId(deviceId)
                    .setKeyId("$deviceId-identity")
                    .build(),
            )
            .setSignedPrekey(
                SignedPrekey.newBuilder()
                    .setUsername(username)
                    .setDeviceId(deviceId)
                    .setKeyId(signedPrekeyId)
                    .setPublicKey(com.google.protobuf.ByteString.copyFromUtf8(signedPrekeyId))
                    .build(),
            )
            .setOneTimePrekey(
                OneTimePrekey.newBuilder()
                    .setUsername(username)
                    .setDeviceId(deviceId)
                    .setKeyId(oneTimePrekeyId)
                    .setPublicKey(com.google.protobuf.ByteString.copyFromUtf8(oneTimePrekeyId))
                    .build(),
            )
            .build()
}
