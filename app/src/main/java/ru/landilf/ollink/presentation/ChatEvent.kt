package ru.landilf.ollink.presentation

sealed interface ChatEvent {
    data class CopyToClipboard(val text: String) : ChatEvent
}
