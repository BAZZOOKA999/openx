package com.example.openx.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.openx.data.db.dao.BatchQueueDao
import com.example.openx.data.db.dao.GlossaryDao
import com.example.openx.data.db.dao.ProjectDao
import com.example.openx.data.db.dao.TranslationDao
import com.example.openx.data.db.dao.TranslationMemoryDao
import com.example.openx.data.db.dao.UserSessionDao
import com.example.openx.data.db.entities.BatchQueueItemEntity
import com.example.openx.data.db.entities.FileItemEntity
import com.example.openx.data.db.entities.GlossaryEntity
import com.example.openx.data.db.entities.ProjectEntity
import com.example.openx.data.db.entities.TranslationMemoryEntity
import com.example.openx.data.db.entities.TranslationUnitEntity
import com.example.openx.data.db.entities.UserSessionEntity

@Database(
    entities = [
        ProjectEntity::class,
        FileItemEntity::class,
        TranslationUnitEntity::class,
        GlossaryEntity::class,
        UserSessionEntity::class,
        TranslationMemoryEntity::class,
        BatchQueueItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class OpenXDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun translationDao(): TranslationDao
    abstract fun glossaryDao(): GlossaryDao
    abstract fun userSessionDao(): UserSessionDao
    abstract fun translationMemoryDao(): TranslationMemoryDao
    abstract fun batchQueueDao(): BatchQueueDao

    companion object {
        @Volatile
        private var INSTANCE: OpenXDatabase? = null

        fun getInstance(context: Context): OpenXDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OpenXDatabase::class.java,
                    "openx_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
