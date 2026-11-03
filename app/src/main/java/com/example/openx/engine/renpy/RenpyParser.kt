package com.example.openx.engine.renpy

import com.example.openx.data.db.entities.BlockType
import com.example.openx.data.db.entities.TranslationStatus
import com.example.openx.data.db.entities.TranslationUnitEntity

object RenpyParser {

    data class ParsedRenpyItem(
        val orderIndex: Int,
        val lineIndex: Int,
        val speaker: String,
        val keyOrContext: String,
        val originalText: String,
        val translatedText: String,
        val blockType: BlockType,
        val status: TranslationStatus,
        val rawTemplateLine: String? = null
    )

    data class ParseResult(
        val items: List<ParsedRenpyItem>,
        val isOfficialTemplate: Boolean,
        val detectedLanguage: String?,
        val totalLines: Int,
        val notes: String
    )

    // Regex patterns
    // Dialogue line: optional speaker tokens, then a quoted string
    // e.g. eileen "Hello!" or eileen happy "Hello!" or "Narrator text"
    private val DIALOGUE_REGEX = Regex("^(\\s*)(?:([a-zA-Z0-9_]+)(?:\\s+[a-zA-Z0-9_]+)*\\s+)?\"((?:[^\"\\\\]|\\\\.)*)\"\\s*$")
    
    // Menu choice line: e.g. "Go to the forest":
    private val MENU_CHOICE_REGEX = Regex("^(\\s*)\"((?:[^\"\\\\]|\\\\.)*)\"\\s*:\\s*$")

    // Official translation header: translate <language> <identifier>:
    private val TRANSLATE_BLOCK_REGEX = Regex("^\\s*translate\\s+([a-zA-Z0-9_]+)\\s+([a-zA-Z0-9_]+)\\s*:\\s*$")
    
    // Official translation strings header: translate <language> strings:
    private val TRANSLATE_STRINGS_REGEX = Regex("^\\s*translate\\s+([a-zA-Z0-9_]+)\\s+strings\\s*:\\s*$")

    // Old string line: old "..."
    private val OLD_STRING_REGEX = Regex("^(\\s*)old\\s+\"((?:[^\"\\\\]|\\\\.)*)\"\\s*$")

    // New string line: new "..."
    private val NEW_STRING_REGEX = Regex("^(\\s*)new\\s+\"((?:[^\"\\\\]|\\\\.)*)\"\\s*$")

    // Commented dialogue in official translation template: # eileen "Original text"
    private val COMMENTED_ORIGINAL_REGEX = Regex("^\\s*#\\s*(?:([a-zA-Z0-9_]+)(?:\\s+[a-zA-Z0-9_]+)*\\s+)?\"((?:[^\"\\\\]|\\\\.)*)\"\\s*$")

    // Commands to strictly ignore / never treat as dialogue
    private val RENPY_COMMANDS = setOf(
        "scene", "show", "hide", "with", "play", "stop", "queue", "voice",
        "jump", "call", "return", "label", "define", "default", "init",
        "python", "style", "transform", "screen", "image", "window", "pause",
        "renpy", "pass", "nvl", "if", "elif", "else", "while", "for"
    )

    fun parse(content: String): ParseResult {
        val lines = content.lines()
        val items = mutableListOf<ParsedRenpyItem>()

        var isOfficialTemplate = false
        var detectedLang: String? = null

        var currentTranslateBlockId: String? = null
        var inStringsBlock = false
        var lastCommentedOriginal: String? = null
        var lastCommentedSpeaker: String? = null
        var pendingOldString: String? = null
        var pendingOldLineIndex: Int = 0

        var orderIndex = 0

        for ((index, rawLine) in lines.withIndex()) {
            val lineNum = index + 1
            val trimmed = rawLine.trim()

            // Check translate strings:
            val stringsMatch = TRANSLATE_STRINGS_REGEX.matchEntire(rawLine)
            if (stringsMatch != null) {
                isOfficialTemplate = true
                detectedLang = stringsMatch.groupValues[1]
                inStringsBlock = true
                currentTranslateBlockId = null
                lastCommentedOriginal = null
                continue
            }

            // Check translate <lang> <identifier>:
            val translateBlockMatch = TRANSLATE_BLOCK_REGEX.matchEntire(rawLine)
            if (translateBlockMatch != null) {
                isOfficialTemplate = true
                detectedLang = translateBlockMatch.groupValues[1]
                inStringsBlock = false
                currentTranslateBlockId = translateBlockMatch.groupValues[2]
                lastCommentedOriginal = null
                continue
            }

            // In official template strings block: old "..." and new "..."
            if (inStringsBlock) {
                val oldMatch = OLD_STRING_REGEX.matchEntire(rawLine)
                if (oldMatch != null) {
                    pendingOldString = unescapeRenpyString(oldMatch.groupValues[2])
                    pendingOldLineIndex = lineNum
                    continue
                }

                val newMatch = NEW_STRING_REGEX.matchEntire(rawLine)
                if (newMatch != null && pendingOldString != null) {
                    val translatedText = unescapeRenpyString(newMatch.groupValues[2])
                    val status = if (translatedText.isNotBlank() && translatedText != pendingOldString) {
                        TranslationStatus.TRANSLATED
                    } else {
                        TranslationStatus.PENDING
                    }

                    items.add(
                        ParsedRenpyItem(
                            orderIndex = orderIndex++,
                            lineIndex = pendingOldLineIndex,
                            speaker = "",
                            keyOrContext = "strings:${detectedLang ?: "tl"}",
                            originalText = pendingOldString,
                            translatedText = translatedText,
                            blockType = BlockType.OLD_NEW,
                            status = status
                        )
                    )
                    pendingOldString = null
                    continue
                }
            }

            // Inside translate <lang> <identifier>: block
            if (currentTranslateBlockId != null) {
                val commentMatch = COMMENTED_ORIGINAL_REGEX.matchEntire(rawLine)
                if (commentMatch != null) {
                    lastCommentedSpeaker = commentMatch.groupValues[1].ifBlank { "" }
                    lastCommentedOriginal = unescapeRenpyString(commentMatch.groupValues[2])
                    continue
                }

                val dialogueMatch = DIALOGUE_REGEX.matchEntire(rawLine)
                if (dialogueMatch != null && !isCommand(rawLine)) {
                    val speaker = dialogueMatch.groupValues[2].ifBlank { lastCommentedSpeaker ?: "" }
                    val currentDialog = unescapeRenpyString(dialogueMatch.groupValues[3])

                    val original = lastCommentedOriginal ?: currentDialog
                    val translated = if (lastCommentedOriginal != null) currentDialog else ""
                    val status = if (translated.isNotBlank() && translated != original) {
                        TranslationStatus.TRANSLATED
                    } else {
                        TranslationStatus.PENDING
                    }

                    items.add(
                        ParsedRenpyItem(
                            orderIndex = orderIndex++,
                            lineIndex = lineNum,
                            speaker = speaker,
                            keyOrContext = "translate:$currentTranslateBlockId",
                            originalText = original,
                            translatedText = translated,
                            blockType = BlockType.DIALOGUE,
                            status = status,
                            rawTemplateLine = rawLine
                        )
                    )
                    lastCommentedOriginal = null
                    lastCommentedSpeaker = null
                    continue
                }
            }

            // Standard script parsing (when not in a translate block or parsing raw script.rpy)
            if (trimmed.startsWith("#") || trimmed.startsWith("$") || isCommand(trimmed)) {
                continue
            }

            // Check menu choice
            val menuMatch = MENU_CHOICE_REGEX.matchEntire(rawLine)
            if (menuMatch != null) {
                val menuText = unescapeRenpyString(menuMatch.groupValues[2])
                items.add(
                    ParsedRenpyItem(
                        orderIndex = orderIndex++,
                        lineIndex = lineNum,
                        speaker = "[Menu]",
                        keyOrContext = "menu:$lineNum",
                        originalText = menuText,
                        translatedText = "",
                        blockType = BlockType.MENU,
                        status = TranslationStatus.PENDING
                    )
                )
                continue
            }

            // Check standard dialogue
            val dialogueMatch = DIALOGUE_REGEX.matchEntire(rawLine)
            if (dialogueMatch != null) {
                val rawSpeaker = dialogueMatch.groupValues[2]
                if (rawSpeaker.isNotBlank() && RENPY_COMMANDS.contains(rawSpeaker.lowercase())) {
                    continue
                }

                val dialogueText = unescapeRenpyString(dialogueMatch.groupValues[3])
                if (dialogueText.isNotBlank()) {
                    items.add(
                        ParsedRenpyItem(
                            orderIndex = orderIndex++,
                            lineIndex = lineNum,
                            speaker = rawSpeaker,
                            keyOrContext = "dialogue:$lineNum",
                            originalText = dialogueText,
                            translatedText = "",
                            blockType = BlockType.DIALOGUE,
                            status = TranslationStatus.PENDING
                        )
                    )
                }
            }
        }

        val notes = if (isOfficialTemplate) {
            "Modelo oficial de tradução Ren'Py detectado (${detectedLang ?: "idioma padrão"}). Preservação completa de identificadores."
        } else {
            "Script base Ren'Py detectado. Na exportação, serão gerados blocos de tradução 'translate strings' e/ou scripts tl correspondentes."
        }

        return ParseResult(
            items = items,
            isOfficialTemplate = isOfficialTemplate,
            detectedLanguage = detectedLang,
            totalLines = lines.size,
            notes = notes
        )
    }

    private fun isCommand(line: String): Boolean {
        val trimmed = line.trim()
        val firstToken = trimmed.split(Regex("\\s+")).firstOrNull()?.lowercase() ?: return false
        return RENPY_COMMANDS.contains(firstToken) || trimmed.startsWith("$") || trimmed.startsWith("python:")
    }

    fun unescapeRenpyString(s: String): String {
        return s.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\t", "\t")
            .replace("\\'", "'")
    }

    fun escapeRenpyString(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
    }

    fun toTranslationUnitEntities(
        items: List<ParsedRenpyItem>,
        projectId: Long,
        fileId: Long
    ): List<TranslationUnitEntity> {
        return items.map { item ->
            val placeholders = RenpyValidator.extractPlaceholders(item.originalText)
            val tags = RenpyValidator.extractTags(item.originalText)

            val validation = if (item.translatedText.isNotBlank()) {
                RenpyValidator.validateTranslation(item.originalText, item.translatedText).errorMessage
            } else null

            TranslationUnitEntity(
                projectId = projectId,
                fileId = fileId,
                orderIndex = item.orderIndex,
                lineIndex = item.lineIndex,
                speaker = item.speaker,
                keyOrContext = item.keyOrContext,
                originalText = item.originalText,
                translatedText = item.translatedText,
                status = item.status,
                blockType = item.blockType,
                placeholdersJson = placeholders.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]").let { if (placeholders.isEmpty()) "[]" else it },
                tagsJson = tags.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]").let { if (tags.isEmpty()) "[]" else it },
                validationError = validation
            )
        }
    }
}
