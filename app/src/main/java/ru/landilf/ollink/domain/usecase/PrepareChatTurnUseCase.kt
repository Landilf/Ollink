package ru.landilf.ollink.domain.usecase

import ru.landilf.ollink.domain.model.*

class PrepareChatTurnUseCase {
    fun send(chat: ChatSession, now: Long = System.currentTimeMillis()): ChatSession? {
        val prompt = chat.draft.trim()
        if (prompt.isEmpty()) return null
        return chat.copy(draft = "", updatedAt = now,
            title = if (chat.messages.isEmpty() && (chat.title.isBlank() || chat.title == "Новый чат"))
                Conversation.title(prompt) else chat.title,
            messages = Conversation.finish(chat.messages) + ChatMessage("user", prompt))
    }
    fun retry(chat: ChatSession): ChatSession? {
        val history = Conversation.retryContext(chat.messages)
        return if (history.isEmpty()) null else chat.copy(messages = history)
    }
}
