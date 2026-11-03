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

class GroqTranslationEngine : ITranslationEngine {
    override val engineId: String = "groq"
    override val displayName: String = "Groq LPU (Experimental)"
    override val isZeroCost: Boolean = false
    override val isOfflineSupported: Boolean = false
    override val isExperimental: Boolean = true
    override val rateLimitNotice: String = "Groq Free Tier: limites de tokens por minuto e 30 RPM. Requer chave individual configurada na Central de IA."

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    override suspend fun testConnection(apiKey: String, modelName: String): EngineConnectionTestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext EngineConnectionTestResult(
                isSuccess = false,
                message = "Chave da API Groq não informada. Insira sua chave pessoal na Central de IA.",
                availableModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")
            )
        }

        val start = System.currentTimeMillis()
        try {
            val model = if (modelName.isNotBlank()) modelName else "llama-3.3-70b-versatile"
            val bodyJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Translate 'Start' to Portuguese. Output single word.")
                    })
                })
                put("max_tokens", 10)
            }

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - start

            if (response.isSuccessful) {
                EngineConnectionTestResult(
                    isSuccess = true,
                    message = "Conexão com Groq OK ($model). Velocidade LPU ultra-rápida.",
                    latencyMs = latency,
                    availableModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768")
                )
            } else {
                EngineConnectionTestResult(
                    isSuccess = false,
                    message = "Erro Groq HTTP ${response.code}: ${response.message}",
                    latencyMs = latency,
                    availableModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant")
                )
            }
        } catch (e: Exception) {
            EngineConnectionTestResult(
                isSuccess = false,
                message = "Falha ao conectar com Groq: ${e.message}"
            )
        }
    }

    override suspend fun translate(request: TranslationRequest): EngineTranslationResponse = withContext(Dispatchers.IO) {
        if (request.apiKey.isBlank()) {
            return@withContext EngineTranslationResponse(
                translatedText = "",
                isSuccess = false,
                errorMessage = "Chave da API Groq ausente",
                isOnlineCall = false
            )
        }

        try {
            val model = if (request.modelName.isNotBlank()) request.modelName else "llama-3.3-70b-versatile"

            val systemInstruction = "Você é o tradutor oficial do OpenX para Visual Novels e jogos. Estilo: ${request.context.style.promptInstruction}. Traduza de ${request.sourceLang} para ${request.targetLang}. Preserve estritamente todas as tags Ren'Py ({b}, {i}, {color=...}, {w}), variáveis ([player], %(name)s) e escapes (\\n). Retorne EXCLUSIVAMENTE o texto traduzido final, sem introdução ou explicações."

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
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer ${request.apiKey}")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(httpRequest).execute()

            if (response.code == 429) {
                val retryAfter = response.header("retry-after")?.toLongOrNull() ?: 10L
                return@withContext EngineTranslationResponse(
                    translatedText = "",
                    isSuccess = false,
                    errorMessage = "Groq Limite de taxa (HTTP 429). Aguarde ${retryAfter}s.",
                    isOnlineCall = true,
                    retryAfterSeconds = retryAfter
                )
            }

            if (!response.isSuccessful) {
                return@withContext EngineTranslationResponse(
                    translatedText = "",
                    isSuccess = false,
                    errorMessage = "Groq Erro HTTP ${response.code}: ${response.message}",
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
                errorMessage = "Erro Groq: ${e.message}",
                isOnlineCall = true
            )
        }
    }
}
