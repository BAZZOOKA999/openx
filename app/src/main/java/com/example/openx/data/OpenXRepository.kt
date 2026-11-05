package com.example.openx.data

import android.content.Context
import android.net.Uri
import com.example.openx.data.db.OpenXDatabase
import com.example.openx.data.db.entities.BatchQueueItemEntity
import com.example.openx.data.db.entities.BlockType
import com.example.openx.data.db.entities.FileItemEntity
import com.example.openx.data.db.entities.FileType
import com.example.openx.data.db.entities.GlossaryEntity
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.data.db.entities.ProjectType
import com.example.openx.data.db.entities.QueueStatus
import com.example.openx.data.db.entities.TranslationMemoryEntity
import com.example.openx.data.db.entities.TranslationStatus
import com.example.openx.data.db.entities.TranslationUnitEntity
import com.example.openx.data.db.entities.UserSessionEntity
import com.example.openx.engine.androidres.AndroidResParser
import com.example.openx.engine.apk.ApkInspectionReport
import com.example.openx.engine.apk.ApkInspector
import com.example.openx.engine.mlkit.MlKitManager
import com.example.openx.engine.renpy.RenpyParser
import com.example.openx.engine.renpy.RenpyValidator
import com.example.openx.engine.translation.EngineConnectionTestResult
import com.example.openx.engine.translation.TranslationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class OpenXRepository(private val db: OpenXDatabase) {

    val allProjects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()
    val userSession: Flow<UserSessionEntity?> = db.userSessionDao().getSessionFlow()
    val tmCount: Flow<Int> = db.translationMemoryDao().getTotalCountFlow()
    val allGgufModels: Flow<List<com.example.openx.data.db.entities.GgufModelEntity>> = db.ggufModelDao().getAllModelsFlow()
    val selectedGgufModel: Flow<com.example.openx.data.db.entities.GgufModelEntity?> = db.ggufModelDao().getSelectedModelFlow()

    fun getProjectByIdFlow(id: Long): Flow<ProjectEntity?> = db.projectDao().getProjectByIdFlow(id)
    suspend fun getProjectById(id: Long): ProjectEntity? = db.projectDao().getProjectById(id)

    fun getFilesForProject(projectId: Long): Flow<List<FileItemEntity>> = db.translationDao().getFilesForProject(projectId)
    suspend fun getFilesForProjectSync(projectId: Long): List<FileItemEntity> = db.translationDao().getFilesForProjectSync(projectId)
    suspend fun getFileById(fileId: Long): FileItemEntity? = db.translationDao().getFileById(fileId)

    fun getUnitsForProject(projectId: Long): Flow<List<TranslationUnitEntity>> = db.translationDao().getAllUnitsForProject(projectId)
    suspend fun getUnitsForProjectSync(projectId: Long): List<TranslationUnitEntity> = db.translationDao().getAllUnitsForProjectSync(projectId)

    fun getUnitsForFile(fileId: Long): Flow<List<TranslationUnitEntity>> = db.translationDao().getUnitsForFile(fileId)
    suspend fun getUnitsForFileSync(fileId: Long): List<TranslationUnitEntity> = db.translationDao().getUnitsForFileSync(fileId)

    fun getGlossaryForProject(projectId: Long): Flow<List<GlossaryEntity>> = db.glossaryDao().getGlossaryForProject(projectId)
    suspend fun getGlossaryForProjectSync(projectId: Long): List<GlossaryEntity> = db.glossaryDao().getGlossaryForProjectSync(projectId)

    fun getQueueForProject(projectId: Long): Flow<List<BatchQueueItemEntity>> = db.batchQueueDao().getQueueForProjectFlow(projectId)

    suspend fun createNewProject(
        name: String,
        type: ProjectType,
        sourceLang: String = "en",
        targetLang: String = "pt-BR",
        sourceUri: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val proj = ProjectEntity(
            name = name,
            type = type,
            sourceLang = sourceLang,
            targetLang = targetLang,
            sourceUri = sourceUri
        )
        db.projectDao().insertProject(proj)
    }

    suspend fun deleteProject(projectId: Long) = withContext(Dispatchers.IO) {
        db.projectDao().deleteProjectById(projectId)
        db.batchQueueDao().clearQueueForProject(projectId)
    }

    suspend fun updateUnitTranslation(
        unitId: Long,
        newTranslation: String,
        newStatus: TranslationStatus = TranslationStatus.TRANSLATED
    ) = withContext(Dispatchers.IO) {
        val unit = db.translationDao().getUnitById(unitId) ?: return@withContext
        val validation = RenpyValidator.validateTranslation(unit.originalText, newTranslation)
        val status = if (newTranslation.isBlank()) TranslationStatus.PENDING else newStatus

        db.translationDao().updateTranslation(
            unitId = unitId,
            newTranslation = newTranslation,
            newStatus = status,
            timestamp = System.currentTimeMillis()
        )
        db.translationDao().updateValidationError(unitId, validation.errorMessage)
        
        // If user marked as REVIEWED, save to Translation Memory
        if (status == TranslationStatus.REVIEWED && newTranslation.isNotBlank()) {
            val project = db.projectDao().getProjectById(unit.projectId)
            val srcLang = project?.sourceLang ?: "en"
            val tgtLang = project?.targetLang ?: "pt-BR"
            val hash = computeHash(unit.originalText.trim(), srcLang, tgtLang)
            db.translationMemoryDao().insertOrUpdate(
                TranslationMemoryEntity(
                    textHash = hash,
                    sourceText = unit.originalText.trim(),
                    translatedText = newTranslation.trim(),
                    sourceLang = srcLang,
                    targetLang = tgtLang,
                    engineId = "user_reviewed",
                    isReviewed = true
                )
            )
        }

        recalculateProjectStats(unit.projectId)
    }

    suspend fun setUnitStatus(unitId: Long, newStatus: TranslationStatus) = withContext(Dispatchers.IO) {
        val unit = db.translationDao().getUnitById(unitId) ?: return@withContext
        db.translationDao().updateStatus(unitId, newStatus)

        if (newStatus == TranslationStatus.REVIEWED && unit.translatedText.isNotBlank()) {
            val project = db.projectDao().getProjectById(unit.projectId)
            val srcLang = project?.sourceLang ?: "en"
            val tgtLang = project?.targetLang ?: "pt-BR"
            val hash = computeHash(unit.originalText.trim(), srcLang, tgtLang)
            db.translationMemoryDao().insertOrUpdate(
                TranslationMemoryEntity(
                    textHash = hash,
                    sourceText = unit.originalText.trim(),
                    translatedText = unit.translatedText.trim(),
                    sourceLang = srcLang,
                    targetLang = tgtLang,
                    engineId = "user_reviewed",
                    isReviewed = true
                )
            )
        }

        recalculateProjectStats(unit.projectId)
    }

    suspend fun recalculateProjectStats(projectId: Long) = withContext(Dispatchers.IO) {
        val total = db.translationDao().getTotalUnitsCount(projectId)
        val translated = db.translationDao().getUnitsCountByStatus(projectId, TranslationStatus.TRANSLATED)
        val reviewed = db.translationDao().getUnitsCountByStatus(projectId, TranslationStatus.REVIEWED)
        val errors = db.translationDao().getUnitsCountByStatus(projectId, TranslationStatus.ERROR)

        val project = db.projectDao().getProjectById(projectId) ?: return@withContext
        val updated = project.copy(
            totalUnits = total,
            translatedUnits = translated + reviewed,
            reviewedUnits = reviewed,
            errorUnits = errors,
            lastModified = System.currentTimeMillis()
        )
        db.projectDao().updateProject(updated)
    }

    suspend fun importSingleTextFile(
        context: Context,
        uri: Uri,
        fileName: String,
        targetProjectId: Long? = null
    ): Long = withContext(Dispatchers.IO) {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
        val fileType = when {
            fileName.endsWith(".rpy", ignoreCase = true) -> FileType.RPY
            fileName.endsWith(".xml", ignoreCase = true) -> FileType.XML
            fileName.endsWith(".json", ignoreCase = true) -> FileType.JSON
            fileName.endsWith(".csv", ignoreCase = true) -> FileType.CSV
            else -> FileType.TXT
        }

        val projId = targetProjectId ?: run {
            val projType = if (fileType == FileType.RPY) ProjectType.RENPY else ProjectType.ANDROID_RES
            val cleanName = fileName.substringBeforeLast(".")
            createNewProject("Projeto $cleanName", projType, sourceUri = uri.toString())
        }

        val fileItem = FileItemEntity(
            projectId = projId,
            relativePath = fileName,
            fileName = fileName,
            fileType = fileType,
            fileSize = content.length.toLong(),
            rawContent = content
        )
        val fileId = db.translationDao().insertFile(fileItem)

        when (fileType) {
            FileType.RPY -> {
                val parsed = RenpyParser.parse(content)
                val units = RenpyParser.toTranslationUnitEntities(parsed.items, projId, fileId)
                db.translationDao().insertUnits(units)
            }
            FileType.XML -> {
                val items = AndroidResParser.parseStringsXml(content)
                val units = items.map { item ->
                    TranslationUnitEntity(
                        projectId = projId,
                        fileId = fileId,
                        orderIndex = item.orderIndex,
                        keyOrContext = item.key,
                        originalText = item.originalText,
                        translatedText = item.translatedText,
                        blockType = item.blockType
                    )
                }
                db.translationDao().insertUnits(units)
            }
            FileType.JSON -> {
                val items = AndroidResParser.parseJson(content)
                val units = items.map { item ->
                    TranslationUnitEntity(
                        projectId = projId,
                        fileId = fileId,
                        orderIndex = item.orderIndex,
                        keyOrContext = item.key,
                        originalText = item.originalText,
                        translatedText = item.translatedText,
                        blockType = item.blockType
                    )
                }
                db.translationDao().insertUnits(units)
            }
            FileType.CSV, FileType.TXT -> {
                val items = AndroidResParser.parseCsv(content)
                val units = items.map { item ->
                    TranslationUnitEntity(
                        projectId = projId,
                        fileId = fileId,
                        orderIndex = item.orderIndex,
                        lineIndex = item.lineIndex,
                        keyOrContext = item.key,
                        originalText = item.originalText,
                        translatedText = item.translatedText,
                        blockType = item.blockType
                    )
                }
                db.translationDao().insertUnits(units)
            }
            else -> {}
        }

        recalculateProjectStats(projId)
        projId
    }

    suspend fun inspectApk(context: Context, uri: Uri): ApkInspectionReport = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Não foi possível abrir o APK")
        stream.use { ApkInspector.inspectApkStream(it) }
    }

    suspend fun importApkAsProject(
        context: Context,
        uri: Uri,
        apkName: String,
        report: ApkInspectionReport
    ): Long = withContext(Dispatchers.IO) {
        val projId = createNewProject(
            name = "APK: $apkName",
            type = if (report.renpyEntries.isNotEmpty()) ProjectType.RENPY else ProjectType.APK_INSPECTION,
            sourceUri = uri.toString()
        )

        for (item in report.readableEntries.take(15)) {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                ApkInspector.extractTextFileContent(stream, item.path)
            } ?: ""

            if (content.isNotBlank()) {
                val fileType = when {
                    item.path.endsWith(".rpy") -> FileType.RPY
                    item.path.endsWith(".json") -> FileType.JSON
                    item.path.endsWith(".xml") -> FileType.XML
                    else -> FileType.TXT
                }
                val fileEntity = FileItemEntity(
                    projectId = projId,
                    relativePath = item.path,
                    fileName = item.path.substringAfterLast("/"),
                    fileType = fileType,
                    fileSize = item.size,
                    rawContent = content
                )
                val fileId = db.translationDao().insertFile(fileEntity)

                if (fileType == FileType.RPY) {
                    val parsed = RenpyParser.parse(content)
                    val units = RenpyParser.toTranslationUnitEntities(parsed.items, projId, fileId)
                    db.translationDao().insertUnits(units)
                } else if (fileType == FileType.JSON) {
                    val items = AndroidResParser.parseJson(content)
                    val units = items.map { res ->
                        TranslationUnitEntity(
                            projectId = projId,
                            fileId = fileId,
                            orderIndex = res.orderIndex,
                            keyOrContext = res.key,
                            originalText = res.originalText,
                            blockType = res.blockType
                        )
                    }
                    db.translationDao().insertUnits(units)
                }
            }
        }

        for (bin in report.binaryEntries) {
            val fileType = when {
                bin.path.endsWith(".rpa") -> FileType.RPA
                bin.path.endsWith(".rpyc") -> FileType.RPYC
                bin.path == "resources.arsc" -> FileType.BINARY
                else -> FileType.BINARY
            }
            val binEntity = FileItemEntity(
                projectId = projId,
                relativePath = bin.path,
                fileName = bin.path.substringAfterLast("/"),
                fileType = fileType,
                fileSize = bin.size,
                rawContent = "",
                isBinary = true,
                notes = bin.description
            )
            db.translationDao().insertFile(binEntity)
        }

        recalculateProjectStats(projId)
        projId
    }

    suspend fun addGlossaryItem(item: GlossaryEntity) = withContext(Dispatchers.IO) {
        db.glossaryDao().insertItem(item)
    }

    suspend fun deleteGlossaryItem(id: Long) = withContext(Dispatchers.IO) {
        db.glossaryDao().deleteItemById(id)
    }

    suspend fun saveSession(session: UserSessionEntity) = withContext(Dispatchers.IO) {
        db.userSessionDao().saveSession(session)
    }

    suspend fun setOfflineMode(isOffline: Boolean) = withContext(Dispatchers.IO) {
        db.userSessionDao().setOfflineMode(isOffline)
    }

    suspend fun testEngineConnection(engineId: String, apiKey: String, modelName: String): EngineConnectionTestResult = withContext(Dispatchers.IO) {
        val engine = TranslationEngine(db.translationMemoryDao()).getEngine(engineId)
        engine.testConnection(apiKey, modelName)
    }

    private fun computeHash(text: String, src: String, tgt: String): String {
        val input = "${text.trim().lowercase()}|$src|$tgt"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(32)
    }
}
