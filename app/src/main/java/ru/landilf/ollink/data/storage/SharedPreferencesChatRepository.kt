package ru.landilf.ollink.data.storage

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import ru.landilf.ollink.domain.model.ChatHistory
import ru.landilf.ollink.domain.repository.ChatRepository

class SharedPreferencesChatRepository(
    context: Context,
    applicationScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChatRepository {
    private val preferences = context.getSharedPreferences("history", Context.MODE_PRIVATE)
    private val snapshots = Channel<ChatHistory>(Channel.CONFLATED)
    init {
        applicationScope.launch(ioDispatcher) {
            for (history in snapshots) {
                val encoded = ChatHistoryCodec.encode(history)
                preferences.edit {
                    putString("chats", encoded)
                    putString("currentId", history.currentId)
                }
            }
        }
    }
    override suspend fun load(): ChatHistory = withContext(ioDispatcher) {
        ChatHistoryCodec.decode(preferences.getString("chats", "[]").orEmpty(),
            preferences.getString("currentId", "").orEmpty())
    }
    override fun save(history: ChatHistory) { snapshots.trySend(history) }
}
