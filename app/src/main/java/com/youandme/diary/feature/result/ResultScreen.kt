package com.youandme.diary.feature.result

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.provider.MediaStore
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.youandme.diary.core.designsystem.CircleIconAction
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.DiaryPreviewTheme
import com.youandme.diary.core.designsystem.GentleCard
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.data.mock.MockDiaryRepository
import com.youandme.diary.domain.model.DiaryEntry
import com.youandme.diary.domain.model.DiaryIds
import com.youandme.diary.domain.model.DiarySlide
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes
import com.youandme.diary.domain.model.EntryMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
@Suppress("UNUSED_PARAMETER")
fun ResultScreen(
    entry: DiaryEntry,
    slide: DiarySlide,
    slideIndex: Int,
    theme: DiaryTheme,
    favoriteIds: List<String>,
    selectedNoteIndex: Int?,
    noteMode: String,
    isEditingNote: Boolean,
    sharePreviewVisible: Boolean,
    onBack: () -> Unit,
    onPreviousSlide: () -> Unit,
    onNextSlide: () -> Unit,
    onSelectNote: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onNoteModeChange: (String) -> Unit,
    onEditTextChange: (String) -> Unit,
    onEditDiaryContentChange: (String, String) -> Unit,
    onToggleEdit: () -> Unit,
    onToggleSharePreview: () -> Unit,
    onDeleteSlide: () -> Unit,
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var hasLocationPermission by remember(context) {
        mutableStateOf(context.hasCoarseLocationPermission())
    }
    var locationLabel by remember { mutableStateOf(if (hasLocationPermission) "Locating..." else "My City") }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasLocationPermission = granted
            locationLabel = if (granted) "Locating..." else "My City"
        },
    )
    LaunchedEffect(context, hasLocationPermission) {
        if (hasLocationPermission) {
            locationLabel = resolveEnglishCity(context) ?: "Current City"
        }
    }
    val currentNote = selectedNoteIndex?.let { slide.notes.getOrNull(it) } ?: slide.notes.firstOrNull()
    val diaryText = currentNote?.selfText
        ?.ifBlank { entry.rawText.ifBlank { entry.timelineSummary } }
        ?: entry.rawText.ifBlank { entry.timelineSummary }
    val babyText = currentNote?.babyText.orEmpty()
    val cardSummary = slide.cardSummaryText()
    val sourceText = cardSummary.ifBlank { currentNote?.selfText?.trim().orEmpty() }
    val currentMedia = slide.mediaId?.let { mediaId ->
        entry.media.firstOrNull { it.id == mediaId }
    }
    var draftTitle by remember(entry.id, slide.id) { mutableStateOf(entry.title) }
    var draftText by remember(entry.id, slide.id) { mutableStateOf(diaryText) }
    LaunchedEffect(isEditingNote, entry.id, slide.id) {
        if (isEditingNote) {
            draftTitle = entry.title
            draftText = diaryText
        }
    }
    val isFavorite = entry.slides.any { entrySlide ->
        favoriteIds.contains(DiaryIds.favoriteId(entry.id, entrySlide.id))
    }
    val finishEditing = {
        if (isEditingNote) {
            if (draftTitle.isBlank()) {
                val fallbackTitle = DEFAULT_DIARY_TITLE
                draftTitle = fallbackTitle
                onEditDiaryContentChange(fallbackTitle, draftText)
            }
            onToggleEdit()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DiaryPage(
            theme = theme,
            modifier = Modifier.testTag("result-screen"),
        ) {
            ResultTopBar(
                dateLabel = entry.dateLabel,
                theme = theme,
                isFavorite = isFavorite,
                sharePreviewVisible = sharePreviewVisible,
                canDeleteSlide = slide.isSubmittedSlide(),
                onBack = {
                    finishEditing()
                    onBack()
                },
                onToggleFavorite = {
                    finishEditing()
                    onToggleFavorite()
                },
                onToggleSharePreview = {
                    finishEditing()
                    onToggleSharePreview()
                },
                onDeleteSlide = {
                    finishEditing()
                    showDeleteConfirm = true
                },
            )
            Spacer(Modifier.height(8.dp))

            ResultImageCard(
                entry = entry,
                slide = slide,
                slideIndex = slideIndex,
                slideCount = entry.slides.size,
                media = currentMedia,
                sourceText = sourceText,
                preferSourceText = cardSummary.isNotBlank(),
                locationLabel = locationLabel,
                canRequestLocation = !hasLocationPermission,
                theme = theme,
                onPreviousSlide = onPreviousSlide,
                onNextSlide = onNextSlide,
                onInteract = finishEditing,
                onRequestLocation = {
                    finishEditing()
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
            )
            Spacer(Modifier.height(8.dp))
            SlideDots(count = entry.slides.size, selectedIndex = slideIndex, theme = theme)
            Spacer(Modifier.height(8.dp))

            if (isEditingNote) {
                DiaryContentEditor(
                    title = draftTitle,
                    text = draftText,
                    theme = theme,
                    onChange = { title, text ->
                        draftTitle = title
                        draftText = text
                        onEditDiaryContentChange(title, text)
                    },
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        entry.title.ifBlank { DEFAULT_DIARY_TITLE },
                        modifier = Modifier.weight(1f),
                        fontSize = 22.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    TextButton(
                        onClick = onToggleEdit,
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("note-edit-toggle"),
                        shape = RoundedCornerShape(999.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    ) {
                        Text("编辑", color = argb(theme.muted), fontSize = 13.sp)
                    }
                }
                Text(
                    text = diaryText,
                    fontSize = 17.sp,
                    lineHeight = 28.sp,
                )
            }

            if (babyText.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                GentleCard(
                    theme = theme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isEditingNote) Modifier.clickable(onClick = finishEditing) else Modifier),
                ) {
                    Text("宝宝说", color = argb(theme.muted), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(babyText, fontSize = 15.sp, lineHeight = 24.sp)
                }
            }
        }

        if (sharePreviewVisible) {
            SharePreviewOverlay(
                entry = entry,
                slide = slide,
                media = currentMedia,
                diaryText = diaryText,
                babyText = babyText,
                locationLabel = locationLabel,
                canRequestLocation = !hasLocationPermission,
                theme = theme,
                onDismiss = onToggleSharePreview,
                onRequestLocation = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                onShare = {
                    shareCurrentSlide(
                        context = context,
                        entry = entry,
                        slide = slide,
                        media = currentMedia,
                        diaryText = diaryText,
                        babyText = babyText,
                        locationLabel = locationLabel,
                        theme = theme,
                    )
                },
            )
        }

        if (showDeleteConfirm) {
            DeleteSlideConfirmDialog(
                theme = theme,
                onDismissRequest = { showDeleteConfirm = false },
                onConfirm = {
                    showDeleteConfirm = false
                    onDeleteSlide()
                },
            )
        }
    }
}

@Composable
private fun ResultTopBar(
    dateLabel: String,
    theme: DiaryTheme,
    isFavorite: Boolean,
    sharePreviewVisible: Boolean,
    canDeleteSlide: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleSharePreview: () -> Unit,
    onDeleteSlide: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircleIconAction(
            label = "<",
            theme = theme,
            modifier = Modifier
                .testTag("page-back-button"),
            onClick = onBack,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(dateLabel, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text("已自动记入时间线", color = argb(theme.muted), fontSize = 12.sp)
        }
        CircleIconAction(
            label = if (isFavorite) "★" else "☆",
            theme = theme,
            modifier = Modifier
                .testTag("favorite-button"),
            contentColor = if (isFavorite) argb(0xFFD6A06F) else argb(theme.text),
            onClick = onToggleFavorite,
        )
        CircleIconAction(
            label = if (sharePreviewVisible) "✓" else "↗",
            theme = theme,
            onClick = onToggleSharePreview,
        )
        if (canDeleteSlide) {
            CircleIconAction(
                label = "×",
                theme = theme,
                contentColor = argb(0xFFC4777F),
                onClick = onDeleteSlide,
            )
        }
    }
}

@Composable
private fun DiaryContentEditor(
    title: String,
    text: String,
    theme: DiaryTheme,
    onChange: (String, String) -> Unit,
) {
    var titleField by remember {
        mutableStateOf(TextFieldValue(title, selection = TextRange(title.length)))
    }
    var textField by remember {
        mutableStateOf(TextFieldValue(text, selection = TextRange(text.length)))
    }
    LaunchedEffect(title) {
        if (title != titleField.text) {
            titleField = TextFieldValue(title, selection = TextRange(title.length))
        }
    }
    LaunchedEffect(text) {
        if (text != textField.text) {
            textField = TextFieldValue(text, selection = TextRange(text.length))
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(argb(theme.surface).copy(alpha = 0.92f))
            .border(BorderStroke(1.dp, argb(theme.muted).copy(alpha = 0.16f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("diary-content-editor"),
    ) {
        BasicTextField(
            value = titleField,
            onValueChange = { value ->
                val cleanTitle = value.text.replace('\n', ' ').take(20)
                val cleanSelection = TextRange(
                    value.selection.start.coerceIn(0, cleanTitle.length),
                    value.selection.end.coerceIn(0, cleanTitle.length),
                )
                titleField = value.copy(text = cleanTitle, selection = cleanSelection)
                onChange(cleanTitle, textField.text)
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = argb(theme.text),
                fontSize = 20.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 2,
            cursorBrush = SolidColor(argb(theme.primary)),
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(argb(theme.muted).copy(alpha = 0.14f)),
        )
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = textField,
            onValueChange = { value ->
                textField = value
                onChange(titleField.text.take(20), value.text)
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = argb(theme.text),
                fontSize = 16.sp,
                lineHeight = 26.sp,
            ),
            minLines = 4,
            cursorBrush = SolidColor(argb(theme.primary)),
        )
    }
}

@Composable
private fun ResultImageCard(
    entry: DiaryEntry,
    slide: DiarySlide,
    slideIndex: Int,
    slideCount: Int,
    media: EntryMedia?,
    sourceText: String,
    preferSourceText: Boolean,
    locationLabel: String,
    canRequestLocation: Boolean,
    theme: DiaryTheme,
    onPreviousSlide: () -> Unit,
    onNextSlide: () -> Unit,
    onInteract: () -> Unit,
    onRequestLocation: () -> Unit,
    showPageIndicator: Boolean = true,
) {
    val imageBitmap = remember(media?.localPath) {
        media?.localPath
            ?.let { BitmapFactory.decodeFile(it) }
            ?.asImageBitmap()
    }
    val primaryColor = media?.dominantColor ?: slide.gradientStart
    val shortText = sourceText.takeIf { it.isNotBlank() && (preferSourceText || it.length < 10) }
    val timeLabel = formatTimeLabel(slide.createdAt.takeIf { it > 0L } ?: media?.createdAt ?: entry.createdAt)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(548.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(argb(theme.surface))
            .clickable(onClick = onInteract)
            .pointerInput(slide.id) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        onInteract()
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        when {
                            totalDrag < -48f -> onNextSlide()
                            totalDrag > 48f -> onPreviousSlide()
                        }
                    },
                )
            }
            .testTag("diary-slide-card"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.44f)
                    .background(Brush.linearGradient(listOf(argb(primaryColor), argb(slide.gradientEnd)))),
                contentAlignment = Alignment.Center,
            ) {
                if (shortText != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(26.dp)) {
                        Text(
                            shortText,
                            color = Color.White,
                            fontSize = 26.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(timeLabel, color = Color.White.copy(alpha = 0.76f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(horizontal = 26.dp)
                            .then(
                                if (canRequestLocation) {
                                    Modifier.clickable(onClick = onRequestLocation)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.86f),
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                locationLabel,
                                color = Color.White,
                                fontSize = 21.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                timeLabel,
                                color = Color.White.copy(alpha = 0.76f),
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.56f)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                argb(slide.gradientStart).copy(alpha = 0.26f),
                                argb(slide.gradientEnd).copy(alpha = 0.44f),
                            ),
                        ),
                    ),
            ) {
                if (imageBitmap != null) {
                    RoiImage(bitmap = imageBitmap, media = media)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                                ),
                            ),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(34.dp)
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.42f)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(topStart = 140.dp, topEnd = 140.dp))
                            .background(Color.White.copy(alpha = 0.30f)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 72.dp)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.72f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)), CircleShape),
                        )
                }
                if (imageBitmap == null) {
                    Text(
                        slide.caption,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 28.dp, vertical = 20.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        }
        if (showPageIndicator) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.50f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text("${slideIndex + 1}/$slideCount", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RoiImage(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    media: EntryMedia?,
) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val src = calculateRoiSourceRect(
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            roiScale = media?.roiScale ?: 1f,
            roiOffsetX = media?.roiOffsetX ?: 0f,
            roiOffsetY = media?.roiOffsetY ?: 0f,
            dstRatio = size.width / size.height,
        )
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(src.left, src.top),
            srcSize = IntSize(src.width(), src.height()),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
        )
    }
}

@Composable
private fun SharePreviewOverlay(
    entry: DiaryEntry,
    slide: DiarySlide,
    media: EntryMedia?,
    diaryText: String,
    babyText: String,
    theme: DiaryTheme,
    locationLabel: String,
    canRequestLocation: Boolean,
    onDismiss: () -> Unit,
    onRequestLocation: () -> Unit,
    onShare: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .padding(18.dp)
            .testTag("share-preview-overlay"),
        contentAlignment = Alignment.Center,
    ) {
        GentleCard(
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "长图预览",
                    modifier = Modifier.weight(1f),
                    color = argb(theme.muted),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                CircleIconAction(
                    label = "×",
                    theme = theme,
                    onClick = onDismiss,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(argb(0xFFFFF8F1))
                    .border(1.dp, argb(theme.muted).copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .padding(14.dp),
            ) {
                Column {
                    ResultImageCard(
                        entry = entry,
                        slide = slide,
                        slideIndex = 0,
                        slideCount = 1,
                        media = media,
                        sourceText = slide.cardSummaryText().ifBlank { diaryText.trim() },
                        preferSourceText = slide.cardSummaryText().isNotBlank(),
                        locationLabel = locationLabel,
                        canRequestLocation = canRequestLocation,
                        theme = theme,
                        onPreviousSlide = {},
                        onNextSlide = {},
                        onInteract = {},
                        onRequestLocation = onRequestLocation,
                        showPageIndicator = false,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(entry.title.ifBlank { DEFAULT_DIARY_TITLE }, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(diaryText, fontSize = 14.sp, lineHeight = 22.sp)
                    if (babyText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(argb(theme.background))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Column {
                                Text("宝宝说", color = argb(theme.muted), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(5.dp))
                                Text(babyText, fontSize = 13.sp, lineHeight = 21.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("You & Me Diary", color = argb(theme.muted), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = argb(theme.primary)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("分享当前这一页")
            }
        }
    }
}

@Composable
private fun SlideDots(
    count: Int,
    selectedIndex: Int,
    theme: DiaryTheme,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == selectedIndex) 7.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (index == selectedIndex) argb(theme.primary) else argb(theme.muted).copy(alpha = 0.24f)),
            )
            if (index != count - 1) Spacer(Modifier.width(7.dp))
        }
    }
}

private fun formatTimeLabel(timestamp: Long): String {
    if (timestamp <= 0L) return "TODAY"
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
        .lowercase(Locale.ENGLISH)
}

private fun DiarySlide.cardSummaryText(): String =
    quote.trim()
        .takeUnless { it == "What you see can hold what you feel." }
        .orEmpty()

private fun DiarySlide.isSubmittedSlide(): Boolean =
    id.matches(Regex("""slide-\d{4}-\d{2}-\d{2}-\d+"""))

private const val RESULT_TOP_WEIGHT = 0.44f
private const val RESULT_IMAGE_WEIGHT = 0.56f
private const val RESULT_CARD_RATIO = 394f / 548f
private const val RESULT_IMAGE_AREA_RATIO = RESULT_CARD_RATIO / RESULT_IMAGE_WEIGHT
private const val DEFAULT_DIARY_TITLE = "今天也留一页"

@Composable
private fun DeleteSlideConfirmDialog(
    theme: DiaryTheme,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .width(286.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(argb(theme.surface).copy(alpha = 0.98f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.62f)), RoundedCornerShape(22.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                "删除这一帧？",
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = argb(theme.text),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "这一页的文字、图片和本地记录都会一起删除。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = argb(theme.muted),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text("取消", fontSize = 13.sp, color = argb(theme.muted))
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = onConfirm,
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text("删除", fontSize = 13.sp, color = argb(0xFFC4777F), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun shareCurrentSlide(
    context: Context,
    entry: DiaryEntry,
    slide: DiarySlide,
    media: EntryMedia?,
    diaryText: String,
    babyText: String,
    locationLabel: String,
    theme: DiaryTheme,
) {
    val bitmap = renderShareBitmap(entry, slide, media, diaryText, babyText, locationLabel, theme)
    val uri = saveBitmapToMediaStore(context, bitmap) ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享今天这一页"))
}

private fun renderShareBitmap(
    entry: DiaryEntry,
    slide: DiarySlide,
    media: EntryMedia?,
    diaryText: String,
    babyText: String,
    locationLabel: String,
    theme: DiaryTheme,
): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawColor(theme.background.toInt())

    val margin = 72f
    val card = RectF(margin, margin, width - margin, height - margin)
    paint.color = 0xFFFFF8F1.toInt()
    canvas.drawRoundRect(card, 42f, 42f, paint)

    val primaryColor = media?.dominantColor ?: slide.gradientStart
    val heroTop = 112f
    val heroLeft = 112f
    val heroWidth = width - 224f
    val imageHeight = heroWidth / RESULT_IMAGE_AREA_RATIO
    val colorHeight = imageHeight * RESULT_TOP_WEIGHT / RESULT_IMAGE_WEIGHT
    val heroRect = RectF(heroLeft, heroTop, heroLeft + heroWidth, heroTop + colorHeight + imageHeight)
    canvas.save()
    val heroPath = android.graphics.Path().apply {
        addRoundRect(heroRect, 36f, 36f, android.graphics.Path.Direction.CW)
    }
    canvas.clipPath(heroPath)
    paint.shader = LinearGradient(
        heroLeft,
        heroTop,
        heroLeft + heroWidth,
        heroTop + colorHeight,
        primaryColor.toInt(),
        slide.gradientEnd.toInt(),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(heroLeft, heroTop, heroLeft + heroWidth, heroTop + colorHeight, paint)
    paint.shader = null
    paint.alpha = 255

    val imageBitmap = media?.localPath?.let { BitmapFactory.decodeFile(it) }
    val imageRect = RectF(heroLeft, heroTop + colorHeight, heroLeft + heroWidth, heroTop + colorHeight + imageHeight)
    if (imageBitmap != null) {
        drawCenterCrop(canvas, imageBitmap, imageRect, paint, media)
        imageBitmap.recycle()
    } else {
        paint.color = slide.gradientEnd.toInt()
        canvas.drawRect(imageRect, paint)
        paint.color = 0x55FFFFFF
        canvas.drawCircle(heroLeft + heroWidth - 120f, imageRect.top + 130f, 90f, paint)
        canvas.drawOval(RectF(heroLeft - 90f, imageRect.bottom - 160f, heroLeft + heroWidth + 90f, imageRect.bottom + 80f), paint)
    }
    canvas.restore()

    val shareTopText = slide.cardSummaryText().ifBlank { diaryText.trim().takeIf { it.length < 10 }.orEmpty() }
    val shortText = shareTopText.takeIf { it.isNotBlank() }
    if (shortText != null) {
        val textTop = heroTop + colorHeight / 2f - 72f
        drawTextBlock(
            canvas = canvas,
            text = shortText,
            x = heroLeft + 72f,
            y = textTop,
            width = (heroWidth - 144f).roundToInt(),
            textSize = 52f,
            color = 0xFFFFFFFF.toInt(),
            isBold = true,
            maxLines = 2,
            alignment = Layout.Alignment.ALIGN_CENTER,
        )
        drawTextBlock(
            canvas = canvas,
            text = formatTimeLabel(slide.createdAt.takeIf { it > 0L } ?: media?.createdAt ?: entry.createdAt),
            x = heroLeft + 72f,
            y = textTop + 116f,
            width = (heroWidth - 144f).roundToInt(),
            textSize = 30f,
            color = 0xC2FFFFFF.toInt(),
            isBold = true,
            maxLines = 1,
            alignment = Layout.Alignment.ALIGN_CENTER,
        )
    } else {
        val rowWidth = 470f
        val rowLeft = heroLeft + (heroWidth - rowWidth) / 2f
        val rowTop = heroTop + colorHeight / 2f - 36f
        drawLocationPin(canvas, rowLeft, rowTop + 14f, 28f, 0xDFFFFFFF.toInt(), paint)
        drawTextBlock(
            canvas = canvas,
            text = locationLabel,
            x = rowLeft + 48f,
            y = rowTop,
            width = (rowWidth - 48f).roundToInt(),
            textSize = 38f,
            color = 0xFFFFFFFF.toInt(),
            isBold = true,
            maxLines = 1,
        )
        drawTextBlock(
            canvas = canvas,
            text = formatTimeLabel(slide.createdAt.takeIf { it > 0L } ?: media?.createdAt ?: entry.createdAt),
            x = rowLeft + 48f,
            y = rowTop + 48f,
            width = (rowWidth - 48f).roundToInt(),
            textSize = 25f,
            color = 0xDFFFFFFF.toInt(),
            isBold = true,
            maxLines = 1,
        )
    }

    val footerY = height - 102f
    var textY = heroRect.bottom + 52f
    textY += drawTextBlock(
        canvas = canvas,
        text = entry.title.ifBlank { DEFAULT_DIARY_TITLE },
        x = 112f,
        y = textY,
        width = 800,
        textSize = 42f,
        color = theme.text.toInt(),
        isBold = true,
        maxLines = 2,
    )
    textY += 18f
    textY += drawTextBlock(
        canvas = canvas,
        text = diaryText,
        x = 112f,
        y = textY,
        width = 800,
        textSize = 32f,
        color = theme.text.toInt(),
        isBold = false,
        maxLines = if (babyText.isNotBlank()) 3 else 6,
    )
    if (babyText.isNotBlank()) {
        textY += 26f
        paint.color = theme.background.toInt()
        val babyHeight = 184f
        val babyTop = textY.coerceAtMost(footerY - babyHeight - 42f)
        val babyCard = RectF(112f, babyTop, width - 112f, babyTop + babyHeight)
        canvas.drawRoundRect(babyCard, 30f, 30f, paint)
        drawTextBlock(canvas, "宝宝说", 148f, babyTop + 28f, 720, 26f, theme.muted.toInt(), true, 1)
        drawTextBlock(canvas, babyText, 148f, babyTop + 76f, 720, 28f, theme.text.toInt(), false, 3)
    }

    drawTextBlock(
        canvas = canvas,
        text = "You & Me Diary",
        x = 112f,
        y = footerY,
        width = 800,
        textSize = 28f,
        color = theme.muted.toInt(),
        isBold = true,
        maxLines = 1,
    )
    return bitmap
}

private fun drawCenterCrop(canvas: Canvas, bitmap: Bitmap, dst: RectF, paint: Paint, media: EntryMedia? = null) {
    val src = calculateRoiSourceRect(
        imageWidth = bitmap.width,
        imageHeight = bitmap.height,
        roiScale = media?.roiScale ?: 1f,
        roiOffsetX = media?.roiOffsetX ?: 0f,
        roiOffsetY = media?.roiOffsetY ?: 0f,
        dstRatio = dst.width() / dst.height(),
    )
    canvas.save()
    canvas.clipRect(dst)
    canvas.drawBitmap(bitmap, src, dst, paint)
    canvas.restore()
}

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
    val squareRatio = 1f
    return if (squareRatio > dstRatio) {
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

private fun drawLocationPin(canvas: Canvas, x: Float, y: Float, size: Float, color: Int, paint: Paint) {
    paint.color = color
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 4f
    canvas.drawCircle(x + size / 2f, y + size / 2f, size / 2.8f, paint)
    canvas.drawLine(x + size / 2f, y + size, x + size / 2f, y + size + 10f, paint)
    paint.style = Paint.Style.FILL
}

private fun drawTextBlock(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Int,
    textSize: Float,
    color: Int,
    isBold: Boolean,
    maxLines: Int,
    alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
): Float {
    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        typeface = if (isBold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
    }
    val layout = StaticLayout.Builder
        .obtain(text, 0, text.length, textPaint, width)
        .setAlignment(alignment)
        .setLineSpacing(6f, 1.0f)
        .setMaxLines(maxLines)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
    return layout.height.toFloat()
}

private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap): Uri? {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "you-and-me-diary-${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/YouAndMeDiary")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    resolver.openOutputStream(uri)?.use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    } ?: return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    return uri
}

private fun Context.hasCoarseLocationPermission(): Boolean =
    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private suspend fun resolveEnglishCity(context: Context): String? =
    withContext(Dispatchers.IO) {
        if (!context.hasCoarseLocationPermission()) return@withContext null
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val providers = locationManager?.getProviders(true).orEmpty()
        val location = providers
            .asSequence()
            .mapNotNull { provider -> locationManager?.getLastKnownLocation(provider) }
            .maxByOrNull { it.time }
            ?: return@withContext null
        try {
            @Suppress("DEPRECATION")
            val address = Geocoder(context, Locale.ENGLISH)
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
            address?.locality
                ?: address?.subAdminArea
                ?: address?.adminArea
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

@Preview(name = "结果页 / 大图", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun ResultScreenPreview() {
    val theme = DiaryThemes.Rose
    val entry = MockDiaryRepository.entries.last()
    val slide = entry.slides.first()
    DiaryPreviewTheme(theme = theme) {
        ResultScreen(
            entry = entry,
            slide = slide,
            slideIndex = 0,
            theme = theme,
            favoriteIds = MockDiaryRepository.defaultFavoriteSlideIds().toList(),
            selectedNoteIndex = 0,
            noteMode = "self",
            isEditingNote = false,
            sharePreviewVisible = false,
            onBack = {},
            onPreviousSlide = {},
            onNextSlide = {},
            onSelectNote = {},
            onToggleFavorite = {},
            onNoteModeChange = {},
            onEditTextChange = {},
            onEditDiaryContentChange = { _, _ -> },
            onToggleEdit = {},
            onToggleSharePreview = {},
            onDeleteSlide = {},
        )
    }
}
