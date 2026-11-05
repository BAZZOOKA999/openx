package com.example.openx.player.engine

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ZipExtractionProgress(
    val filesExtracted: Int,
    val totalEstimatedFiles: Int,
    val bytesExtracted: Long,
    val currentFileName: String,
    val isComplete: Boolean = false,
    val error: String? = null
)

class ZipExtractor(private val context: Context) {

    companion object {
        const val MAX_UNCOMPRESSED_BYTES: Long = 3L * 1024 * 1024 * 1024 // 3GB max for mobile safety
        const val MAX_ENTRY_COUNT: Int = 50000
        const val BUFFER_SIZE = 8192
    }

    suspend fun extractZip(
        sourceUri: Uri,
        destDir: File,
        onProgress: (ZipExtractionProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!destDir.exists()) {
                destDir.mkdirs()
            }
            val canonicalDestDir = destDir.canonicalFile

            var totalBytesRead = 0L
            var entryCount = 0

            // Pass 1: Quick estimate of entry count (optional, fallback to open-ended)
            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalArgumentException("Não foi possível abrir o arquivo ZIP selecionado.")

            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var zipEntry: ZipEntry? = zis.nextEntry

                while (zipEntry != null) {
                    entryCount++
                    if (entryCount > MAX_ENTRY_COUNT) {
                        throw SecurityException("Segurança OpenX: O arquivo ZIP excede o limite de $MAX_ENTRY_COUNT entradas (possível Zip Bomb).")
                    }

                    val rawName = zipEntry.name
                    // Sanitize path (strip leading slashes or Windows drive letters)
                    val cleanName = rawName.replace('\\', '/').trimStart('/')
                    if (cleanName.isNotBlank()) {
                        val targetFile = File(canonicalDestDir, cleanName)
                        val targetCanonical = targetFile.canonicalFile

                        // ZIP SLIP / PATH TRAVERSAL CHECK
                        if (!targetCanonical.path.startsWith(canonicalDestDir.path + File.separator) &&
                            targetCanonical.path != canonicalDestDir.path
                        ) {
                            throw SecurityException("Segurança OpenX: Tentativa de Path Traversal detectada na entrada: $rawName")
                        }

                        if (zipEntry.isDirectory) {
                            targetCanonical.mkdirs()
                        } else {
                            targetCanonical.parentFile?.mkdirs()
                            BufferedOutputStream(FileOutputStream(targetCanonical), BUFFER_SIZE).use { bos ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    totalBytesRead += len
                                    if (totalBytesRead > MAX_UNCOMPRESSED_BYTES) {
                                        throw SecurityException("Segurança OpenX: Arquivo excede o limite de descompactação de 3 GB (proteção contra ZIP Bomb).")
                                    }
                                    bos.write(buffer, 0, len)
                                }
                            }
                        }

                        if (entryCount % 10 == 0 || zipEntry.isDirectory) {
                            onProgress(
                                ZipExtractionProgress(
                                    filesExtracted = entryCount,
                                    totalEstimatedFiles = entryCount + 10,
                                    bytesExtracted = totalBytesRead,
                                    currentFileName = targetCanonical.name
                                )
                            )
                        }
                    }

                    zis.closeEntry()
                    zipEntry = zis.nextEntry
                }
            }

            onProgress(
                ZipExtractionProgress(
                    filesExtracted = entryCount,
                    totalEstimatedFiles = entryCount,
                    bytesExtracted = totalBytesRead,
                    currentFileName = "Concluído",
                    isComplete = true
                )
            )

            canonicalDestDir
        }
    }
}
