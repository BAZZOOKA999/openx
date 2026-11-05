package com.example.openx.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.openx.data.db.entities.GgufModelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GgufModelDao {
    @Query("SELECT * FROM gguf_models ORDER BY sizeBytes ASC")
    fun getAllModelsFlow(): Flow<List<GgufModelEntity>>

    @Query("SELECT * FROM gguf_models ORDER BY sizeBytes ASC")
    suspend fun getAllModels(): List<GgufModelEntity>

    @Query("SELECT * FROM gguf_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: Long): GgufModelEntity?

    @Query("SELECT * FROM gguf_models WHERE modelKey = :modelKey LIMIT 1")
    suspend fun getModelByKey(modelKey: String): GgufModelEntity?

    @Query("SELECT * FROM gguf_models WHERE isSelected = 1 LIMIT 1")
    fun getSelectedModelFlow(): Flow<GgufModelEntity?>

    @Query("SELECT * FROM gguf_models WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedModel(): GgufModelEntity?

    @Query("SELECT * FROM gguf_models WHERE isDownloaded = 1")
    fun getDownloadedModelsFlow(): Flow<List<GgufModelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: GgufModelEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertModels(models: List<GgufModelEntity>)

    @Update
    suspend fun updateModel(model: GgufModelEntity)

    @Delete
    suspend fun deleteModel(model: GgufModelEntity)

    @Query("DELETE FROM gguf_models WHERE id = :id")
    suspend fun deleteModelById(id: Long)

    @Query("UPDATE gguf_models SET isSelected = 0")
    suspend fun clearAllSelected()

    @Query("UPDATE gguf_models SET isSelected = 1 WHERE id = :id")
    suspend fun setSelected(id: Long)

    @Query("""
        UPDATE gguf_models 
        SET downloadProgress = :progress, downloadStatus = :status, errorMessage = :error 
        WHERE id = :id
    """)
    suspend fun updateDownloadProgress(id: Long, progress: Float, status: String, error: String? = null)

    @Query("""
        UPDATE gguf_models 
        SET isDownloaded = 1, downloadStatus = 'COMPLETED', localFilePath = :filePath, 
            downloadProgress = 1.0, sha256Calculated = :sha256 
        WHERE id = :id
    """)
    suspend fun markCompleted(id: Long, filePath: String, sha256: String)
}
