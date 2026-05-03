package com.youandme.diary.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class DiaryEntryWithSlides(
    @Embedded val entry: DiaryEntryEntity,
    @Relation(
        entity = DiarySlideEntity::class,
        parentColumn = "id",
        entityColumn = "entryId",
    )
    val slides: List<DiarySlideWithNotes>,
)

data class DiarySlideWithNotes(
    @Embedded val slide: DiarySlideEntity,
    @Relation(
        parentColumn = "slideKey",
        entityColumn = "slideKey",
    )
    val notes: List<DiaryNoteEntity>,
)
