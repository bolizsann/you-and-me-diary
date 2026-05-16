package com.youandme.diary.feature.record

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.CircleIconAction
import com.youandme.diary.core.designsystem.DiaryPreviewTheme
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes
import kotlin.math.roundToInt

@Composable
fun RecordScreen(
    text: String,
    imagePath: String,
    imageRoiScale: Float,
    imageRoiOffsetX: Float,
    imageRoiOffsetY: Float,
    theme: DiaryTheme,
    onTextChange: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onImageRoiChange: (Float, Float, Float) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onImageSelected) },
    )
    val canSubmit = text.isNotBlank() || imagePath.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        argb(theme.background).copy(alpha = 0.96f),
                        argb(theme.background),
                    ),
                ),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp)
            .testTag("record-screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconAction(
                label = "<",
                theme = theme,
                modifier = Modifier.testTag("page-back-button"),
                onClick = onBack,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("记录今天", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text("留下一张图，也留下一点心情", color = argb(theme.muted), fontSize = 12.sp)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ImagePickerCard(
                imagePath = imagePath,
                roiScale = imageRoiScale,
                roiOffsetX = imageRoiOffsetX,
                roiOffsetY = imageRoiOffsetY,
                theme = theme,
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRoiChange = onImageRoiChange,
            )
        }

        ComposerBar(
            text = text,
            canSubmit = canSubmit,
            theme = theme,
            onTextChange = onTextChange,
            onSubmit = onSubmit,
        )
    }
}

@Composable
private fun ImagePickerCard(
    imagePath: String,
    roiScale: Float,
    roiOffsetX: Float,
    roiOffsetY: Float,
    theme: DiaryTheme,
    onClick: () -> Unit,
    onRoiChange: (Float, Float, Float) -> Unit,
) {
    val imageBitmap = remember(imagePath) {
        imagePath
            .takeIf { it.isNotBlank() }
            ?.let { BitmapFactory.decodeFile(it) }
            ?.asImageBitmap()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        argb(theme.primary).copy(alpha = 0.26f),
                        argb(theme.accent).copy(alpha = 0.20f),
                        argb(0xFFD7C19F).copy(alpha = 0.30f),
                    ),
                ),
            )
            .border(1.dp, argb(theme.muted).copy(alpha = 0.12f), RoundedCornerShape(30.dp))
            .then(if (imageBitmap == null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag("record-image-picker"),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            CroppableImage(
                image = imageBitmap,
                initialScale = roiScale,
                initialOffsetX = roiOffsetX,
                initialOffsetY = roiOffsetY,
                onRoiChange = onRoiChange,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.20f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("更换图片", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.74f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", color = argb(theme.primary), fontSize = 30.sp, lineHeight = 30.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text("点击上传图片", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(
                    "What you see can hold what you feel.",
                    color = argb(theme.muted),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CroppableImage(
    image: ImageBitmap,
    initialScale: Float,
    initialOffsetX: Float,
    initialOffsetY: Float,
    onRoiChange: (Float, Float, Float) -> Unit,
) {
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(image) { mutableFloatStateOf(initialScale.coerceIn(1f, 4f)) }
    var offset by remember(image, cardSize) {
        mutableStateOf(Offset(cardSize.width * initialOffsetX, cardSize.height * initialOffsetY))
    }
    fun clampOffset(candidate: Offset, candidateScale: Float): Offset {
        if (cardSize.width == 0 || cardSize.height == 0) return Offset.Zero
        val baseScale = maxOf(
            cardSize.width.toFloat() / image.width.toFloat(),
            cardSize.height.toFloat() / image.height.toFloat(),
        )
        val renderedWidth = image.width * baseScale * candidateScale
        val renderedHeight = image.height * baseScale * candidateScale
        val maxX = ((renderedWidth - cardSize.width) / 2f).coerceAtLeast(0f)
        val maxY = ((renderedHeight - cardSize.height) / 2f).coerceAtLeast(0f)
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY),
        )
    }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        val nextOffset = clampOffset(
            candidate = offset + panChange,
            candidateScale = nextScale,
        )
        scale = nextScale
        offset = nextOffset
        onRoiChange(
            nextScale,
            if (cardSize.width == 0) 0f else nextOffset.x / cardSize.width,
            if (cardSize.height == 0) 0f else nextOffset.y / cardSize.height,
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { cardSize = it }
            .transformable(transformState),
    ) {
        val baseScale = maxOf(
            size.width / image.width.toFloat(),
            size.height / image.height.toFloat(),
        )
        val renderedWidth = image.width * baseScale * scale
        val renderedHeight = image.height * baseScale * scale
        val left = (size.width - renderedWidth) / 2f + offset.x
        val top = (size.height - renderedHeight) / 2f + offset.y
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(renderedWidth.roundToInt().coerceAtLeast(1), renderedHeight.roundToInt().coerceAtLeast(1)),
        )
    }
}

@Composable
private fun ComposerBar(
    text: String,
    canSubmit: Boolean,
    theme: DiaryTheme,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val estimatedLines = remember(text) {
        text
            .ifBlank { "" }
            .lines()
            .sumOf { line -> (line.length / 18).coerceAtLeast(0) + 1 }
    }
    val showScrollBar = estimatedLines > 6
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(argb(theme.surface).copy(alpha = 0.82f))
            .border(1.dp, argb(theme.muted).copy(alpha = 0.10f), RoundedCornerShape(26.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp, max = 156.dp)
                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (text.isEmpty()) {
                Text("今天想说...", color = argb(theme.muted).copy(alpha = 0.76f), fontSize = 15.sp)
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (showScrollBar) 10.dp else 0.dp)
                    .verticalScroll(scrollState)
                    .testTag("record-input"),
                textStyle = TextStyle(
                    color = argb(theme.text),
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                ),
                minLines = 1,
                maxLines = Int.MAX_VALUE,
                cursorBrush = SolidColor(argb(theme.primary)),
            )
            if (showScrollBar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(2.dp)
                        .height(96.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(argb(theme.muted).copy(alpha = 0.12f)),
                ) {
                    val progress = if (scrollState.maxValue == 0) 0f else scrollState.value.toFloat() / scrollState.maxValue
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (progress * 64).dp)
                            .width(2.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(argb(theme.muted).copy(alpha = 0.42f)),
                    )
                }
            }
        }
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(argb(theme.background).copy(alpha = 0.76f)),
        ) {
            Icon(Icons.Outlined.Mic, contentDescription = "语音输入", tint = argb(theme.muted))
        }
        IconButton(
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    if (canSubmit) {
                        Brush.linearGradient(listOf(argb(theme.primary), argb(0xFFC4777F)))
                    } else {
                        Brush.linearGradient(listOf(argb(theme.muted).copy(alpha = 0.26f), argb(theme.muted).copy(alpha = 0.18f)))
                    },
                )
                .testTag("generate-button"),
        ) {
            Text("↑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(name = "记录今天 / 新版", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun RecordScreenPreview() {
    val theme = DiaryThemes.Rose
    DiaryPreviewTheme(theme = theme) {
        RecordScreen(
            text = "今天下午感觉到了一点点胎动。",
            imagePath = "",
            imageRoiScale = 1f,
            imageRoiOffsetX = 0f,
            imageRoiOffsetY = 0f,
            theme = theme,
            onTextChange = {},
            onImageSelected = {},
            onImageRoiChange = { _, _, _ -> },
            onBack = {},
            onSubmit = {},
        )
    }
}
