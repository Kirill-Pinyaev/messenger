package com.example.messenger.ui.conversations

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationEventsTest {
    @Test
    fun `reload when message belongs to unknown conversation`() {
        assertTrue(shouldReloadForIncomingMessage(listOf("alice|bob"), "alice|carol"))
    }

    @Test
    fun `do not reload when conversation already exists`() {
        assertFalse(shouldReloadForIncomingMessage(listOf("alice|bob"), "alice|bob"))
    }

    @Test
    fun `ignore blank conversation id`() {
        assertFalse(shouldReloadForIncomingMessage(emptyList(), ""))
    }
}
