package ru.landilf.ollink.presentation

sealed interface ChatAction {
    data class Connect(val server: String) : ChatAction
    data class DraftChanged(val text: String) : ChatAction
    data class SelectModel(val model: String) : ChatAction
    data class OpenChat(val id: String) : ChatAction
    data class RenameChat(val id: String, val title: String) : ChatAction
    data class DeleteChat(val id: String) : ChatAction
    data class CopyText(val text: String) : ChatAction
    data object NewChat : ChatAction
    data object Send : ChatAction
    data object Retry : ChatAction
    data object Stop : ChatAction
    data object PersistHistory : ChatAction
}
