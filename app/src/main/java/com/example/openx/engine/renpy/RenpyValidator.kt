package com.example.openx.engine.renpy

object RenpyValidator {

    // Regex for placeholders: [var_name], %(name)s, %(count)d, {variable}
    private val PLACEHOLDER_REGEX = Regex("\\[[a-zA-Z0-9_.]+]|%\\([a-zA-Z0-9_]+\\)[sdifx]|\\{[a-zA-Z0-9_]+}")
    
    // Regex for text formatting tags: {b}, {/b}, {i}, {/i}, {color=#fff}, {/color}, {size=20}, {/size}, {w=1.0}, {p=0.5}, {nw}, {cps=15}, etc.
    private val TAG_REGEX = Regex("\\{/?([a-zA-Z0-9_]+)(?:=[^}]+)?\\}")

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val missingPlaceholders: List<String> = emptyList(),
        val unbalancedTags: List<String> = emptyList()
    )

    fun extractPlaceholders(text: String): List<String> {
        return PLACEHOLDER_REGEX.findAll(text).map { it.value }.toList()
    }

    fun extractTags(text: String): List<String> {
        return TAG_REGEX.findAll(text).map { it.value }.toList()
    }

    fun validateTranslation(original: String, translated: String): ValidationResult {
        if (translated.isBlank()) {
            return ValidationResult(isValid = true)
        }

        val originalPlaceholders = extractPlaceholders(original).distinct()
        val translatedPlaceholders = extractPlaceholders(translated).toSet()

        val missingPlaceholders = originalPlaceholders.filter { it !in translatedPlaceholders }

        // Check tags balance
        val unbalanced = mutableListOf<String>()
        val tagPairs = listOf("b", "i", "u", "s", "color", "size", "alpha", "font", "outlinecolor")
        
        for (tag in tagPairs) {
            val openCount = Regex("\\{$tag(?:=[^}]*)?\\}").findAll(translated).count()
            val closeCount = Regex("\\{/$tag\\}").findAll(translated).count()
            if (openCount != closeCount) {
                unbalanced.add("Tag {$tag} desbalanceada: $openCount aberta(s), $closeCount fechada(s)")
            }
        }

        // Check for unclosed curly braces or brackets
        val openCurly = translated.count { it == '{' }
        val closeCurly = translated.count { it == '}' }
        if (openCurly != closeCurly) {
            unbalanced.add("Chaves '{' ou '}' não correspondidas no texto traduzido")
        }

        val openSquare = translated.count { it == '[' }
        val closeSquare = translated.count { it == ']' }
        if (openSquare != closeSquare) {
            unbalanced.add("Colchetes '[' ou ']' não correspondidos no texto traduzido")
        }

        val errors = mutableListOf<String>()
        if (missingPlaceholders.isNotEmpty()) {
            errors.add("Variáveis ausentes: ${missingPlaceholders.joinToString(", ")}")
        }
        if (unbalanced.isNotEmpty()) {
            errors.addAll(unbalanced)
        }

        return if (errors.isEmpty()) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(
                isValid = false,
                errorMessage = errors.joinToString(" | "),
                missingPlaceholders = missingPlaceholders,
                unbalancedTags = unbalanced
            )
        }
    }
}
