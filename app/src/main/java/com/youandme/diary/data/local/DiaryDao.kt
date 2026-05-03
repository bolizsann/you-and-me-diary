package com.youandme.diary.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DiaryDao {
    @Transaction
    @Query("SELECT * FROM diary_entries ORDER BY dateId ASC, createdAt ASC")
    abstract fun observeEntries(): Flow<List<DiaryEntryWithSlides>>

    @Transaction
    @Query("SELECT * FROM diary_entries WHERE id = :entryId LIMIT 1")
    abstract suspend fun getEntry(entryId: String): DiaryEntryWithSlides?

    @Query("SELECT slideKey FROM diary_slides WHERE isFavorite = 1 ORDER BY entryId ASC, sortOrder ASC")
    abstract fun observeFavoriteIds(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM diary_entries")
    abstract suspend fun entryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertEntry(entry: DiaryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertSlides(slides: List<DiarySlideEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertNotes(notes: List<DiaryNoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertMedia(media: List<EntryMediaEntity>)

    @Query("DELETE FROM diary_entries WHERE id = :entryId")
    protected abstract suspend fun deleteEntryById(entryId: String)

    @Query("DELETE FROM diary_entries")
    abstract suspend fun deleteAllEntries()

    @Query("UPDATE diary_slides SET isFavorite = NOT isFavorite WHERE slideKey = :slideKey")
    abstract suspend fun toggleFavorite(slideKey: String)

    @Query("UPDATE diary_notes SET editedSelfText = :text WHERE id = :noteId")
    abstract suspend fun updateEditedSelfText(noteId: String, text: String)

    @Query("UPDATE diary_notes SET editedBabyText = :text WHERE id = :noteId")
    abstract suspend fun updateEditedBabyText(noteId: String, text: String)

    @Transaction
    open suspend fun upsertEntry(
        entry: DiaryEntryEntity,
        slides: List<DiarySlideEntity>,
        notes: List<DiaryNoteEntity>,
        media: List<EntryMediaEntity> = emptyList(),
    ) {
        deleteEntryById(entry.id)
        insertEntry(entry)
        insertSlides(slides)
        insertNotes(notes)
        if (media.isNotEmpty()) {
            insertMedia(media)
        }
    }
}
