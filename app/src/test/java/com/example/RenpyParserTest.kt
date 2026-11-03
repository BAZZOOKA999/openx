package com.example

import com.example.openx.data.db.entities.BlockType
import com.example.openx.data.db.entities.TranslationUnitEntity
import com.example.openx.engine.androidres.AndroidResParser
import com.example.openx.engine.renpy.RenpyExporter
import com.example.openx.engine.renpy.RenpyParser
import com.example.openx.engine.renpy.RenpyValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RenpyParserTest {

    @Test
    fun `test renpy script parsing dialogue tags placeholders and commands`() {
        val script = """
# Test script
label start:
    scene bg fantasy_castle with fade
    play music "theme.ogg"
    
    eileen "Hello, [player_name]! Welcome to {b}OpenX{/b}."
    eileen "Notice the {i}ancient magic{/i} in this room."
    
    menu:
        "Help Eileen with the ritual":
            jump ritual
        "Inspect the dungeon":
            jump dungeon

    extend " May fortune favor us!"
        """.trimIndent()

        val result = RenpyParser.parse(script)
        assertEquals(5, result.items.size)

        // Verify first dialogue
        val item0 = result.items[0]
        assertEquals("eileen", item0.speaker)
        assertEquals("Hello, [player_name]! Welcome to {b}OpenX{/b}.", item0.originalText)
        assertEquals(BlockType.DIALOGUE, item0.blockType)

        // Verify menu items
        val menu0 = result.items[2]
        assertEquals("[Menu]", menu0.speaker)
        assertEquals("Help Eileen with the ritual", menu0.originalText)
        assertEquals(BlockType.MENU, menu0.blockType)

        // Verify extend dialogue
        val extendItem = result.items[4]
        assertEquals("extend", extendItem.speaker)
        assertEquals("May fortune favor us!", extendItem.originalText.trim())
    }

    @Test
    fun `test validator detects missing placeholders and unbalanced tags`() {
        val original = "Welcome back [hero]! Click {b}here{/b} for details."
        
        // Good translation
        val goodTrans = "Bem-vindo de volta [hero]! Clique {b}aqui{/b} para detalhes."
        val resultGood = RenpyValidator.validateTranslation(original, goodTrans)
        assertTrue(resultGood.isValid)

        // Missing placeholder
        val missingPlaceholder = "Bem-vindo de volta! Clique {b}aqui{/b} para detalhes."
        val resultMissing = RenpyValidator.validateTranslation(original, missingPlaceholder)
        assertFalse(resultMissing.isValid)
        assertTrue(resultMissing.missingPlaceholders.contains("[hero]"))

        // Unbalanced tag (missing closing tag)
        val unbalanced = "Bem-vindo de volta [hero]! Clique {b}aqui para detalhes."
        val resultUnbalanced = RenpyValidator.validateTranslation(original, unbalanced)
        assertFalse(resultUnbalanced.isValid)
    }

    @Test
    fun `test renpy exporter generates valid strings block`() {
        val units = listOf(
            TranslationUnitEntity(
                projectId = 1,
                fileId = 1,
                orderIndex = 0,
                speaker = "eileen",
                originalText = "Hello world!",
                translatedText = "Olá mundo!"
            ),
            TranslationUnitEntity(
                projectId = 1,
                fileId = 1,
                orderIndex = 1,
                speaker = "",
                originalText = "Press start",
                translatedText = "Pressione iniciar"
            )
        )

        val exported = RenpyExporter.exportToRenpy("", units, "portuguese")
        assertTrue(exported.contains("translate portuguese strings:"))
        assertTrue(exported.contains("old \"Hello world!\""))
        assertTrue(exported.contains("new \"Olá mundo!\""))
        assertTrue(exported.contains("old \"Press start\""))
        assertTrue(exported.contains("new \"Pressione iniciar\""))
    }

    @Test
    fun `test android strings xml parsing and export`() {
        val xml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="title">Epic Quest</string>
    <string name="points">Player has %1${'$'}d points</string>
</resources>""".trimIndent()

        val parsed = AndroidResParser.parseStringsXml(xml)
        assertEquals(2, parsed.size)
        assertEquals("title", parsed[0].key)
        assertEquals("Epic Quest", parsed[0].originalText)
        assertEquals("points", parsed[1].key)

        val units = listOf(
            TranslationUnitEntity(
                projectId = 1,
                fileId = 1,
                keyOrContext = "title",
                originalText = "Epic Quest",
                translatedText = "Missão Épica",
                blockType = BlockType.XML_STRING
            )
        )
        val exportedXml = AndroidResParser.exportStringsXml(units)
        assertTrue(exportedXml.contains("<string name=\"title\">Missão Épica</string>"))
    }

    @Test
    fun `test character glossary entity attributes and role preservation`() {
        val characterGlossary = com.example.openx.data.db.entities.GlossaryEntity(
            projectId = 1,
            sourceTerm = "Lady Sylphia",
            targetTerm = "Lady Sylphia",
            doNotTranslate = true,
            characterRole = "Main Sorceress",
            gender = "Feminino",
            relationship = "Mentora do jogador",
            personality = "Altiva e protetora",
            speechStyle = "Formal e arcaico",
            category = "Personagens"
        )

        assertEquals("Lady Sylphia", characterGlossary.sourceTerm)
        assertTrue(characterGlossary.doNotTranslate)
        assertEquals("Feminino", characterGlossary.gender)
        assertEquals("Mentora do jogador", characterGlossary.relationship)
        assertEquals("Formal e arcaico", characterGlossary.speechStyle)
    }

    @Test
    fun `test translation memory hash computation is deterministic and case-normalized`() {
        fun computeHash(text: String, src: String, tgt: String): String {
            val input = "${text.trim().lowercase()}|$src|$tgt"
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }.take(32)
        }

        val hash1 = computeHash("Save Game", "en", "pt-BR")
        val hash2 = computeHash("  save game  ", "en", "pt-BR")
        val hashDifferentLang = computeHash("Save Game", "en", "es")

        assertEquals(hash1, hash2)
        org.junit.Assert.assertNotEquals(hash1, hashDifferentLang)
    }

    @Test
    fun `test renpy multiline dialogue and escape preservation`() {
        val script = """
label chapter2:
    "First line with \"escaped quotes\" inside."
    eileen "Line with tags {color=#ff0000}Red Text{/color} and variable %(player)s."
    $ affection_points += 10
    python:
        renpy.notify("Affection increased!")
    eileen "Thank you so much!"
        """.trimIndent()

        val parsed = RenpyParser.parse(script)
        // Python lines starting with $ or within python block should not be parsed as translatable dialogue
        assertEquals(3, parsed.items.size)
        assertEquals("First line with \"escaped quotes\" inside.", parsed.items[0].originalText)
        assertEquals("Line with tags {color=#ff0000}Red Text{/color} and variable %(player)s.", parsed.items[1].originalText)
        assertEquals("Thank you so much!", parsed.items[2].originalText)

        // Validate placeholders and tags
        val valResult = RenpyValidator.validateTranslation(
            parsed.items[1].originalText,
            "Linha com tags {color=#ff0000}Texto Vermelho{/color} e variável %(player)s."
        )
        assertTrue(valResult.isValid)
    }

    @Test
    fun `test offline rule dictionary translations for standard VN menus`() {
        val engine = com.example.openx.engine.translation.TranslationEngine(null)
        val translated = engine.applyOfflineRuleTranslation("Start Game", "en", "pt-BR")
        assertEquals("Iniciar Jogo", translated)

        val translated2 = engine.applyOfflineRuleTranslation("Preferences", "en", "pt-BR")
        assertEquals("Configurações", translated2)

        val translated3 = engine.applyOfflineRuleTranslation("Good morning", "en", "pt-BR")
        assertEquals("Bom dia", translated3)
    }
}
