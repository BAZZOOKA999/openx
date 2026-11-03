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

class FirebaseAiEngine : ITranslationEngine {
    override val engineId: String = "firebase_ai"
    override val displayName: String = "Google Gemini (Firebase AI Logic)"
    override val isZeroCost: Boolean = false
    override val isOfflineSupported: Boolean = false
    override val isExperimental: Boolean = false
    override val rateLimitNotice: String = "Plano gratuito do Gemini: aprox. 15 requisições por minuto (RPM). O OpenX pausa automaticamente em caso de limite."

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    override suspend fun testConnection(apiKey: String, modelName: String): EngineConnectionTestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext EngineConnectionTestResult(
                isSuccess = false,
                message = "Chave do Gemini não configurada. Configure na Central de IA ou no painel Secrets do AI Studio.",
                availableModels = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash")
            )
        }

        val start = System.currentTimeMillis()
        try {
            val model = if (modelName.isNotBlank()) modelName else "gemini-1.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val bodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Translate 'Hello' to Portuguese. Output only the translation."))
                        })
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - start

            if (response.isSuccessful) {
                EngineConnectionTestResult(
                    isSuccess = true,
                    message = "Conexão com Gemini estabelecida com sucesso ($model).",
                    latencyMs = latency,
                    availableModels = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash")
                )
            } else {
                EngineConnectionTestResult(
                    isSuccess = false,
                    message = "Erro Gemini HTTP ${response.code}: ${response.message}",
                    latencyMs = latency,
                    availableModels = listOf("gemini-1.5-flash", "gemini-1.5-pro")
                )
            }
        } catch (e: Exception) {
            EngineConnectionTestResult(
                isSuccess = false,
                message = "Falha ao conectar com Gemini: ${e.message}"
            )
        }
    }

    override suspend fun translate(request: TranslationRequest): EngineTranslationResponse = withContext(Dispatchers.IO) {
        if (request.apiKey.isBlank()) {
            return@withContext EngineTranslationResponse(
                translatedText = "",
                isSuccess = false,
                errorMessage = "Chave de API do Gemini ausente",
                isOnlineCall = false
            )
        }

        try {
            val model = if (request.modelName.isNotBlank()) request.modelName else "gemini-1.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${request.apiKey}"

            // Build system prompt with style, character info, and preservation rules
            val promptBuilder = StringBuilder()
            promptBuilder.append("Você é o tradutor especializado do OpenX para Visual Novels e jogos.\n")
            promptBuilder.append("Estilo: ${request.context.style.promptInstruction}\n")
            promptBuilder.append("Idioma de origem: ${request.sourceLang}, Idioma de destino: ${request.targetLang}\n")
            
            if (request.context.speaker.isNotBlank()) {
                promptBuilder.append("Personagem falando: ${request.context.speaker}\n")
            }

            // Character glossary constraints
            val relevantGlossary = request.context.glossary.filter { request.text.contains(it.sourceTerm, ignoreCase = true) }
            if (relevantGlossary.isNotEmpty()) {
                promptBuilder.append("Regras de glossário e personagens:\n")
                for (g in relevantGlossary) {
                    if (g.doNotTranslate) {
                        promptBuilder.append("- Mantenha '${g.sourceTerm}' SEM TRADUZIR.\n")
                    } else if (g.targetTerm.isNotBlank()) {
                        promptBuilder.append("- '${g.sourceTerm}' deve ser traduzido como '${g.targetTerm}'.\n")
                    }
                    if (g.gender != "Não especificado") {
                        promptBuilder.append("  (Gênero: ${g.gender})\n")
                    }
                    if (g.speechStyle.isNotBlank()) {
                        promptBuilder.append("  (Estilo de fala: ${g.speechStyle})\n")
                    }
                }
            }

            promptBuilder.append("ATENÇÃO: Preserve estritamente todas as tags Ren'Py (ex: {b}, {/b}, {i}, {color=...}, {w}), variáveis (ex: [player], %(name)s) e escapes (\\n, \\\"). Retorne APENAS o texto traduzido final, sem aspas adicionais e sem explicações.\n\n")
            promptBuilder.append("Texto a traduzir:\n${request.text}")

            val bodyJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", promptBuilder.toString()))
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                })
            }

            val httpRequest = Request.Builder()
                .url(url)
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(httpRequest).execute()

            if (response.code == 429) {
                val retryAfter = response.header("Retry-After")?.toLongOrNull() ?: 15L
                return@withContext EngineTranslationResponse(
                    translatedText = "",
                    isSuccess = false,
                    errorMessage = "Limite de taxa atingido (HTTP 429). Aguarde ${retryAfter}s.",
                    isOnlineCall = true,
                    retryAfterSeconds = retryAfter
                )
            }

            if (!response.isSuccessful) {
                return@withContext EngineTranslationResponse(
                    translatedText = "",
                    isSuccess = false,
                    errorMessage = "Erro HTTP ${response.code}: ${response.message}",
                    isOnlineCall = true
                )
            }

            val respBody = response.body?.string() ?: ""
            val jsonResp = JSONObject(respBody)
            val candidates = jsonResp.optJSONArray("candidates")
            val textOut = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text", "")
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
                errorMessage = "Exceção na chamada Gemini: ${e.message}",
                isOnlineCall = true
            )
        }
    }
}
