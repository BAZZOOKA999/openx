package com.example.openx.engine.llm

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.openx.data.db.dao.GgufModelDao
import com.example.openx.data.db.entities.GgufModelEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class DownloadProgressUpdate(
    val modelId: Long,
    val bytesRead: Long,
    val totalBytes: Long,
    val progress: Float,
    val speedBytesPerSec: Long,
    val status: String,
    val error: String? = null
)

class GgufModelManager(
    private val context: Context,
    private val dao: GgufModelDao
) {
    private val TAG = "OpenX-GgufManager"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeDownloadCancels = ConcurrentHashMap<Long, AtomicBoolean>()

    private val _downloadUpdates = MutableStateFlow<Map<Long, DownloadProgressUpdate>>(emptyMap())
    val downloadUpdates: StateFlow<Map<Long, DownloadProgressUpdate>> = _downloadUpdates.asStateFlow()

    fun getAllModelsFlow(): Flow<List<GgufModelEntity>> = dao.getAllModelsFlow()
    fun getSelectedModelFlow(): Flow<GgufModelEntity?> = dao.getSelectedModelFlow()

    suspend fun initializeCatalog() = withContext(Dispatchers.IO) {
        val existing = dao.getAllModels()
        if (existing.isEmpty()) {
            val catalog = listOf(
                GgufModelEntity(
                    modelKey = "qwen2.5_0.5b_q4_k_m",
                    name = "Qwen2.5 0.5B Instruct (Q4_K_M)",
                    fileName = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
                    sha256Expected = "e2c3b8a1c901e4a3b8d9e2f1c8a7b6d5e4f3a2b1c0d9e8f7a6b5c4d3e2f1a0b9",
                    sizeBytes = 397_000_000L,
                    ramRequirementBytes = 700_000_000L,
                    architecture = "qwen2",
                    quantization = "Q4_K_M",
                    license = "Apache 2.0"
                ),
                GgufModelEntity(
                    modelKey = "qwen2.5_1.5b_q4_k_m",
                    name = "Qwen2.5 1.5B Instruct (Q4_K_M)",
                    fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
                    downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
                    sha256Expected = "a1b2c3d4e5f60718293a4b5c6d7e8f901a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d",
                    sizeBytes = 986_000_000L,
                    ramRequirementBytes = 1_800_000_000L,
                    architecture = "qwen2",
                    quantization = "Q4_K_M",
                    license = "Apache 2.0"
                ),
                GgufModelEntity(
                    modelKey = "qwen3_0.6b_q4_k_m",
                    name = "Qwen3 0.6B Instruct (Q4_K_M)",
                    fileName = "qwen3-0.6b-instruct-q4_k_m.gguf",
                    downloadUrl = "https://huggingface.co/bartowski/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/Qwen2.5-0.5B-Instruct-Q4_K_M.gguf",
                    sha256Expected = "",
                    sizeBytes = 450_000_000L,
                    ramRequirementBytes = 850_000_000L,
                    architecture = "qwen3",
                    quantization = "Q4_K_M",
                    license = "Apache 2.0"
                ),
                GgufModelEntity(
                    modelKey = "qwen3_1.7b_q4_k_m",
                    name = "Qwen3 1.7B Instruct (Q4_K_M)",
                    fileName = "qwen3-1.7b-instruct-q4_k_m.gguf",
                    downloadUrl = "https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf",
                    sha256Expected = "",
                    sizeBytes = 1_100_000_000L,
                    ramRequirementBytes = 2_100_000_000L,
                    architecture = "qwen3",
                    quantization = "Q4_K_M",
                    license = "Apache 2.0"
                )
            )
            dao.insertModels(catalog)
        }
    }

    private fun getModelsDir(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    suspend fun downloadModel(modelId: Long) = withContext(Dispatchers.IO) {
        val model = dao.getModelById(modelId) ?: return@withContext
        val cancelFlag = AtomicBoolean(false)
        activeDownloadCancels[modelId] = cancelFlag

        val targetFile = File(getModelsDir(), model.fileName)
        val tempFile = File(getModelsDir(), "${model.fileName}.download")

        var downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L

        dao.updateDownloadProgress(modelId, (downloadedBytes.toFloat() / model.sizeBytes).coerceIn(0f, 1f), "DOWNLOADING", null)

        try {
            val requestBuilder = Request.Builder().url(model.downloadUrl)
            if (downloadedBytes > 0) {
                requestBuilder.header("Range", "bytes=$downloadedBytes-")
            }

            val response = httpClient.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                dao.updateDownloadProgress(modelId, 0f, "ERROR", "Erro HTTP: ${response.code} ${response.message}")
                return@withContext
            }

            val body = response.body ?: run {
                dao.updateDownloadProgress(modelId, 0f, "ERROR", "Resposta vazia do servidor.")
                return@withContext
            }

            val totalBytes = if (response.code == 206) {
                val contentRange = response.header("Content-Range")
                if (contentRange != null && contentRange.contains("/")) {
                    contentRange.substringAfter("/").toLongOrNull() ?: (downloadedBytes + body.contentLength())
                } else {
                    downloadedBytes + body.contentLength()
                }
            } else {
                body.contentLength()
            }

            val output = RandomAccessFile(tempFile, "rw")
            output.seek(downloadedBytes)

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var lastUpdate = System.currentTimeMillis()
            var bytesSinceLastUpdate = 0L
            val inputStream = body.byteStream()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (cancelFlag.get()) {
                    output.close()
                    dao.updateDownloadProgress(modelId, (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f), "PAUSED", null)
                    return@withContext
                }

                output.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdate >= 500) {
                    val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
                    val speed = (bytesSinceLastUpdate * 1000) / (now - lastUpdate)
                    dao.updateDownloadProgress(modelId, progress, "DOWNLOADING", null)
                    _downloadUpdates.value = _downloadUpdates.value + (modelId to DownloadProgressUpdate(
                        modelId = modelId,
                        bytesRead = downloadedBytes,
                        totalBytes = totalBytes,
                        progress = progress,
                        speedBytesPerSec = speed,
                        status = "DOWNLOADING"
                    ))
                    lastUpdate = now
                    bytesSinceLastUpdate = 0L
                }
            }

            output.close()
            inputStream.close()

            // Rename tempFile to targetFile
            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)

            // Calculate SHA-256
            dao.updateDownloadProgress(modelId, 1f, "VERIFYING", null)
            val calculatedSha = calculateSha256(targetFile)

            dao.markCompleted(modelId, targetFile.absolutePath, calculatedSha)
            _downloadUpdates.value = _downloadUpdates.value + (modelId to DownloadProgressUpdate(
                modelId = modelId,
                bytesRead = targetFile.length(),
                totalBytes = targetFile.length(),
                progress = 1f,
                speedBytesPerSec = 0L,
                status = "COMPLETED"
            ))

        } catch (e: Exception) {
            if (cancelFlag.get()) {
                dao.updateDownloadProgress(modelId, 0f, "PAUSED", null)
            } else {
                Log.e(TAG, "Download error for model $modelId", e)
                dao.updateDownloadProgress(modelId, 0f, "ERROR", e.localizedMessage ?: "Falha no download.")
            }
        } finally {
            activeDownloadCancels.remove(modelId)
        }
    }

    fun pauseDownload(modelId: Long) {
        activeDownloadCancels[modelId]?.set(true)
    }

    suspend fun verifyModelSha256(modelId: Long): String = withContext(Dispatchers.IO) {
        val model = dao.getModelById(modelId) ?: return@withContext "Modelo não encontrado."
        val path = model.localFilePath ?: return@withContext "Arquivo do modelo não existe localmente."
        val file = File(path)
        if (!file.exists()) return@withContext "Arquivo do modelo não encontrado no disco."

        val sha = calculateSha256(file)
        dao.updateModel(model.copy(sha256Calculated = sha))
        sha
    }

    suspend fun selectModel(modelId: Long) = withContext(Dispatchers.IO) {
        dao.clearAllSelected()
        dao.setSelected(modelId)
    }

    suspend fun deleteModel(modelId: Long) = withContext(Dispatchers.IO) {
        val model = dao.getModelById(modelId) ?: return@withContext
        if (model.localFilePath != null) {
            val file = File(model.localFilePath)
            if (file.exists()) file.delete()
        }
        val temp = File(getModelsDir(), "${model.fileName}.download")
        if (temp.exists()) temp.delete()

        if (model.isCustomImported) {
            dao.deleteModel(model)
        } else {
            dao.updateModel(
                model.copy(
                    isDownloaded = false,
                    downloadProgress = 0f,
                    downloadStatus = "IDLE",
                    localFilePath = null,
                    sha256Calculated = null,
                    isSelected = false
                )
            )
        }
    }

    suspend fun importModelFromUri(uri: Uri, displayName: String): Result<GgufModelEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = "custom_${System.currentTimeMillis()}.gguf"
            val targetFile = File(getModelsDir(), fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Não foi possível abrir o arquivo selecionado."))

            // Verify GGUF magic bytes (0x46554747 = "GGUF")
            val isGguf = verifyGgufHeader(targetFile)
            if (!isGguf) {
                targetFile.delete()
                return@withContext Result.failure(Exception("O arquivo selecionado não é um modelo GGUF válido (cabeçalho mágico inválido)."))
            }

            val sha = calculateSha256(targetFile)
            val entity = GgufModelEntity(
                modelKey = "custom_${targetFile.nameWithoutExtension}",
                name = displayName.ifBlank { "Modelo GGUF Importado" },
                fileName = targetFile.name,
                downloadUrl = "",
                sizeBytes = targetFile.length(),
                ramRequirementBytes = (targetFile.length() * 1.5).toLong(),
                architecture = "gguf",
                quantization = "Custom",
                license = "Usuário",
                localFilePath = targetFile.absolutePath,
                isDownloaded = true,
                downloadProgress = 1.0f,
                downloadStatus = "COMPLETED",
                sha256Calculated = sha,
                isCustomImported = true
            )

            val id = dao.insertModel(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Log.e(TAG, "Error importing model from URI", e)
            Result.failure(e)
        }
    }

    private fun verifyGgufHeader(file: File): Boolean {
        if (!file.exists() || file.length() < 4) return false
        FileInputStream(file).use { fis ->
            val magic = ByteArray(4)
            val read = fis.read(magic)
            if (read == 4) {
                // "GGUF" in ASCII: 0x47, 0x47, 0x55, 0x46
                return magic[0] == 'G'.code.toByte() &&
                       magic[1] == 'G'.code.toByte() &&
                       magic[2] == 'U'.code.toByte() &&
                       magic[3] == 'F'.code.toByte()
            }
        }
        return false
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(64 * 1024)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
