package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiResponse {
    data class TextResponse(val text: String) : GeminiResponse()
    data class FunctionCallResponse(
        val functionName: String,
        val arguments: Map<String, Any?>,
        val preSpokenText: String? = null
    ) : GeminiResponse()
    data class Error(val errorMessage: String, val isNetworkOrKeyIssue: Boolean) : GeminiResponse()
}

class GeminiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateMahiResponse(
        userMessage: String,
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>, // sender to text
        model: String = "gemini-3.5-flash"
    ): GeminiResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiClient", "Gemini API key is not configured or placeholder.")
            return@withContext GeminiResponse.Error("API_KEY_MISSING", true)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val rootJson = JSONObject()

            // System instruction
            val sysInstructionObj = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            }
            rootJson.put("systemInstruction", sysInstructionObj)

            // Contents array
            val contentsArray = JSONArray()

            // Add recent history turns
            conversationHistory.takeLast(10).forEach { (sender, text) ->
                val role = if (sender == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                })
            }

            // Current user turn
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userMessage) })
                })
            })
            rootJson.put("contents", contentsArray)

            // Tools definitions
            val functionDeclarations = JSONArray().apply {
                put(createFunctionDeclaration("open_app", "Launch an installed Android app by name", listOf("app_name")))
                put(createFunctionDeclaration("open_website", "Open a web URL in browser", listOf("url")))
                put(createFunctionDeclaration("search_web", "Search the web or Google for a query", listOf("query")))
                put(createFunctionDeclaration("open_settings", "Open Android device settings (bluetooth, wifi, location, notifications, sound, display, general)", listOf("setting_type")))
                put(createFunctionDeclaration("make_phone_call", "Call a contact by name or phone number", listOf("contact_or_number")))
                put(createFunctionDeclaration("send_sms", "Prepare an SMS message to a contact or number", listOf("recipient", "message")))
                put(createFunctionDeclaration("set_alarm", "Set an alarm on phone with hour and minute", listOf("hour", "minute")))
                put(createFunctionDeclaration("set_timer", "Set a countdown timer on phone with duration in seconds", listOf("duration_seconds")))
                put(createFunctionDeclaration("control_media", "Control audio playback: play, pause, next, previous", listOf("action")))
                put(createFunctionDeclaration("save_memory", "Save a user personal detail or preference to persistent memory", listOf("category", "key", "value")))
            }

            rootJson.put("tools", JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", functionDeclarations)
                })
            })

            // Generation config
            val generationConfig = JSONObject().apply {
                put("temperature", 0.75)
                put("topP", 0.95)
                put("topK", 40)
            }
            rootJson.put("generationConfig", generationConfig)

            val requestBody = rootJson.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiClient", "API error: ${response.code} $responseBody")
                return@withContext GeminiResponse.Error("HTTP ${response.code}: $responseBody", false)
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiResponse.Error("No candidates returned", false)
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts == null || parts.length() == 0) {
                return@withContext GeminiResponse.Error("Empty parts in response", false)
            }

            var textContent: String? = null
            var functionCallObj: JSONObject? = null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    val t = part.optString("text", "")
                    textContent = if (textContent == null) t else "$textContent $t"
                }
                if (part.has("functionCall")) {
                    functionCallObj = part.getJSONObject("functionCall")
                }
            }

            if (functionCallObj != null) {
                val funcName = functionCallObj.getString("name")
                val argsObj = functionCallObj.optJSONObject("args") ?: JSONObject()
                val argsMap = mutableMapOf<String, Any?>()
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = argsObj.opt(k)
                }
                return@withContext GeminiResponse.FunctionCallResponse(
                    functionName = funcName,
                    arguments = argsMap,
                    preSpokenText = textContent
                )
            }

            if (!textContent.isNullOrBlank()) {
                return@withContext GeminiResponse.TextResponse(textContent.trim())
            }

            GeminiResponse.Error("Received empty response from Gemini", false)

        } catch (e: Exception) {
            Log.e("GeminiClient", "Gemini request failure", e)
            GeminiResponse.Error(e.localizedMessage ?: "Network error", true)
        }
    }

    private fun createFunctionDeclaration(
        name: String,
        description: String,
        requiredParams: List<String>
    ): JSONObject {
        val func = JSONObject()
        func.put("name", name)
        func.put("description", description)

        val parameters = JSONObject()
        parameters.put("type", "OBJECT")
        val properties = JSONObject()

        requiredParams.forEach { param ->
            val paramProp = JSONObject()
            if (param.contains("seconds") || param == "hour" || param == "minute") {
                paramProp.put("type", "INTEGER")
            } else {
                paramProp.put("type", "STRING")
            }
            properties.put(param, paramProp)
        }
        parameters.put("properties", properties)
        parameters.put("required", JSONArray(requiredParams))
        func.put("parameters", parameters)

        return func
    }
}
