package com.example.openx.engine.renpy

import com.example.openx.data.db.entities.BlockType
import com.example.openx.data.db.entities.TranslationUnitEntity

object RenpyExporter {

    fun exportToRenpy(
        originalContent: String,
        units: List<TranslationUnitEntity>,
        targetLang: String = "portuguese"
    ): String {
        val sanitizedLang = targetLang.lowercase().replace("-", "_").trim()
        val isTemplate = originalContent.contains(Regex("^\\s*translate\\s+", RegexOption.MULTILINE))

        if (isTemplate) {
            return exportUpdatedTemplate(originalContent, units)
        } else {
            return exportGeneratedStrings(units, sanitizedLang)
        }
    }

    private fun exportUpdatedTemplate(
        originalContent: String,
        units: List<TranslationUnitEntity>
    ): String {
        val lines = originalContent.lines().toMutableList()
        // Map unit by original line index or order
        val unitMapByLine = units.associateBy { it.lineIndex }
        val unitMapByOriginal = units.groupBy { it.originalText }

        val resultLines = mutableListOf<String>()

        var inTranslateBlock = false
        var currentBlockUnit: TranslationUnitEntity? = null

        for ((idx, line) in lines.withIndex()) {
            val lineNum = idx + 1
            val trimmed = line.trim()

            if (trimmed.startsWith("translate ")) {
                inTranslateBlock = true
            }

            // If this is a new "..." line in strings block
            if (trimmed.startsWith("new \"")) {
                // Find matching unit from previous old line
                val prevLine = lines.getOrNull(idx - 1)?.trim()
                if (prevLine != null && prevLine.startsWith("old \"")) {
                    val oldText = prevLine.removePrefix("old \"").removeSuffix("\"").trim()
                    val unescaped = RenpyParser.unescapeRenpyString(oldText)
                    val matchingUnit = unitMapByOriginal[unescaped]?.firstOrNull()
                    if (matchingUnit != null && matchingUnit.translatedText.isNotBlank()) {
                        val indent = line.takeWhile { it.isWhitespace() }
                        val escapedTrans = RenpyParser.escapeRenpyString(matchingUnit.translatedText)
                        resultLines.add("${indent}new \"$escapedTrans\"")
                        continue
                    }
                }
            }

            // Dialogue line inside translate block
            val directUnit = unitMapByLine[lineNum]
            if (directUnit != null && directUnit.translatedText.isNotBlank() && directUnit.blockType == BlockType.DIALOGUE) {
                val indent = line.takeWhile { it.isWhitespace() }
                val escapedTrans = RenpyParser.escapeRenpyString(directUnit.translatedText)
                val speakerPrefix = if (directUnit.speaker.isNotBlank()) "${directUnit.speaker} " else ""
                resultLines.add("$indent$speakerPrefix\"$escapedTrans\"")
                continue
            }

            resultLines.add(line)
        }

        return resultLines.joinToString("\n")
    }

    private fun exportGeneratedStrings(
        units: List<TranslationUnitEntity>,
        targetLang: String
    ): String {
        val sb = StringBuilder()
        sb.append("# TODO: Translation updated by OpenX Translation Suite\n")
        sb.append("# Target Language: $targetLang\n\n")

        sb.append("translate $targetLang strings:\n\n")

        for (unit in units) {
            val originalEscaped = RenpyParser.escapeRenpyString(unit.originalText)
            val translatedEscaped = if (unit.translatedText.isNotBlank()) {
                RenpyParser.escapeRenpyString(unit.translatedText)
            } else {
                originalEscaped
            }

            if (unit.speaker.isNotBlank() && unit.speaker != "[Menu]") {
                sb.append("    # Speaker: ${unit.speaker}\n")
            }
            sb.append("    old \"$originalEscaped\"\n")
            sb.append("    new \"$translatedEscaped\"\n\n")
        }

        return sb.toString()
    }
}
