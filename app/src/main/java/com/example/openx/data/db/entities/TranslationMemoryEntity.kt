package com.example.openx.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "translation_memory",
    indices = [
        Index(value = ["textHash", "sourceLang", "targetLang"], unique = true),
        Index(value = ["isReviewed"])
    ]
)
data class TranslationMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val textHash: String,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String = "en",
    val targetLang: String = "pt-BR",
    val engineId: String = "manual",
    val glossaryHash: String = "",
    val isReviewed: Boolean = false,
    val usageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)
