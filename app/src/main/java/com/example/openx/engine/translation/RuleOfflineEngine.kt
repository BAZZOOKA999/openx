package com.example.openx.engine.translation

class RuleOfflineEngine : ITranslationEngine {
    override val engineId: String = "offline_rule"
    override val displayName: String = "Regras & Glossário Local"
    override val isZeroCost: Boolean = true
    override val isOfflineSupported: Boolean = true
    override val isExperimental: Boolean = false
    override val rateLimitNotice: String = "100% offline e ilimitado. Traduz termos comuns de UI, menus, diálogos e aplica o glossário de personagens."

    override suspend fun testConnection(apiKey: String, modelName: String): EngineConnectionTestResult {
        return EngineConnectionTestResult(
            isSuccess = true,
            message = "Motor de regras offline pronto. Dicionário de visual novel e termos carregado.",
            latencyMs = 1,
            availableModels = listOf("OpenX Local Dictionary v1.0")
        )
    }

    override suspend fun translate(request: TranslationRequest): EngineTranslationResponse {
        val engine = TranslationEngine()
        val text = request.text
        var translated = engine.applyOfflineRuleTranslation(text, request.sourceLang, request.targetLang)

        // Apply character glossary substitutions
        for (g in request.context.glossary) {
            if (g.doNotTranslate) continue
            if (g.sourceTerm.isNotBlank() && g.targetTerm.isNotBlank()) {
                val regex = Regex("\\b${Regex.escape(g.sourceTerm)}\\b", if (g.caseSensitive) setOf() else setOf(RegexOption.IGNORE_CASE))
                translated = regex.replace(translated, g.targetTerm)
            }
        }

        return EngineTranslationResponse(
            translatedText = translated,
            isSuccess = true,
            isOnlineCall = false
        )
    }
}
