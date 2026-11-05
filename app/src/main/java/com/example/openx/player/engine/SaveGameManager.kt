package com.example.openx.player.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class SaveSlotInfo(
    val slotName: String,
    val file: File,
    val sizeBytes: Long,
    val lastModifiedTimestamp: Long,
    val formattedDate: String,
    val isAutoSave: Boolean,
    val isQuickSave: Boolean
)

data class SaveBackupResult(
    val success: Boolean,
    val backupFile: File?,
    val savedCount: Int,
    val message: String
)

class SaveGameManager(private val context: Context) {

    fun getSavesDirectory(gameDir: File): File {
        // Ren'Py standard save location is game/saves
        val savesDir = File(gameDir, "game/saves")
        if (!savesDir.exists()) {
            savesDir.mkdirs()
        }
        return savesDir
    }

    suspend fun listSaves(gameDir: File): List<SaveSlotInfo> = withContext(Dispatchers.IO) {
        val savesDir = getSavesDirectory(gameDir)
        val files = savesDir.listFiles() ?: return@withContext emptyList()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        files.filter { it.isFile && (it.name.endsWith(".save") || it.name.endsWith(".bak") || it.name.startsWith("auto-") || it.name.startsWith("quick-")) }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                val name = file.name
                val isAuto = name.startsWith("auto-")
                val isQuick = name.startsWith("quick-")
                SaveSlotInfo(
                    slotName = name.substringBeforeLast("."),
                    file = file,
                    sizeBytes = file.length(),
                    lastModifiedTimestamp = file.lastModified(),
                    formattedDate = dateFormat.format(Date(file.lastModified())),
                    isAutoSave = isAuto,
                    isQuickSave = isQuick
                )
            }
    }

    suspend fun backupSavesToZip(gameDir: File, gameTitle: String): SaveBackupResult = withContext(Dispatchers.IO) {
        runCatching {
            val savesDir = getSavesDirectory(gameDir)
            val saveFiles = savesDir.listFiles()?.filter { it.isFile } ?: emptyList()
            if (saveFiles.isEmpty()) {
                return@runCatching SaveBackupResult(
                    success = false,
                    backupFile = null,
                    savedCount = 0,
                    message = "Nenhum save encontrado para backup."
                )
            }

            val backupsDir = File(context.filesDir, "backups/saves")
            if (!backupsDir.exists()) {
                backupsDir.mkdirs()
            }

            val sanitizedTitle = gameTitle.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val backupZip = File(backupsDir, "saves_${sanitizedTitle}_$timeStamp.zip")

            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupZip))).use { zos ->
                val buffer = ByteArray(8192)
                for (saveFile in saveFiles) {
                    val entry = ZipEntry(saveFile.name)
                    zos.putNextEntry(entry)
                    BufferedInputStream(FileInputStream(saveFile)).use { bis ->
                        var len: Int
                        while (bis.read(buffer).also { len = it } > 0) {
                            zos.write(buffer, 0, len)
                        }
                    }
                    zos.closeEntry()
                }
            }

            SaveBackupResult(
                success = true,
                backupFile = backupZip,
                savedCount = saveFiles.size,
                message = "Backup de ${saveFiles.size} saves criado com sucesso: ${backupZip.name}"
            )
        }.getOrElse { e ->
            SaveBackupResult(
                success = false,
                backupFile = null,
                savedCount = 0,
                message = "Erro ao criar backup de saves: ${e.message}"
            )
        }
    }

    suspend fun restoreSavesFromZip(backupZipFile: File, gameDir: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val savesDir = getSavesDirectory(gameDir)
            val canonicalSaves = savesDir.canonicalFile

            ZipInputStream(BufferedInputStream(FileInputStream(backupZipFile))).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                val buffer = ByteArray(8192)

                while (entry != null) {
                    val cleanName = entry.name.replace('\\', '/').trimStart('/')
                    val target = File(canonicalSaves, cleanName).canonicalFile

                    // Prevent Zip Slip
                    if (target.path.startsWith(canonicalSaves.path)) {
                        BufferedOutputStream(FileOutputStream(target)).use { bos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                bos.write(buffer, 0, len)
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        }.getOrDefault(false)
    }

    suspend fun deleteSave(saveFile: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            saveFile.delete()
        }.getOrDefault(false)
    }
}
