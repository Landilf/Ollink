package ru.landilf.ollink

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import ru.landilf.ollink.presentation.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
class ChatFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private class FakeOllama : AutoCloseable {
        val socket = ServerSocket(0)
        val executor = Executors.newCachedThreadPool()
        @Volatile var slow = false
        init {
            executor.execute {
                while (!socket.isClosed) {
                    val client = try { socket.accept() } catch (_: Exception) { break }
                    executor.execute { runCatching { handle(client) } }
                }
            }
        }
        private fun handle(client: Socket) = client.use {
            it.soTimeout = 5000
            val input = it.getInputStream().bufferedReader()
            val request = input.readLine()
            var length = 0
            while (true) {
                val header = input.readLine() ?: break
                if (header.isEmpty()) break
                if (header.startsWith("Content-Length:", true)) length = header.substringAfter(':').trim().toInt()
            }
            // Test prompts use ASCII, so byte and character lengths match.
            repeat(length) { input.read() }
            val output = it.getOutputStream()
            output.write("HTTP/1.1 200 OK\r\nConnection: close\r\nContent-Type: application/json\r\n\r\n".toByteArray())
            if (request.contains("/api/tags")) {
                output.write("{\"models\":[{\"name\":\"test-model\"}]}".toByteArray()); output.flush()
            } else {
                output.write("{\"message\":{\"content\":\"First\"},\"done\":false}\n".toByteArray()); output.flush()
                if (slow) Thread.sleep(2500)
                output.write("{\"message\":{\"content\":\" answer\"},\"done\":true}\n".toByteArray()); output.flush()
            }
        }
        override fun close() { socket.close(); executor.shutdownNow() }
    }

    private fun prepare(server: FakeOllama) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("history", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("connection", Context.MODE_PRIVATE).edit().clear()
            .putString("server", "http://127.0.0.1:${server.socket.localPort}").commit()
    }
    private fun vm(activity: MainActivity) = ViewModelProvider(activity)[ChatViewModel::class.java]
    private fun await(scenario: ActivityScenario<MainActivity>, condition: (ChatUiState) -> Boolean) {
        val deadline = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < deadline) {
            var success = false
            scenario.onActivity { success = condition(vm(it).uiState.value) }
            if (success) return
            Thread.sleep(100)
        }
        throw AssertionError("Timed out waiting for chat state")
    }

    @Test fun chatsPersistAndDraftsFollowSelectedConversation() {
        FakeOllama().use { server ->
            prepare(server)
            var id = ""
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await(scenario) { !it.busy }
                scenario.onActivity { vm(it).onAction(ChatAction.Connect("http://127.0.0.1:${server.socket.localPort}")) }
                await(scenario) { it.canSend && !it.busy }
                compose.onNodeWithTag(ChatTags.Draft).performTextReplacement("Hello")
                compose.onNodeWithTag(ChatTags.Send).performClick()
                await(scenario) { !it.generating && it.current!!.messages.size == 2 }
                scenario.onActivity {
                    val model = vm(it)
                    assertEquals("First answer", model.uiState.value.current!!.messages.last().content)
                    id = model.uiState.value.history.currentId
                    model.onAction(ChatAction.DraftChanged("Unsent draft"))
                }
                scenario.recreate()
                compose.onNodeWithTag(ChatTags.Draft).assertTextEquals("Unsent draft")
                compose.onNodeWithTag(ChatTags.NewChat).performClick()
                compose.onNodeWithTag(ChatTags.Draft).assert(SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.EditableText, androidx.compose.ui.text.AnnotatedString("")))
                scenario.onActivity { vm(it).onAction(ChatAction.OpenChat(id)) }
                compose.onNodeWithTag(ChatTags.Draft).assertTextEquals("Unsent draft")
                compose.onNodeWithTag(ChatTags.Menu).performClick()
                compose.onNodeWithTag(ChatTags.Search).performTextReplacement("Hello")
                compose.onNodeWithTag("chat_$id").assertIsDisplayed()
            }
            compose.waitUntil(5000) {
                ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("history", Context.MODE_PRIVATE)
                    .getString("chats", "").orEmpty().contains("Unsent draft")
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await(scenario) { !it.busy }
                scenario.onActivity { vm(it).onAction(ChatAction.Connect("http://127.0.0.1:${server.socket.localPort}")) }
                await(scenario) { it.canSend && !it.busy }
                scenario.onActivity {
                    val chat = vm(it).uiState.value.current!!
                    assertEquals(id, chat.id)
                    assertEquals("Unsent draft", chat.draft)
                    assertEquals(2, chat.messages.size)
                    vm(it).onAction(ChatAction.RenameChat(id, "Renamed"))
                    assertEquals("Renamed", vm(it).uiState.value.current!!.title)
                    vm(it).onAction(ChatAction.DeleteChat(id))
                    assertTrue(vm(it).uiState.value.history.chats.none { saved -> saved.id == id })
                }
            }
        }
    }
    @Test fun stopKeepsPartialAnswerAndIgnoresLateCompletion() {
        FakeOllama().use { server ->
            server.slow = true
            prepare(server)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await(scenario) { !it.busy }
                scenario.onActivity { vm(it).onAction(ChatAction.Connect("http://127.0.0.1:${server.socket.localPort}")) }
                await(scenario) { it.canSend && !it.busy }
                compose.onNodeWithTag(ChatTags.Draft).performClick().performTextReplacement("Hello")
                compose.onNodeWithTag(ChatTags.Composer).assertIsDisplayed()
                compose.onNodeWithTag(ChatTags.Send).assertIsDisplayed().performClick()
                await(scenario) { it.generating && it.current!!.messages.last().content == "First" }
                compose.onNodeWithTag(ChatTags.Send).performClick()
                await(scenario) { !it.generating }
                Thread.sleep(2700)
                scenario.onActivity {
                    assertEquals("First", vm(it).uiState.value.current!!.messages.last().content)
                    server.slow = false
                    vm(it).onAction(ChatAction.Retry)
                }
                await(scenario) { !it.generating && it.current!!.messages.last().content == "First answer" }
                scenario.onActivity { assertEquals(2, vm(it).uiState.value.current!!.messages.size) }
            }
        }
    }
}
