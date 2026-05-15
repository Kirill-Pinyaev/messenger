package com.example.messenger.ui.chat

import com.example.messenger.proto.Message

internal fun isMessageRelevantForChat(
    activeConversationId: String,
    msg: Message
): Boolean = msg.conversationId == activeConversationId

internal fun directSendConversationId(): String = ""

internal fun shouldUseGroupDecryption(isGroupChat: Boolean): Boolean = isGroupChat

internal fun shouldDecryptDirectAsSender(
    messageFrom: String,
    username: String,
    senderDeviceId: String,
    currentDeviceId: String,
    recipientSignedPrekeyId: String,
    recipientOneTimePrekeyId: String,
    currentSignedPrekeyId: String,
    currentOneTimePrekeyIds: Set<String>,
): Boolean {
    if (messageFrom != username) {
        return false
    }
    if (senderDeviceId.isNotBlank() && senderDeviceId != currentDeviceId) {
        return false
    }
    if (recipientSignedPrekeyId.isNotBlank() && recipientSignedPrekeyId == currentSignedPrekeyId) {
        return false
    }
    if (recipientOneTimePrekeyId.isNotBlank() && recipientOneTimePrekeyId in currentOneTimePrekeyIds) {
        return false
    }
    return true
}
