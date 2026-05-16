package com.youandme.diary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DiaryEntryEntity::class,
        DiarySlideEntity::class,
        DiaryNoteEntity::class,
        EntryMediaEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class YouAndMeDiaryDatabase : RoomDatabase() {
    abstract fun diaryDao(): DiaryDao

    companion object {
        @Volatile
        private var instance: YouAndMeDiaryDatabase? = null

        fun getInstance(context: Context): YouAndMeDiaryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    YouAndMeDiaryDatabase::class.java,
                    "you_and_me_diary.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entry_media ADD COLUMN roiScale REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE entry_media ADD COLUMN roiOffsetX REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE entry_media ADD COLUMN roiOffsetY REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}
