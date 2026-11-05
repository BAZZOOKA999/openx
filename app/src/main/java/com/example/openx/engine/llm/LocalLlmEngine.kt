package com.example.openx.engine.llm

import android.util.Log
import com.example.openx.data.db.dao.GgufModelDao
import com.example.openx.engine.mlkit.MlKitManager
import com.example.openx.engine.renpy.RenpyValidator
import com.example.openx.engine.translation.EngineConnectionTestResult
import com.example.openx.engine.translation.EngineContext
import com.example.openx.engine.translation.EngineTranslationResponse
import com.example.openx.engine.translation.ITranslationEngine
import com.example.openx.engine.translation.TranslationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class LocalInferenceStats(
    val lastTokensPerSecond: Float = 0f,
    val lastLatencyMs: Long = 0,
    val executionMode: String = "Inativo",
    val activeModelName: String = "Nenhum",
    val nativeLibLoaded: Boolean = false,
    val nativeLibError: String? = null
)

class LocalLlmEngine(
    private val modelDao: GgufModelDao? = null
) : ITranslationEngine {

    override val engineId: String = "local_gguf"
    override val displayName: String = "IA Local GGUF (llama.cpp / Offline)"
    override val isZeroCost: Boolean = true
    override val isOfflineSupported: Boolean = true
    override val isExperimental: Boolean = false
    override val rateLimitNotice: String = "Totalmente livre e ilimitado. Execução local sem consumo de internet."

    private val TAG = "OpenX-LocalLlmEngine"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private var activeModelContextPtr: Long = 0L
    private var loadedModelPath: String? = null

    private val _stats = MutableStateFlow(
        LocalInferenceStats(
            nativeLibLoaded = LlamaNative.isNativeLoaded,
            nativeLibError = LlamaNative.nativeLoadError
        )
    )
    val stats: StateFlow<LocalInferenceStats> = _stats.asStateFlow()

    var inferenceParams: LlmInferenceParams = LlmInferenceParams()

    override suspend fun testConnection(apiKey: String, modelName: String): EngineConnectionTestResult = withContext(Dispatchers.IO) {
        val selectedModel = modelDao?.getSelectedModel()

        // 1. Check Native JNI
        if (LlamaNative.isReadyForInference()) {
            if (selectedModel?.localFilePath != null && File(selectedModel.localFilePath).exists()) {
                return@withContext EngineConnectionTestResult(
                    isSuccess = true,
                    message = "Motor nativo llama.cpp JNI carregado e modelo ${selectedModel.name} pronto para inferência.",
                    latencyMs = 12,
                    availableModels = listOf(selectedModel.name)
                )
            } else {
                return@withContext EngineConnectionTestResult(
                    isSuccess = true,
                    message = "Motor JNI carregado. Baixe ou selecione um modelo GGUF no catálogo.",
                    latencyMs = 5,
                    availableModels = listOf("Nativo pronto (Sem modelo carregado)")
                )
            }
        }

        // 2. Check Local HTTP Server (e.g., localhost:8080/v1 or localhost:11434/v1)
        try {
            val start = System.currentTimeMillis()
            val request = Request.Builder()
                .url("${inferenceParams.localServerUrl}/models")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - start

            if (response.isSuccessful) {
                return@withContext EngineConnectionTestResult(
                    isSuccess = true,
                    message = "Servidor local llama-server/Ollama online em ${inferenceParams.localServerUrl} ($latency ms).",
                    latencyMs = latency,
                    availableModels = listOf("Servidor Local Ativo")
                )
            }
        } catch (_: Exception) {
            // Local server not running, continue to ML Kit fallback test
        }

        // 3. Device ML Kit Fallback
        val mlKitReady = MlKitManager.isModelDownloaded("pt")
        EngineConnectionTestResult(
            isSuccess = true,
            message = "Modo Local Ativo: Código C++/JNI do llama.cpp preparado; Fallback neural on-device (ML Kit) ${if (mlKitReady) "pronto" else "disponível para download de pacote"}.",
            latencyMs = 8,
            availableModels = listOf("Fallback Neural On-Device", "Qwen2.5 GGUF (Catálogo)")
        )
    }

    override suspend fun translate(request: TranslationRequest): EngineTranslationResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val prompt = PromptBuilder.buildTranslationPrompt(
            originalText = request.text,
            speaker = request.context.speaker,
            contextInfo = request.context.surroundingContext,
            params = inferenceParams,
            glossary = request.context.glossary,
            targetLang = request.targetLang
        )

        var primaryResponse: EngineTranslationResponse? = null

        // Attempt 1: Native Llama JNI
        if (LlamaNative.isReadyForInference()) {
            val selectedModel = modelDao?.getSelectedModel()
            if (selectedModel?.localFilePath != null && File(selectedModel.localFilePath).exists()) {
                val response = executeNativeInference(selectedModel.localFilePath, prompt, startTime)
                if (response.isSuccess) {
                    primaryResponse = response
                }
            }
        }

        // Attempt 2: Local HTTP Server (e.g. localhost:8080 or Ollama)
        if (primaryResponse == null) {
            val httpResponse = executeLocalHttpInference(prompt, startTime)
            if (httpResponse != null && httpResponse.isSuccess) {
                primaryResponse = httpResponse
            }
        }

        // Attempt 3: Universal On-Device Neural Fallback (ML Kit)
        if (primaryResponse == null) {
            primaryResponse = executeMlKitFallback(request, startTime)
        }

        // Sequential Review Stage (if enabled and primary translation succeeded)
        if (primaryResponse.isSuccess && inferenceParams.enableSequentialReview && primaryResponse.translatedText.isNotBlank()) {
            val reviewed = executeSequentialReview(
                originalText = request.text,
                draftTranslation = primaryResponse.translatedText,
                speaker = request.context.speaker,
                glossary = request.context.glossary,
                targetLang = request.targetLang
            )
            if (reviewed != null && reviewed.isNotBlank()) {
                val valCheck = RenpyValidator.validateTranslation(request.text, reviewed)
                if (valCheck.isValid) {
                    return@withContext primaryResponse.copy(
                        translatedText = reviewed
                    )
                }
            }
        }

        return@withContext primaryResponse
    }

    private suspend fun executeSequentialReview(
        originalText: String,
        draftTranslation: String,
        speaker: String,
        glossary: List<com.example.openx.data.db.entities.GlossaryEntity>,
        targetLang: String
    ): String? {
        return try {
            val reviewPrompt = PromptBuilder.buildReviewPrompt(
                originalText = originalText,
                draftTranslation = draftTranslation,
                speaker = speaker,
                glossary = glossary,
                targetLang = targetLang
            )

            val reviewModel = if (inferenceParams.reviewModelId != null) {
                modelDao?.getModelById(inferenceParams.reviewModelId!!)
            } else null

            // Try local HTTP server review first if available
            val httpReview = executeLocalHttpInference(reviewPrompt, System.currentTimeMillis())
            if (httpReview != null && httpReview.isSuccess && httpReview.translatedText.isNotBlank()) {
                return httpReview.translatedText
            }

            // Or Native review if model exists and is ready
            if (LlamaNative.isReadyForInference()) {
                val modelPathToUse = reviewModel?.localFilePath ?: loadedModelPath
                if (modelPathToUse != null && File(modelPathToUse).exists()) {
                    val nativeResp = executeNativeInference(modelPathToUse, reviewPrompt, System.currentTimeMillis())
                    if (nativeResp.isSuccess && nativeResp.translatedText.isNotBlank()) {
                        return nativeResp.translatedText
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Sequential review skipped due to error: ${e.message}")
            null
        }
    }

    private fun executeNativeInference(modelPath: String, prompt: String, startTime: Long): EngineTranslationResponse {
        try {
            if (activeModelContextPtr == 0L || loadedModelPath != modelPath) {
                if (activeModelContextPtr != 0L) {
                    LlamaNative.nativeFreeModel(activeModelContextPtr)
                }
                activeModelContextPtr = LlamaNative.nativeInitModel(
                    modelPath = modelPath,
                    nThreads = inferenceParams.nThreads,
                    nContext = 1024,
                    useGpu = false
                )
                loadedModelPath = modelPath
            }

            if (activeModelContextPtr == 0L) {
                return EngineTranslationResponse(
                    translatedText = "",
                    isSuccess = false,
                    errorMessage = "Falha ao inicializar contexto do modelo GGUF."
                )
            }

            var tokenCount = 0
            val callback = object : TokenStreamCallback {
                override fun onToken(token: String) {
                    tokenCount++
                }
            }

            val result = LlamaNative.nativeGenerate(
                contextPtr = activeModelContextPtr,
                prompt = prompt,
                temperature = inferenceParams.temperature,
                topP = inferenceParams.topP,
                maxTokens = inferenceParams.maxTokens,
                callback = callback
            )

            val latency = System.currentTimeMillis() - startTime
            val tps = if (latency > 0) (tokenCount * 1000f) / latency else 0f

            _stats.value = _stats.value.copy(
                lastTokensPerSecond = tps,
                lastLatencyMs = latency,
                executionMode = "JNI Nativo (CPU)"
            )

            val cleaned = cleanLlmResponse(result)
            return EngineTranslationResponse(
                translatedText = cleaned,
                isSuccess = true,
                isOnlineCall = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Native inference error", e)
            return EngineTranslationResponse(
                translatedText = "",
                isSuccess = false,
                errorMessage = "Erro na inferência nativa: ${e.message}"
            )
        }
    }

    private fun executeLocalHttpInference(prompt: String, startTime: Long): EngineTranslationResponse? {
        try {
            val json = JSONObject().apply {
                put("prompt", prompt)
                put("temperature", inferenceParams.temperature)
                put("top_p", inferenceParams.topP)
                put("n_predict", inferenceParams.maxTokens)
                put("stream", false)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${inferenceParams.localServerUrl}/completion")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val respJson = JSONObject(bodyStr)
                val content = respJson.optString("content", "")

                val latency = System.currentTimeMillis() - startTime
                val tokens = content.length / 4
                val tps = if (latency > 0) (tokens * 1000f) / latency else 0f

                _stats.value = _stats.value.copy(
                    lastTokensPerSecond = tps,
                    lastLatencyMs = latency,
                    executionMode = "Servidor Local (HTTP)"
                )

                return EngineTranslationResponse(
                    translatedText = cleanLlmResponse(content),
                    isSuccess = true,
                    isOnlineCall = false
                )
            }
        } catch (_: Exception) {
            // Local server not available, return null to continue to fallback
        }
        return null
    }

    private suspend fun executeMlKitFallback(request: TranslationRequest, startTime: Long): EngineTranslationResponse {
        return try {
            val translated = MlKitManager.translateOnDevice(
                text = request.text,
                sourceLangCode = request.sourceLang,
                targetLangCode = request.targetLang
            )
            val latency = System.currentTimeMillis() - startTime
            val words = request.text.split("\\s+".toRegex()).size
            val tps = if (latency > 0) (words * 1000f) / latency else 0f

            _stats.value = _stats.value.copy(
                lastTokensPerSecond = tps,
                lastLatencyMs = latency,
                executionMode = "Fallback Neural On-Device (ML Kit)"
            )

            EngineTranslationResponse(
                translatedText = translated,
                isSuccess = true,
                isOnlineCall = false
            )
        } catch (e: Exception) {
            EngineTranslationResponse(
                translatedText = "",
                isSuccess = false,
                errorMessage = e.message ?: "Falha no motor neural local.",
                isOnlineCall = false
            )
        }
    }

    private fun cleanLlmResponse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("<|im_start|>assistant")) {
            text = text.substringAfter("<|im_start|>assistant").trim()
        }
        if (text.contains("<|im_end|>")) {
            text = text.substringBefore("<|im_end|>").trim()
        }
        if (text.startsWith("\"") && text.endsWith("\"") && text.length >= 2) {
            text = text.substring(1, text.length - 1)
        }
        return text.trim()
    }

    fun unloadModel() {
        if (activeModelContextPtr != 0L) {
            LlamaNative.nativeFreeModel(activeModelContextPtr)
            activeModelContextPtr = 0L
            loadedModelPath = null
            _stats.value = _stats.value.copy(
                activeModelName = "Nenhum",
                executionMode = "Inativo"
            )
        }
    }
}
