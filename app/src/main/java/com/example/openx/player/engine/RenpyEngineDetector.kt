package com.example.openx.player.engine

import java.io.File
import java.io.FileInputStream

data class GameInspectionResult(
    val title: String,
    val version: String,
    val gameDirectory: File,
    val actualGameSubdir: File, // where game/ is located
    val estimatedEngineVersion: String,
    val confidenceScore: Int,
    val rpycCount: Int,
    val rpyCount: Int,
    val rpaCount: Int,
    val detectedLanguages: List<String>,
    val coverFile: File?,
    val hasWindowsBinaries: Boolean,
    val totalSizeBytes: Long,
    val compatibilityStatus: String, // COMPATIBLE, WARNING, INCOMPATIBLE
    val diagnosticSummary: String,
    val detectedPythonVersion: String
)

object RenpyEngineDetector {

    fun inspectDirectory(directory: File): GameInspectionResult {
        var baseDir = directory
        // Check if the directory has a single subfolder containing game/
        val directGameFolder = File(directory, "game")
        var gameSubdir = if (directGameFolder.exists() && directGameFolder.isDirectory) {
            directGameFolder
        } else {
            // Search 1 level down
            val nestedGame = directory.listFiles()?.firstOrNull { it.isDirectory && File(it, "game").exists() }
            if (nestedGame != null) {
                baseDir = nestedGame
                File(nestedGame, "game")
            } else {
                // If files are directly in directory
                directory
            }
        }

        var rpyCount = 0
        var rpycCount = 0
        var rpaCount = 0
        var hasWindowsBinaries = false
        var totalSize = 0L

        var py3PickleDetected = false
        var py2PickleDetected = false
        var rpyc1Detected = false

        // Traverse files
        fun scanDir(dir: File, depth: Int = 0) {
            if (depth > 6) return
            val children = dir.listFiles() ?: return
            for (file in children) {
                if (file.isDirectory) {
                    scanDir(file, depth + 1)
                } else {
                    totalSize += file.length()
                    val nameLower = file.name.lowercase()
                    when {
                        nameLower.endsWith(".rpy") -> rpyCount++
                        nameLower.endsWith(".rpyc") -> {
                            rpycCount++
                            // Inspect first .rpyc header if not determined
                            if (!py3PickleDetected && !py2PickleDetected && file.length() > 16) {
                                inspectRpycHeader(file)?.let { (isPy3, isRpyc1) ->
                                    if (isRpyc1) rpyc1Detected = true
                                    else if (isPy3) py3PickleDetected = true
                                    else py2PickleDetected = true
                                }
                            }
                        }
                        nameLower.endsWith(".rpa") -> rpaCount++
                        nameLower.endsWith(".exe") || nameLower.endsWith(".dll") -> {
                            hasWindowsBinaries = true
                        }
                    }
                }
            }
        }

        scanDir(baseDir)

        // Title and Version extraction from options.rpy or script.rpy
        var detectedTitle = baseDir.name.replace("_", " ").capitalizeWords()
        var detectedVersion = "1.0"

        val optionsRpy = File(gameSubdir, "options.rpy").takeIf { it.exists() }
            ?: File(baseDir, "options.rpy").takeIf { it.exists() }

        if (optionsRpy != null) {
            try {
                optionsRpy.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("define config.name =") || trimmed.startsWith("config.name =")) {
                        val parsed = trimmed.substringAfter("=").trim().trim('"', '\'')
                        if (parsed.isNotBlank() && !parsed.contains("gui.TEXT")) {
                            detectedTitle = parsed
                        }
                    } else if (trimmed.startsWith("define config.version =") || trimmed.startsWith("config.version =")) {
                        val parsed = trimmed.substringAfter("=").trim().trim('"', '\'')
                        if (parsed.isNotBlank()) {
                            detectedVersion = parsed
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // Cover detection
        val potentialCovers = listOf(
            File(gameSubdir, "gui/window_icon.png"),
            File(gameSubdir, "window_icon.png"),
            File(gameSubdir, "gui/main_menu.png"),
            File(gameSubdir, "cover.png"),
            File(gameSubdir, "cover.jpg"),
            File(baseDir, "cover.png"),
            File(baseDir, "cover.jpg"),
            File(baseDir, "icon.png")
        )
        val coverFile = potentialCovers.firstOrNull { it.exists() && it.isFile && it.length() > 0 }

        // Language detection in game/tl/
        val tlDir = File(gameSubdir, "tl")
        val detectedLanguages = mutableListOf<String>()
        if (tlDir.exists() && tlDir.isDirectory) {
            tlDir.listFiles()?.filter { it.isDirectory }?.forEach {
                detectedLanguages.add(it.name)
            }
        }
        if (detectedLanguages.isEmpty()) {
            detectedLanguages.add("Original (En)")
        }

        // Determine Engine Version & Confidence
        val (engineVersion, confidence, pyVersion) = when {
            py3PickleDetected -> Triple("Ren'Py 8.x (Python 3)", 95, "Python 3")
            py2PickleDetected -> Triple("Ren'Py 7.x (Python 2.7)", 90, "Python 2.7")
            rpyc1Detected -> Triple("Ren'Py 6.99 (Legado)", 80, "Python 2.7")
            rpyCount > 0 -> Triple("Ren'Py 8.x (Auto-detectado por Scripts)", 85, "Python 3")
            rpaCount > 0 -> Triple("Ren'Py 8.x/7.x (Arquivo RPA)", 75, "Auto")
            else -> Triple("Ren'Py Desconhecido", 40, "Desconhecido")
        }

        // Compatibility Assessment
        val status: String
        val diagnostics = StringBuilder()

        if (rpyCount == 0 && rpycCount == 0 && rpaCount == 0) {
            status = "INCOMPATIBLE"
            diagnostics.append("Não foram encontrados scripts Ren'Py (.rpy, .rpyc) ou pacotes (.rpa). Verifique se a pasta contém o jogo descompactado.")
        } else {
            status = "COMPATIBLE"
            diagnostics.append("Estrutura Ren'Py reconhecida com sucesso. ")
            diagnostics.append("Total de $rpyCount scripts .rpy, $rpycCount binários .rpyc compilados e $rpaCount arquivos RPA. ")

            if (hasWindowsBinaries) {
                diagnostics.append("\n• Aviso Seguro: Arquivos Windows (.exe/.dll) detectados na raiz e ignorados. O OpenX Player utiliza a integração nativa ARM64 Android (sem emulação x86 desnecessária).")
            }
            if (py3PickleDetected) {
                diagnostics.append("\n• Bytecode Python 3 detectado nos arquivos .rpyc: Totalmente compatível com o runtime Ren'Py 8.")
            } else if (py2PickleDetected) {
                diagnostics.append("\n• Bytecode Python 2.7 detectado nos arquivos .rpyc: Recomenda-se motor Ren'Py 7.")
            }
            if (detectedLanguages.any { it.contains("portuguese", ignoreCase = true) || it.contains("pt", ignoreCase = true) }) {
                diagnostics.append("\n• Tradução em Português detectada no diretório game/tl/!")
            }
        }

        return GameInspectionResult(
            title = detectedTitle,
            version = detectedVersion,
            gameDirectory = baseDir,
            actualGameSubdir = gameSubdir,
            estimatedEngineVersion = engineVersion,
            confidenceScore = confidence,
            rpycCount = rpycCount,
            rpyCount = rpyCount,
            rpaCount = rpaCount,
            detectedLanguages = detectedLanguages,
            coverFile = coverFile,
            hasWindowsBinaries = hasWindowsBinaries,
            totalSizeBytes = totalSize,
            compatibilityStatus = status,
            diagnosticSummary = diagnostics.toString(),
            detectedPythonVersion = pyVersion
        )
    }

    private fun inspectRpycHeader(file: File): Pair<Boolean, Boolean>? {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(64)
                val read = fis.read(header)
                if (read < 16) return null

                val headerStr = String(header, 0, minOf(read, 32), Charsets.ISO_8859_1)

                // Check for RPYC-1 vs RPYC-2
                val isRpyc1 = headerStr.startsWith("RENPY RPC-1")
                val isRpyc2 = headerStr.startsWith("RENPY RPC-2")

                // Search for pickle proto marker
                var isPy3 = false
                for (i in 0 until read - 1) {
                    if (header[i] == 0x80.toByte()) {
                        val proto = header[i + 1].toInt()
                        if (proto >= 3) {
                            isPy3 = true
                            break
                        } else if (proto == 2) {
                            isPy3 = false
                            break
                        }
                    }
                }

                Pair(isPy3, isRpyc1)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}
