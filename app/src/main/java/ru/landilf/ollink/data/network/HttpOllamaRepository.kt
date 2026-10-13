package ru.landilf.ollink.data.network

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import ru.landilf.ollink.domain.model.ChatMessage
import ru.landilf.ollink.domain.repository.OllamaRepository

class HttpOllamaRepository(
    private val client: OllamaClient = OllamaClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : OllamaRepository {
    override suspend fun models(server: String): List<String> = request<List<String>> { token, emit ->
        emit(client.models(server, token))
    }.first()

    override fun answer(server: String, model: String, messages: List<ChatMessage>): Flow<String> =
        request { token, emit ->
            val answer = client.chat(server, model, messages, token, emit)
            emit(answer) // Always publish the terminal text, including its last token.
        }

    private fun <T> request(block: (RequestCancellation, (T) -> Unit) -> Unit): Flow<T> = callbackFlow {
        val token = RequestCancellation()
        val worker = launch(ioDispatcher) {
            try {
                block(token) { trySend(it) }
                close()
            } catch (e: Exception) { close(e) }
        }
        awaitClose {
            worker.cancel()
            token.cancel()
        }
    }.buffer(Channel.CONFLATED).flowOn(ioDispatcher)
}
