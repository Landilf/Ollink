package ru.landilf.ollink.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import ru.landilf.ollink.R
import ru.landilf.ollink.presentation.ChatAction
import ru.landilf.ollink.presentation.ChatUiState

@Composable
fun ChatMessages(state: ChatUiState, onAction: (ChatAction) -> Unit, modifier: Modifier = Modifier) {
    val chat = state.current ?: return
    val listState = rememberSaveable(chat.id, saver = LazyListState.Saver) { LazyListState() }
    var follow by remember(chat.id) { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.canScrollForward }.collectLatest { (scrolling, more) ->
            if (scrolling) follow = !more
        }
    }
    LaunchedEffect(chat.id, chat.messages.size, chat.messages.lastOrNull()?.content) {
        if (follow && chat.messages.isNotEmpty()) listState.scrollToItem(chat.messages.lastIndex)
    }
    LazyColumn(modifier, state = listState, contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        itemsIndexed(chat.messages, key = { index, _ -> "${chat.id}_$index" }) { index, message ->
            val user = message.role == "user"
            BoxWithConstraints(Modifier.fillMaxWidth().testTag("message_$index")) {
                val bubbleWidth = maxWidth * 0.85f
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
                    SelectionContainer {
                        if (user) Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.widthIn(max = bubbleWidth)) {
                            Text(message.content, Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyLarge)
                        } else if (message.content.isBlank()) Text(stringResource(R.string.thinking),
                            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else MarkdownText(message.content, Modifier.fillMaxWidth())
                    }
                    if (!user && message.content.isNotBlank() && !(state.generating && index == chat.messages.lastIndex)) {
                        Row {
                            ChatIconButton(R.drawable.ic_copy, R.string.copy) { onAction(ChatAction.CopyText(message.content)) }
                            if (index == chat.messages.lastIndex && !state.busy && state.canSend)
                                ChatIconButton(R.drawable.ic_retry, R.string.retry) { onAction(ChatAction.Retry) }
                        }
                    }
                }
            }
        }
    }
}
