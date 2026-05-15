package com.example.messenger.ui.conversations

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationSearchTest {
    private val items = listOf(
        ConvItem(
            conversationId = "c1",
            displayName = "Анна Петрова",
            peerUsername = "anna",
            isGroup = false,
            members = emptyList()
        ),
        ConvItem(
            conversationId = "c2",
            displayName = "Backend Team",
            peerUsername = "",
            isGroup = true,
            members = listOf("anna", "bob")
        )
    )

    @Test
    fun `returns all items for blank query`() {
        assertEquals(items, filterConversationItems(items, "   "))
    }

    @Test
    fun `matches by display name`() {
        assertEquals(listOf(items[0]), filterConversationItems(items, "анна"))
    }

    @Test
    fun `matches by username`() {
        assertEquals(listOf(items[0]), filterConversationItems(items, "ann"))
    }
}
