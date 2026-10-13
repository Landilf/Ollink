package ru.landilf.ollink.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.landilf.ollink.R
import ru.landilf.ollink.presentation.components.*

object ChatTags {
    const val Draft = "draft"
    const val Send = "send"
    const val Menu = "menu"
    const val NewChat = "new_chat"
    const val Sidebar = "sidebar"
    const val Search = "search"
    const val Composer = "composer"
    const val ChatPane = "chat_pane"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(state: ChatUiState, onAction: (ChatAction) -> Unit, modifier: Modifier = Modifier) {
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var modelsOpen by rememberSaveable { mutableStateOf(false) }
    var dialogChatId by rememberSaveable { mutableStateOf<String?>(null) }
    var dialogMode by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf("") }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val systemInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).union(WindowInsets.waterfall)
    // Keep animated IME insets outside the constraints used to compose this screen.
    BoxWithConstraints(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        .windowInsetsPadding(systemInsets)) {
        val windowLayout = remember(maxWidth, maxHeight, density.fontScale) {
            ResponsiveLayout.calculate(maxWidth.value, maxHeight.value, density.fontScale)
        }
        // The target changes once per opening/closing, rather than once per animation frame.
        val keyboardHeight = with(density) {
            WindowInsets.imeAnimationTarget.exclude(systemInsets).getBottom(this).toDp()
        }
        val layout = remember(windowLayout, maxWidth, maxHeight, keyboardHeight, density.fontScale) {
            ResponsiveLayout.calculate(maxWidth.value, (maxHeight - keyboardHeight).value.coerceAtLeast(0f), density.fontScale)
                .copy(persistentSidebar = windowLayout.persistentSidebar, sidebarWidth = windowLayout.sidebarWidth)
        }
        val openSettings = { settingsOpen = true }
        val onChatMenu: (String, String) -> Unit = { id, mode -> dialogChatId = id; dialogMode = mode }
        val closeDrawer = { scope.launch { drawer.close() }; Unit }
        val sidebar: @Composable (Modifier, Boolean) -> Unit = { sidebarModifier, modal ->
            ChatSidebar(state, search, { search = it }, onAction, onChatMenu,
                { closeDrawer(); openSettings() }, if (modal) closeDrawer else null,
                layout.compactHeight || density.fontScale > 1.5f, sidebarModifier.imePadding())
        }
        val chat: @Composable (Modifier) -> Unit = { chatModifier ->
            ChatPane(state, layout, !layout.persistentSidebar, onAction,
                { scope.launch { drawer.open() } }, openSettings,
                { if (state.connected && state.models.isNotEmpty()) modelsOpen = true else settingsOpen = true },
                onChatMenu, chatModifier)
        }
        if (layout.persistentSidebar) {
            Row(Modifier.fillMaxSize().testTag("layout_wide")) {
                sidebar(Modifier.width(layout.sidebarWidth.dp).fillMaxHeight(), false)
                VerticalDivider()
                chat(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            BackHandler(drawer.isOpen) { closeDrawer() }
            ModalNavigationDrawer(drawerState = drawer, drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width((maxWidth.value - 48f).coerceIn(200f, 320f).dp),
                    windowInsets = WindowInsets(0, 0, 0, 0)) {
                    sidebar(Modifier.fillMaxSize(), true)
                }
            }, modifier = Modifier.fillMaxSize().testTag("layout_compact")) {
                chat(Modifier.fillMaxSize())
            }
        }
    }
    if (settingsOpen) ConnectionDialog(state.server, { settingsOpen = false }) { server ->
        onAction(ChatAction.Connect(server)); settingsOpen = false
    }
    if (modelsOpen) ModelDialog(state.models, state.current?.model.orEmpty(), { modelsOpen = false }) {
        onAction(ChatAction.SelectModel(it)); modelsOpen = false
    }
    val dialogChat = state.history.chats.firstOrNull { it.id == dialogChatId }
    if (dialogChat != null && dialogMode == "rename") RenameChatDialog(dialogChat.title, { dialogChatId = null }) {
        onAction(ChatAction.RenameChat(dialogChat.id, it)); dialogChatId = null
    }
    if (dialogChat != null && dialogMode == "delete") AlertDialog(
        onDismissRequest = { dialogChatId = null }, title = { Text(stringResource(R.string.delete)) },
        text = { Text(stringResource(R.string.delete_confirmation)) },
        confirmButton = { TextButton(onClick = { onAction(ChatAction.DeleteChat(dialogChat.id)); dialogChatId = null }) {
            Text(stringResource(R.string.delete))
        } }, dismissButton = { TextButton(onClick = { dialogChatId = null }) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun ChatPane(state: ChatUiState, layout: ResponsiveLayout, showMenu: Boolean,
                     onAction: (ChatAction) -> Unit, openMenu: () -> Unit,
                     openSettings: () -> Unit, chooseModel: () -> Unit,
                     chatMenu: (String, String) -> Unit, modifier: Modifier) {
    Column(modifier.imePadding().testTag(ChatTags.ChatPane), horizontalAlignment = Alignment.CenterHorizontally) {
        ChatHeader(state, showMenu, onAction, openMenu, chooseModel, openSettings, chatMenu,
            Modifier.fillMaxWidth().heightIn(min = if (layout.compactHeight) 48.dp else 60.dp))
        if (state.current?.messages.isNullOrEmpty()) {
            ChatWelcome(state.connected, layout, openSettings, { onAction(ChatAction.DraftChanged(it)) },
                Modifier.weight(1f).widthIn(max = layout.contentMaxWidth.dp).fillMaxWidth())
        } else {
            ChatMessages(state, onAction, Modifier.weight(1f).widthIn(max = layout.contentMaxWidth.dp)
                .fillMaxWidth().padding(horizontal = layout.horizontalPadding.dp))
        }
        ChatStatus(state, openSettings, Modifier.widthIn(max = layout.contentMaxWidth.dp).fillMaxWidth()
            .padding(horizontal = layout.horizontalPadding.dp))
        ChatComposer(state, layout, onAction,
            Modifier.widthIn(max = layout.contentMaxWidth.dp).fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@Composable
private fun ChatHeader(state: ChatUiState, showMenu: Boolean, onAction: (ChatAction) -> Unit,
                       openMenu: () -> Unit, chooseModel: () -> Unit, openSettings: () -> Unit,
                       chatMenu: (String, String) -> Unit, modifier: Modifier) {
    var actionsOpen by remember { mutableStateOf(false) }
    Row(modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showMenu) ChatIconButton(R.drawable.ic_menu, R.string.menu, Modifier.testTag(ChatTags.Menu), onClick = openMenu)
        TextButton(chooseModel, Modifier.weight(1f), enabled = !state.busy) {
            Text(state.current?.model?.ifBlank { stringResource(R.string.app_name) } ?: stringResource(R.string.app_name),
                Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Icon(painterResource(R.drawable.ic_chevron), null, Modifier.size(18.dp))
        }
        ChatIconButton(R.drawable.ic_new_chat, R.string.new_chat, Modifier.testTag(ChatTags.NewChat), !state.busy) {
            onAction(ChatAction.NewChat)
        }
        Box {
            ChatIconButton(R.drawable.ic_more, R.string.more, enabled = !state.busy) { actionsOpen = true }
            DropdownMenu(actionsOpen, { actionsOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = {
                    state.current?.let { chatMenu(it.id, "rename") }; actionsOpen = false
                })
                DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = {
                    state.current?.let { chatMenu(it.id, "delete") }; actionsOpen = false
                })
                if (state.canSend && state.current?.messages?.any { it.role == "user" } == true)
                    DropdownMenuItem(text = { Text(stringResource(R.string.retry)) }, onClick = {
                        onAction(ChatAction.Retry); actionsOpen = false
                    })
                DropdownMenuItem(text = { Text(stringResource(R.string.settings)) }, onClick = {
                    openSettings(); actionsOpen = false
                })
            }
        }
    }
}

@Composable
private fun ChatComposer(state: ChatUiState, layout: ResponsiveLayout, onAction: (ChatAction) -> Unit,
                         modifier: Modifier) {
    Column(modifier.testTag(ChatTags.Composer)) {
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.Bottom) {
                Box(Modifier.weight(1f).heightIn(min = 48.dp, max = layout.composerMaxHeight.dp)
                    .padding(horizontal = 12.dp, vertical = 12.dp)) {
                    BasicTextField(value = state.current?.draft.orEmpty(), onValueChange = { onAction(ChatAction.DraftChanged(it)) },
                        modifier = Modifier.fillMaxWidth().testTag(ChatTags.Draft),
                        textStyle = MaterialTheme.typography.bodyLarge.merge(TextStyle(color = MaterialTheme.colorScheme.onSurface)),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        enabled = !state.loading, maxLines = layout.composerMaxLines,
                        decorationBox = { inner ->
                            if (state.current?.draft.isNullOrEmpty()) Text(stringResource(R.string.message_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            inner()
                        })
                }
                FilledIconButton(onClick = { onAction(if (state.busy) ChatAction.Stop else ChatAction.Send) },
                    modifier = Modifier.size(48.dp).testTag(ChatTags.Send), shape = CircleShape,
                    enabled = !state.loading && (state.busy || (state.canSend && !state.current?.draft.isNullOrBlank()))) {
                    Icon(painterResource(if (state.busy) R.drawable.ic_stop else R.drawable.ic_arrow_up),
                        stringResource(if (state.busy) R.string.stop else R.string.send))
                }
            }
        }
        if (!layout.compactHeight) Text(stringResource(R.string.composer_note), Modifier.fillMaxWidth().padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun ChatStatus(state: ChatUiState, openSettings: () -> Unit, modifier: Modifier) {
    val message = when {
        state.loading -> stringResource(R.string.loading_history)
        state.connecting -> stringResource(R.string.connecting)
        state.generating -> stringResource(R.string.thinking)
        state.notice != null -> when (val notice = state.notice!!) {
            ChatNotice.Stopped -> stringResource(R.string.stopped)
            ChatNotice.NoModels -> stringResource(R.string.no_models)
            ChatNotice.HistoryLoadFailed -> stringResource(R.string.history_load_failed)
            ChatNotice.ConnectionFailed -> stringResource(R.string.connection_failed)
            is ChatNotice.Error -> notice.message
        }
        !state.connected -> stringResource(R.string.offline)
        state.current?.server != state.server -> stringResource(R.string.different_server)
        state.current?.model !in state.models -> stringResource(R.string.model_unavailable)
        else -> null
    }
    if (message != null) Text(message, modifier.padding(vertical = 4.dp)
        .clickable(enabled = !state.busy && !state.connected, onClick = openSettings),
        color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall,
        maxLines = 2, overflow = TextOverflow.Ellipsis)
}
