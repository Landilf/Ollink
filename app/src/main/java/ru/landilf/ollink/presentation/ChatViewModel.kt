package ru.landilf.ollink.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.landilf.ollink.domain.model.*
import ru.landilf.ollink.domain.repository.*
import ru.landilf.ollink.domain.usecase.*

class ChatViewModel(
    private val settingsRepository: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val ollamaRepository: OllamaRepository
) : ViewModel() {
    private val manageChats = ManageChatsUseCase()
    private val prepareTurn = PrepareChatTurnUseCase()
    private val updateSettings = UpdateConnectionSettingsUseCase(settingsRepository)
    private val mutableState = MutableStateFlow(ChatUiState(server = settingsRepository.settings.value.server))
    val uiState = mutableState.asStateFlow()
    private val mutableEvents = MutableSharedFlow<ChatEvent>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()
    private var request: Job? = null
    private var requestId = 0L
    private val state get() = mutableState.value

    init {
        viewModelScope.launch {
            val loaded = try { Result.success(chatRepository.load()) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure<ChatHistory>(e) }
            mutableState.value = state.copy(history = loaded.getOrDefault(ChatHistory())
                .normalized(settingsRepository.settings.value), loading = false,
                notice = if (loaded.isFailure) ChatNotice.HistoryLoadFailed else null)
            if (state.server.isNotBlank()) connect(state.server)
        }
    }
    private fun update(next: ChatUiState, persist: Boolean = true) {
        mutableState.value = next
        if (persist && !next.loading) chatRepository.save(next.history)
    }
    private fun replace(chat: ChatSession): ChatUiState = state.copy(history = state.history.replace(chat))

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.CopyText -> if (action.text.isNotBlank())
                viewModelScope.launch { mutableEvents.emit(ChatEvent.CopyToClipboard(action.text)) }
            ChatAction.PersistHistory -> if (!state.loading) chatRepository.save(state.history)
            ChatAction.Stop -> stop()
            is ChatAction.DraftChanged -> state.current?.let {
                if (it.draft != action.text) update(replace(it.copy(draft = action.text)))
            }
            else -> {
                if (state.busy) return
                when (action) {
                    is ChatAction.Connect -> connect(action.server)
                    is ChatAction.SelectModel -> selectModel(action.model)
                    is ChatAction.OpenChat -> update(state.copy(history = manageChats.open(state.history, action.id), notice = null))
                    is ChatAction.RenameChat -> update(state.copy(history = manageChats.rename(state.history, action.id, action.title)))
                    is ChatAction.DeleteChat -> update(state.copy(history = manageChats.delete(state.history, action.id,
                        settingsRepository.settings.value)))
                    ChatAction.NewChat -> newChat()
                    ChatAction.Send -> if (state.canSend) state.current?.let { prepareTurn.send(it)?.let(::generate) }
                    ChatAction.Retry -> if (state.canSend) state.current?.let { prepareTurn.retry(it)?.let(::generate) }
                    else -> Unit
                }
            }
        }
    }
    private fun connect(address: String) {
        val server = try { ServerAddress.normalize(address) } catch (e: IllegalArgumentException) {
            update(state.copy(notice = ChatNotice.Error(e.message.orEmpty())), false); return
        }
        val id = ++requestId
        update(state.copy(connecting = true, notice = null), false)
        request = viewModelScope.launch {
            try {
                val models = ollamaRepository.models(server)
                if (id != requestId) return@launch
                val preferred = state.current?.model.takeIf { it in models }
                    ?: settingsRepository.settings.value.model.takeIf { it in models } ?: models.firstOrNull().orEmpty()
                val settings = ConnectionSettings(server, preferred)
                val chat = state.current
                val history = when {
                    chat == null -> state.history.normalized(settings)
                    chat.messages.isEmpty() -> state.history.replace(chat.copy(server = server, model = preferred))
                    chat.server != server -> manageChats.newChat(state.history, settings)
                    else -> state.history
                }
                updateSettings(server, preferred)
                update(state.copy(server = server, models = models, history = history,
                    connecting = false, connected = true, notice = if (models.isEmpty()) ChatNotice.NoModels else null))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (id == requestId) update(state.copy(connecting = false, connected = false, notice = errorNotice(e)), false)
            }
        }
    }
    private fun selectModel(model: String) {
        if (model !in state.models) return
        val settings = ConnectionSettings(state.server, model)
        updateSettings(settings.server, settings.model)
        update(state.copy(history = manageChats.selectModel(state.history, settings), notice = null))
    }
    private fun newChat() {
        val model = state.current?.model.takeIf { it in state.models }
            ?: state.models.firstOrNull() ?: settingsRepository.settings.value.model
        update(state.copy(history = manageChats.newChat(state.history, ConnectionSettings(state.server, model)), notice = null))
    }
    private fun generate(chat: ChatSession) {
        val id = ++requestId
        val history = Conversation.finish(chat.messages)
        update(replace(chat.copy(messages = history + ChatMessage("assistant", ""))).copy(generating = true, notice = null))
        request = viewModelScope.launch {
            try {
                ollamaRepository.answer(chat.server, chat.model, history).collect { partial ->
                    if (id == requestId) state.current?.let { current ->
                        update(replace(current.copy(messages = history + ChatMessage("assistant", partial))), false)
                    }
                }
                if (id == requestId) update(state.copy(generating = false))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (id == requestId) state.current?.let { current ->
                    update(replace(current.copy(messages = Conversation.finish(current.messages)))
                        .copy(generating = false, notice = errorNotice(e)))
                }
            }
        }
    }
    private fun stop() {
        if (state.loading || !state.busy) return
        ++requestId
        request?.cancel()
        val current = state.current ?: return
        update(replace(current.copy(messages = Conversation.finish(current.messages)))
            .copy(generating = false, connecting = false, notice = ChatNotice.Stopped))
    }
    private fun errorNotice(e: Exception): ChatNotice = when (e) {
        is java.net.ConnectException, is java.net.UnknownHostException -> ChatNotice.ConnectionFailed
        else -> ChatNotice.Error(e.message ?: e.javaClass.simpleName)
    }
}
