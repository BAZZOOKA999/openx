package com.example.openx.engine.export

import android.content.Context
import android.net.Uri
import com.example.openx.data.db.entities.BlockType
import com.example.openx.data.db.entities.FileItemEntity
import com.example.openx.data.db.entities.FileType
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.data.db.entities.TranslationStatus
import com.example.openx.data.db.entities.TranslationUnitEntity
import com.example.openx.engine.androidres.AndroidResParser
import com.example.openx.engine.renpy.RenpyExporter
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExportReport(
    val projectName: String,
    val totalFiles: Int,
    val totalUnits: Int,
    val translatedUnits: Int,
    val pendingUnits: Int,
    val errorUnits: Int,
    val completionPercentage: Int,
    val generatedFilesList: List<String>,
    val warnings: List<String>
)

object ExportManager {

    fun generateReport(
        project: ProjectEntity,
        files: List<FileItemEntity>,
        units: List<TranslationUnitEntity>
    ): ExportReport {
        val total = units.size
        val translated = units.count { it.status == TranslationStatus.TRANSLATED || it.status == TranslationStatus.REVIEWED }
        val pending = units.count { it.status == TranslationStatus.PENDING }
        val error = units.count { it.status == TranslationStatus.ERROR || it.validationError != null }
        val percentage = if (total > 0) (translated * 100) / total else 0

        val warnings = mutableListOf<String>()
        if (pending > 0) {
            warnings.add("$pending textos ainda não foram traduzidos e manterão o conteúdo original.")
        }
        if (error > 0) {
            warnings.add("$error textos contêm avisos de sintaxe ou variáveis divergentes.")
        }

        val fileList = files.map { it.fileName }

        return ExportReport(
            projectName = project.name,
            totalFiles = files.size,
            totalUnits = total,
            translatedUnits = translated,
            pendingUnits = pending,
            errorUnits = error,
            completionPercentage = percentage,
            generatedFilesList = fileList,
            warnings = warnings
        )
    }

    fun exportSingleFileContent(
        file: FileItemEntity,
        units: List<TranslationUnitEntity>,
        targetLang: String
    ): String {
        return when (file.fileType) {
            FileType.RPY -> {
                RenpyExporter.exportToRenpy(file.rawContent, units, targetLang)
            }
            FileType.XML -> {
                AndroidResParser.exportStringsXml(units)
            }
            FileType.JSON -> {
                AndroidResParser.exportJson(units)
            }
            FileType.CSV, FileType.TXT -> {
                units.joinToString("\n") { unit ->
                    val text = if (unit.translatedText.isNotBlank()) unit.translatedText else unit.originalText
                    if (unit.blockType == BlockType.CSV_CELL) {
                        "\"${unit.keyOrContext}\",\"$text\""
                    } else {
                        text
                    }
                }
            }
            else -> {
                RenpyExporter.exportToRenpy(file.rawContent, units, targetLang)
            }
        }
    }

    fun createExportZip(
        project: ProjectEntity,
        filesWithUnits: List<Pair<FileItemEntity, List<TranslationUnitEntity>>>
    ): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val zipOut = ZipOutputStream(byteOut)

        for ((file, units) in filesWithUnits) {
            val content = exportSingleFileContent(file, units, project.targetLang)
            
            // Build appropriate output path
            val entryPath = when (file.fileType) {
                FileType.RPY -> {
                    val langFolder = project.targetLang.lowercase().replace("-", "_")
                    if (file.relativePath.startsWith("game/tl/")) {
                        file.relativePath
                    } else {
                        "game/tl/$langFolder/${file.fileName}"
                    }
                }
                FileType.XML -> {
                    val langSuffix = project.targetLang.take(2).lowercase()
                    "res/values-$langSuffix/${file.fileName}"
                }
                else -> {
                    file.relativePath.ifBlank { file.fileName }
                }
            }

            val zipEntry = ZipEntry(entryPath)
            zipOut.putNextEntry(zipEntry)
            zipOut.write(content.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }

        // Add an OpenX manifest inside the ZIP for traceability
        val manifestEntry = ZipEntry("openx_export_info.txt")
        zipOut.putNextEntry(manifestEntry)
        val manifestText = """
            ==================================================
            Exportado via OpenX - Suite de Tradução Ren'Py e Android
            Projeto: ${project.name}
            Idioma de Destino: ${project.targetLang}
            Data de Exportação: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            Total de Arquivos: ${filesWithUnits.size}
            Total de Segmentos: ${filesWithUnits.sumOf { it.second.size }}
            Status: Pronto para integração no motor Ren'Py ou APK
            ==================================================
        """.trimIndent()
        zipOut.write(manifestText.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()

        zipOut.finish()
        return byteOut.toByteArray()
    }

    fun writeToUri(context: Context, uri: Uri, data: ByteArray): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(data)
                stream.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
        return writeToUri(context, uri, text.toByteArray(Charsets.UTF_8))
    }
}
