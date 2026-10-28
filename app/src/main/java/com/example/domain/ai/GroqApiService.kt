package com.example.domain.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GroqResponse(
    val success: Boolean,
    val content: String? = null,
    val errorMessage: String? = null
)

class GroqApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateCompletion(
        apiKey: String,
        model: String = "llama-3.3-70b-versatile",
        systemPrompt: String,
        userPrompt: String
    ): GroqResponse = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty() || trimmedKey == "UNCONFIGURED") {
            return@withContext GroqResponse(
                success = false,
                errorMessage = "GROQ_API_KEY haijawekwa (Unconfigured API key). Tafadhali weka funguo kwenye Settings."
            )
        }

        val url = "https://api.groq.com/openai/v1/chat/completions"

        val messagesArray = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val payload = JSONObject().apply {
            put("model", model.ifBlank { "llama-3.3-70b-versatile" })
            put("messages", messagesArray)
            put("temperature", 0.6)
            put("max_tokens", 400)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $trimmedKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val messageObj = firstChoice.optJSONObject("message")
                        val text = messageObj?.optString("content").orEmpty().trim()
                        GroqResponse(success = true, content = text)
                    } else {
                        GroqResponse(success = false, errorMessage = "Jibu tupu kutoka kwa mfumo wa Groq AI.")
                    }
                } else {
                    val safeError = try {
                        val errObj = JSONObject(responseBody).optJSONObject("error")
                        errObj?.optString("message") ?: "Groq API HTTP ${response.code}"
                    } catch (e: Exception) {
                        "Groq API HTTP ${response.code}"
                    }
                    GroqResponse(success = false, errorMessage = safeError)
                }
            }
        } catch (e: IOException) {
            GroqResponse(success = false, errorMessage = "Mtandao wa Groq haupatikani: ${e.message}")
        } catch (e: Exception) {
            GroqResponse(success = false, errorMessage = "Hitilafu: ${e.localizedMessage}")
        }
    }
}
