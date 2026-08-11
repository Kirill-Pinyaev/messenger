package com.example.messenger.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatArchiveTest {
    @Test
    fun `mergeArchivedMessages replaces unavailable placeholder with archived content`() {
        val live = listOf(
            ChatMessage(
                id = 7,
                conversationId = "1|2",
                from = "1",
                text = "[Сообщение недоступно на этом устройстве]",
                createdAt = 100,
                encrypted = false,
                decryptionError = false,
                isMine = false,
            )
        )
        val archived = listOf(
            ChatMessage(
                id = 7,
                conversationId = "1|2",
                from = "1",
                text = "привет из архива",
                createdAt = 100,
                encrypted = true,
                decryptionError = false,
                isMine = false,
            )
        )

        val merged = mergeArchivedMessages(live, archived)

        assertEquals(1, merged.size)
        assertEquals("привет из архива", merged.first().text)
        assertFalse(merged.first().decryptionError)
    }
}
