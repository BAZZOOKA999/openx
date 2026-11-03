package com.example.openx.engine.translation

import com.example.openx.data.db.entities.GlossaryEntity

data class EngineContext(
    val speaker: String = "",
    val surroundingContext: String = "",
    val glossary: List<GlossaryEntity> = emptyList(),
    val style: TranslationStyle = TranslationStyle.NATURAL
)

data class TranslationRequest(
    val text: String,
    val sourceLang: String,
    val targetLang: String,
    val context: EngineContext,
    val apiKey: String = "",
    val modelName: String = ""
)

data class EngineTranslationResponse(
    val translatedText: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val isOnlineCall: Boolean = false,
    val retryAfterSeconds: Long? = null
)

data class EngineConnectionTestResult(
    val isSuccess: Boolean,
    val message: String,
    val latencyMs: Long = 0,
    val availableModels: List<String> = emptyList()
)

interface ITranslationEngine {
    val engineId: String
    val displayName: String
    val isZeroCost: Boolean
    val isOfflineSupported: Boolean
    val isExperimental: Boolean
    val rateLimitNotice: String

    suspend fun testConnection(apiKey: String, modelName: String): EngineConnectionTestResult
    suspend fun translate(request: TranslationRequest): EngineTranslationResponse
}
