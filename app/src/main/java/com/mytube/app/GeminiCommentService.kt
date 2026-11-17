package com.mytube.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiCommentService {
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions"

    fun generate(apiKey: String, reaction: String, videoContext: String, language: String, firm: Boolean): String {
        require(apiKey.isNotBlank()) { "Gemini API key is required." }
        val tone = if (firm) "firm but respectful" else "polite, constructive and friendly"
        val safeReaction = reaction.trim()
        val safeContext = videoContext.trim().ifBlank { "Not provided" }
        val prompt = """
            Write one natural YouTube comment in $language.
            Tone: $tone.
            User's reaction: $safeReaction
            Video title or context: $safeContext
            Requirements:
            - Respond to the user's reaction; do not invent facts or claim to have watched the video.
            - Do not claim anything is false unless the user supplied evidence; phrase uncertainty carefully.
            - Keep it concise (2 to 4 sentences), specific, respectful and ready for the user to edit.
            - Output only the comment text, with no heading, quotes, analysis or explanation.
        """.trimIndent()

        val requestBody = JSONObject()
            .put("model", "gemini-3.8-flash")
            .put("input", prompt)
            .put("store", false)

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 45000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey.trim())
        }
        try {
            connection.outputStream.use { it.write(requestBody.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (status !in 200..299) {
                val message = json.optJSONObject("error")?.optString("message")
                    ?: "Gemini API request failed ($status)"
                throw IllegalStateException(message)
            }
            val steps = json.optJSONArray("steps") ?: JSONArray()
            val generated = buildString {
                for (i in 0 until steps.length()) {
                    val step = steps.optJSONObject(i) ?: continue
                    if (step.optString("type") != "model_output") continue
                    val parts = step.optJSONArray("content") ?: continue
                    for (j in 0 until parts.length()) {
                        val part = parts.optJSONObject(j) ?: continue
                        if (part.optString("type") == "text") append(part.optString("text"))
                    }
                }
            }.trim()
            if (generated.isBlank()) throw IllegalStateException("Gemini returned an empty comment. Try again.")
            return generated
        } finally {
            connection.disconnect()
        }
    }
}
