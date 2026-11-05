package com.example.openx.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.openx.data.OpenXRepository
import com.example.openx.data.SampleDataGenerator
import com.example.openx.data.db.OpenXDatabase
import com.example.openx.data.db.entities.BatchQueueItemEntity
import com.example.openx.data.db.entities.FileItemEntity
import com.example.openx.data.db.entities.GlossaryEntity
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.data.db.entities.QueueStatus
import com.example.openx.data.db.entities.TranslationStatus
import com.example.openx.data.db.entities.TranslationUnitEntity
import com.example.openx.data.db.entities.UserSessionEntity
import com.example.openx.engine.apk.ApkInspectionReport
import com.example.openx.engine.export.ExportManager
import com.example.openx.engine.export.ExportReport
import com.example.openx.engine.mlkit.LanguageModelStatus
import com.example.openx.engine.mlkit.MlKitManager
import com.example.openx.engine.translation.BatchTranslateItem
import com.example.openx.engine.translation.EngineConnectionTestResult
import com.example.openx.engine.translation.TranslationEngine
import com.example.openx.engine.translation.TranslationProgressState
import com.example.openx.engine.translation.TranslationStyle
import com.example.openx.data.db.entities.GgufModelEntity
import com.example.openx.engine.llm.DeviceMemoryStatus
import com.example.openx.engine.llm.DeviceStorageStatus
import com.example.openx.engine.llm.DownloadProgressUpdate
import com.example.openx.engine.llm.GgufModelManager
import com.example.openx.engine.llm.LlmInferenceParams
import com.example.openx.engine.llm.LocalInferenceStats
import com.example.openx.engine.llm.LocalLlmEngine
import com.example.openx.engine.llm.SystemResourceUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import com.example.openx.player.data.RenpyGameEntity
import com.example.openx.player.engine.DemoGameInstaller
import com.example.openx.player.engine.RenpyEngineDetector
import com.example.openx.player.engine.RenpyEngineManager
import com.example.openx.player.engine.RenpyRuntimeBridge
import com.example.openx.player.engine.SaveGameManager
import com.example.openx.player.engine.SaveSlotInfo
import com.example.openx.player.engine.ZipExtractionProgress
import com.example.openx.player.engine.ZipExtractor
import java.io.File
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScreenDestination(val route: String) {
    object Home : ScreenDestination("home")
    object Projects : ScreenDestination("projects")
    object Translation : ScreenDestination("translation")
    object Files : ScreenDestination("files")
    object Settings : ScreenDestination("settings")
    object AiCenter : ScreenDestination("ai_center")
    object ModelHub : ScreenDestination("model_hub")
    object PlayerLibrary : ScreenDestination("player_library")
    object GameDetail : ScreenDestination("game_detail")
    object InGamePlayer : ScreenDestination("in_game_player")
}

data class TranslationFilterState(
    val query: String = "",
    val statusFilter: TranslationStatus? = null,
    val selectedFileId: Long? = null
)

class OpenXViewModel(application: Application) : AndroidViewModel(application) {

    val db = OpenXDatabase.getInstance(application)
    val repository = OpenXRepository(db)
    val localLlmEngine = LocalLlmEngine(db.ggufModelDao())
    val translationEngine = TranslationEngine(db.translationMemoryDao(), localLlmEngine)
    val ggufModelManager = GgufModelManager(application, db.ggufModelDao())

    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ggufModels: StateFlow<List<GgufModelEntity>> = repository.allGgufModels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGgufModel: StateFlow<GgufModelEntity?> = repository.selectedGgufModel
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val downloadUpdates: StateFlow<Map<Long, DownloadProgressUpdate>> = ggufModelManager.downloadUpdates

    val localInferenceStats: StateFlow<LocalInferenceStats> = localLlmEngine.stats

    private val _inferenceParams = MutableStateFlow(LlmInferenceParams())
    val inferenceParams: StateFlow<LlmInferenceParams> = _inferenceParams.asStateFlow()

    private val _memoryStatus = MutableStateFlow(SystemResourceUtil.getMemoryInfo(application))
    val memoryStatus: StateFlow<DeviceMemoryStatus> = _memoryStatus.asStateFlow()

    private val _storageStatus = MutableStateFlow(SystemResourceUtil.getStorageInfo(application))
    val storageStatus: StateFlow<DeviceStorageStatus> = _storageStatus.asStateFlow()

    val cpuCores: Int = SystemResourceUtil.getCpuCores()

    val userSession: StateFlow<UserSessionEntity?> = repository.userSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tmCount: StateFlow<Int> = repository.tmCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val mlKitStatuses: StateFlow<List<LanguageModelStatus>> = MlKitManager.supportedLanguages

    private val _currentScreen = MutableStateFlow<ScreenDestination>(ScreenDestination.Home)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    private val _filterState = MutableStateFlow(TranslationFilterState())
    val filterState: StateFlow<TranslationFilterState> = _filterState.asStateFlow()

    private val _apkReport = MutableStateFlow<ApkInspectionReport?>(null)
    val apkReport: StateFlow<ApkInspectionReport?> = _apkReport.asStateFlow()

    private val _isApkInspecting = MutableStateFlow(false)
    val isApkInspecting: StateFlow<Boolean> = _isApkInspecting.asStateFlow()

    private val _exportReport = MutableStateFlow<ExportReport?>(null)
    val exportReport: StateFlow<ExportReport?> = _exportReport.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<EngineConnectionTestResult?>(null)
    val connectionTestResult: StateFlow<EngineConnectionTestResult?> = _connectionTestResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val translationProgress: StateFlow<TranslationProgressState> = translationEngine.progress

    // OpenX Player Engine & State
    val zipExtractor = ZipExtractor(application)
    val saveGameManager = SaveGameManager(application)
    val runtimeBridge = RenpyRuntimeBridge(application)

    val games: StateFlow<List<RenpyGameEntity>> = db.renpyGameDao().getAllGamesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGame = MutableStateFlow<RenpyGameEntity?>(null)
    val selectedGame: StateFlow<RenpyGameEntity?> = _selectedGame.asStateFlow()

    private val _gameSaves = MutableStateFlow<List<SaveSlotInfo>>(emptyList())
    val gameSaves: StateFlow<List<SaveSlotInfo>> = _gameSaves.asStateFlow()

    private val _zipExtractionProgress = MutableStateFlow<ZipExtractionProgress?>(null)
    val zipExtractionProgress: StateFlow<ZipExtractionProgress?> = _zipExtractionProgress.asStateFlow()

    private var batchTranslationJob: Job? = null

    init {
        viewModelScope.launch {
            SampleDataGenerator.populateInitialDataIfEmpty(db)
            DemoGameInstaller.installDemoGameIfMissing(application, db.renpyGameDao())
            MlKitManager.refreshModelStatuses()
            ggufModelManager.initializeCatalog()
            val firstProject = repository.allProjects.firstOrNull()?.firstOrNull()
            if (_selectedProjectId.value == null && firstProject != null) {
                _selectedProjectId.value = firstProject.id
            }
        }
    }

    fun navigateTo(screen: ScreenDestination) {
        _currentScreen.value = screen
    }

    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
        _filterState.value = _filterState.value.copy(selectedFileId = null)
    }

    fun setFilterQuery(query: String) {
        _filterState.value = _filterState.value.copy(query = query)
    }

    fun setStatusFilter(status: TranslationStatus?) {
        _filterState.value = _filterState.value.copy(statusFilter = status)
    }

    fun setSelectedFileFilter(fileId: Long?) {
        _filterState.value = _filterState.value.copy(selectedFileId = fileId)
    }

    fun updateTranslation(unitId: Long, newTranslation: String, newStatus: TranslationStatus = TranslationStatus.TRANSLATED) {
        viewModelScope.launch {
            repository.updateUnitTranslation(unitId, newTranslation, newStatus)
        }
    }

    fun updateUnitStatus(unitId: Long, newStatus: TranslationStatus) {
        viewModelScope.launch {
            repository.setUnitStatus(unitId, newStatus)
        }
    }

    fun translateSingleUnit(unit: TranslationUnitEntity) {
        viewModelScope.launch {
            try {
                _toastMessage.emit("Traduzindo segmento #${unit.orderIndex + 1}...")
                val proj = repository.getProjectById(unit.projectId)
                val targetLang = proj?.targetLang ?: "pt-BR"
                val sourceLang = proj?.sourceLang ?: "en"
                val glossary = repository.getGlossaryForProjectSync(unit.projectId)

                val req = com.example.openx.engine.translation.TranslationRequest(
                    text = unit.originalText,
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    context = com.example.openx.engine.translation.EngineContext(
                        speaker = unit.speaker,
                        surroundingContext = unit.keyOrContext,
                        glossary = glossary,
                        style = com.example.openx.engine.translation.TranslationStyle.NATURAL
                    )
                )

                val engineId = userSession.value?.selectedEngineId ?: "local_gguf"
                val engine = translationEngine.getEngine(engineId)
                val resp = engine.translate(req)

                if (resp.isSuccess && resp.translatedText.isNotBlank()) {
                    updateTranslation(unit.id, resp.translatedText, TranslationStatus.TRANSLATED)
                    _toastMessage.emit("Segmento #${unit.orderIndex + 1} traduzido com sucesso!")
                } else {
                    _toastMessage.emit("Falha ao traduzir: ${resp.errorMessage ?: "Erro no motor local"}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao traduzir: ${e.message}")
            }
        }
    }

    fun createDemoProject() {
        viewModelScope.launch {
            val id = SampleDataGenerator.createRenpyDemoProject(db)
            _selectedProjectId.value = id
            _toastMessage.emit("Projeto de demonstração Ren'Py criado com sucesso!")
        }
    }

    fun importTextFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                val projId = repository.importSingleTextFile(
                    context = getApplication(),
                    uri = uri,
                    fileName = fileName,
                    targetProjectId = _selectedProjectId.value
                )
                _selectedProjectId.value = projId
                _toastMessage.emit("Arquivo '$fileName' importado com sucesso!")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao importar arquivo: ${e.message}")
            }
        }
    }

    fun inspectApk(uri: Uri, apkName: String) {
        viewModelScope.launch {
            _isApkInspecting.value = true
            try {
                val report = repository.inspectApk(getApplication(), uri)
                _apkReport.value = report
                _toastMessage.emit("Análise do APK '$apkName' concluída!")
            } catch (e: Exception) {
                _toastMessage.emit("Falha ao analisar APK: ${e.message}")
            } finally {
                _isApkInspecting.value = false
            }
        }
    }

    fun importInspectedApk(uri: Uri, apkName: String) {
        val report = _apkReport.value ?: return
        viewModelScope.launch {
            try {
                val projId = repository.importApkAsProject(getApplication(), uri, apkName, report)
                _selectedProjectId.value = projId
                _currentScreen.value = ScreenDestination.Translation
                _toastMessage.emit("Projeto criado a partir do APK com ${report.readableEntries.size} arquivos legíveis!")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao importar APK como projeto: ${e.message}")
            }
        }
    }

    fun startBatchTranslation(style: TranslationStyle = TranslationStyle.NATURAL) {
        val projId = _selectedProjectId.value ?: return
        batchTranslationJob?.cancel()

        batchTranslationJob = viewModelScope.launch {
            val project = repository.getProjectById(projId) ?: return@launch
            val allUnits = repository.getUnitsForProjectSync(projId)
            val pendingUnits = allUnits.filter { it.status == TranslationStatus.PENDING || it.translatedText.isBlank() }

            if (pendingUnits.isEmpty()) {
                _toastMessage.emit("Todos os textos já estão traduzidos!")
                return@launch
            }

            val glossary = repository.getGlossaryForProjectSync(projId)
            val session = repository.userSession.firstOrNull() ?: UserSessionEntity()
            val engineId = session.selectedEngineId
            val apiKey = when (engineId) {
                "firebase_ai" -> session.geminiApiKey
                "groq" -> session.groqApiKey
                "openrouter" -> session.openRouterApiKey
                else -> ""
            }
            val modelName = when (engineId) {
                "firebase_ai" -> session.geminiModel
                "groq" -> session.groqModel
                "openrouter" -> session.openRouterModel
                else -> ""
            }

            // Populate persistent queue in Room for checkpoint safety
            val queueItems = pendingUnits.mapIndexed { idx, unit ->
                BatchQueueItemEntity(
                    projectId = projId,
                    unitId = unit.id,
                    orderIndex = idx,
                    originalText = unit.originalText,
                    speaker = unit.speaker,
                    contextInfo = unit.keyOrContext,
                    status = QueueStatus.QUEUED
                )
            }
            db.batchQueueDao().clearQueueForProject(projId)
            db.batchQueueDao().insertItems(queueItems)

            val batchItems = pendingUnits.map { unit ->
                BatchTranslateItem(
                    id = unit.id,
                    speaker = unit.speaker,
                    originalText = unit.originalText,
                    contextInfo = unit.keyOrContext
                )
            }

            translationEngine.translateBatch(
                items = batchItems,
                sourceLang = project.sourceLang,
                targetLang = project.targetLang,
                style = style,
                glossary = glossary,
                selectedEngineId = engineId,
                modelName = modelName,
                apiKey = apiKey,
                allowOnlineFallback = session.allowOnlineFallback
            ) { result ->
                if (result.isSuccess && result.translatedText.isNotBlank()) {
                    repository.updateUnitTranslation(
                        unitId = result.id,
                        newTranslation = result.translatedText,
                        newStatus = TranslationStatus.TRANSLATED
                    )
                    // Update persistent queue checkpoint in Room
                    db.batchQueueDao().markCompleted(
                        id = result.id,
                        status = QueueStatus.COMPLETED,
                        translated = result.translatedText,
                        fromTm = result.reusedFromMemory,
                        engine = result.engineUsed
                    )
                }
            }
        }
    }

    fun pauseBatchTranslation() {
        translationEngine.pause()
        val projId = _selectedProjectId.value
        if (projId != null) {
            viewModelScope.launch {
                db.batchQueueDao().updateAllPendingStatus(projId, QueueStatus.PAUSED)
            }
        }
    }

    fun resumeBatchTranslation() {
        translationEngine.resume()
        val projId = _selectedProjectId.value
        if (projId != null) {
            viewModelScope.launch {
                db.batchQueueDao().updateAllPendingStatus(projId, QueueStatus.QUEUED)
            }
        }
    }

    fun cancelBatchTranslation() {
        translationEngine.cancel()
        batchTranslationJob?.cancel()
        val projId = _selectedProjectId.value
        if (projId != null) {
            viewModelScope.launch {
                db.batchQueueDao().clearQueueForProject(projId)
            }
        }
    }

    fun testEngineConnection(engineId: String, apiKey: String, modelName: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            try {
                val result = repository.testEngineConnection(engineId, apiKey, modelName)
                _connectionTestResult.value = result
                _toastMessage.emit(result.message)
            } catch (e: Exception) {
                _connectionTestResult.value = EngineConnectionTestResult(
                    isSuccess = false,
                    message = "Erro no teste: ${e.message}"
                )
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    fun selectEngine(engineId: String, engineName: String) {
        viewModelScope.launch {
            val session = userSession.value ?: UserSessionEntity()
            val updated = session.copy(
                selectedEngineId = engineId,
                selectedModelName = engineName
            )
            repository.saveSession(updated)
            _toastMessage.emit("Motor selecionado: $engineName")
        }
    }

    fun downloadMlKitModel(langCode: String) {
        viewModelScope.launch {
            _toastMessage.emit("Iniciando download do pacote '$langCode'...")
            val success = MlKitManager.downloadLanguageModel(langCode)
            if (success) {
                _toastMessage.emit("Pacote '$langCode' baixado com sucesso!")
            } else {
                _toastMessage.emit("Falha ao baixar pacote '$langCode'.")
            }
        }
    }

    fun deleteMlKitModel(langCode: String) {
        viewModelScope.launch {
            val success = MlKitManager.deleteLanguageModel(langCode)
            if (success) {
                _toastMessage.emit("Pacote '$langCode' excluído do aparelho.")
            }
        }
    }

    fun saveApiKeys(gemini: String, groq: String, openRouter: String) {
        viewModelScope.launch {
            val session = userSession.value ?: UserSessionEntity()
            val updated = session.copy(
                geminiApiKey = gemini.trim(),
                groqApiKey = groq.trim(),
                openRouterApiKey = openRouter.trim()
            )
            repository.saveSession(updated)
            _toastMessage.emit("Chaves salvas localmente no aparelho com segurança.")
        }
    }

    fun clearTranslationMemory() {
        viewModelScope.launch {
            db.translationMemoryDao().clearAll()
            _toastMessage.emit("Memória de tradução limpa com sucesso.")
        }
    }

    fun prepareExportReport() {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val project = repository.getProjectById(projId) ?: return@launch
            val files = repository.getFilesForProjectSync(projId)
            val units = repository.getUnitsForProjectSync(projId)
            val report = ExportManager.generateReport(project, files, units)
            _exportReport.value = report
        }
    }

    fun clearExportReport() {
        _exportReport.value = null
    }

    fun exportProjectZip(targetUri: Uri) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            try {
                val project = repository.getProjectById(projId) ?: return@launch
                val files = repository.getFilesForProjectSync(projId)
                val filesWithUnits = files.map { file ->
                    Pair(file, repository.getUnitsForFileSync(file.id))
                }
                val zipData = ExportManager.createExportZip(project, filesWithUnits)
                val success = ExportManager.writeToUri(getApplication(), targetUri, zipData)
                if (success) {
                    _toastMessage.emit("Pacote ZIP exportado com sucesso via SAF!")
                } else {
                    _toastMessage.emit("Falha ao gravar arquivo no destino selecionado.")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro na exportação: ${e.message}")
            }
        }
    }

    fun exportSingleFile(fileId: Long, targetUri: Uri) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            try {
                val project = repository.getProjectById(projId) ?: return@launch
                val file = repository.getFileById(fileId) ?: return@launch
                val units = repository.getUnitsForFileSync(fileId)
                val content = ExportManager.exportSingleFileContent(file, units, project.targetLang)
                val success = ExportManager.writeTextToUri(getApplication(), targetUri, content)
                if (success) {
                    _toastMessage.emit("Arquivo '${file.fileName}' exportado com sucesso!")
                } else {
                    _toastMessage.emit("Falha ao gravar o arquivo exportado.")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao exportar arquivo: ${e.message}")
            }
        }
    }

    fun replaceAll(findText: String, replaceWith: String, matchCase: Boolean) {
        val projId = _selectedProjectId.value ?: return
        if (findText.isBlank()) return

        viewModelScope.launch {
            val units = repository.getUnitsForProjectSync(projId)
            var count = 0
            for (unit in units) {
                if (unit.translatedText.isNotBlank()) {
                    val contains = if (matchCase) unit.translatedText.contains(findText)
                    else unit.translatedText.contains(findText, ignoreCase = true)

                    if (contains) {
                        val newText = if (matchCase) {
                            unit.translatedText.replace(findText, replaceWith)
                        } else {
                            unit.translatedText.replace(Regex(Regex.escape(findText), RegexOption.IGNORE_CASE), replaceWith)
                        }
                        repository.updateUnitTranslation(unit.id, newText, unit.status)
                        count++
                    }
                }
            }
            _toastMessage.emit("Substituição concluída em $count texto(s).")
        }
    }

    fun addGlossaryItem(
        source: String,
        target: String,
        doNotTranslate: Boolean,
        category: String,
        characterRole: String = "",
        gender: String = "Não especificado",
        relationship: String = "",
        personality: String = "",
        speechStyle: String = "",
        notes: String = ""
    ) {
        val projId = _selectedProjectId.value ?: 0
        viewModelScope.launch {
            repository.addGlossaryItem(
                GlossaryEntity(
                    projectId = projId,
                    sourceTerm = source.trim(),
                    targetTerm = target.trim(),
                    doNotTranslate = doNotTranslate,
                    category = category,
                    characterRole = characterRole,
                    gender = gender,
                    relationship = relationship,
                    personality = personality,
                    speechStyle = speechStyle,
                    notes = notes
                )
            )
            _toastMessage.emit("Termo/Personagem adicionado ao glossário!")
        }
    }

    fun deleteGlossaryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteGlossaryItem(id)
            _toastMessage.emit("Termo removido do glossário.")
        }
    }

    fun updatePinSecurity(pin: String, enabled: Boolean) {
        viewModelScope.launch {
            val current = userSession.value ?: UserSessionEntity()
            val updated = current.copy(
                pinCodeHash = pin,
                requirePinOnLaunch = enabled
            )
            repository.saveSession(updated)
            _toastMessage.emit(if (enabled) "Bloqueio por PIN ativado." else "Bloqueio por PIN desativado.")
        }
    }

    fun toggleOfflineMode(isOffline: Boolean) {
        viewModelScope.launch {
            repository.setOfflineMode(isOffline)
            _toastMessage.emit(if (isOffline) "Modo 100% Offline ativado." else "Modo Online ativado.")
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_selectedProjectId.value == projectId) {
                val next = repository.allProjects.firstOrNull()?.firstOrNull()
                _selectedProjectId.value = next?.id
            }
            _toastMessage.emit("Projeto excluído com sucesso.")
        }
    }

    fun downloadGgufModel(modelId: Long) {
        viewModelScope.launch {
            try {
                _toastMessage.emit("Iniciando download do modelo GGUF...")
                ggufModelManager.downloadModel(modelId)
            } catch (e: Exception) {
                _toastMessage.emit("Erro no download: ${e.message}")
            }
        }
    }

    fun pauseGgufDownload(modelId: Long) {
        try {
            ggufModelManager.pauseDownload(modelId)
        } catch (_: Exception) {
        }
    }

    fun selectGgufModel(modelId: Long) {
        viewModelScope.launch {
            try {
                ggufModelManager.selectModel(modelId)
                val model = db.ggufModelDao().getModelById(modelId)
                if (model != null) {
                    val current = userSession.value ?: UserSessionEntity()
                    repository.saveSession(
                        current.copy(
                            selectedEngineId = "local_gguf",
                            selectedModelName = model.name
                        )
                    )
                    _toastMessage.emit("Modelo ${model.name} ativado como motor padrão!")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao ativar modelo: ${e.message}")
            }
        }
    }

    fun deleteGgufModel(modelId: Long) {
        viewModelScope.launch {
            try {
                ggufModelManager.deleteModel(modelId)
                _toastMessage.emit("Modelo excluído com sucesso.")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao excluir modelo: ${e.message}")
            }
        }
    }

    fun verifyGgufSha256(modelId: Long) {
        viewModelScope.launch {
            try {
                _toastMessage.emit("Calculando hash SHA-256 do arquivo GGUF...")
                val hash = ggufModelManager.verifyModelSha256(modelId)
                _toastMessage.emit("Hash verificado: ${hash.take(16)}...")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao verificar SHA-256: ${e.message}")
            }
        }
    }

    fun importGgufModelFromUri(uri: Uri, displayName: String) {
        viewModelScope.launch {
            try {
                _toastMessage.emit("Importando arquivo GGUF via SAF...")
                val result = ggufModelManager.importModelFromUri(uri, displayName)
                if (result.isSuccess) {
                    val model = result.getOrNull()
                    _toastMessage.emit("Modelo GGUF importado com sucesso!")
                    if (model != null) {
                        selectGgufModel(model.id)
                    }
                } else {
                    _toastMessage.emit("Erro na importação: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro na importação: ${e.message}")
            }
        }
    }

    fun updateInferenceParams(params: LlmInferenceParams) {
        _inferenceParams.value = params
        localLlmEngine.inferenceParams = params
        viewModelScope.launch {
            _toastMessage.emit("Parâmetros do motor local atualizados.")
        }
    }

    fun refreshSystemResources() {
        try {
            _memoryStatus.value = SystemResourceUtil.getMemoryInfo(getApplication())
            _storageStatus.value = SystemResourceUtil.getStorageInfo(getApplication())
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    // --- OpenX Player Actions ---

    fun selectGame(game: RenpyGameEntity) {
        _selectedGame.value = game
        refreshGameSaves(game)
        _currentScreen.value = ScreenDestination.GameDetail
    }

    fun refreshGameSaves(game: RenpyGameEntity) {
        viewModelScope.launch {
            try {
                val slots = saveGameManager.listSaves(File(game.gameDir))
                _gameSaves.value = slots
            } catch (_: Exception) {}
        }
    }

    fun importGameFromZip(uri: Uri) {
        viewModelScope.launch {
            try {
                _toastMessage.emit("Iniciando extração segura do jogo ZIP...")
                val gamesRoot = File(getApplication<Application>().filesDir, "renpy_games")
                val tempTargetName = "game_" + System.currentTimeMillis()
                val targetDir = File(gamesRoot, tempTargetName)

                val result = zipExtractor.extractZip(uri, targetDir) { progress ->
                    _zipExtractionProgress.value = progress
                }

                if (result.isSuccess) {
                    val extractedDir = result.getOrThrow()
                    val inspection = RenpyEngineDetector.inspectDirectory(extractedDir)

                    val entity = RenpyGameEntity(
                        title = inspection.title,
                        version = inspection.version,
                        gameDir = inspection.gameDirectory.absolutePath,
                        coverPath = inspection.coverFile?.absolutePath,
                        estimatedEngineVersion = inspection.estimatedEngineVersion,
                        confidenceScore = inspection.confidenceScore,
                        selectedEngineVersion = RenpyEngineManager.getRecommendedEngine(inspection.estimatedEngineVersion).name,
                        totalSizeBytes = inspection.totalSizeBytes,
                        availableLanguages = inspection.detectedLanguages.joinToString(", "),
                        compatibilityStatus = inspection.compatibilityStatus,
                        compatibilityDetails = inspection.diagnosticSummary,
                        saveCount = 0,
                        hasPortugueseTl = inspection.detectedLanguages.any { it.contains("portuguese", ignoreCase = true) || it.contains("pt", ignoreCase = true) }
                    )

                    val id = db.renpyGameDao().insertGame(entity)
                    val savedGame = entity.copy(id = id)
                    _selectedGame.value = savedGame
                    _toastMessage.emit("Jogo '${inspection.title}' importado com sucesso!")
                    _currentScreen.value = ScreenDestination.GameDetail
                } else {
                    _toastMessage.emit("Falha na importação: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao importar jogo: ${e.message}")
            } finally {
                _zipExtractionProgress.value = null
            }
        }
    }

    fun importGameFromFolder(treeUri: Uri) {
        viewModelScope.launch {
            try {
                _toastMessage.emit("Acessando pasta via SAF...")
                val folderName = treeUri.lastPathSegment?.substringAfterLast(":") ?: "Jogo_Renpy"
                val gamesRoot = File(getApplication<Application>().filesDir, "renpy_games")
                val localGameDir = File(gamesRoot, folderName.replace(Regex("[^a-zA-Z0-9_]"), "_"))
                if (!localGameDir.exists()) localGameDir.mkdirs()

                val inspection = RenpyEngineDetector.inspectDirectory(localGameDir)
                val entity = RenpyGameEntity(
                    title = folderName,
                    version = "1.0",
                    gameDir = localGameDir.absolutePath,
                    estimatedEngineVersion = "Ren'Py 8.x (SAF)",
                    confidenceScore = 85,
                    selectedEngineVersion = "Ren'Py 8.x",
                    totalSizeBytes = localGameDir.length(),
                    availableLanguages = "Inglês, Português",
                    compatibilityStatus = "COMPATIBLE",
                    compatibilityDetails = "Pasta autorizada pelo usuário via Storage Access Framework."
                )
                val id = db.renpyGameDao().insertGame(entity)
                _selectedGame.value = entity.copy(id = id)
                _toastMessage.emit("Pasta de jogo registrada com sucesso!")
                _currentScreen.value = ScreenDestination.GameDetail
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao importar pasta: ${e.message}")
            }
        }
    }

    fun playGame(game: RenpyGameEntity) {
        viewModelScope.launch {
            try {
                _selectedGame.value = game
                db.renpyGameDao().updateGame(game.copy(lastPlayedTimestamp = System.currentTimeMillis()))
                runtimeBridge.loadAndStartGame(File(game.gameDir), game.title, game.hasPortugueseTl)
                _currentScreen.value = ScreenDestination.InGamePlayer
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao iniciar jogo: ${e.message}")
            }
        }
    }

    fun translateGameWithAi(game: RenpyGameEntity) {
        viewModelScope.launch {
            try {
                _toastMessage.emit("Escaneando scripts de '${game.title}' para tradução...")
                val gameDir = File(game.gameDir)
                val gameSubdir = File(gameDir, "game").takeIf { it.exists() } ?: gameDir
                val rpyFiles = gameSubdir.walkTopDown().filter { it.isFile && it.extension == "rpy" && !it.path.contains("tl/") }.toList()

                if (rpyFiles.isEmpty()) {
                    _toastMessage.emit("Nenhum script .rpy simples encontrado. Abrindo demonstração.")
                    loadDemoGame()
                    return@launch
                }

                // Create or find project
                val projectName = "TL: ${game.title}"
                val existing = repository.allProjects.firstOrNull()?.find { it.name == projectName }
                val projectId = existing?.id ?: repository.createNewProject(
                    name = projectName,
                    type = com.example.openx.data.db.entities.ProjectType.RENPY,
                    sourceLang = "en",
                    targetLang = "pt-BR"
                )

                var totalImported = 0
                for (script in rpyFiles) {
                    val content = script.readText()
                    val parseResult = com.example.openx.engine.renpy.RenpyParser.parse(content)
                    val parsedItems = parseResult.items
                    if (parsedItems.isNotEmpty()) {
                        val fileId = db.translationDao().insertFile(
                            com.example.openx.data.db.entities.FileItemEntity(
                                projectId = projectId,
                                fileName = script.name,
                                relativePath = script.relativeTo(gameDir).path,
                                fileType = com.example.openx.data.db.entities.FileType.RPY,
                                totalUnits = parsedItems.size
                            )
                        )
                        val entities = parsedItems.map { u ->
                            com.example.openx.data.db.entities.TranslationUnitEntity(
                                projectId = projectId,
                                fileId = fileId,
                                orderIndex = u.orderIndex,
                                lineIndex = u.lineIndex,
                                speaker = u.speaker,
                                keyOrContext = u.keyOrContext,
                                originalText = u.originalText,
                                translatedText = u.translatedText,
                                status = u.status,
                                blockType = u.blockType
                            )
                        }
                        db.translationDao().insertUnits(entities)
                        totalImported += entities.size
                    }
                }

                selectProject(projectId)
                db.renpyGameDao().updateGame(game.copy(associatedProjectId = projectId, hasPortugueseTl = true))
                _toastMessage.emit("$totalImported linhas importadas no projeto de tradução!")
                _currentScreen.value = ScreenDestination.Translation
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao preparar tradução do jogo: ${e.message}")
            }
        }
    }

    fun backupGameSaves(game: RenpyGameEntity) {
        viewModelScope.launch {
            val res = saveGameManager.backupSavesToZip(File(game.gameDir), game.title)
            _toastMessage.emit(res.message)
            refreshGameSaves(game)
        }
    }

    fun restoreGameSaves(game: RenpyGameEntity) {
        viewModelScope.launch {
            val backupsDir = File(getApplication<Application>().filesDir, "backups/saves")
            val latestBackup = backupsDir.listFiles()?.filter { it.extension == "zip" }?.maxByOrNull { it.lastModified() }
            if (latestBackup != null) {
                val ok = saveGameManager.restoreSavesFromZip(latestBackup, File(game.gameDir))
                _toastMessage.emit(if (ok) "Saves restaurados com sucesso!" else "Falha ao restaurar saves.")
                refreshGameSaves(game)
            } else {
                _toastMessage.emit("Nenhum arquivo de backup encontrado.")
            }
        }
    }

    fun deleteGameSave(slot: SaveSlotInfo) {
        viewModelScope.launch {
            val ok = saveGameManager.deleteSave(slot.file)
            _toastMessage.emit(if (ok) "Save removido com sucesso." else "Erro ao remover save.")
            _selectedGame.value?.let { refreshGameSaves(it) }
        }
    }

    fun deleteGame(game: RenpyGameEntity) {
        viewModelScope.launch {
            try {
                db.renpyGameDao().deleteGame(game)
                _toastMessage.emit("Jogo '${game.title}' removido da biblioteca.")
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao remover: ${e.message}")
            }
        }
    }

    fun selectEngine(game: RenpyGameEntity, engineName: String) {
        viewModelScope.launch {
            val updated = game.copy(selectedEngineVersion = engineName)
            db.renpyGameDao().updateGame(updated)
            _selectedGame.value = updated
            _toastMessage.emit("Motor alterado para: $engineName")
        }
    }

    fun loadDemoGame() {
        viewModelScope.launch {
            try {
                DemoGameInstaller.installDemoGameIfMissing(getApplication(), db.renpyGameDao())
                val demo = db.renpyGameDao().getBuiltInDemoGame()
                if (demo != null) {
                    selectGame(demo)
                }
            } catch (e: Exception) {
                _toastMessage.emit("Erro ao carregar demonstração: ${e.message}")
            }
        }
    }
}
