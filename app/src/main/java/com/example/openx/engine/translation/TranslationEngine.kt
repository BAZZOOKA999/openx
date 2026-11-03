package com.example.openx.engine.translation

import com.example.openx.data.db.dao.TranslationMemoryDao
import com.example.openx.data.db.entities.GlossaryEntity
import com.example.openx.data.db.entities.TranslationMemoryEntity
import com.example.openx.engine.renpy.RenpyValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class TranslationEngine(private val tmDao: TranslationMemoryDao? = null) {

    private val engines = mapOf(
        "mlkit" to MlKitTranslationEngine(),
        "firebase_ai" to FirebaseAiEngine(),
        "groq" to GroqTranslationEngine(),
        "openrouter" to OpenRouterTranslationEngine(),
        "offline_rule" to RuleOfflineEngine()
    )

    private val _progress = MutableStateFlow(TranslationProgressState())
    val progress: StateFlow<TranslationProgressState> = _progress.asStateFlow()

    private val isPaused = AtomicBoolean(false)
    private val isCancelled = AtomicBoolean(false)

    fun getEngine(engineId: String): ITranslationEngine {
        return engines[engineId] ?: engines["mlkit"]!!
    }

    fun getAllEngines(): List<ITranslationEngine> = engines.values.toList()

    fun pause() {
        isPaused.set(true)
        _progress.value = _progress.value.copy(
            isPaused = true,
            statusMessage = "Fila de tradução pausada no checkpoint atual."
        )
    }

    fun resume() {
        isPaused.set(false)
        _progress.value = _progress.value.copy(
            isPaused = false,
            statusMessage = "Retomando fila a partir do checkpoint..."
        )
    }

    fun cancel() {
        isCancelled.set(true)
        isPaused.set(false)
        _progress.value = _progress.value.copy(
            isRunning = false,
            isPaused = false,
            statusMessage = "Fila de tradução cancelada pelo usuário."
        )
    }

    suspend fun translateBatch(
        items: List<BatchTranslateItem>,
        sourceLang: String,
        targetLang: String,
        style: TranslationStyle,
        glossary: List<GlossaryEntity>,
        selectedEngineId: String = "mlkit",
        modelName: String = "",
        apiKey: String = "",
        allowOnlineFallback: Boolean = false,
        onCheckpoint: suspend (BatchTranslateResult) -> Unit
    ) {
        isCancelled.set(false)
        isPaused.set(false)

        val primaryEngine = getEngine(selectedEngineId)
        val mlKitEngine = engines["mlkit"]!!
        val ruleEngine = engines["offline_rule"]!!

        _progress.value = TranslationProgressState(
            isRunning = true,
            isPaused = false,
            currentIndex = 0,
            totalCount = items.size,
            activeEngineName = primaryEngine.displayName,
            statusMessage = "Iniciando fila com ${primaryEngine.displayName}..."
        )

        var errorCount = 0
        var tmReusedCount = 0
        var onlineCallsCount = 0

        for ((index, item) in items.withIndex()) {
            if (isCancelled.get()) break

            while (isPaused.get()) {
                delay(300)
                if (isCancelled.get()) break
            }
            if (isCancelled.get()) break

            _progress.value = _progress.value.copy(
                currentIndex = index + 1,
                currentOriginal = item.originalText.take(55),
                statusMessage = "Processando item ${index + 1} de ${items.size}..."
            )

            try {
                val originalTrimmed = item.originalText.trim()

                // Step 1: Check if term is protected in Glossary (Do Not Translate)
                val isDnt = glossary.any { it.doNotTranslate && it.sourceTerm.equals(originalTrimmed, ignoreCase = !it.caseSensitive) }
                if (isDnt) {
                    val res = BatchTranslateResult(
                        id = item.id,
                        translatedText = item.originalText,
                        isSuccess = true,
                        reusedFromMemory = false,
                        engineUsed = "Glossário (DNT)"
                    )
                    onCheckpoint(res)
                    delay(20)
                    continue
                }

                // Step 2: Check Local Translation Memory (TM) cache
                val textHash = computeTextHash(originalTrimmed, sourceLang, targetLang)
                var tmHit: TranslationMemoryEntity? = null
                if (tmDao != null) {
                    tmHit = tmDao.findExactMatch(textHash, sourceLang, targetLang)
                }

                if (tmHit != null && tmHit.translatedText.isNotBlank()) {
                    tmReusedCount++
                    tmDao?.incrementUsage(tmHit.id, System.currentTimeMillis())
                    val res = BatchTranslateResult(
                        id = item.id,
                        translatedText = tmHit.translatedText,
                        isSuccess = true,
                        reusedFromMemory = true,
                        engineUsed = "Memória de Tradução"
                    )
                    onCheckpoint(res)
                    _progress.value = _progress.value.copy(reusedFromMemoryCount = tmReusedCount)
                    delay(15)
                    continue
                }

                // Step 3: Check glossary exact matches
                var glossaryMatch: String? = null
                for (g in glossary) {
                    if (g.sourceTerm.equals(originalTrimmed, ignoreCase = !g.caseSensitive) && g.targetTerm.isNotBlank()) {
                        glossaryMatch = g.targetTerm
                        break
                    }
                }
                if (glossaryMatch != null) {
                    val res = BatchTranslateResult(
                        id = item.id,
                        translatedText = glossaryMatch,
                        isSuccess = true,
                        engineUsed = "Glossário"
                    )
                    saveToTranslationMemory(textHash, originalTrimmed, glossaryMatch, sourceLang, targetLang, "glossary")
                    onCheckpoint(res)
                    delay(20)
                    continue
                }

                // Step 4: Execute Engine Translation with Fallback
                val engineContext = EngineContext(
                    speaker = item.speaker,
                    surroundingContext = item.contextInfo,
                    glossary = glossary,
                    style = style
                )
                val request = TranslationRequest(
                    text = item.originalText,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    context = engineContext,
                    apiKey = apiKey,
                    modelName = modelName
                )

                var response = primaryEngine.translate(request)

                // 429 Rate-limit handling with Retry-After header
                if (!response.isSuccess && response.retryAfterSeconds != null) {
                    val waitSec = response.retryAfterSeconds.coerceAtMost(30L)
                    _progress.value = _progress.value.copy(
                        statusMessage = "Limite de taxa atingido. Pausando ${waitSec}s para respeitar provedor..."
                    )
                    delay(waitSec * 1000L)
                    response = primaryEngine.translate(request)
                }

                // If primary online engine fails, fallback gracefully to ML Kit on-device
                var usedEngineId = primaryEngine.engineId
                if (!response.isSuccess && primaryEngine != mlKitEngine) {
                    val fallbackResp = mlKitEngine.translate(request)
                    if (fallbackResp.isSuccess) {
                        response = fallbackResp
                        usedEngineId = "mlkit (fallback)"
                    } else {
                        // Offline Rule fallback
                        response = ruleEngine.translate(request)
                        usedEngineId = "regras_locais (fallback)"
                    }
                }

                if (response.isOnlineCall) {
                    onlineCallsCount++
                }

                if (response.isSuccess && response.translatedText.isNotBlank()) {
                    var finalTranslated = response.translatedText

                    // Apply character glossary terms replacements in sentence
                    for (g in glossary) {
                        if (g.doNotTranslate) continue
                        if (g.sourceTerm.isNotBlank() && g.targetTerm.isNotBlank()) {
                            val regex = Regex("\\b${Regex.escape(g.sourceTerm)}\\b", if (g.caseSensitive) setOf() else setOf(RegexOption.IGNORE_CASE))
                            finalTranslated = regex.replace(finalTranslated, g.targetTerm)
                        }
                    }

                    // Validate placeholders & tags
                    val validation = RenpyValidator.validateTranslation(item.originalText, finalTranslated)
                    if (!validation.isValid) {
                        // Re-inject missing placeholders if omitted by external model
                        for (p in validation.missingPlaceholders) {
                            if (!finalTranslated.contains(p)) {
                                finalTranslated = "$finalTranslated $p"
                            }
                        }
                    }

                    // Save to Translation Memory for future zero-cost instant hits
                    saveToTranslationMemory(textHash, originalTrimmed, finalTranslated, sourceLang, targetLang, usedEngineId)

                    onCheckpoint(
                        BatchTranslateResult(
                            id = item.id,
                            translatedText = finalTranslated,
                            isSuccess = true,
                            reusedFromMemory = false,
                            engineUsed = usedEngineId,
                            isOnlineCall = response.isOnlineCall
                        )
                    )
                } else {
                    errorCount++
                    onCheckpoint(
                        BatchTranslateResult(
                            id = item.id,
                            translatedText = "",
                            isSuccess = false,
                            errorMessage = response.errorMessage ?: "Erro desconhecido na tradução"
                        )
                    )
                }

                // Rate-limit safety pacing
                delay(60)

            } catch (e: CancellationException) {
                break
            } catch (e: Exception) {
                errorCount++
                onCheckpoint(
                    BatchTranslateResult(
                        id = item.id,
                        translatedText = "",
                        isSuccess = false,
                        errorMessage = e.message ?: "Falha ao processar item"
                    )
                )
            }

            _progress.value = _progress.value.copy(
                currentIndex = index + 1,
                errorCount = errorCount,
                reusedFromMemoryCount = tmReusedCount,
                onlineCallsCount = onlineCallsCount
            )
        }

        _progress.value = _progress.value.copy(
            isRunning = false,
            isPaused = false,
            statusMessage = if (isCancelled.get()) "Fila interrompida no checkpoint." else "Lote concluído com sucesso!"
        )
    }

    private suspend fun saveToTranslationMemory(
        hash: String,
        source: String,
        translated: String,
        sourceLang: String,
        targetLang: String,
        engineId: String
    ) {
        if (tmDao == null || source.isBlank() || translated.isBlank()) return
        try {
            tmDao.insertOrUpdate(
                TranslationMemoryEntity(
                    textHash = hash,
                    sourceText = source,
                    translatedText = translated,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    engineId = engineId,
                    isReviewed = false
                )
            )
        } catch (_: Exception) {}
    }

    private fun computeTextHash(text: String, sourceLang: String, targetLang: String): String {
        val input = "${text.trim().lowercase()}|$sourceLang|$targetLang"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }

    fun applyOfflineRuleTranslation(text: String, sourceLang: String, targetLang: String): String {
        val ruleEngine = RuleOfflineEngine()
        return (ruleEngine as? RuleOfflineEngine)?.let {
            // Common phrase dictionary
            val commonPhrases = mapOf(
                "Start Game" to "Iniciar Jogo",
                "Start" to "Iniciar",
                "Load Game" to "Carregar Jogo",
                "Load" to "Carregar",
                "Save Game" to "Salvar Jogo",
                "Save" to "Salvar",
                "Preferences" to "Configurações",
                "Settings" to "Ajustes",
                "Options" to "Opções",
                "Quit" to "Sair",
                "Exit" to "Sair",
                "Return" to "Voltar",
                "Back" to "Voltar",
                "Cancel" to "Cancelar",
                "Confirm" to "Confirmar",
                "Yes" to "Sim",
                "No" to "Não",
                "OK" to "OK",
                "Auto" to "Automático",
                "Skip" to "Avançar",
                "History" to "Histórico",
                "Log" to "Registro",
                "Help" to "Ajuda",
                "About" to "Sobre",
                "Music Volume" to "Volume da Música",
                "Sound Volume" to "Volume dos Efeitos",
                "Voice Volume" to "Volume das Vozes",
                "Text Speed" to "Velocidade do Texto",
                "Auto-Forward Time" to "Avanço Automático",
                "Display" to "Exibição",
                "Window" to "Janela",
                "Fullscreen" to "Tela Cheia",
                "Skip Unseen Text" to "Pular Texto Não Visto",
                "After Choices" to "Após Escolhas",
                "Transitions" to "Transições",
                "Chapter" to "Capítulo",
                "Prologue" to "Prólogo",
                "Epilogue" to "Epílogo",
                "Loading..." to "Carregando...",
                "Please wait..." to "Por favor, aguarde...",
                "Are you sure you want to quit?" to "Tem certeza de que deseja sair?",
                "Are you sure you want to overwrite your save?" to "Tem certeza de que deseja sobrescrever o salvamento?",
                "Empty slot" to "Espaço vazio",
                "Empty" to "Vazio"
            )

            for ((en, pt) in commonPhrases) {
                if (text.equals(en, ignoreCase = true)) {
                    return pt
                }
            }

            var output = text
            val expressions = listOf(
                Pair(Regex("\\bGood morning\\b", RegexOption.IGNORE_CASE), "Bom dia"),
                Pair(Regex("\\bGood afternoon\\b", RegexOption.IGNORE_CASE), "Boa tarde"),
                Pair(Regex("\\bGood evening\\b", RegexOption.IGNORE_CASE), "Boa noite"),
                Pair(Regex("\\bGood night\\b", RegexOption.IGNORE_CASE), "Boa noite"),
                Pair(Regex("\\bThank you\\b", RegexOption.IGNORE_CASE), "Obrigado"),
                Pair(Regex("\\bThanks\\b", RegexOption.IGNORE_CASE), "Valeu"),
                Pair(Regex("\\bYou're welcome\\b", RegexOption.IGNORE_CASE), "De nada"),
                Pair(Regex("\\bExcuse me\\b", RegexOption.IGNORE_CASE), "Com licença"),
                Pair(Regex("\\bI'm sorry\\b", RegexOption.IGNORE_CASE), "Sinto muito"),
                Pair(Regex("\\bSorry\\b", RegexOption.IGNORE_CASE), "Desculpe"),
                Pair(Regex("\\bWhat happened\\?\\b", RegexOption.IGNORE_CASE), "O que aconteceu?"),
                Pair(Regex("\\bWhere are you going\\?\\b", RegexOption.IGNORE_CASE), "Aonde você vai?"),
                Pair(Regex("\\bI don't know\\b", RegexOption.IGNORE_CASE), "Eu não sei"),
                Pair(Regex("\\bLet's go\\b", RegexOption.IGNORE_CASE), "Vamos lá"),
                Pair(Regex("\\bSee you later\\b", RegexOption.IGNORE_CASE), "Até mais")
            )
            for ((pattern, replacement) in expressions) {
                output = pattern.replace(output, replacement)
            }
            output
        } ?: text
    }
}
