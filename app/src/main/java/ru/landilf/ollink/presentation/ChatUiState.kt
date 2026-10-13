package ru.landilf.ollink.presentation

import ru.landilf.ollink.domain.model.*

data class ChatUiState(
    val history: ChatHistory = ChatHistory(),
    val server: String = "", val models: List<String> = emptyList(),
    val loading: Boolean = true, val connecting: Boolean = false,
    val generating: Boolean = false, val connected: Boolean = false,
    val notice: ChatNotice? = null
) {
    val current: ChatSession? get() = history.current
    val busy: Boolean get() = loading || connecting || generating
    val canSend: Boolean get() = connected && current?.model in models && current?.server == server
}

sealed interface ChatNotice {
    data object Stopped : ChatNotice
    data object NoModels : ChatNotice
    data object HistoryLoadFailed : ChatNotice
    data object ConnectionFailed : ChatNotice
    data class Error(val message: String) : ChatNotice
}
