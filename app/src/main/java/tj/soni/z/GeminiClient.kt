package tj.soni.z

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiClient(private val apiKey: String) {
    suspend fun ask(prompt: String, history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Аввал дар Танзимот Gemini API Key-ро ворид кунед")
        }

        val contents = JSONArray()
        history.takeLast(20).forEach { message ->
            if (message.role == "user" || message.role == "model") {
                contents.put(
                    JSONObject()
                        .put("role", message.role)
                        .put("parts", JSONArray().put(JSONObject().put("text", message.text)))
                )
            }
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
        )

        val body = JSONObject()
            .put(
                "system_instruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put(
                            "text",
                            "Ту ёвари зеҳнии Z ҳастӣ. Ба корбар асосан бо забони тоҷикӣ, равшан, дӯстона ва дуруст ҷавоб деҳ. Агар корбар забони дигарро истифода барад, ба ҳамон забон ҷавоб деҳ. Фармонҳои Android-ро худсарона иҷро накун."
                        )
                    )
                )
            )
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 2048))

        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }

        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val response = JSONObject(responseText)

            if (code !in 200..299) {
                val detail = response.optJSONObject("error")?.optString("message")
                    ?: "HTTP $code"
                throw IllegalStateException("Gemini API: $detail")
            }

            val candidates = response.optJSONArray("candidates")
                ?: throw IllegalStateException("Gemini ҷавоб надод")
            val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                ?: throw IllegalStateException("Матни ҷавоб ёфт нашуд")
            val answer = buildString {
                for (i in 0 until parts.length()) {
                    val text = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (text.isNotBlank()) {
                        if (isNotEmpty()) append("\n")
                        append(text)
                    }
                }
            }.trim()
            answer.ifBlank { "Ҷавоби матнӣ дастрас нест. Саволро дигар хел нависед." }
        } finally {
            connection.disconnect()
        }
    }
}
