package com.example.messenger.ui.chat

import com.example.messenger.proto.ConversationKeyEnvelope
import com.example.messenger.proto.PrekeyBundle

internal data class DirectBundleTarget(
    val username: String,
    val deviceId: String,
    val signedPrekeyId: String,
    val signedPrekeyPublic: ByteArray,
    val oneTimePrekeyId: String,
    val oneTimePrekeyPublic: ByteArray?,
)

internal fun buildDirectBundleTargets(
    currentUsername: String,
    currentDeviceId: String,
    localSignedPrekeyId: String,
    localSignedPrekeyPublic: ByteArray,
    recipientBundles: List<PrekeyBundle>,
    ownBundles: List<PrekeyBundle>,
): List<DirectBundleTarget> {
    val out = mutableListOf<DirectBundleTarget>()
    val seen = linkedSetOf<Pair<String, String>>()

    fun append(
        username: String,
        deviceId: String,
        signedPrekeyId: String,
        signedPrekeyPublic: ByteArray,
        oneTimePrekeyId: String,
        oneTimePrekeyPublic: ByteArray?,
    ) {
        val key = username to deviceId
        if (!seen.add(key)) return
        out += DirectBundleTarget(
            username = username,
            deviceId = deviceId,
            signedPrekeyId = signedPrekeyId,
            signedPrekeyPublic = signedPrekeyPublic,
            oneTimePrekeyId = oneTimePrekeyId,
            oneTimePrekeyPublic = oneTimePrekeyPublic,
        )
    }

    recipientBundles.forEach { bundle ->
        append(
            username = bundle.username,
            deviceId = bundle.deviceId,
            signedPrekeyId = bundle.signedPrekey.keyId,
            signedPrekeyPublic = bundle.signedPrekey.publicKey.toByteArray(),
            oneTimePrekeyId = bundle.oneTimePrekey.keyId,
            oneTimePrekeyPublic = bundle.oneTimePrekey.publicKey.takeIf { !it.isEmpty }?.toByteArray(),
        )
    }

    append(
        username = currentUsername,
        deviceId = currentDeviceId,
        signedPrekeyId = localSignedPrekeyId,
        signedPrekeyPublic = localSignedPrekeyPublic,
        oneTimePrekeyId = "",
        oneTimePrekeyPublic = null,
    )

    ownBundles.forEach { bundle ->
        append(
            username = bundle.username,
            deviceId = bundle.deviceId,
            signedPrekeyId = bundle.signedPrekey.keyId,
            signedPrekeyPublic = bundle.signedPrekey.publicKey.toByteArray(),
            oneTimePrekeyId = bundle.oneTimePrekey.keyId,
            oneTimePrekeyPublic = bundle.oneTimePrekey.publicKey.takeIf { !it.isEmpty }?.toByteArray(),
        )
    }

    return out
}

internal fun findConversationEnvelopeForDevice(
    envelopes: List<ConversationKeyEnvelope>,
    username: String,
    deviceId: String,
): ConversationKeyEnvelope? = envelopes.firstOrNull {
    it.username == username && it.deviceId == deviceId
}
