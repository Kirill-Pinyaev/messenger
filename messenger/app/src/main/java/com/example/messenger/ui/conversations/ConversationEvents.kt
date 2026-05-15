package com.example.messenger.ui.conversations

internal fun shouldReloadForIncomingMessage(existingConversationIds: List<String>, conversationId: String): Boolean {
    if (conversationId.isBlank()) return false
    return conversationId !in existingConversationIds
}
