package com.example.openx.data

import com.example.openx.data.db.OpenXDatabase
import com.example.openx.data.db.entities.BlockType
import com.example.openx.data.db.entities.FileItemEntity
import com.example.openx.data.db.entities.FileType
import com.example.openx.data.db.entities.GlossaryEntity
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.data.db.entities.ProjectType
import com.example.openx.data.db.entities.TranslationStatus
import com.example.openx.data.db.entities.TranslationUnitEntity
import com.example.openx.data.db.entities.UserSessionEntity
import com.example.openx.engine.renpy.RenpyParser

object SampleDataGenerator {

    suspend fun populateInitialDataIfEmpty(db: OpenXDatabase) {
        // Ensure session exists
        val session = db.userSessionDao().getSessionSync()
        if (session == null) {
            db.userSessionDao().saveSession(
                UserSessionEntity(
                    userId = "offline_user",
                    displayName = "Tradutor OpenX",
                    email = "offline@openx.local",
                    isOfflineMode = true,
                    preferredLanguage = "pt-BR"
                )
            )
        }

        val count = db.projectDao().getProjectsCount()
        if (count == 0) {
            createRenpyDemoProject(db)
            createAndroidResDemoProject(db)
        }
    }

    suspend fun createRenpyDemoProject(db: OpenXDatabase): Long {
        val project = ProjectEntity(
            name = "The Forgotten Realm (Ren'Py Demo)",
            type = ProjectType.RENPY,
            sourceLang = "en",
            targetLang = "pt-BR",
            totalUnits = 8,
            translatedUnits = 3,
            reviewedUnits = 1,
            errorUnits = 0,
            isDemo = true,
            notes = "Projeto exemplo oficial de Visual Novel Ren'Py com tags {b}, variáveis [player_name] e escolhas menu:"
        )
        val projId = db.projectDao().insertProject(project)

        val renpyScript = """
# Script de exemplo para demonstração do OpenX
label start:
    scene bg fantasy_castle with fade
    play music "theme.ogg"
    
    eileen "Welcome back, [player_name]! We've been waiting for you."
    eileen "The mystical seals on the {b}Ancient Citadel{/b} are fading rapidly."
    
    menu:
        "Help Eileen examine the runes":
            jump runes
        "Venture into the dark forest alone":
            jump forest

label runes:
    eileen "Look at this inscription... It whispers of {i}ancient sorrow{/i}."
    extend " We must decode it before midnight strikes!"
    return

translate portuguese strings:
    # game/script.rpy:15
    old "New Game"
    new "Novo Jogo"
    
    # game/script.rpy:18
    old "Continue Adventure"
    new ""
""".trimIndent()

        val fileItem = FileItemEntity(
            projectId = projId,
            relativePath = "game/script.rpy",
            fileName = "script.rpy",
            fileType = FileType.RPY,
            fileSize = renpyScript.length.toLong(),
            totalUnits = 8,
            translatedUnits = 3,
            rawContent = renpyScript
        )
        val fileId = db.translationDao().insertFile(fileItem)

        val parseResult = RenpyParser.parse(renpyScript)
        val units = RenpyParser.toTranslationUnitEntities(parseResult.items, projId, fileId).toMutableList()

        // Pre-fill some translations to show real progress
        val updatedUnits = units.mapIndexed { idx, unit ->
            when (idx) {
                0 -> unit.copy(
                    translatedText = "Bem-vindo de volta, [player_name]! Estávamos esperando por você.",
                    status = TranslationStatus.REVIEWED
                )
                1 -> unit.copy(
                    translatedText = "Os selos místicos na {b}Cidadela Antiga{/b} estão desaparecendo rapidamente.",
                    status = TranslationStatus.TRANSLATED
                )
                6 -> unit.copy(
                    translatedText = "Novo Jogo",
                    status = TranslationStatus.TRANSLATED
                )
                else -> unit
            }
        }
        db.translationDao().insertUnits(updatedUnits)

        // Seed some demo glossary entries
        val glossary = listOf(
            GlossaryEntity(projectId = projId, sourceTerm = "Ancient Citadel", targetTerm = "Cidadela Antiga", category = "Lugar"),
            GlossaryEntity(projectId = projId, sourceTerm = "Eileen", targetTerm = "Eileen", doNotTranslate = true, category = "Personagem"),
            GlossaryEntity(projectId = projId, sourceTerm = "Ren'Py", targetTerm = "Ren'Py", doNotTranslate = true, category = "Termo")
        )
        db.glossaryDao().insertItems(glossary)

        return projId
    }

    suspend fun createAndroidResDemoProject(db: OpenXDatabase): Long {
        val project = ProjectEntity(
            name = "Galaxy Quest (Android Strings Demo)",
            type = ProjectType.ANDROID_RES,
            sourceLang = "en",
            targetLang = "pt-BR",
            totalUnits = 5,
            translatedUnits = 2,
            reviewedUnits = 1,
            errorUnits = 0,
            isDemo = true,
            notes = "Projeto exemplo de recursos Android strings.xml com parâmetros posicionais %1\$s"
        )
        val projId = db.projectDao().insertProject(project)

        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Galaxy Quest</string>
    <string name="welcome_captain">Welcome aboard the starship, %1${'$'}s!</string>
    <string name="shields_critical">Warning! Shields are at %1${'$'}d percent capacity!</string>
    <string name="engage_hyperdrive">Engage Hyperdrive</string>
    <string name="mission_accomplished">Mission accomplished successfully.</string>
</resources>""".trimIndent()

        val fileItem = FileItemEntity(
            projectId = projId,
            relativePath = "res/values/strings.xml",
            fileName = "strings.xml",
            fileType = FileType.XML,
            fileSize = xmlContent.length.toLong(),
            totalUnits = 5,
            translatedUnits = 2,
            rawContent = xmlContent
        )
        val fileId = db.translationDao().insertFile(fileItem)

        val units = listOf(
            TranslationUnitEntity(
                projectId = projId,
                fileId = fileId,
                orderIndex = 0,
                speaker = "",
                keyOrContext = "app_name",
                originalText = "Galaxy Quest",
                translatedText = "Galaxy Quest",
                status = TranslationStatus.REVIEWED,
                blockType = BlockType.XML_STRING
            ),
            TranslationUnitEntity(
                projectId = projId,
                fileId = fileId,
                orderIndex = 1,
                speaker = "",
                keyOrContext = "welcome_captain",
                originalText = "Welcome aboard the starship, %1\$s!",
                translatedText = "Bem-vindo a bordo da nave estelar, %1\$s!",
                status = TranslationStatus.TRANSLATED,
                blockType = BlockType.XML_STRING
            ),
            TranslationUnitEntity(
                projectId = projId,
                fileId = fileId,
                orderIndex = 2,
                speaker = "",
                keyOrContext = "shields_critical",
                originalText = "Warning! Shields are at %1\$d percent capacity!",
                translatedText = "",
                status = TranslationStatus.PENDING,
                blockType = BlockType.XML_STRING
            ),
            TranslationUnitEntity(
                projectId = projId,
                fileId = fileId,
                orderIndex = 3,
                speaker = "",
                keyOrContext = "engage_hyperdrive",
                originalText = "Engage Hyperdrive",
                translatedText = "",
                status = TranslationStatus.PENDING,
                blockType = BlockType.XML_STRING
            ),
            TranslationUnitEntity(
                projectId = projId,
                fileId = fileId,
                orderIndex = 4,
                speaker = "",
                keyOrContext = "mission_accomplished",
                originalText = "Mission accomplished successfully.",
                translatedText = "",
                status = TranslationStatus.PENDING,
                blockType = BlockType.XML_STRING
            )
        )
        db.translationDao().insertUnits(units)

        return projId
    }
}
