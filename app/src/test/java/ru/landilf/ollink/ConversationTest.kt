package ru.landilf.ollink

import ru.landilf.ollink.domain.model.*
import ru.landilf.ollink.data.network.*

import org.junit.Assert.*
import org.junit.Test

class ConversationTest {
    @Test fun retryExcludesOnlyLastAnswerAndKeepsContext() {
        val messages = listOf(ChatMessage("user", "First"), ChatMessage("assistant", "Answer"),
            ChatMessage("user", "Second"), ChatMessage("assistant", "Partial"))
        assertEquals(messages.take(3), Conversation.retryContext(messages))
        assertEquals(messages.take(3), Conversation.retryContext(messages.take(3)))
        assertTrue(Conversation.retryContext(emptyList()).isEmpty())
    }
    @Test fun stopRemovesEmptyPlaceholderButRetainsPartialAnswerAndPrompt() {
        val prompt = ChatMessage("user", "Question")
        assertEquals(listOf(prompt), Conversation.finish(listOf(prompt, ChatMessage("assistant", ""))))
        val partial = ChatMessage("assistant", "Half an answer")
        assertEquals(listOf(prompt, partial), Conversation.finish(listOf(prompt, partial)))
    }
    @Test fun titlesCollapseWhitespaceAndLimitLength() {
        assertEquals("Explain this topic", Conversation.title("  Explain\n this   topic  "))
        assertEquals(48, Conversation.title("a".repeat(100)).length)
    }
}
