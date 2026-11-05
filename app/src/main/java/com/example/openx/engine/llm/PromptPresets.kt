package com.example.openx.engine.llm

import com.example.openx.data.db.entities.GlossaryEntity

enum class PromptPresetType(
    val title: String,
    val description: String,
    val defaultInstruction: String
) {
    NATURAL(
        "Natural (Visual Novel)",
        "Adapta gírias, expressividade coloquial e ritmo fluido em pt-BR sem soar tradução automática.",
        "Traduza o diálogo a seguir para português brasileiro com naturalidade expressiva de Visual Novel. Adapte gírias e expressões coloquiais. Mantenha intactos todos os códigos entre chaves como {b}, {color}, {i}, e variáveis entre colchetes como [player] ou com porcentagem como %(name)s."
    ),
    LITERAL(
        "Literal (Fiel ao Original)",
        "Tradução palavra por palavra rigorosa, mantendo a estrutura exata das frases e termos.",
        "Traduza com fidelidade literal estrita para o português brasileiro. Não altere a ordem ou estilo sintático. Preserve rigorosamente todos os identificadores, variáveis e tags Ren'Py."
    ),
    NARRATIVE(
        "Narrativo (Literário & Dramático)",
        "Foco em descrições de cenários, pensamentos internos, clima e tom literário.",
        "Traduza a narração a seguir com riqueza literária, vocabulário expressivo e tom dramático para visual novel em português brasileiro. Preserve todas as tags de formatação e nomes próprios."
    ),
    DIALOGUE(
        "Diálogo Dinâmico",
        "Ritmo de fala rápido, interjeições naturais e distinção forte da voz de cada personagem.",
        "Traduza o diálogo falado preservando o estilo, emoção e tom do personagem falante. Evite construções formais arcaicas em diálogos cotidianos. Mantenha todas as tags intactas."
    ),
    UI(
        "Interface, Botões e Menus",
        "Textos curtos, concisos e sem ambiguidades, adequados para telas de jogo e botões.",
        "Traduza o texto de interface ou item de menu a seguir de forma concisa e direta para português brasileiro, respeitando limitações de espaço na interface. Não adicione pontuação desnecessária."
    ),
    REVIEW_POLISH(
        "Revisão e Polimento",
        "Aprimora traduções já existentes corrigindo concordância, gramática e estilo.",
        "Revise e aprimore o texto traduzido fornecido, tornando-o mais elegante e gramaticalmente perfeito em português brasileiro, mantendo exatamente o mesmo significado e todas as tags."
    )
}

data class LlmInferenceParams(
    val preset: PromptPresetType = PromptPresetType.NATURAL,
    val customInstruction: String = "",
    val temperature: Float = 0.3f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 256,
    val nThreads: Int = 4,
    val localServerUrl: String = "http://127.0.0.1:8080/v1",
    val enableSequentialReview: Boolean = false,
    val reviewModelId: Long? = null,
    val reviewTemperature: Float = 0.2f,
    val reviewPreset: PromptPresetType = PromptPresetType.REVIEW_POLISH
) {
    fun getEffectiveInstruction(): String {
        return if (customInstruction.isNotBlank()) customInstruction else preset.defaultInstruction
    }
}

object PromptBuilder {

    fun buildTranslationPrompt(
        originalText: String,
        speaker: String,
        contextInfo: String,
        params: LlmInferenceParams,
        glossary: List<GlossaryEntity>,
        targetLang: String = "pt-BR"
    ): String {
        val sb = StringBuilder()
        sb.append("<|im_start|>system\n")
        sb.append(params.getEffectiveInstruction()).append("\n")
        sb.append("Idioma de destino: ").append(targetLang).append(".\n")
        sb.append("REGRAS ESTRITAS DE PRESERVAÇÃO:\n")
        sb.append("1. NUNCA traduza comandos Ren'Py (ex: scene, show, hide, play, with).\n")
        sb.append("2. NUNCA altere variáveis ou placeholders como [player], %(name)s, %s, {b}, {/b}, {i}, {color=...}, {size=...}.\n")
        sb.append("3. Retorne APENAS a tradução direta do texto, sem introduções, aspas extras ou explicações.\n")

        if (speaker.isNotBlank()) {
            sb.append("\nFalante/Personagem: ").append(speaker)
            val charGlossary = glossary.firstOrNull { it.sourceTerm.equals(speaker, ignoreCase = true) }
            if (charGlossary != null) {
                if (charGlossary.characterRole.isNotBlank()) sb.append(" (Papel: ${charGlossary.characterRole})")
                if (charGlossary.personality.isNotBlank()) sb.append(" [Personalidade: ${charGlossary.personality}]")
                if (charGlossary.speechStyle.isNotBlank()) sb.append(" [Estilo de fala: ${charGlossary.speechStyle}]")
            }
            sb.append(".\n")
        }

        val matchingGlossary = glossary.filter { item ->
            originalText.contains(item.sourceTerm, ignoreCase = true)
        }
        if (matchingGlossary.isNotEmpty()) {
            sb.append("\nGLOSSÁRIO OBRIGATÓRIO (use estas traduções exatas):\n")
            matchingGlossary.forEach { g ->
                sb.append("- \"").append(g.sourceTerm).append("\" -> \"").append(g.targetTerm).append("\"")
                if (g.notes.isNotBlank()) sb.append(" (").append(g.notes).append(")")
                sb.append("\n")
            }
        }

        if (contextInfo.isNotBlank()) {
            sb.append("\nContexto da cena: ").append(contextInfo).append("\n")
        }

        sb.append("<|im_end|>\n")
        sb.append("<|im_start|>user\n")
        sb.append(originalText).append("\n")
        sb.append("<|im_end|>\n")
        sb.append("<|im_start|>assistant\n")

        return sb.toString()
    }

    fun buildReviewPrompt(
        originalText: String,
        draftTranslation: String,
        speaker: String,
        glossary: List<GlossaryEntity>,
        targetLang: String = "pt-BR"
    ): String {
        val sb = StringBuilder()
        sb.append("<|im_start|>system\n")
        sb.append("Você é um revisor linguístico profissional especializado em jogos e visual novels.\n")
        sb.append("Sua função é polir e aprimorar a tradução em ").append(targetLang).append(" mantendo concordância perfeita, expressividade e naturalidade.\n")
        sb.append("REGRAS ESTRITAS DE PRESERVAÇÃO:\n")
        sb.append("1. NUNCA remova ou altere variáveis ou tags como [player], %(name)s, %s, {b}, {/b}, {i}, {color=...}.\n")
        sb.append("2. Preserve comandos Ren'Py e termos protegidos.\n")
        sb.append("3. Retorne APENAS a versão polida final, sem explicações ou comentários adicionais.\n")

        if (speaker.isNotBlank()) {
            sb.append("\nFalante: ").append(speaker).append("\n")
        }

        val matchingGlossary = glossary.filter { item ->
            originalText.contains(item.sourceTerm, ignoreCase = true)
        }
        if (matchingGlossary.isNotEmpty()) {
            sb.append("\nGLOSSÁRIO OBRIGATÓRIO (preserve estas palavras exatas):\n")
            matchingGlossary.forEach { g ->
                sb.append("- \"").append(g.sourceTerm).append("\" -> \"").append(g.targetTerm).append("\"\n")
            }
        }

        sb.append("<|im_end|>\n")
        sb.append("<|im_start|>user\n")
        sb.append("Original: ").append(originalText).append("\n")
        sb.append("Tradução Inicial: ").append(draftTranslation).append("\n")
        sb.append("Por favor, refine o texto traduzido mantendo o significado exato e todas as tags.\n")
        sb.append("<|im_end|>\n")
        sb.append("<|im_start|>assistant\n")

        return sb.toString()
    }
}
