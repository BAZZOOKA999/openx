package com.example.openx.player.engine

import android.content.Context
import com.example.openx.player.data.RenpyGameDao
import com.example.openx.player.data.RenpyGameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DemoGameInstaller {

    suspend fun installDemoGameIfMissing(context: Context, gameDao: RenpyGameDao): Long = withContext(Dispatchers.IO) {
        val existing = gameDao.getBuiltInDemoGame()
        if (existing != null && File(existing.gameDir).exists()) {
            return@withContext existing.id
        }

        val gamesRoot = File(context.filesDir, "renpy_games")
        val demoDir = File(gamesRoot, "demo_verdeluz")
        if (!demoDir.exists()) {
            demoDir.mkdirs()
        }

        val gameSubdir = File(demoDir, "game")
        gameSubdir.mkdirs()

        val tlDir = File(gameSubdir, "tl/portuguese")
        tlDir.mkdirs()

        val savesDir = File(gameSubdir, "saves")
        savesDir.mkdirs()

        val guiDir = File(gameSubdir, "gui")
        guiDir.mkdirs()

        // 1. Script.rpy (Original in English)
        val scriptRpy = File(gameSubdir, "script.rpy")
        if (!scriptRpy.exists()) {
            scriptRpy.writeText(
                """
# OpenX Demo Game - Official Ren'Py 8.x Script
# Title: The Riddle of Verdeluz
define eileen = Character("Clara", color="#E50914")
define investigator = Character("Investigador", color="#3B82F6")

label start:
    scene bg_mansion with fade
    play music "audio/mystery.ogg"

    "The heavy oak doors of Verdeluz Manor slammed shut with a thunderous echo behind you."
    
    eileen "Welcome, Detective. You finally arrived. Things have turned... quite unsettling since nightfall."
    
    investigator "Tell me, Clara. Where did you discover the encrypted manuscript?"
    
    eileen "In the secret archives, right behind the Count's portrait. But there were fresh muddy footprints on the Persian rug."

    menu:
        "Investigate the secret archives immediately":
            jump investigate_archives
        "Interrogate Clara about the Count's disappearance":
            jump interrogate_clara
        "Examine the heavy entrance lock":
            jump examine_lock

label investigate_archives:
    scene bg_secret_library with dissolve
    investigator "The thick layer of dust on the mahogany shelves gives it away: someone removed several rare Latin codices."
    eileen "Look over here! An unfinished letter bearing the royal crest of 1892..."
    jump conclusion

label interrogate_clara:
    scene bg_salon with dissolve
    investigator "You seemed remarkably close to the Count before he vanished into thin air."
    eileen "He entrusted me with the master key to the vault, but I swear I never stepped inside without his permission!"
    jump conclusion

label examine_lock:
    scene bg_entrance with dissolve
    investigator "The lock was not forced from the exterior. Whoever trapped us in this mansion did it from the inside."
    eileen "Good heavens... that means he might still be lurking within these walls!"
    jump conclusion

label conclusion:
    "The shadows begin to whisper as the investigation deepens."
    "OpenX Player - Demonstração Ren'Py 8 Oficial executada com sucesso!"
    return
                """.trimIndent()
            )
        }

        // 2. Options.rpy
        val optionsRpy = File(gameSubdir, "options.rpy")
        if (!optionsRpy.exists()) {
            optionsRpy.writeText(
                """
define config.name = "O Mistério de Verdeluz (Demo Ren'Py 8)"
define config.version = "8.2.3"
define gui.show_name = True
define config.has_sound = True
define config.has_music = True
define config.has_voice = False
define config.window_icon = "gui/window_icon.png"
                """.trimIndent()
            )
        }

        // 3. Official Translation Template: game/tl/portuguese/script.rpy
        val tlScript = File(tlDir, "script.rpy")
        if (!tlScript.exists()) {
            tlScript.writeText(
                """
# Translation template generated for Portuguese
# OpenX AI Translation Engine

translate portuguese start_1:
    old "The heavy oak doors of Verdeluz Manor slammed shut with a thunderous echo behind you."
    new "As portas de carvalho maciço do Solar Verdeluz fecharam-se com um estrondo atrás de você."

translate portuguese start_2:
    old "Welcome, Detective. You finally arrived. Things have turned... quite unsettling since nightfall."
    new "Bem-vindo, detetive. Finalmente você chegou. As coisas ficaram... bastante inquietantes desde o anoitecer."

translate portuguese start_3:
    old "Tell me, Clara. Where did you discover the encrypted manuscript?"
    new "Diga-me, Clara. Onde você descobriu o manuscrito encriptado?"

translate portuguese start_4:
    old "In the secret archives, right behind the Count's portrait. But there were fresh muddy footprints on the Persian rug."
    new "Nos arquivos secretos, logo atrás do retrato do Conde. Mas havia pegadas frescas de lama sobre o tapete persa."

translate portuguese choice_1:
    old "Investigate the secret archives immediately"
    new "Investigar os arquivos secretos imediatamente"

translate portuguese choice_2:
    old "Interrogate Clara about the Count's disappearance"
    new "Interrogar Clara sobre o desaparecimento do Conde"

translate portuguese choice_3:
    old "Examine the heavy entrance lock"
    new "Examinar a tranca pesada da entrada"
                """.trimIndent()
            )
        }

        val totalSize = demoDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

        val entity = RenpyGameEntity(
            title = "O Mistério de Verdeluz (Demo)",
            version = "8.2.3",
            gameDir = demoDir.absolutePath,
            coverPath = null,
            estimatedEngineVersion = "Ren'Py 8.x Oficial (Python 3.11 ARM64)",
            confidenceScore = 98,
            selectedEngineVersion = "Ren'Py 8.x",
            totalSizeBytes = totalSize,
            availableLanguages = "Inglês (Original), Português (TL)",
            compatibilityStatus = "COMPATIBLE",
            compatibilityDetails = "Jogo de demonstração Ren'Py 8 oficial pronto para execução, testes de tradução e gerenciamento de saves.",
            saveCount = 0,
            hasPortugueseTl = true,
            isBuiltInDemo = true
        )

        gameDao.insertGame(entity)
    }
}
