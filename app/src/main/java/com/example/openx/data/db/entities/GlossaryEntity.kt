package com.example.openx.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glossary_items")
data class GlossaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0, // 0 for global across all projects
    val sourceTerm: String,
    val targetTerm: String = "",
    val doNotTranslate: Boolean = false, // If true, term must remain untranslated
    val caseSensitive: Boolean = true,
    val category: String = "Geral", // e.g. "Personagem", "Item", "Lugar", "Termo"
    val characterRole: String = "", // e.g. "Protagonista", "NPC", "Vilão"
    val gender: String = "Não especificado", // "Masculino", "Feminino", "Neutro"
    val relationship: String = "", // e.g. "Irmã mais nova", "Rival", "Mestre"
    val personality: String = "", // e.g. "Tímida e meiga", "Sarcástico e impaciente"
    val speechStyle: String = "", // e.g. "Formal, usa 'Senhor'", "Usa gírias urbanas"
    val notes: String = ""
)
