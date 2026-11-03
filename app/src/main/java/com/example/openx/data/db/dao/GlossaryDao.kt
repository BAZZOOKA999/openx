package com.example.openx.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.openx.data.db.entities.GlossaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossaryDao {
    @Query("SELECT * FROM glossary_items WHERE projectId = :projectId OR projectId = 0 ORDER BY sourceTerm ASC")
    fun getGlossaryForProject(projectId: Long): Flow<List<GlossaryEntity>>

    @Query("SELECT * FROM glossary_items WHERE projectId = :projectId OR projectId = 0")
    suspend fun getGlossaryForProjectSync(projectId: Long): List<GlossaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GlossaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<GlossaryEntity>)

    @Update
    suspend fun updateItem(item: GlossaryEntity)

    @Delete
    suspend fun deleteItem(item: GlossaryEntity)

    @Query("DELETE FROM glossary_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)
}
