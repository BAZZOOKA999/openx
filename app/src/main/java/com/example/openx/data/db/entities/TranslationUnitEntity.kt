package com.example.openx.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TranslationStatus {
    PENDING,
    TRANSLATED,
    REVIEWED,
    ERROR
}

enum class BlockType {
    DIALOGUE,
    MENU,
    OLD_NEW,
    XML_STRING,
    JSON_KEY,
    CSV_CELL,
    GENERIC
}

@Entity(
    tableName = "translation_units",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FileItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["projectId"]),
        Index(value = ["fileId"]),
        Index(value = ["status"])
    ]
)
data class TranslationUnitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val fileId: Long,
    val orderIndex: Int = 0,
    val lineIndex: Int = 0,
    val speaker: String = "",
    val keyOrContext: String = "",
    val originalText: String,
    val translatedText: String = "",
    val status: TranslationStatus = TranslationStatus.PENDING,
    val blockType: BlockType = BlockType.GENERIC,
    val placeholdersJson: String = "[]",
    val tagsJson: String = "[]",
    val validationError: String? = null,
    val isLocked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
