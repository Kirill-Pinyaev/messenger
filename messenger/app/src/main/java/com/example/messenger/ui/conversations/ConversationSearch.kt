package com.example.messenger.ui.conversations

internal fun filterConversationItems(items: List<ConvItem>, query: String): List<ConvItem> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return items
    return items.filter { item ->
        item.displayName.contains(normalized, ignoreCase = true) ||
            item.peerUsername.contains(normalized, ignoreCase = true)
    }
}
