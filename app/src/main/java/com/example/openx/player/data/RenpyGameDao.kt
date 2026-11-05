package com.example.openx.player.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RenpyGameDao {
    @Query("SELECT * FROM renpy_games ORDER BY lastPlayedTimestamp DESC, importedTimestamp DESC")
    fun getAllGamesFlow(): Flow<List<RenpyGameEntity>>

    @Query("SELECT * FROM renpy_games ORDER BY lastPlayedTimestamp DESC, importedTimestamp DESC")
    suspend fun getAllGamesSync(): List<RenpyGameEntity>

    @Query("SELECT * FROM renpy_games WHERE id = :id LIMIT 1")
    fun getGameByIdFlow(id: Long): Flow<RenpyGameEntity?>

    @Query("SELECT * FROM renpy_games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Long): RenpyGameEntity?

    @Query("SELECT * FROM renpy_games WHERE isBuiltInDemo = 1 LIMIT 1")
    suspend fun getBuiltInDemoGame(): RenpyGameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: RenpyGameEntity): Long

    @Update
    suspend fun updateGame(game: RenpyGameEntity)

    @Delete
    suspend fun deleteGame(game: RenpyGameEntity)

    @Query("DELETE FROM renpy_games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    @Query("SELECT COUNT(*) FROM renpy_games")
    suspend fun getGamesCount(): Int
}
