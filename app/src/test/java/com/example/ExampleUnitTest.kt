package com.example

import com.example.openx.data.db.entities.GlossaryEntity
import com.example.openx.engine.llm.LlamaNative
import com.example.openx.engine.llm.LlmInferenceParams
import com.example.openx.engine.llm.PromptBuilder
import com.example.openx.engine.llm.PromptPresetType
import com.example.openx.engine.llm.SystemResourceUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testPromptBuilderPreservesRulesAndInjectsGlossary() {
        val glossary = listOf(
            GlossaryEntity(
                sourceTerm = "Eileen",
                targetTerm = "Eileen",
                characterRole = "Maga Real",
                personality = "Sábia e misteriosa",
                speechStyle = "Formal e cortês"
            ),
            GlossaryEntity(
                sourceTerm = "Soulstone",
                targetTerm = "Pedra da Alma"
            )
        )

        val params = LlmInferenceParams(preset = PromptPresetType.NATURAL)
        val prompt = PromptBuilder.buildTranslationPrompt(
            originalText = "Eileen handed over the Soulstone to [player_name].",
            speaker = "Eileen",
            contextInfo = "No salão principal do castelo",
            params = params,
            glossary = glossary,
            targetLang = "pt-BR"
        )

        // Check system prompt instructions
        assertTrue(prompt.contains("<|im_start|>system"))
        assertTrue(prompt.contains("[player_name]"))
        assertTrue(prompt.contains("Maga Real"))
        assertTrue(prompt.contains("Sábia e misteriosa"))
        assertTrue(prompt.contains("Pedra da Alma"))
        assertTrue(prompt.contains("<|im_start|>user"))
        assertTrue(prompt.contains("<|im_start|>assistant"))
    }

    @Test
    fun testSystemResourceUtilByteFormatting() {
        assertEquals("0 B", SystemResourceUtil.formatBytes(0))
        assertEquals("500 B", SystemResourceUtil.formatBytes(500))
        assertEquals("1 KB", SystemResourceUtil.formatBytes(1024))
        assertEquals("450 MB", SystemResourceUtil.formatBytes(450 * 1024 * 1024L))
        assertEquals("1.5 GB", SystemResourceUtil.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testLlamaNativeSafeInit() {
        // Must not crash or throw unhandled exceptions even if .so is missing in container
        assertNotNull(LlamaNative.isNativeLoaded)
        // If not loaded, error message must be present explaining the status
        if (!LlamaNative.isNativeLoaded) {
            assertNotNull(LlamaNative.nativeLoadError)
        }
    }

    @Test
    fun testRenpyEngineManagerRecommendations() {
        val eng8 = com.example.openx.player.engine.RenpyEngineManager.getRecommendedEngine("Ren'Py 8.3 (Python 3)")
        assertEquals("renpy_8", eng8.id)
        assertTrue(eng8.isBundled)

        val eng7 = com.example.openx.player.engine.RenpyEngineManager.getRecommendedEngine("Ren'Py 7.5 (Python 2)")
        assertEquals("renpy_7", eng7.id)

        val eng699 = com.example.openx.player.engine.RenpyEngineManager.getRecommendedEngine("Ren'Py 6.99 (Legado)")
        assertEquals("renpy_699", eng699.id)
    }

    @Test
    fun testRenpyEngineDetectorInspection() {
        val tempDir = java.io.File.createTempFile("renpy_test", "")
        tempDir.delete()
        tempDir.mkdirs()

        val gameDir = java.io.File(tempDir, "game")
        gameDir.mkdirs()

        // Write sample script and options
        java.io.File(gameDir, "script.rpy").writeText("label start:\n    \"Hello!\"\n")
        java.io.File(gameDir, "options.rpy").writeText("define config.name = \"Shadows of Mist\"\ndefine config.version = \"2.1\"\n")
        java.io.File(tempDir, "game.exe").writeText("MZ fake windows binary")

        val result = com.example.openx.player.engine.RenpyEngineDetector.inspectDirectory(tempDir)
        assertEquals("Shadows of Mist", result.title)
        assertEquals("2.1", result.version)
        assertEquals(2, result.rpyCount) // script.rpy and options.rpy
        assertTrue(result.hasWindowsBinaries)
        assertEquals("COMPATIBLE", result.compatibilityStatus)
        assertTrue(result.diagnosticSummary.contains("Aviso Seguro"))

        tempDir.deleteRecursively()
    }
}
