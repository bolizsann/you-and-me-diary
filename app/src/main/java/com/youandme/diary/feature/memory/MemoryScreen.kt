package com.youandme.diary.feature.memory

import android.graphics.BitmapFactory
import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.DiaryPreviewTheme
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.GentleCard
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryIds
import com.youandme.diary.domain.model.DiarySlide
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes
import com.youandme.diary.domain.model.EntryMedia
import kotlin.math.roundToInt

@Composable
fun MemoryScreen(
    entries: List<DiaryEntry>,
    favoriteIds: List<String>,
    theme: DiaryTheme,
    onBack: () -> Unit,
    onOpenSlide: (DiaryEntry, Int) -> Unit,
) {
    val favoriteSet = favoriteIds.toSet()
    val favorites = entries
        .asSequence()
        .filter { entry ->
            entry.slides.any { slide ->
                favoriteSet.contains(DiaryIds.favoriteId(entry.id, slide.id))
            }
        }
        .sortedWith(
            compareByDescending<DiaryEntry> { it.dateId }
                .thenByDescending { it.createdAt },
        )
        .mapNotNull { entry ->
            val coverSlide = entry.slides.firstOrNull() ?: return@mapNotNull null
            MemoryItem(
                entry = entry,
                coverSlide = coverSlide,
                coverMedia = coverSlide.mediaId
                    ?.let { mediaId -> entry.media.firstOrNull { it.id == mediaId } }
                    ?: entry.media.firstOrNull(),
            )
        }
        .toList()

    DiaryPage(
        title = "纪念册",
        theme = theme,
        onBack = onBack,
        scrollEnabled = false,
        modifier = Modifier.testTag("memory-screen"),
    ) {
        Text("已收藏 ${favorites.size} 页", color = argb(theme.muted), fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        if (favorites.isEmpty()) {
            GentleCard(theme = theme) {
                Text("还没有收藏。回到结果页，把想留给未来的那一天放进来。", lineHeight = 25.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 18.dp),
            ) {
                items(
                    items = favorites,
                    key = { it.entry.id },
                ) { item ->
                    MemoryCard(
                        item = item,
                        theme = theme,
                        onClick = { onOpenSlide(item.entry, 0) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryCard(
    item: MemoryItem,
    theme: DiaryTheme,
    onClick: () -> Unit,
) {
    val coverText = item.coverSlide.notes.firstOrNull()?.selfText
        ?.ifBlank { item.entry.timelineSummary }
        ?: item.entry.timelineSummary
    GentleCard(
        theme = theme,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MemoryThumbnail(
                slide = item.coverSlide,
                media = item.coverMedia,
                theme = theme,
            )
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    item.entry.dateLabel,
                    color = argb(theme.muted),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    item.entry.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 23.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    coverText,
                    color = argb(theme.muted),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MemoryThumbnail(
    slide: DiarySlide,
    media: EntryMedia?,
    theme: DiaryTheme,
) {
    val imageBitmap = remember(media?.localPath) {
        media?.localPath
            ?.let { BitmapFactory.decodeFile(it) }
            ?.asImageBitmap()
    }
    Box(
        modifier = Modifier
            .size(width = 88.dp, height = 112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        argb(media?.dominantColor ?: slide.gradientStart),
                        argb(slide.gradientEnd),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.66f), RoundedCornerShape(18.dp)),
    ) {
        if (imageBitmap != null) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val src = calculateRoiSourceRect(
                    imageWidth = imageBitmap.width,
                    imageHeight = imageBitmap.height,
                    roiScale = media?.roiScale ?: 1f,
                    roiOffsetX = media?.roiOffsetX ?: 0f,
                    roiOffsetY = media?.roiOffsetY ?: 0f,
                    dstRatio = size.width / size.height,
                )
                drawImage(
                    image = imageBitmap,
                    srcOffset = IntOffset(src.left, src.top),
                    srcSize = IntSize(src.width(), src.height()),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(
                        size.width.roundToInt().coerceAtLeast(1),
                        size.height.roundToInt().coerceAtLeast(1),
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.12f)),
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(26.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.42f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .fillMaxWidth(0.58f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(argb(theme.surface).copy(alpha = 0.76f)),
            )
        }
    }
}

private data class MemoryItem(
    val entry: DiaryEntry,
    val coverSlide: DiarySlide,
    val coverMedia: EntryMedia?,
)

private fun calculateRoiSourceRect(
    imageWidth: Int,
    imageHeight: Int,
    roiScale: Float,
    roiOffsetX: Float,
    roiOffsetY: Float,
    dstRatio: Float,
): Rect {
    val square = calculateSquareRoi(
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        roiScale = roiScale,
        roiOffsetX = roiOffsetX,
        roiOffsetY = roiOffsetY,
    )
    return if (1f > dstRatio) {
        val cropWidth = (square.height() * dstRatio).roundToInt().coerceAtLeast(1)
        val left = square.left + (square.width() - cropWidth) / 2
        Rect(left, square.top, left + cropWidth, square.bottom)
    } else {
        val cropHeight = (square.width() / dstRatio).roundToInt().coerceAtLeast(1)
        val top = square.top + (square.height() - cropHeight) / 2
        Rect(square.left, top, square.right, top + cropHeight)
    }
}

private fun calculateSquareRoi(
    imageWidth: Int,
    imageHeight: Int,
    roiScale: Float,
    roiOffsetX: Float,
    roiOffsetY: Float,
): Rect {
    val safeScale = roiScale.coerceIn(1f, 4f)
    val baseScale = maxOf(1f / imageWidth.toFloat(), 1f / imageHeight.toFloat())
    val renderedWidth = imageWidth * baseScale * safeScale
    val renderedHeight = imageHeight * baseScale * safeScale
    val cropSize = (1f / (baseScale * safeScale))
        .roundToInt()
        .coerceIn(1, minOf(imageWidth, imageHeight))
    val left = ((renderedWidth / 2f - 0.5f - roiOffsetX) / (baseScale * safeScale))
        .roundToInt()
        .coerceIn(0, (imageWidth - cropSize).coerceAtLeast(0))
    val top = ((renderedHeight / 2f - 0.5f - roiOffsetY) / (baseScale * safeScale))
        .roundToInt()
        .coerceIn(0, (imageHeight - cropSize).coerceAtLeast(0))
    return Rect(left, top, left + cropSize, top + cropSize)
}

@Preview(name = "纪念册", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun MemoryScreenPreview() {
    val theme = DiaryThemes.Rose
    DiaryPreviewTheme(theme = theme) {
        MemoryScreen(
            entries = MockDiaryRepository.entries,
            favoriteIds = MockDiaryRepository.defaultFavoriteSlideIds().toList(),
            theme = theme,
            onBack = {},
            onOpenSlide = { _, _ -> },
        )
    }
}
