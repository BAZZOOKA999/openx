package com.example.openx.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.openx.data.db.entities.BatchQueueItemEntity
import com.example.openx.data.db.entities.QueueStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BatchQueueDao {

    @Query("SELECT * FROM batch_queue WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getQueueForProjectFlow(projectId: Long): Flow<List<BatchQueueItemEntity>>

    @Query("SELECT * FROM batch_queue WHERE projectId = :projectId AND status = :status ORDER BY orderIndex ASC")
    suspend fun getItemsByStatus(projectId: Long, status: QueueStatus): List<BatchQueueItemEntity>

    @Query("SELECT * FROM batch_queue WHERE projectId = :projectId AND (status = 'QUEUED' OR status = 'PAUSED') ORDER BY orderIndex ASC")
    suspend fun getPendingItems(projectId: Long): List<BatchQueueItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<BatchQueueItemEntity>)

    @Update
    suspend fun updateItem(item: BatchQueueItemEntity)

    @Query("UPDATE batch_queue SET status = :status, translatedText = :translated, reusedFromMemory = :fromTm, usedEngine = :engine WHERE id = :id")
    suspend fun markCompleted(id: Long, status: QueueStatus, translated: String, fromTm: Boolean, engine: String)

    @Query("UPDATE batch_queue SET status = :status, errorMessage = :error, retryCount = retryCount + 1, lastAttemptTime = :time WHERE id = :id")
    suspend fun markFailed(id: Long, status: QueueStatus, error: String, time: Long)

    @Query("UPDATE batch_queue SET status = :status WHERE projectId = :projectId AND status != 'COMPLETED'")
    suspend fun updateAllPendingStatus(projectId: Long, status: QueueStatus)

    @Query("DELETE FROM batch_queue WHERE projectId = :projectId")
    suspend fun clearQueueForProject(projectId: Long)

    @Query("SELECT COUNT(*) FROM batch_queue WHERE projectId = :projectId AND status = 'COMPLETED'")
    suspend fun getCompletedCount(projectId: Long): Int

    @Query("SELECT COUNT(*) FROM batch_queue WHERE projectId = :projectId")
    suspend fun getTotalQueueCount(projectId: Long): Int
}
