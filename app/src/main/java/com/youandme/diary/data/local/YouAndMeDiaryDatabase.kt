package com.youandme.diary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DiaryEntryEntity::class,
        DiarySlideEntity::class,
        DiaryNoteEntity::class,
        EntryMediaEntity::class,
    ],
    version = 1,
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
                ).build().also { instance = it }
            }
    }
}
