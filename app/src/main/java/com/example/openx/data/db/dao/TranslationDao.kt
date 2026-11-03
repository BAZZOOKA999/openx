package com.example.openx.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.openx.data.db.entities.FileItemEntity
import com.example.openx.data.db.entities.TranslationStatus
import com.example.openx.data.db.entities.TranslationUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    // Files
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<FileItemEntity>): List<Long>

    @Update
    suspend fun updateFile(file: FileItemEntity)

    @Delete
    suspend fun deleteFile(file: FileItemEntity)

    @Query("SELECT * FROM file_items WHERE projectId = :projectId ORDER BY fileName ASC")
    fun getFilesForProject(projectId: Long): Flow<List<FileItemEntity>>

    @Query("SELECT * FROM file_items WHERE projectId = :projectId")
    suspend fun getFilesForProjectSync(projectId: Long): List<FileItemEntity>

    @Query("SELECT * FROM file_items WHERE id = :fileId")
    suspend fun getFileById(fileId: Long): FileItemEntity?

    @Query("SELECT COUNT(*) FROM file_items WHERE projectId = :projectId")
    suspend fun getFilesCount(projectId: Long): Int

    // Translation Units
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: TranslationUnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<TranslationUnitEntity>)

    @Update
    suspend fun updateUnit(unit: TranslationUnitEntity)

    @Delete
    suspend fun deleteUnit(unit: TranslationUnitEntity)

    @Query("SELECT * FROM translation_units WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getAllUnitsForProject(projectId: Long): Flow<List<TranslationUnitEntity>>

    @Query("SELECT * FROM translation_units WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun getAllUnitsForProjectSync(projectId: Long): List<TranslationUnitEntity>

    @Query("SELECT * FROM translation_units WHERE fileId = :fileId ORDER BY orderIndex ASC")
    fun getUnitsForFile(fileId: Long): Flow<List<TranslationUnitEntity>>

    @Query("SELECT * FROM translation_units WHERE fileId = :fileId ORDER BY orderIndex ASC")
    suspend fun getUnitsForFileSync(fileId: Long): List<TranslationUnitEntity>

    @Query("SELECT * FROM translation_units WHERE id = :unitId")
    suspend fun getUnitById(unitId: Long): TranslationUnitEntity?

    @Query("SELECT * FROM translation_units WHERE projectId = :projectId AND status = :status ORDER BY orderIndex ASC")
    suspend fun getUnitsByStatus(projectId: Long, status: TranslationStatus): List<TranslationUnitEntity>

    @Query("SELECT COUNT(*) FROM translation_units WHERE projectId = :projectId")
    suspend fun getTotalUnitsCount(projectId: Long): Int

    @Query("SELECT COUNT(*) FROM translation_units WHERE projectId = :projectId AND status = :status")
    suspend fun getUnitsCountByStatus(projectId: Long, status: TranslationStatus): Int

    @Query("UPDATE translation_units SET translatedText = :newTranslation, status = :newStatus, lastUpdated = :timestamp WHERE id = :unitId")
    suspend fun updateTranslation(unitId: Long, newTranslation: String, newStatus: TranslationStatus, timestamp: Long)

    @Query("UPDATE translation_units SET status = :newStatus WHERE id = :unitId")
    suspend fun updateStatus(unitId: Long, newStatus: TranslationStatus)

    @Query("UPDATE translation_units SET validationError = :error WHERE id = :unitId")
    suspend fun updateValidationError(unitId: Long, error: String?)
}
