package com.example.openx.engine.apk

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ApkEntryInfo(
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    val isBinaryResource: Boolean,
    val isReadableText: Boolean,
    val isRenpyAsset: Boolean,
    val category: String,
    val description: String
)

data class ApkInspectionReport(
    val totalEntries: Int,
    val readableEntries: List<ApkEntryInfo>,
    val binaryEntries: List<ApkEntryInfo>,
    val renpyEntries: List<ApkEntryInfo>,
    val dexFilesCount: Int,
    val hasResourcesArsc: Boolean,
    val hasCompiledManifest: Boolean,
    val technicalNotes: List<String>
)

object ApkInspector {

    fun inspectApkStream(inputStream: InputStream): ApkInspectionReport {
        val zipIn = ZipInputStream(inputStream)
        var entry: ZipEntry? = zipIn.nextEntry

        val readable = mutableListOf<ApkEntryInfo>()
        val binary = mutableListOf<ApkEntryInfo>()
        val renpy = mutableListOf<ApkEntryInfo>()

        var totalEntries = 0
        var dexCount = 0
        var hasArsc = false
        var hasManifest = false

        while (entry != null) {
            totalEntries++
            val name = entry.name
            val size = entry.size

            if (name.endsWith(".dex")) {
                dexCount++
                binary.add(
                    ApkEntryInfo(
                        path = name,
                        size = size,
                        isDirectory = false,
                        isBinaryResource = true,
                        isReadableText = false,
                        isRenpyAsset = false,
                        category = "Executável DEX",
                        description = "Bytecode Dalvik/ART compilado"
                    )
                )
            } else if (name == "resources.arsc") {
                hasArsc = true
                binary.add(
                    ApkEntryInfo(
                        path = name,
                        size = size,
                        isDirectory = false,
                        isBinaryResource = true,
                        isReadableText = false,
                        isRenpyAsset = false,
                        category = "Tabela Binária de Recursos",
                        description = "Tabela ARSC pré-compilada pelo AAPT2. Requer descompilação com framework aapt2/apktool"
                    )
                )
            } else if (name == "AndroidManifest.xml") {
                hasManifest = true
                binary.add(
                    ApkEntryInfo(
                        path = name,
                        size = size,
                        isDirectory = false,
                        isBinaryResource = true,
                        isReadableText = false,
                        isRenpyAsset = false,
                        category = "Manifest Binário (AXML)",
                        description = "XML Android binário tokenizado. Não é texto simples"
                    )
                )
            } else if (name.endsWith(".rpa") || name.endsWith(".rpyc")) {
                val isRpyc = name.endsWith(".rpyc")
                val item = ApkEntryInfo(
                    path = name,
                    size = size,
                    isDirectory = false,
                    isBinaryResource = true,
                    isReadableText = false,
                    isRenpyAsset = true,
                    category = if (isRpyc) "Ren'Py Compilado (.rpyc)" else "Pacote de Recursos Ren'Py (.rpa)",
                    description = if (isRpyc)
                        "Script compilado em bytecode Python/Ren'Py. Requer descompilador especializado (ex: Unrpyc)"
                    else
                        "Arquivo de empacotamento Ren'Py. Requer extração RPA para acessar os arquivos .rpy"
                )
                renpy.add(item)
                binary.add(item)
            } else if (name.endsWith(".rpy") || name.endsWith(".json") || name.endsWith(".txt") || name.endsWith(".csv")) {
                readable.add(
                    ApkEntryInfo(
                        path = name,
                        size = size,
                        isDirectory = false,
                        isBinaryResource = false,
                        isReadableText = true,
                        isRenpyAsset = name.endsWith(".rpy"),
                        category = "Recurso Textual Acessível",
                        description = "Texto puro UTF-8 que pode ser extraído e traduzido diretamente no OpenX"
                    )
                )
            } else if (name.startsWith("assets/") && (name.endsWith(".xml") || name.endsWith(".properties") || name.endsWith(".ini"))) {
                readable.add(
                    ApkEntryInfo(
                        path = name,
                        size = size,
                        isDirectory = false,
                        isBinaryResource = false,
                        isReadableText = true,
                        isRenpyAsset = false,
                        category = "Ativo Textual (assets/)",
                        description = "Arquivo textual da pasta de ativos do aplicativo"
                    )
                )
            } else if (name.startsWith("res/") && name.endsWith(".xml")) {
                binary.add(
                    ApkEntryInfo(
                        path = name,
                        size = size,
                        isDirectory = false,
                        isBinaryResource = true,
                        isReadableText = false,
                        isRenpyAsset = false,
                        category = "Layout / Drawable XML Compilado",
                        description = "AXML binário compilado. Necessita de decodificador AXML"
                    )
                )
            }

            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }

        val technicalNotes = mutableListOf<String>()
        technicalNotes.add("Contêiner ZIP analisado: $totalEntries entradas mapeadas.")
        if (hasArsc) {
            technicalNotes.add("Detectado resources.arsc (Tabela binária): o Android compila strings em chunks binários. Modificação direta exigirá pipeline aapt2/apktool.")
        }
        if (renpy.isNotEmpty()) {
            technicalNotes.add("${renpy.size} arquivos de engine Ren'Py detectados. Scripts .rpa/.rpyc exigem pipeline de extração.")
        }
        technicalNotes.add("Aviso de Assinatura: Reconstruir um APK requer nova assinatura digital (apksigner/zipalign). Aplicativos assinados com nova chave não podem sobrescrever atualizações do APK original instalado.")

        return ApkInspectionReport(
            totalEntries = totalEntries,
            readableEntries = readable,
            binaryEntries = binary,
            renpyEntries = renpy,
            dexFilesCount = dexCount,
            hasResourcesArsc = hasArsc,
            hasCompiledManifest = hasManifest,
            technicalNotes = technicalNotes
        )
    }

    fun extractTextFileContent(inputStream: InputStream, targetPath: String): String? {
        val zipIn = ZipInputStream(inputStream)
        var entry: ZipEntry? = zipIn.nextEntry

        while (entry != null) {
            if (entry.name == targetPath) {
                return zipIn.bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            zipIn.closeEntry()
            entry = zipIn.nextEntry
        }
        return null
    }
}
