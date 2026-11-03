package com.example.openx.engine.translation

import com.example.openx.engine.mlkit.MlKitManager

class MlKitTranslationEngine : ITranslationEngine {
    override val engineId: String = "mlkit"
    override val displayName: String = "Google ML Kit (No Dispositivo)"
    override val isZeroCost: Boolean = true
    override val isOfflineSupported: Boolean = true
    override val isExperimental: Boolean = false
    override val rateLimitNotice: String = "Ilimitado e gratuito. Roda 100% no hardware do seu aparelho sem uso de internet."

    override suspend fun testConnection(apiKey: String, modelName: String): EngineConnectionTestResult {
        val start = System.currentTimeMillis()
        val isPtDownloaded = MlKitManager.isModelDownloaded("pt")
        val isEnDownloaded = MlKitManager.isModelDownloaded("en")

        val latency = System.currentTimeMillis() - start
        return if (isPtDownloaded && isEnDownloaded) {
            EngineConnectionTestResult(
                isSuccess = true,
                message = "Modelos de Português e Inglês prontos para tradução offline com custo zero.",
                latencyMs = latency,
                availableModels = listOf("ML Kit On-Device V3 (pt/en/ja/es/fr/de)")
            )
        } else if (isPtDownloaded) {
            EngineConnectionTestResult(
                isSuccess = true,
                message = "Modelo de Português disponível no aparelho. Modelos de origem adicionais podem ser baixados na Central de IA.",
                latencyMs = latency,
                availableModels = listOf("ML Kit On-Device V3")
            )
        } else {
            EngineConnectionTestResult(
                isSuccess = false,
                message = "O pacote de Português ainda não foi baixado no aparelho. Toque em 'Baixar Modelo' para ativar a tradução offline.",
                latencyMs = latency,
                availableModels = emptyList()
            )
        }
    }

    override suspend fun translate(request: TranslationRequest): EngineTranslationResponse {
        return try {
            val result = MlKitManager.translateOnDevice(
                text = request.text,
                sourceLangCode = request.sourceLang,
                targetLangCode = request.targetLang
            )
            EngineTranslationResponse(
                translatedText = result,
                isSuccess = true,
                isOnlineCall = false
            )
        } catch (e: Exception) {
            EngineTranslationResponse(
                translatedText = "",
                isSuccess = false,
                errorMessage = "Falha no ML Kit on-device: ${e.message}",
                isOnlineCall = false
            )
        }
    }
}
