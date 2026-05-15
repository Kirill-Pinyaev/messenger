package com.example.messenger.ui.conversations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationDiscoveryTest {
    @Test
    fun `direct conversation id is deterministic`() {
        assertEquals("alice|bob", directConversationId("alice", "bob"))
        assertEquals("alice|bob", directConversationId("bob", "alice"))
    }

    @Test
    fun `search users only for non blank query`() {
        assertFalse(shouldSearchUsers("   "))
        assertTrue(shouldSearchUsers("anna"))
    }
}
