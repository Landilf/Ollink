package ru.landilf.ollink

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import ru.landilf.ollink.domain.model.*
import ru.landilf.ollink.domain.repository.*
import ru.landilf.ollink.domain.usecase.*
import ru.landilf.ollink.presentation.*
import ru.landilf.ollink.data.storage.ChatHistoryCodec

@OptIn(ExperimentalCoroutinesApi::class)
class ChatArchitectureTest {
    private class Settings : SettingsRepository {
        override val settings = MutableStateFlow(ConnectionSettings())
        override fun save(settings: ConnectionSettings) { this.settings.value = settings }
    }
    private class History : ChatRepository {
        var saved = ChatHistory()
        override suspend fun load() = saved
        override fun save(history: ChatHistory) { saved = history }
    }
    private class Ollama : OllamaRepository {
        var slow = false
        var suppliedHistory: List<ChatMessage> = emptyList()
        override suspend fun models(server: String) = listOf("first", "second")
        override fun answer(server: String, model: String, messages: List<ChatMessage>) = flow {
            suppliedHistory = messages
            emit("Partial")
            if (slow) delay(1000)
            emit("Complete")
        }
    }
    @Test fun viewModelRoutesActionsCancelsStreamAndPreservesDraftAndRetryContext() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val history = History()
            val backend = Ollama()
            val viewModel = ChatViewModel(Settings(), history, backend)
            runCurrent()
            viewModel.onAction(ChatAction.Connect("http://localhost:11434")); runCurrent()
            assertTrue(viewModel.uiState.value.canSend)
            viewModel.onAction(ChatAction.DraftChanged("Question"))
            backend.slow = true
            viewModel.onAction(ChatAction.Send); runCurrent()
            assertEquals("Partial", viewModel.uiState.value.current!!.messages.last().content)
            viewModel.onAction(ChatAction.DraftChanged("Next question"))
            viewModel.onAction(ChatAction.Stop); advanceUntilIdle()
            assertFalse(viewModel.uiState.value.generating)
            assertEquals("Partial", viewModel.uiState.value.current!!.messages.last().content)
            assertEquals("Next question", history.saved.current!!.draft)
            backend.slow = false
            viewModel.onAction(ChatAction.Retry); advanceUntilIdle()
            assertEquals(listOf(ChatMessage("user", "Question")), backend.suppliedHistory)
            assertEquals("Complete", viewModel.uiState.value.current!!.messages.last().content)
            assertEquals(2, viewModel.uiState.value.current!!.messages.size)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun changingModelOrServerArchivesContextInsteadOfSendingItElsewhere() {
        val original = ChatSession(server = "http://first:11434", model = "first",
            messages = listOf(ChatMessage("user", "Private context")))
        val history = ChatHistory(listOf(original), original.id)
        val updated = ManageChatsUseCase().selectModel(history, ConnectionSettings("http://second:11434", "second"))
        assertEquals(2, updated.chats.size)
        assertEquals(original, updated.chats.first { it.id == original.id })
        assertTrue(updated.current!!.messages.isEmpty())
        assertEquals("http://second:11434", updated.current!!.server)
    }
    @Test fun codecReadsLegacySchemaAndKeepsChatsDraftsAndSelection() {
        val json = """[{"id":"legacy","title":"Old chat","server":"http://laptop:11434","model":"test","draft":"Unsent","updatedAt":123,"messages":[{"role":"user","content":"Hello"},{"role":"assistant","content":"Answer"}]}]"""
        val decoded = ChatHistoryCodec.decode(json, "legacy")
        assertEquals("Unsent", decoded.current!!.draft)
        assertEquals(2, decoded.current!!.messages.size)
        assertEquals(decoded, ChatHistoryCodec.decode(ChatHistoryCodec.encode(decoded), "legacy"))
    }
    @Test fun deletingSelectedChatAndInvalidSavedIdAlwaysProvideAValidSelection() {
        val a = ChatSession(id = "a"); val b = ChatSession(id = "b")
        val useCase = ManageChatsUseCase()
        val selected = useCase.delete(ChatHistory(listOf(a, b), "a"), "a", ConnectionSettings())
        assertEquals("b", selected.currentId)
        val lastDeleted = useCase.delete(selected, "b", ConnectionSettings("http://laptop", "model"))
        assertNotNull(lastDeleted.current)
        assertEquals("model", lastDeleted.current!!.model)
        assertEquals("a", ChatHistory(listOf(a), "missing").normalized(ConnectionSettings()).currentId)
    }
}
