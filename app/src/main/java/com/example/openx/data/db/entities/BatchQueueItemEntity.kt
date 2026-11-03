package com.example.openx.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class QueueStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    PAUSED
}

@Entity(
    tableName = "batch_queue",
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["status"])
    ]
)
data class BatchQueueItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val unitId: Long,
    val orderIndex: Int,
    val originalText: String,
    val speaker: String = "",
    val contextInfo: String = "",
    val status: QueueStatus = QueueStatus.QUEUED,
    val translatedText: String = "",
    val reusedFromMemory: Boolean = false,
    val usedEngine: String = "",
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val lastAttemptTime: Long = 0
)
