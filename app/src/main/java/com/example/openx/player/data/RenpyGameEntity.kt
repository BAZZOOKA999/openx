package com.example.openx.player.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "renpy_games")
data class RenpyGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val version: String = "1.0",
    val gameDir: String,
    val coverPath: String? = null,
    val estimatedEngineVersion: String = "Ren'Py 8.x (Python 3)",
    val confidenceScore: Int = 90,
    val selectedEngineVersion: String = "Ren'Py 8.x",
    val totalSizeBytes: Long = 0,
    val availableLanguages: String = "Inglês",
    val compatibilityStatus: String = "COMPATIBLE", // COMPATIBLE, WARNING, INCOMPATIBLE
    val compatibilityDetails: String = "",
    val saveCount: Int = 0,
    val lastPlayedTimestamp: Long = 0,
    val importedTimestamp: Long = System.currentTimeMillis(),
    val associatedProjectId: Long? = null,
    val hasPortugueseTl: Boolean = false,
    val isBuiltInDemo: Boolean = false
)
