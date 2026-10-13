package ru.landilf.ollink.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.landilf.ollink.R
import ru.landilf.ollink.presentation.*

@Composable
fun ChatSidebar(state: ChatUiState, search: String, onSearch: (String) -> Unit,
                onAction: (ChatAction) -> Unit, chatMenu: (String, String) -> Unit,
                openSettings: () -> Unit, close: (() -> Unit)?, scrollEverything: Boolean, modifier: Modifier = Modifier) {
    val columnModifier = if (scrollEverything) modifier.fillMaxSize().verticalScroll(rememberScrollState())
        else modifier.fillMaxSize()
    Column(columnModifier.testTag(ChatTags.Sidebar).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (close != null) ChatIconButton(R.drawable.ic_menu, R.string.close_menu, onClick = close)
        }
        Button(onClick = { onAction(ChatAction.NewChat); close?.invoke() }, Modifier.fillMaxWidth(), enabled = !state.busy) {
            Text(stringResource(R.string.new_chat))
        }
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth().testTag(ChatTags.Search),
            singleLine = true, placeholder = { Text(stringResource(R.string.search_chats)) }, shape = RoundedCornerShape(16.dp))
        Text(stringResource(R.string.history), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        val chats = remember(state.history.chats, search) {
            val query = search.trim()
            state.history.chats.filter { it.title.contains(query, true) || it.messages.any { message -> message.content.contains(query, true) } }
                .sortedByDescending { it.updatedAt }
        }
        if (scrollEverything) {
            if (chats.isEmpty()) Text(stringResource(R.string.no_chats), Modifier.padding(12.dp))
            chats.forEach { chat ->
                key(chat.id) { HistoryRow(chat, state, onAction, chatMenu, close) }
            }
        } else LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            if (chats.isEmpty()) item { Text(stringResource(R.string.no_chats), Modifier.padding(12.dp)) }
            items(chats, key = { it.id }) { chat -> HistoryRow(chat, state, onAction, chatMenu, close) }
        }
        TextButton(openSettings, Modifier.fillMaxWidth(), enabled = !state.busy) { Text(stringResource(R.string.settings)) }
        if (state.server.isNotBlank()) Text(state.server, Modifier.fillMaxWidth(), maxLines = 1,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HistoryRow(chat: ru.landilf.ollink.domain.model.ChatSession, state: ChatUiState,
                       onAction: (ChatAction) -> Unit, chatMenu: (String, String) -> Unit,
                       close: (() -> Unit)?) {
    val keyboard = LocalSoftwareKeyboardController.current

    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (chat.id == state.history.currentId) MaterialTheme.colorScheme.surfaceContainer
            else MaterialTheme.colorScheme.surface)
        .combinedClickable(enabled = !state.busy, onClick = {
            keyboard?.hide(); onAction(ChatAction.OpenChat(chat.id)); close?.invoke()
        }, onLongClick = { menuOpen = true })
        .testTag("chat_${chat.id}"), verticalAlignment = Alignment.CenterVertically) {
        Text(chat.title.ifBlank { stringResource(R.string.new_chat) }, Modifier.weight(1f).padding(12.dp),
            maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        Box {
            ChatIconButton(R.drawable.ic_more, R.string.more, enabled = !state.busy) { menuOpen = true }
            DropdownMenu(menuOpen, { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = {
                    menuOpen = false; chatMenu(chat.id, "rename")
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = {
                    menuOpen = false; chatMenu(chat.id, "delete")
                })
            }
        }
    }

}
