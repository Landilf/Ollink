package ru.landilf.ollink.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.landilf.ollink.domain.model.ChatMessage

interface OllamaRepository {
    suspend fun models(server: String): List<String>
    /** Emits the complete text received so far; cancellation closes the HTTP request. */
    fun answer(server: String, model: String, messages: List<ChatMessage>): Flow<String>
}
