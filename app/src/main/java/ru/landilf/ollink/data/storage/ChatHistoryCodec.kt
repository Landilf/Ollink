package ru.landilf.ollink.data.storage

import org.json.JSONArray
import org.json.JSONObject
import ru.landilf.ollink.domain.model.*

/** Keeps the previous JSON schema so existing chats and drafts remain readable. */
object ChatHistoryCodec {
    fun decode(json: String, currentId: String): ChatHistory {
        val array = JSONArray(json)
        val chats = (0 until array.length()).map { i ->
            val chat = array.getJSONObject(i)
            val messages = chat.getJSONArray("messages")
            ChatSession(id = chat.getString("id"), title = chat.optString("title"),
                server = chat.optString("server"), model = chat.optString("model"),
                draft = chat.optString("draft"), updatedAt = chat.optLong("updatedAt"),
                messages = (0 until messages.length()).map { j ->
                    val item = messages.getJSONObject(j)
                    ChatMessage(item.getString("role"), item.getString("content"))
                })
        }
        return ChatHistory(chats, currentId)
    }
    fun encode(history: ChatHistory): String {
        val array = JSONArray()
        history.chats.forEach { chat ->
            val messages = JSONArray()
            Conversation.finish(chat.messages).forEach {
                messages.put(JSONObject().put("role", it.role).put("content", it.content))
            }
            array.put(JSONObject().put("id", chat.id).put("title", chat.title)
                .put("server", chat.server).put("model", chat.model).put("draft", chat.draft)
                .put("updatedAt", chat.updatedAt).put("messages", messages))
        }
        return array.toString()
    }
}
