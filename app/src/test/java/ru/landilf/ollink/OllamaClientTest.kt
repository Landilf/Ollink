package ru.landilf.ollink

import ru.landilf.ollink.domain.model.*
import ru.landilf.ollink.data.network.*

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedInputStream
import java.io.IOException
import java.net.ServerSocket
import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.CancellationException

class OllamaClientTest {
    private fun withServer(response: String, status: Int = 200, block: (String) -> Unit): String {
        ServerSocket(0).use { server ->
            server.soTimeout = 5000
            val executor = Executors.newSingleThreadExecutor()
            val request = executor.submit<String> {
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val input = BufferedInputStream(socket.getInputStream())
                    fun line(): String {
                        val bytes = ArrayList<Byte>()
                        while (true) {
                            val next = input.read()
                            if (next == -1 || next == 10) break
                            if (next != 13) bytes.add(next.toByte())
                        }
                        return bytes.toByteArray().toString(UTF_8)
                    }
                    line()
                    var length = 0
                    while (true) {
                        val header = line()
                        if (header.isEmpty()) break
                        if (header.startsWith("Content-Length:", true)) length = header.substringAfter(':').trim().toInt()
                    }
                    val body = ByteArray(length)
                    var offset = 0
                    while (offset < length) {
                        val count = input.read(body, offset, length - offset)
                        check(count > 0)
                        offset += count
                    }
                    val payload = response.toByteArray(UTF_8)
                    socket.getOutputStream().write(("HTTP/1.1 $status Test\r\nContent-Type: application/x-ndjson\r\n" +
                        "Content-Length: ${payload.size}\r\nConnection: close\r\n\r\n").toByteArray(UTF_8) + payload)
                    body.toString(UTF_8)
                }
            }
            try {
                block("http://127.0.0.1:${server.localPort}")
                return request.get(5, TimeUnit.SECONDS)
            } finally { executor.shutdownNow() }
        }
    }

    @Test fun streamedChatAccumulatesUnicodeAndFinalTokenWithoutThinking() {
        val updates = mutableListOf<String>()
        val body = withServer("""
            {"message":{"thinking":"private reasoning","content":""},"done":false}
            {"message":{"content":"Привет"},"done":false}
            {"message":{"content":"!"},"done":true}
        """.trimIndent()) { server ->
            val reply = OllamaClient().chat(server, "test", listOf(ChatMessage("user", "Вопрос")),
                RequestCancellation()) { updates.add(it) }
            assertEquals("Привет!", reply)
        }
        assertEquals(listOf("Привет", "Привет!"), updates)
        val json = JSONObject(body)
        assertTrue(json.getBoolean("stream"))
        assertEquals("test", json.getString("model"))
        assertEquals("Вопрос", json.getJSONArray("messages").getJSONObject(0).getString("content"))
    }

    @Test fun truncatedStreamDoesNotCountAsSuccessfulAnswer() {
        withServer("{\"message\":{\"content\":\"partial\"},\"done\":false}\n") { server ->
            assertThrows(IllegalStateException::class.java) {
                OllamaClient().chat(server, "test", emptyList(), RequestCancellation()) {}
            }
        }
    }
    @Test fun httpAndStreamErrorsAreReported() {
        withServer("{\"error\":\"model missing\"}", 404) { server ->
            val error = assertThrows(IOException::class.java) {
                OllamaClient().chat(server, "test", emptyList(), RequestCancellation()) {}
            }
            assertTrue(error.message!!.contains("model missing"))
        }
        withServer("{\"error\":\"out of memory\"}\n") { server ->
            assertThrows(IOException::class.java) {
                OllamaClient().chat(server, "test", emptyList(), RequestCancellation()) {}
            }
        }
    }
    @Test fun cancelledQueuedRequestNeverConnects() {
        val cancellation = RequestCancellation().apply { cancel() }
        assertThrows(CancellationException::class.java) {
            OllamaClient().chat("http://127.0.0.1:1", "test", emptyList(), cancellation) {}
        }
    }
}
