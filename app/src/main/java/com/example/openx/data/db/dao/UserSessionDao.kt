package com.example.openx.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.openx.data.db.entities.UserSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions LIMIT 1")
    fun getSessionFlow(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_sessions LIMIT 1")
    suspend fun getSessionSync(): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: UserSessionEntity)

    @Update
    suspend fun updateSession(session: UserSessionEntity)

    @Query("UPDATE user_sessions SET isOfflineMode = :isOffline")
    suspend fun setOfflineMode(isOffline: Boolean)

    @Query("UPDATE user_sessions SET pinCodeHash = :pinHash, requirePinOnLaunch = :requirePin")
    suspend fun updateSecurityPin(pinHash: String, requirePin: Boolean)
}
