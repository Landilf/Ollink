package ru.landilf.ollink.data.network

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import ru.landilf.ollink.domain.model.ChatMessage
import ru.landilf.ollink.domain.model.ServerAddress
import java.net.SocketTimeoutException

/** One cancellation handle per request, including requests not started yet. */
class RequestCancellation {
    @Volatile var cancelled = false
        private set
    private var connection: HttpURLConnection? = null

    @Synchronized fun attach(conn: HttpURLConnection) {
        if (cancelled) {
            conn.disconnect()
            throw java.util.concurrent.CancellationException()
        }
        connection = conn
    }
    @Synchronized fun detach() { connection = null }
    @Synchronized fun cancel() {
        cancelled = true
        connection?.disconnect()
        connection = null
    }
    fun check() { if (cancelled) throw java.util.concurrent.CancellationException() }
}

class OllamaClient {
    fun models(server: String, cancellation: RequestCancellation): List<String> =
        request(server, "/api/tags", cancellation) { conn ->
            val items = JSONObject(conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
                .getJSONArray("models")
            (0 until items.length()).map { items.getJSONObject(it).getString("name") }.distinct().sorted()
        }

    fun chat(server: String, model: String, history: List<ChatMessage>,
             cancellation: RequestCancellation, onContent: (String) -> Unit): String {
        val messages = JSONArray()
        history.forEach { messages.put(JSONObject().put("role", it.role).put("content", it.content)) }
        val body = JSONObject().put("model", model).put("messages", messages).put("stream", true)
        return request(server, "/api/chat", cancellation, body) { conn ->
            val content = StringBuilder()
            var done = false
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    cancellation.check()
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val chunk = JSONObject(line)
                    if (chunk.has("error")) throw IOException(chunk.getString("error"))
                    val token = chunk.optJSONObject("message")?.optString("content").orEmpty()
                    if (token.isNotEmpty()) {
                        content.append(token)
                        onContent(content.toString())
                    }
                    if (chunk.optBoolean("done")) { done = true; break }
                }
            }
            cancellation.check()
            check(done) { "Соединение оборвалось до завершения ответа" }
            check(content.isNotBlank()) { "Модель вернула пустой ответ. Выберите другую модель." }
            content.toString()
        }
    }

    private fun <T> request(server: String, path: String, cancellation: RequestCancellation,
                            body: JSONObject? = null, read: (HttpURLConnection) -> T): T {
        cancellation.check()
        val conn = URI(ServerAddress.normalize(server) + path).toURL().openConnection() as HttpURLConnection
        cancellation.attach(conn)
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = if (body == null) 15_000 else 300_000
            conn.requestMethod = if (body == null) "GET" else "POST"
            conn.setRequestProperty("Accept", if (body == null) "application/json" else "application/x-ndjson")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            cancellation.check()
            if (status !in 200..299) {
                val text = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val detail = runCatching { JSONObject(text).optString("error") }.getOrDefault("")
                throw IOException("Ollama: HTTP $status${if (detail.isBlank()) "" else " — $detail"}")
            }
            return read(conn)
        } catch (e: SocketTimeoutException) {
            throw IOException("Сервер не ответил вовремя. Проверьте сеть; загрузка модели может занять несколько минут.", e)
        } finally {
            conn.disconnect()
            cancellation.detach()
        }
    }
}
