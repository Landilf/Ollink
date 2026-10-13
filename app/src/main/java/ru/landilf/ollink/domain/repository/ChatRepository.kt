package ru.landilf.ollink.domain.repository

import kotlinx.coroutines.flow.StateFlow
import ru.landilf.ollink.domain.model.ChatHistory
import ru.landilf.ollink.domain.model.ConnectionSettings

interface ChatRepository {
    suspend fun load(): ChatHistory
    /** Enqueues the latest snapshot; writing outlives an individual ViewModel. */
    fun save(history: ChatHistory)
}

interface SettingsRepository {
    val settings: StateFlow<ConnectionSettings>
    fun save(settings: ConnectionSettings)
}
