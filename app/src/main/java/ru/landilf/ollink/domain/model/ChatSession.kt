package ru.landilf.ollink.domain.model

import java.util.UUID

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val server: String = "",
    val model: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

object Conversation {
    fun title(prompt: String): String = prompt.trim().replace(Regex("\\s+"), " ").take(48)

    /** Context for regenerating the last turn, excluding its previous answer. */
    fun retryContext(messages: List<ChatMessage>): List<ChatMessage> {
        val index = messages.indexOfLast { it.role == "user" }
        return if (index < 0) emptyList() else messages.take(index + 1)
    }

    fun finish(messages: List<ChatMessage>): List<ChatMessage> =
        messages.filterNot { it.role == "assistant" && it.content.isBlank() }
}
