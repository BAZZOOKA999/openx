package com.example.openx.engine.translation

import com.example.openx.data.db.entities.GlossaryEntity

enum class TranslationStyle(val label: String, val promptInstruction: String) {
    NATURAL(
        "Natural (Visual Novel)",
        "Traduza com naturalidade para português brasileiro fluido, adaptando gírias, mantendo o tom dramático e o carisma dos personagens sem parecer robótico."
    ),
    LITERAL(
        "Literal (Fiel)",
        "Traduza com precisão literal estrita, mantendo a estrutura de frases e terminologia exata da língua fonte."
    ),
    CONTEXTUAL(
        "Contextual (Narrativo de Jogos)",
        "Traduza focando no contexto da narrativa de jogos e interfaces, respeitando limites de espaço e termos comuns da indústria de jogos."
    )
}

data class BatchTranslateItem(
    val id: Long,
    val speaker: String,
    val originalText: String,
    val contextInfo: String
)

data class BatchTranslateResult(
    val id: Long,
    val translatedText: String,
    val isSuccess: Boolean,
    val reusedFromMemory: Boolean = false,
    val engineUsed: String = "",
    val isOnlineCall: Boolean = false,
    val errorMessage: String? = null
)

data class TranslationProgressState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentOriginal: String = "",
    val errorCount: Int = 0,
    val reusedFromMemoryCount: Int = 0,
    val onlineCallsCount: Int = 0,
    val activeEngineName: String = "Google ML Kit (No Dispositivo)",
    val statusMessage: String = ""
)
