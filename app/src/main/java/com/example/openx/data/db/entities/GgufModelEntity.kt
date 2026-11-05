package com.example.openx.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gguf_models")
data class GgufModelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val modelKey: String,
    val name: String,
    val fileName: String,
    val downloadUrl: String,
    val sha256Expected: String = "",
    val sha256Calculated: String? = null,
    val sizeBytes: Long,
    val ramRequirementBytes: Long,
    val architecture: String,
    val quantization: String,
    val license: String = "Apache 2.0",
    val localFilePath: String? = null,
    val isDownloaded: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadStatus: String = "IDLE", // IDLE, DOWNLOADING, PAUSED, COMPLETED, VERIFYING, ERROR
    val errorMessage: String? = null,
    val isSelected: Boolean = false,
    val isCustomImported: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)
