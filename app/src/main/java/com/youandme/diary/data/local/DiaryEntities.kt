package com.youandme.diary.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "diary_entries",
    indices = [Index(value = ["dateId"])],
)
data class DiaryEntryEntity(
    @PrimaryKey val id: String,
    val dateId: String,
    val dateLabel: String,
    val title: String,
    val moodEmoji: String,
    val moodColor: Long,
    val comfortText: String,
    val timelineSummary: String,
    val rawText: String,
    val createdAt: Long,
)

@Entity(
    tableName = "diary_slides",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["entryId"]), Index(value = ["isFavorite"])],
)
data class DiarySlideEntity(
    @PrimaryKey val slideKey: String,
    val entryId: String,
    val id: String,
    val title: String,
    val quote: String,
    val caption: String,
    val gradientStart: Long,
    val gradientEnd: Long,
    val sortOrder: Int,
    val isFavorite: Boolean,
    val mediaId: String?,
)

@Entity(
    tableName = "diary_notes",
    foreignKeys = [
        ForeignKey(
            entity = DiarySlideEntity::class,
            parentColumns = ["slideKey"],
            childColumns = ["slideKey"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["slideKey"])],
)
data class DiaryNoteEntity(
    @PrimaryKey val id: String,
    val slideKey: String,
    val label: String,
    val selfText: String,
    val babyText: String,
    val editedSelfText: String?,
    val editedBabyText: String?,
    val x: Float,
    val y: Float,
    val sortOrder: Int,
)

@Entity(
    tableName = "entry_media",
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["entryId"])],
)
data class EntryMediaEntity(
    @PrimaryKey val id: String,
    val entryId: String,
    val localPath: String,
    val type: String,
    val dominantColor: Long?,
    val createdAt: Long,
)
