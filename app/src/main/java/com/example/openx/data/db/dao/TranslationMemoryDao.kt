package com.example.openx.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.openx.data.db.entities.TranslationMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationMemoryDao {

    @Query("SELECT * FROM translation_memory WHERE textHash = :hash AND sourceLang = :sourceLang AND targetLang = :targetLang LIMIT 1")
    suspend fun findExactMatch(hash: String, sourceLang: String, targetLang: String): TranslationMemoryEntity?

    @Query("SELECT COUNT(*) FROM translation_memory")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM translation_memory")
    suspend fun getTotalCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entry: TranslationMemoryEntity): Long

    @Query("UPDATE translation_memory SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long)

    @Query("DELETE FROM translation_memory WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM translation_memory")
    suspend fun clearAll()
}
