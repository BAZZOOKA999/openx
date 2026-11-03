package com.example.openx.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProjectType {
    RENPY,
    ANDROID_RES,
    APK_INSPECTION,
    GENERIC_TEXT
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: ProjectType,
    val sourceLang: String = "en",
    val targetLang: String = "pt-BR",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val totalUnits: Int = 0,
    val translatedUnits: Int = 0,
    val reviewedUnits: Int = 0,
    val errorUnits: Int = 0,
    val sourceUri: String = "",
    val isDemo: Boolean = false,
    val notes: String = ""
)
