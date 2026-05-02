package com.youandme.diary.core.designsystem

import androidx.compose.ui.graphics.Color

fun argb(color: Long): Color = Color(color.toInt())

fun Int.floorMod(other: Int): Int = ((this % other) + other) % other
