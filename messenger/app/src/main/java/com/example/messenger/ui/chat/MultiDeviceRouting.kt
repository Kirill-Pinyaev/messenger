package com.example.messenger.ui.chat

import com.example.messenger.proto.ConversationKeyEnvelope
import com.example.messenger.proto.PrekeyBundle

internal data class DirectBundleTarget(
    val username: String,
    val deviceId: String,
    val identityKeyId: String,
    val identityPublic: ByteArray,
    val signedPrekeyId: String,
    val signedPrekeyPublic: ByteArray,
    val signedPrekeySignature: ByteArray,
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
        identityKeyId: String,
        identityPublic: ByteArray,
        signedPrekeyId: String,
        signedPrekeyPublic: ByteArray,
        signedPrekeySignature: ByteArray,
        oneTimePrekeyId: String,
        oneTimePrekeyPublic: ByteArray?,
    ) {
        val key = username to deviceId
        if (!seen.add(key)) return
        out += DirectBundleTarget(
            username = username,
            deviceId = deviceId,
            identityKeyId = identityKeyId,
            identityPublic = identityPublic,
            signedPrekeyId = signedPrekeyId,
            signedPrekeyPublic = signedPrekeyPublic,
            signedPrekeySignature = signedPrekeySignature,
            oneTimePrekeyId = oneTimePrekeyId,
            oneTimePrekeyPublic = oneTimePrekeyPublic,
        )
    }

    recipientBundles.forEach { bundle ->
        append(
            username = bundle.username,
            deviceId = bundle.deviceId,
            identityKeyId = bundle.identityKey.keyId,
            identityPublic = bundle.identityKey.publicKey.toByteArray(),
            signedPrekeyId = bundle.signedPrekey.keyId,
            signedPrekeyPublic = bundle.signedPrekey.publicKey.toByteArray(),
            signedPrekeySignature = bundle.signedPrekey.signature.toByteArray(),
            oneTimePrekeyId = bundle.oneTimePrekey.keyId,
            oneTimePrekeyPublic = bundle.oneTimePrekey.publicKey.takeIf { !it.isEmpty }?.toByteArray(),
        )
    }

    ownBundles.forEach { bundle ->
        if (bundle.username == currentUsername && bundle.deviceId == currentDeviceId) return@forEach
        append(
            username = bundle.username,
            deviceId = bundle.deviceId,
            identityKeyId = bundle.identityKey.keyId,
            identityPublic = bundle.identityKey.publicKey.toByteArray(),
            signedPrekeyId = bundle.signedPrekey.keyId,
            signedPrekeyPublic = bundle.signedPrekey.publicKey.toByteArray(),
            signedPrekeySignature = bundle.signedPrekey.signature.toByteArray(),
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
