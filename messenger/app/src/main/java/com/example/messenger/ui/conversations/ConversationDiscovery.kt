package com.example.messenger.ui.conversations

internal fun directConversationId(a: String, b: String): String =
    if (a <= b) "$a|$b" else "$b|$a"

internal fun shouldSearchUsers(query: String): Boolean = query.trim().isNotEmpty()
