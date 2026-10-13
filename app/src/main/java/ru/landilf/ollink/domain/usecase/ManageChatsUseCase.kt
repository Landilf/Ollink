package ru.landilf.ollink.domain.usecase

import ru.landilf.ollink.domain.model.*

class ManageChatsUseCase {
    fun newChat(history: ChatHistory, settings: ConnectionSettings): ChatHistory {
        val chat = ChatSession(server = settings.server, model = settings.model)
        return ChatHistory(listOf(chat) + history.chats.filterNot {
            it.messages.isEmpty() && it.draft.isBlank()
        }, chat.id)
    }
    fun open(history: ChatHistory, id: String): ChatHistory =
        if (history.chats.any { it.id == id }) history.copy(currentId = id) else history
    fun rename(history: ChatHistory, id: String, title: String): ChatHistory {
        val chat = history.chats.firstOrNull { it.id == id } ?: return history
        return if (title.isBlank()) history else history.replace(chat.copy(title = title.trim().take(100)))
    }
    fun delete(history: ChatHistory, id: String, settings: ConnectionSettings): ChatHistory =
        history.copy(chats = history.chats.filterNot { it.id == id }).normalized(settings)
    fun selectModel(history: ChatHistory, settings: ConnectionSettings): ChatHistory {
        val chat = history.current ?: return history.normalized(settings)
        return if (chat.messages.isEmpty()) history.replace(chat.copy(server = settings.server, model = settings.model))
        else if (chat.model != settings.model || chat.server != settings.server) newChat(history, settings)
        else history
    }
}
