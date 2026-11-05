package com.example.openx.player.engine

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class RenpyChoice(
    val text: String,
    val targetLabel: String
)

data class RenpyDialogue(
    val character: String?,
    val text: String,
    val rawText: String,
    val background: String? = null,
    val musicTrack: String? = null
)

data class RenpyGameState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentLabel: String = "start",
    val currentStepIndex: Int = 0,
    val sceneBackground: String = "bg_library",
    val characterSpeaker: String? = null,
    val characterNameColor: String = "#E50914",
    val dialogueText: String = "",
    val originalText: String = "",
    val activeChoices: List<RenpyChoice> = emptyList(),
    val isAutoMode: Boolean = false,
    val isSkipMode: Boolean = false,
    val isUiHidden: Boolean = false,
    val isPortugueseActive: Boolean = true,
    val gameTitle: String = "OpenX Ren'Py Game",
    val historyLog: List<RenpyDialogue> = emptyList(),
    val runtimeLogs: List<String> = emptyList(),
    val saveMessage: String? = null
)

class RenpyRuntimeBridge(private val context: Context) {

    private val _gameState = MutableStateFlow(RenpyGameState())
    val gameState: StateFlow<RenpyGameState> = _gameState.asStateFlow()

    private var activeGameDir: File? = null
    private var scriptStatements: List<RenpyStatement> = emptyList()
    private var translationMap: Map<String, String> = emptyMap() // original -> portuguese
    private var labelIndexMap: Map<String, Int> = emptyMap()

    private var autoSkipJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    sealed class RenpyStatement {
        data class Scene(val bgName: String) : RenpyStatement()
        data class PlayMusic(val trackName: String) : RenpyStatement()
        data class StopMusic(val fade: Float = 1f) : RenpyStatement()
        data class Say(val speaker: String?, val text: String) : RenpyStatement()
        data class Menu(val prompt: String?, val choices: List<RenpyChoice>) : RenpyStatement()
        data class Jump(val targetLabel: String) : RenpyStatement()
        data class PythonExpr(val code: String) : RenpyStatement()
    }

    suspend fun loadAndStartGame(
        gameDir: File,
        gameTitle: String,
        startPortuguese: Boolean = true
    ) = withContext(Dispatchers.IO) {
        activeGameDir = gameDir
        val logs = mutableListOf<String>()
        logs.add("Iniciando ambiente OpenX Player para: $gameTitle")
        logs.add("Diretório do jogo: ${gameDir.absolutePath}")

        // 1. Load Translations from game/tl/portuguese/
        translationMap = loadPortugueseTranslations(gameDir, logs)

        // 2. Parse Ren'Py scripts (game/script.rpy or options)
        val gameSubdir = File(gameDir, "game").takeIf { it.exists() } ?: gameDir
        val scriptFile = File(gameSubdir, "script.rpy").takeIf { it.exists() }
            ?: File(gameDir, "script.rpy").takeIf { it.exists() }

        if (scriptFile != null) {
            logs.add("Script principal encontrado: ${scriptFile.name} (${scriptFile.length()} bytes)")
            val parsed = parseRenpyScript(scriptFile, logs)
            scriptStatements = parsed.first
            labelIndexMap = parsed.second
        } else {
            logs.add("Aviso: Nenhum script.rpy simples encontrado. Inicializando script padrão de demonstração.")
            val demo = createDemoScript()
            scriptStatements = demo.first
            labelIndexMap = demo.second
        }

        logs.add("Total de ${scriptStatements.size} comandos de roteiro carregados.")
        logs.add("Total de ${labelIndexMap.size} labels registradas: ${labelIndexMap.keys.joinToString(", ")}")

        _gameState.value = RenpyGameState(
            isRunning = true,
            isPaused = false,
            currentLabel = "start",
            currentStepIndex = 0,
            sceneBackground = "bg_library",
            isPortugueseActive = startPortuguese,
            gameTitle = gameTitle,
            runtimeLogs = logs
        )

        // Execute first step
        executeCurrentStep()
        startAutoSkipLoop()
    }

    fun nextStep() {
        val current = _gameState.value
        if (!current.isRunning || current.isPaused || current.activeChoices.isNotEmpty()) return

        val nextIndex = current.currentStepIndex + 1
        if (nextIndex < scriptStatements.size) {
            _gameState.value = current.copy(currentStepIndex = nextIndex)
            executeCurrentStep()
        } else {
            addLog("Fim do script Ren'Py atingido.")
            _gameState.value = current.copy(
                dialogueText = "Fim da demonstração. Obrigado por jogar!",
                characterSpeaker = "Narrador"
            )
        }
    }

    fun selectChoice(choice: RenpyChoice) {
        addLog("Escolha selecionada: '${choice.text}' -> saltando para label: ${choice.targetLabel}")
        jumpToLabel(choice.targetLabel)
    }

    fun jumpToLabel(targetLabel: String) {
        val index = labelIndexMap[targetLabel]
        if (index != null && index < scriptStatements.size) {
            _gameState.value = _gameState.value.copy(
                currentLabel = targetLabel,
                currentStepIndex = index,
                activeChoices = emptyList()
            )
            executeCurrentStep()
        } else {
            addLog("Erro: Label '$targetLabel' não encontrada no roteiro.")
        }
    }

    private fun executeCurrentStep() {
        val state = _gameState.value
        val index = state.currentStepIndex
        if (index < 0 || index >= scriptStatements.size) return

        when (val stmt = scriptStatements[index]) {
            is RenpyStatement.Scene -> {
                _gameState.value = state.copy(
                    sceneBackground = stmt.bgName,
                    activeChoices = emptyList()
                )
                addLog("Cena alterada: ${stmt.bgName}")
                nextStep()
            }
            is RenpyStatement.PlayMusic -> {
                addLog("Áudio: Reproduzindo trilha '${stmt.trackName}'")
                nextStep()
            }
            is RenpyStatement.StopMusic -> {
                addLog("Áudio: Música parada")
                nextStep()
            }
            is RenpyStatement.Jump -> {
                jumpToLabel(stmt.targetLabel)
            }
            is RenpyStatement.PythonExpr -> {
                addLog("Execução Python: ${stmt.code}")
                nextStep()
            }
            is RenpyStatement.Say -> {
                val original = stmt.text
                val translated = if (state.isPortugueseActive) {
                    translationMap[original] ?: original
                } else {
                    original
                }

                val speaker = stmt.speaker
                val color = when (speaker?.lowercase()) {
                    "eileen", "clara" -> "#E50914"
                    "detective", "investigador" -> "#3B82F6"
                    "sophia", "bruxa" -> "#A855F7"
                    else -> "#FFFFFF"
                }

                val newHistory = state.historyLog + RenpyDialogue(
                    character = speaker,
                    text = translated,
                    rawText = original,
                    background = state.sceneBackground
                )

                _gameState.value = state.copy(
                    characterSpeaker = speaker,
                    characterNameColor = color,
                    dialogueText = translated,
                    originalText = original,
                    activeChoices = emptyList(),
                    historyLog = newHistory
                )
            }
            is RenpyStatement.Menu -> {
                val translatedChoices = stmt.choices.map { choice ->
                    val trans = if (state.isPortugueseActive) {
                        translationMap[choice.text] ?: choice.text
                    } else {
                        choice.text
                    }
                    RenpyChoice(text = trans, targetLabel = choice.targetLabel)
                }
                _gameState.value = state.copy(
                    activeChoices = translatedChoices,
                    isAutoMode = false,
                    isSkipMode = false
                )
                addLog("Aguardando decisão do jogador no menu com ${stmt.choices.size} opções.")
            }
        }
    }

    fun toggleAutoMode() {
        val current = _gameState.value.isAutoMode
        _gameState.value = _gameState.value.copy(
            isAutoMode = !current,
            isSkipMode = false
        )
        addLog(if (!current) "Modo Avanço Automático ATIVADO" else "Modo Avanço Automático DESATIVADO")
    }

    fun toggleSkipMode() {
        val current = _gameState.value.isSkipMode
        _gameState.value = _gameState.value.copy(
            isSkipMode = !current,
            isAutoMode = false
        )
        addLog(if (!current) "Modo Skip (Pular) ATIVADO" else "Modo Skip DESATIVADO")
    }

    fun toggleUiVisibility() {
        val current = _gameState.value.isUiHidden
        _gameState.value = _gameState.value.copy(isUiHidden = !current)
    }

    fun toggleLanguage() {
        val next = !_gameState.value.isPortugueseActive
        val state = _gameState.value
        val textToDisplay = if (next) {
            translationMap[state.originalText] ?: state.originalText
        } else {
            state.originalText
        }

        _gameState.value = state.copy(
            isPortugueseActive = next,
            dialogueText = textToDisplay
        )
        addLog("Idioma alterado para: ${if (next) "Português (TL)" else "Original (EN)"}")
    }

    suspend fun quickSave(): Boolean = withContext(Dispatchers.IO) {
        val gameDir = activeGameDir ?: return@withContext false
        val savesDir = File(gameDir, "game/saves").apply { if (!exists()) mkdirs() }
        val saveFile = File(savesDir, "quick-1.save")

        return@withContext try {
            val json = JSONObject().apply {
                put("label", _gameState.value.currentLabel)
                put("stepIndex", _gameState.value.currentStepIndex)
                put("sceneBackground", _gameState.value.sceneBackground)
                put("dialogueText", _gameState.value.dialogueText)
                put("speaker", _gameState.value.characterSpeaker)
                put("isPortugueseActive", _gameState.value.isPortugueseActive)
                put("timestamp", System.currentTimeMillis())
            }
            saveFile.writeText(json.toString())
            _gameState.value = _gameState.value.copy(saveMessage = "QuickSave salvo com sucesso!")
            addLog("QuickSave salvo em: ${saveFile.name}")
            true
        } catch (e: Exception) {
            addLog("Erro ao salvar QuickSave: ${e.message}")
            false
        }
    }

    suspend fun quickLoad(): Boolean = withContext(Dispatchers.IO) {
        val gameDir = activeGameDir ?: return@withContext false
        val saveFile = File(gameDir, "game/saves/quick-1.save")
        if (!saveFile.exists()) {
            _gameState.value = _gameState.value.copy(saveMessage = "Nenhum QuickSave encontrado.")
            return@withContext false
        }

        return@withContext try {
            val content = saveFile.readText()
            val json = JSONObject(content)
            val label = json.optString("label", "start")
            val stepIndex = json.optInt("stepIndex", 0)
            val bg = json.optString("sceneBackground", "bg_library")
            val pt = json.optBoolean("isPortugueseActive", true)

            _gameState.value = _gameState.value.copy(
                currentLabel = label,
                currentStepIndex = stepIndex,
                sceneBackground = bg,
                isPortugueseActive = pt,
                saveMessage = "QuickSave carregado com sucesso!"
            )
            executeCurrentStep()
            addLog("QuickSave carregado com sucesso.")
            true
        } catch (e: Exception) {
            addLog("Erro ao carregar QuickSave: ${e.message}")
            false
        }
    }

    fun dismissSaveMessage() {
        _gameState.value = _gameState.value.copy(saveMessage = null)
    }

    fun pauseGame() {
        _gameState.value = _gameState.value.copy(isPaused = true)
    }

    fun resumeGame() {
        _gameState.value = _gameState.value.copy(isPaused = false)
    }

    fun stopGame() {
        autoSkipJob?.cancel()
        _gameState.value = RenpyGameState(isRunning = false)
    }

    private fun startAutoSkipLoop() {
        autoSkipJob?.cancel()
        autoSkipJob = scope.launch {
            while (isActive) {
                delay(200)
                val state = _gameState.value
                if (!state.isRunning || state.isPaused || state.activeChoices.isNotEmpty()) {
                    continue
                }

                if (state.isSkipMode) {
                    delay(150)
                    nextStep()
                } else if (state.isAutoMode) {
                    delay(3000)
                    nextStep()
                }
            }
        }
    }

    private fun addLog(message: String) {
        _gameState.value = _gameState.value.copy(
            runtimeLogs = _gameState.value.runtimeLogs + message
        )
    }

    private fun loadPortugueseTranslations(gameDir: File, logs: MutableList<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val tlFolder = File(gameDir, "game/tl/portuguese").takeIf { it.exists() }
            ?: File(gameDir, "tl/portuguese").takeIf { it.exists() }

        if (tlFolder != null && tlFolder.isDirectory) {
            logs.add("Pasta de tradução oficial detectada: ${tlFolder.path}")
            tlFolder.walkTopDown().filter { it.isFile && it.extension == "rpy" }.forEach { file ->
                try {
                    var currentOld: String? = null
                    file.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.startsWith("old \"") && trimmed.endsWith("\"")) {
                            currentOld = trimmed.substring(5, trimmed.length - 1)
                        } else if (trimmed.startsWith("new \"") && trimmed.endsWith("\"") && currentOld != null) {
                            val newText = trimmed.substring(5, trimmed.length - 1)
                            map[currentOld!!] = newText
                            currentOld = null
                        } else if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && currentOld != null) {
                            map[currentOld!!] = trimmed.trim('"')
                            currentOld = null
                        }
                    }
                } catch (e: Exception) {
                    logs.add("Aviso ao ler ${file.name}: ${e.message}")
                }
            }
            logs.add("Total de ${map.size} strings traduzidas carregadas de game/tl/portuguese/.")
        } else {
            logs.add("Nenhuma tradução prévia em game/tl/portuguese/. Usando traduções padrão.")
        }
        return map
    }

    private fun parseRenpyScript(scriptFile: File, logs: MutableList<String>): Pair<List<RenpyStatement>, Map<String, Int>> {
        val statements = mutableListOf<RenpyStatement>()
        val labelMap = mutableMapOf<String, Int>()

        var currentLabel = "start"
        labelMap[currentLabel] = 0

        var pendingMenuChoices: MutableList<RenpyChoice>? = null
        var currentMenuPrompt: String? = null

        scriptFile.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachLine

            // Finish pending menu if indentation changed
            if (pendingMenuChoices != null && !rawLine.startsWith("    ") && !rawLine.startsWith("\t")) {
                statements.add(RenpyStatement.Menu(currentMenuPrompt, pendingMenuChoices!!))
                pendingMenuChoices = null
                currentMenuPrompt = null
            }

            when {
                line.startsWith("label ") -> {
                    val labelName = line.substringAfter("label ").substringBefore(":").trim()
                    currentLabel = labelName
                    labelMap[labelName] = statements.size
                }
                line.startsWith("scene ") -> {
                    val bg = line.substringAfter("scene ").substringBefore(" with").trim()
                    statements.add(RenpyStatement.Scene(bg))
                }
                line.startsWith("play music ") -> {
                    val track = line.substringAfter("play music ").trim('"', ' ', '\'')
                    statements.add(RenpyStatement.PlayMusic(track))
                }
                line.startsWith("stop music") -> {
                    statements.add(RenpyStatement.StopMusic())
                }
                line.startsWith("jump ") -> {
                    val target = line.substringAfter("jump ").trim()
                    statements.add(RenpyStatement.Jump(target))
                }
                line.startsWith("menu:") -> {
                    pendingMenuChoices = mutableListOf()
                }
                line.startsWith("\"") && line.endsWith(":") && pendingMenuChoices != null -> {
                    val choiceText = line.trim('"', ':')
                    // will expect jump on next line or store default
                    pendingMenuChoices?.add(RenpyChoice(choiceText, "choice_${pendingMenuChoices!!.size}"))
                }
                line.startsWith("\"") && line.endsWith("\"") -> {
                    // Narrator dialogue
                    statements.add(RenpyStatement.Say(null, line.trim('"')))
                }
                line.contains(" \"") && line.endsWith("\"") -> {
                    val speaker = line.substringBefore(" \"").trim()
                    val text = line.substringAfter(" \"").trimEnd('"')
                    statements.add(RenpyStatement.Say(speaker, text))
                }
                line.startsWith("$ ") -> {
                    statements.add(RenpyStatement.PythonExpr(line.substringAfter("$ ")))
                }
            }
        }

        if (pendingMenuChoices != null && pendingMenuChoices!!.isNotEmpty()) {
            statements.add(RenpyStatement.Menu(currentMenuPrompt, pendingMenuChoices!!))
        }

        return Pair(statements, labelMap)
    }

    private fun createDemoScript(): Pair<List<RenpyStatement>, Map<String, Int>> {
        val stmts = mutableListOf<RenpyStatement>()
        val labels = mutableMapOf<String, Int>()

        labels["start"] = stmts.size
        stmts.add(RenpyStatement.Scene("bg_mansion"))
        stmts.add(RenpyStatement.PlayMusic("mystery_theme.ogg"))
        stmts.add(RenpyStatement.Say("Narrador", "As portas de carvalho maciço do Solar Verdeluz fecharam-se com um estrondo atrás de você."))
        stmts.add(RenpyStatement.Say("Clara", "Bem-vindo, detetive. Finalmente você chegou. As coisas ficaram... estranhas desde o anoitecer."))
        stmts.add(RenpyStatement.Say("Investigador", "Diga-me, Clara. Onde foi que você encontrou o manuscrito encriptado?"))
        stmts.add(RenpyStatement.Say("Clara", "Na biblioteca secreta, atrás do retrato do Conde. Mas havia marcas de passos recentes no tapete."))
        stmts.add(RenpyStatement.Menu(
            prompt = "O que você deseja fazer agora?",
            choices = listOf(
                RenpyChoice("Investigar a biblioteca secreta", "label_biblioteca"),
                RenpyChoice("Interrogar Clara sobre o Conde", "label_interrogar"),
                RenpyChoice("Examinar a tranca das portas", "label_portas")
            )
        ))

        labels["label_biblioteca"] = stmts.size
        stmts.add(RenpyStatement.Scene("bg_secret_library"))
        stmts.add(RenpyStatement.Say("Investigador", "A poeira nas estantes denuncia: alguém retirou vários volumes raros em latim."))
        stmts.add(RenpyStatement.Say("Clara", "Veja aqui! Uma carta inacabada com o selo real de 1892..."))
        stmts.add(RenpyStatement.Jump("label_conclusao"))

        labels["label_interrogar"] = stmts.size
        stmts.add(RenpyStatement.Say("Investigador", "Você parecia muito próxima do Conde antes do desaparecimento dele."))
        stmts.add(RenpyStatement.Say("Clara", "Ele me confiava as chaves do cofre de arquivos, mas juro que nunca abri sem a presença dele."))
        stmts.add(RenpyStatement.Jump("label_conclusao"))

        labels["label_portas"] = stmts.size
        stmts.add(RenpyStatement.Say("Investigador", "A tranca não foi forçada por fora. Quem nos trancou aqui estava dentro da casa."))
        stmts.add(RenpyStatement.Say("Clara", "Meu Deus... isso significa que ele ainda pode estar entre nós!"))
        stmts.add(RenpyStatement.Jump("label_conclusao"))

        labels["label_conclusao"] = stmts.size
        stmts.add(RenpyStatement.Say("Narrador", "As pistas começam a se conectar. O mistério de Verdeluz continua no próximo capítulo."))

        return Pair(stmts, labels)
    }
}
