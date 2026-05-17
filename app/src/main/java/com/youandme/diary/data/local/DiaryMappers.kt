package com.youandme.diary.data.local

import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryIds
import com.youandme.diary.domain.model.DiaryNote
import com.youandme.diary.domain.model.DiarySlide
import com.youandme.diary.domain.model.EntryMedia

fun DiaryEntryWithSlides.toDomain(): DiaryEntry =
    DiaryEntry(
        id = entry.id,
        dateId = entry.dateId,
        dateLabel = entry.dateLabel,
        title = entry.title,
        moodEmoji = entry.moodEmoji,
        moodColor = entry.moodColor,
        comfortText = entry.comfortText,
        timelineSummary = entry.timelineSummary,
        rawText = entry.rawText,
        createdAt = entry.createdAt,
        slides = slides
            .sortedBy { it.slide.sortOrder }
            .map { it.toDomain() },
        media = media
            .sortedBy { it.createdAt }
            .map { it.toDomain() },
    )

fun DiarySlideWithNotes.toDomain(): DiarySlide =
    DiarySlide(
        id = slide.id,
        title = slide.title,
        quote = slide.quote,
        caption = slide.caption,
        gradientStart = slide.gradientStart,
        gradientEnd = slide.gradientEnd,
        defaultFavorite = slide.isFavorite,
        mediaId = slide.mediaId,
        generationSource = slide.generationSource,
        createdAt = slide.createdAt,
        notes = notes
            .sortedBy { it.sortOrder }
            .map { it.toDomain() },
    )

private fun EntryMediaEntity.toDomain(): EntryMedia =
    EntryMedia(
        id = id,
        entryId = entryId,
        localPath = localPath,
        type = type,
        dominantColor = dominantColor,
        createdAt = createdAt,
        roiScale = roiScale,
        roiOffsetX = roiOffsetX,
        roiOffsetY = roiOffsetY,
    )

private fun DiaryNoteEntity.toDomain(): DiaryNote =
    DiaryNote(
        label = label,
        selfText = editedSelfText ?: selfText,
        babyText = editedBabyText ?: babyText,
        x = x,
        y = y,
    )

fun DiaryEntry.toLocalEntities(): LocalDiaryRecord {
    val entryEntity = DiaryEntryEntity(
        id = id,
        dateId = dateId,
        dateLabel = dateLabel,
        title = title,
        moodEmoji = moodEmoji,
        moodColor = moodColor,
        comfortText = comfortText,
        timelineSummary = timelineSummary,
        rawText = rawText,
        createdAt = createdAt,
    )
    val slideEntities = slides.mapIndexed { index, slide ->
        val slideKey = DiaryIds.favoriteId(id, slide.id)
        DiarySlideEntity(
            slideKey = slideKey,
            entryId = id,
            id = slide.id,
            title = slide.title,
            quote = slide.quote,
            caption = slide.caption,
            gradientStart = slide.gradientStart,
            gradientEnd = slide.gradientEnd,
            sortOrder = index,
            isFavorite = slide.defaultFavorite,
            mediaId = slide.mediaId,
            generationSource = slide.generationSource,
            createdAt = slide.createdAt,
        )
    }
    val noteEntities = slides.flatMapIndexed { slideIndex, slide ->
        val slideKey = DiaryIds.favoriteId(id, slide.id)
        slide.notes.mapIndexed { noteIndex, note ->
            DiaryNoteEntity(
                id = "$slideKey::note-$noteIndex",
                slideKey = slideKey,
                label = note.label,
                selfText = note.selfText,
                babyText = note.babyText,
                editedSelfText = null,
                editedBabyText = null,
                x = note.x,
                y = note.y,
                sortOrder = slideIndex * 100 + noteIndex,
            )
        }
    }
    val mediaEntities = media.map { item ->
        EntryMediaEntity(
            id = item.id,
            entryId = id,
            localPath = item.localPath,
            type = item.type,
            dominantColor = item.dominantColor,
            createdAt = item.createdAt,
            roiScale = item.roiScale,
            roiOffsetX = item.roiOffsetX,
            roiOffsetY = item.roiOffsetY,
        )
    }
    return LocalDiaryRecord(entryEntity, slideEntities, noteEntities, mediaEntities)
}

data class LocalDiaryRecord(
    val entry: DiaryEntryEntity,
    val slides: List<DiarySlideEntity>,
    val notes: List<DiaryNoteEntity>,
    val media: List<EntryMediaEntity> = emptyList(),
)
