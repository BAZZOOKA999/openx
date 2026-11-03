package com.example.openx.engine.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterTranslationEngine : ITranslationEngine {
    override val engineId: String = "openrouter"
    override val displayName: String = "OpenRouter (Experimental)"
    override val isZeroCost: Boolean = false
    override val isOfflineSupported: Boolean = false
    override val isExperimental: Boolean = true
    override val rateLimitNotice: String = "Acesso a modelos livres e multi-provedores. Requer chave configurada pelo usuário. Limites de acordo com o modelo escolhido."

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    override suspend fun testConnection(apiKey: String, modelName: String): EngineConnectionTestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext EngineConnectionTestResult(
                isSuccess = false,
                message = "Chave OpenRouter ausente. Configure na Central de IA.",
                availableModels = listOf(
                    "meta-llama/llama-3.3-70b-instruct:free",
                    "google/gemini-2.0-flash-exp:free",
                    "mistralai/mistral-7b-instruct:free"
                )
            )
        }

        val start = System.currentTimeMillis()
        try {
            val model = if (modelName.isNotBlank()) modelName else "meta-llama/llama-3.3-70b-instruct:free"
            val bodyJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Translate 'Hi' to Portuguese. Output single word.")
                    })
                })
                put("max_tokens", 5)
            }

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("HTTP-Referer", "https://openx.local")
                .addHeader("X-Title", "OpenX Translation Studio")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - start

            if (response.isSuccessful) {
                EngineConnectionTestResult(
                    isSuccess = true,
                    message = "OpenRouter conectado com sucesso ($model).",
                    latencyMs = latency,
                    availableModels = listOf(
                        "meta-llama/llama-3.3-70b-instruct:free",
                        "google/gemini-2.0-flash-exp:free",
                        "mistralai/mistral-7b-instruct:free"
                    )
                )
            } else {
                EngineConnectionTestResult(
                    isSuccess = false,
                    message = "Erro OpenRouter HTTP ${response.code}: ${response.message}",
                    latencyMs = latency,
                    availableModels = listOf("meta-llama/llama-3.3-70b-instruct:free")
                )
            }
        } catch (e: Exception) {
            EngineConnectionTestResult(
                isSuccess = false,
                message = "Falha ao conectar ao OpenRouter: ${e.message}"
            )
        }
    }

    override suspend fun translate(request: TranslationRequest): EngineTranslationResponse = withContext(Dispatchers.IO) {
        if (request.apiKey.isBlank()) {
            return@withContext EngineTranslationResponse(
                translatedText = "",
                isSuccess = false,
                errorMessage = "Chave do OpenRouter ausente",
                isOnlineCall = false
            )
        }

        try {
            val model = if (request.modelName.isNotBlank()) request.modelName else "meta-llama/llama-3.3-70b-instruct:free"
            val systemInstruction = "Você é o tradutor oficial do OpenX. Estilo: ${request.context.style.promptInstruction}. Traduza de ${request.sourceLang} para ${request.targetLang}. Preserve estritamente todas as tags Ren'Py ({b}, {i}, {color=...}, {w}), variáveis ([player], %(name)s) e escapes (\\n). Retorne EXCLUSIVAMENTE o texto traduzido final, sem introdução ou explicações."

            val bodyJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstruction)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", request.text)
                    })
                })
                put("temperature", 0.3)
            }

            val httpRequest = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${request.apiKey}")
                .addHeader("HTTP-Referer", "https://openx.local")
                .addHeader("X-Title", "OpenX Translation Studio")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(httpRequest).execute()

            if (response.code == 429) {
                val retryAfter = response.header("retry-after")?.toLongOrNull() ?: 15L
                return@withContext EngineTranslationResponse(
                    translatedText = "",
                    isSuccess = false,
                    errorMessage = "OpenRouter Limite de taxa (HTTP 429). Aguarde ${retryAfter}s.",
                    isOnlineCall = true,
                    retryAfterSeconds = retryAfter
                )
            }

            if (!response.isSuccessful) {
                return@withContext EngineTranslationResponse(
                    translatedText = "",
                    isSuccess = false,
                    errorMessage = "OpenRouter Erro HTTP ${response.code}: ${response.message}",
                    isOnlineCall = true
                )
            }

            val respBody = response.body?.string() ?: ""
            val jsonResp = JSONObject(respBody)
            val choices = jsonResp.optJSONArray("choices")
            val textOut = choices?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "")
                ?.trim() ?: ""

            EngineTranslationResponse(
                translatedText = textOut,
                isSuccess = textOut.isNotBlank(),
                isOnlineCall = true
            )
        } catch (e: Exception) {
            EngineTranslationResponse(
                translatedText = "",
                isSuccess = false,
                errorMessage = "Erro OpenRouter: ${e.message}",
                isOnlineCall = true
            )
        }
    }
}
