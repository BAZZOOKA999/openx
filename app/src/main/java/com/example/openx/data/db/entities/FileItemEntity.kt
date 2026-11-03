package com.example.openx.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FileType {
    RPY,
    RPYC,
    RPA,
    XML,
    JSON,
    TXT,
    CSV,
    APK,
    ZIP,
    BINARY,
    UNKNOWN
}

@Entity(
    tableName = "file_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class FileItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val relativePath: String,
    val fileName: String,
    val fileType: FileType,
    val fileSize: Long = 0,
    val totalUnits: Int = 0,
    val translatedUnits: Int = 0,
    val rawContent: String = "",
    val isBinary: Boolean = false,
    val notes: String = ""
)
