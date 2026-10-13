package ru.landilf.ollink.domain.model

data class ChatHistory(val chats: List<ChatSession> = emptyList(), val currentId: String = "") {
    val current: ChatSession? get() = chats.firstOrNull { it.id == currentId }
    fun replace(chat: ChatSession): ChatHistory = copy(chats = chats.map { if (it.id == chat.id) chat else it })
    fun normalized(settings: ConnectionSettings): ChatHistory {
        val saved = chats.distinctBy { it.id }.ifEmpty {
            listOf(ChatSession(server = settings.server, model = settings.model))
        }
        return copy(chats = saved, currentId = currentId.takeIf { id -> saved.any { it.id == id } } ?: saved.first().id)
    }
}

data class ConnectionSettings(val server: String = "", val model: String = "")
