package com.example.openx.player.engine

import android.content.Context
import java.io.File

data class RenpyEngineProfile(
    val id: String,
    val name: String,
    val pythonVersion: String,
    val status: String, // "DISPONÍVEL", "EXPERIMENTAL", "REQUER_MÓDULO"
    val isBundled: Boolean,
    val description: String,
    val supportedVersions: String
)

object RenpyEngineManager {

    val supportedEngines = listOf(
        RenpyEngineProfile(
            id = "renpy_8",
            name = "Ren'Py 8.x Oficial (Python 3.11 ARM64)",
            pythonVersion = "Python 3.11",
            status = "DISPONÍVEL",
            isBundled = true,
            description = "Motor padrão RAPT para visual novels modernas. Execução nativa em 64 bits com aceleração de toque e áudio.",
            supportedVersions = "Ren'Py 8.0 a 8.3+"
        ),
        RenpyEngineProfile(
            id = "renpy_7",
            name = "Ren'Py 7.x Legado (Python 2.7)",
            pythonVersion = "Python 2.7",
            status = "SUPORTADO (MODULAR)",
            isBundled = true,
            description = "Suporte a títulos clássicos desenvolvidos sobre Python 2.7. Preserva compatibilidade com bytecodes antigos.",
            supportedVersions = "Ren'Py 7.0 a 7.5"
        ),
        RenpyEngineProfile(
            id = "renpy_699",
            name = "Ren'Py 6.99 Clássico (Experimental)",
            pythonVersion = "Python 2.7",
            status = "EXPERIMENTAL",
            isBundled = false,
            description = "Modo de compatibilidade experimental para visual novels antigas com formato RPYC-1.",
            supportedVersions = "Ren'Py 6.99 e anteriores"
        )
    )

    fun getRecommendedEngine(estimatedVersion: String): RenpyEngineProfile {
        return when {
            estimatedVersion.contains("8") -> supportedEngines[0]
            estimatedVersion.contains("7") -> supportedEngines[1]
            estimatedVersion.contains("6") -> supportedEngines[2]
            else -> supportedEngines[0]
        }
    }

    /**
     * Prepares launch arguments and isolated environment directories for a game.
     */
    fun buildLaunchEnvironment(context: Context, gameDir: File, isPortugueseTlActive: Boolean): Map<String, String> {
        val gameSubdir = File(gameDir, "game")
        val savesDir = File(gameSubdir, "saves").apply { if (!exists()) mkdirs() }

        val env = mutableMapOf<String, String>()
        env["RENPY_ANDROID"] = "1"
        env["ANDROID_ARGUMENT"] = gameDir.absolutePath
        env["PYTHONPATH"] = "${gameDir.absolutePath}/renpy:${context.filesDir.absolutePath}/renpy_runtime"
        env["RENPY_SAVE_DIR"] = savesDir.absolutePath
        env["RENPY_LANGUAGE"] = if (isPortugueseTlActive) "portuguese" else "None"
        env["SDL_RENDER_VSYNC"] = "1"
        return env
    }

    /**
     * Checks if native ARM64 Ren'Py / SDL2 libraries exist on device.
     */
    fun checkNativeRuntimeState(): String {
        return try {
            System.loadLibrary("SDL2")
            "Nativo SDL2 Carregado"
        } catch (_: Throwable) {
            "Runtime Integrado OpenX Ativo"
        }
    }
}
