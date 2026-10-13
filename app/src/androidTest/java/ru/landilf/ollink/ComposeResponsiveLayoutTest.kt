package ru.landilf.ollink

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.landilf.ollink.domain.model.*
import ru.landilf.ollink.presentation.*
import ru.landilf.ollink.presentation.theme.OllinkTheme

@OptIn(ExperimentalTestApi::class)
class ComposeResponsiveLayoutTest {
    @get:Rule val compose = createComposeRule()
    private data class Window(val width: Int, val height: Int, val fontScale: Float, val wide: Boolean)

    @Test fun controlsFitSmallLandscapeTabletAndLargeFontWindowsAfterFirstMeasureAndResize() {
        var window by mutableStateOf(Window(320, 640, 1f, false))
        val chat = ChatSession(model = "a-very-long-installed-model-name:latest", server = "http://laptop", draft = "Draft")
        val state = ChatUiState(history = ChatHistory(listOf(chat), chat.id), loading = false,
            connected = true, server = chat.server, models = listOf(chat.model))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(window.width.dp, window.height.dp)) then
                DeviceConfigurationOverride.FontScale(window.fontScale)) {
                OllinkTheme { ChatScreen(state, {}) }
            }
        }
        listOf(Window(320, 640, 1f, false), Window(280, 500, 2f, false),
            Window(411, 280, 2f, false), Window(700, 360, 1f, true),
            Window(1000, 800, 1.5f, true), Window(560, 700, 1f, false)).forEach { size ->
            compose.runOnIdle { window = size }
            compose.onNodeWithTag(if (size.wide) "layout_wide" else "layout_compact").assertExists()
            compose.onNodeWithTag(ChatTags.Send).assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithTag(ChatTags.Draft).assertIsDisplayed().assertTextEquals("Draft")
            val pane = compose.onNodeWithTag(ChatTags.ChatPane).fetchSemanticsNode().boundsInRoot
            val input = compose.onNodeWithTag(ChatTags.Composer).fetchSemanticsNode().boundsInRoot
            assertTrue("Composer fits ${size.width}x${size.height}", input.left >= pane.left - 1 && input.right <= pane.right + 1 &&
                input.bottom <= pane.bottom + 1 && input.top >= pane.top)
            if (size.wide) {
                val sidebar = compose.onNodeWithTag(ChatTags.Sidebar).fetchSemanticsNode().boundsInRoot
                assertTrue("Sidebar must not overlap the chat", sidebar.right <= pane.left + 1)
            }
        }
    }

    @Test fun userBubbleIsBoundedByChatPaneAndLongReplyCanScroll() {
        val chat = ChatSession(model = "test", messages = listOf(
            ChatMessage("user", "A question ".repeat(40)), ChatMessage("assistant", "A long answer\n".repeat(80))))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 480.dp)) then
                DeviceConfigurationOverride.FontScale(1.8f)) {
                OllinkTheme { ChatScreen(ChatUiState(history = ChatHistory(listOf(chat), chat.id), loading = false), {}) }
            }
        }
        compose.onNodeWithTag(ChatTags.Draft).assertIsDisplayed()
        compose.onNodeWithTag(ChatTags.Send).assertIsDisplayed()
        compose.onNodeWithTag("message_1").assertExists()
        compose.onNodeWithTag(ChatTags.ChatPane).assertIsDisplayed()
    }
    @Test fun darkThemeUsesLightContentAndNarrowPhoneKeepsTwoByTwoSuggestions() {
        var contentColor = androidx.compose.ui.graphics.Color.Unspecified
        val chat = ChatSession(server = "http://laptop", model = "test")
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 800.dp))) {
                OllinkTheme(darkTheme = true) {
                    val inherited = androidx.compose.material3.LocalContentColor.current
                    SideEffect { contentColor = inherited }
                    ChatScreen(ChatUiState(history = ChatHistory(listOf(chat), chat.id), loading = false,
                        connected = true, server = chat.server, models = listOf(chat.model)), {})
                }
            }
        }
        compose.runOnIdle { assertEquals(androidx.compose.ui.graphics.Color(0xFFECECEC), contentColor) }
        compose.onNodeWithTag("prompt_row_0").assertIsDisplayed()
        compose.onNodeWithTag("prompt_row_1").assertIsDisplayed()
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val explain = compose.onNodeWithText(context.getString(R.string.suggest_explain)).fetchSemanticsNode().boundsInRoot
        val code = compose.onNodeWithText(context.getString(R.string.suggest_code)).fetchSemanticsNode().boundsInRoot
        val idea = compose.onNodeWithText(context.getString(R.string.suggest_idea)).fetchSemanticsNode().boundsInRoot
        val plan = compose.onNodeWithText(context.getString(R.string.suggest_plan)).fetchSemanticsNode().boundsInRoot
        assertTrue(explain.right <= code.left)
        assertTrue(idea.right <= plan.left)
        assertEquals(explain.top, code.top, 1f)
        assertEquals(idea.top, plan.top, 1f)
        assertTrue(idea.top > explain.bottom)
    }

}
