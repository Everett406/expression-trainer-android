package com.sisi.expressiontrainer.ai

import com.sisi.expressiontrainer.ai.Prompts.PromptPair
import com.sisi.expressiontrainer.data.model.CustomPrompt
import com.sisi.expressiontrainer.data.model.Settings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

class AIFeedbackClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json".toMediaType()

    @Throws(IOException::class)
    suspend fun sendFeedback(text: String, settings: Settings, customPrompt: CustomPrompt?): String {
        val (endpoint, apiKey, model) = getProviderConfig(settings)
        val prompt = Prompts.getRealtimePrompt(text, null, customPrompt)
        return callAPI(endpoint, apiKey, model, prompt, maxTokens = 150)
    }

    @Throws(IOException::class)
    suspend fun sendReport(
        fullText: String,
        stats: Prompts.ReportPromptStats,
        settings: Settings,
        customPrompt: CustomPrompt?
    ): String {
        val (endpoint, apiKey, model) = getProviderConfig(settings)
        val prompt = Prompts.getReportPrompt(fullText, stats, customPrompt)
        return callAPI(endpoint, apiKey, model, prompt, maxTokens = 8192)
    }

    private fun getProviderConfig(settings: Settings): Triple<String, String, String> {
        return when (settings.aiProvider) {
            "deepseek" -> Triple(
                "https://api.deepseek.com/v1/chat/completions",
                settings.aiApiKey,
                settings.aiModel.ifBlank { "deepseek-chat" }
            )
            "openai" -> Triple(
                "https://api.openai.com/v1/chat/completions",
                settings.aiApiKey,
                settings.aiModel.ifBlank { "gpt-4o-mini" }
            )
            "ollama" -> Triple(
                "${settings.customEndpoint.ifBlank { "http://localhost:11434" }}/v1/chat/completions",
                "ollama",
                settings.aiModel.ifBlank { "qwen2.5:7b" }
            )
            "custom" -> Triple(
                settings.customEndpoint,
                settings.aiApiKey,
                settings.aiModel
            )
            else -> throw IllegalArgumentException("未知的 AI provider: ${settings.aiProvider}")
        }
    }

    @Throws(IOException::class)
    private suspend fun callAPI(
        endpoint: String,
        apiKey: String,
        model: String,
        prompt: PromptPair,
        maxTokens: Int
    ): String {
        val requestBody = ChatCompletionRequest(
            model = model,
            messages = listOf(
                Message(role = "system", content = prompt.system),
                Message(role = "user", content = prompt.user)
            ),
            maxTokens = maxTokens,
            temperature = 0.7
        )

        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(json.encodeToString(requestBody).toRequestBody(mediaType))
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IOException("API 请求失败 (${response.code}): $bodyString")
        }

        val completion = json.decodeFromString<ChatCompletionResponse>(bodyString)
        return completion.choices.firstOrNull()?.message?.content
            ?: throw IOException("AI 返回为空")
    }

    @Serializable
    data class ChatCompletionRequest(
        val model: String,
        val messages: List<Message>,
        @SerialName("max_tokens")
        val maxTokens: Int,
        val temperature: Double
    )

    @Serializable
    data class Message(
        val role: String,
        val content: String
    )

    @Serializable
    data class ChatCompletionResponse(
        val choices: List<Choice>
    )

    @Serializable
    data class Choice(
        val message: Message
    )
}
