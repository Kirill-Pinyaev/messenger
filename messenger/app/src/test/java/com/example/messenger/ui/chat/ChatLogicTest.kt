package com.example.messenger.ui.chat

import com.example.messenger.proto.message
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatLogicTest {
    @Test
    fun `message relevance is based on active conversation id`() {
        val relevant = message {
            conversationId = "direct_1_2"
            from = "1"
            to = "2"
        }
        val irrelevant = message {
            conversationId = "direct_1_3"
            from = "3"
            to = "1"
        }

        assertTrue(isMessageRelevantForChat("direct_1_2", relevant))
        assertFalse(isMessageRelevantForChat("direct_1_2", irrelevant))
    }

    @Test
    fun `direct send always omits conversation id`() {
        assertEquals("", directSendConversationId())
    }

    @Test
    fun `decryption mode follows opened chat type`() {
        assertTrue(shouldUseGroupDecryption(true))
        assertFalse(shouldUseGroupDecryption(false))
    }

    @Test
    fun `same-account message targeted to current signed prekey is decrypted as recipient`() {
        assertFalse(
            shouldDecryptDirectAsSender(
                messageFrom = "alice",
                username = "alice",
                senderDeviceId = "phone",
                currentDeviceId = "phone",
                recipientSignedPrekeyId = "local-spk",
                recipientOneTimePrekeyId = "",
                currentSignedPrekeyId = "local-spk",
                currentOneTimePrekeyIds = emptySet(),
            )
        )
    }

    @Test
    fun `same-account message sent to another device is decrypted as sender copy`() {
        assertTrue(
            shouldDecryptDirectAsSender(
                messageFrom = "alice",
                username = "alice",
                senderDeviceId = "phone",
                currentDeviceId = "phone",
                recipientSignedPrekeyId = "tablet-spk",
                recipientOneTimePrekeyId = "",
                currentSignedPrekeyId = "local-spk",
                currentOneTimePrekeyIds = emptySet(),
            )
        )
    }

    @Test
    fun `same-account message from another device is not decrypted as local sender copy`() {
        assertFalse(
            shouldDecryptDirectAsSender(
                messageFrom = "alice",
                username = "alice",
                senderDeviceId = "web",
                currentDeviceId = "phone",
                recipientSignedPrekeyId = "tablet-spk",
                recipientOneTimePrekeyId = "",
                currentSignedPrekeyId = "local-spk",
                currentOneTimePrekeyIds = emptySet(),
            )
        )
    }
}
