package tj.soni.z

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ZStore(context: Context) {
    private val prefs = context.getSharedPreferences("z_v5_settings", Context.MODE_PRIVATE)

    fun apiKey(): String = prefs.getString("gemini_api_key", "") ?: ""

    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun loadMessages(): List<ChatMessage> {
        return try {
            val array = JSONArray(prefs.getString("chat_history", "[]"))
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(ChatMessage(item.optString("role"), item.optString("text")))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveMessages(messages: List<ChatMessage>) {
        val array = JSONArray()
        messages.takeLast(80).forEach { message ->
            array.put(JSONObject().put("role", message.role).put("text", message.text))
        }
        prefs.edit().putString("chat_history", array.toString()).apply()
    }
}
