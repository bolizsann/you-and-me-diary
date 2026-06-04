package com.youandme.diary.feature.record

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.youandme.diary.core.designsystem.CircleIconAction
import com.youandme.diary.core.designsystem.DiaryPreviewTheme
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

@Composable
fun RecordScreen(
    text: String,
    voiceText: String,
    isVoiceMode: Boolean,
    isVoiceRecording: Boolean,
    isVoiceTranscribing: Boolean,
    voiceElapsedSeconds: Int,
    voiceError: String,
    imagePath: String,
    imageRoiScale: Float,
    imageRoiOffsetX: Float,
    imageRoiOffsetY: Float,
    theme: DiaryTheme,
    onTextChange: (String) -> Unit,
    onVoiceModeToggle: () -> Unit,
    onVoiceRecordingStarted: () -> Unit,
    onVoiceRecordingStopped: () -> Unit,
    onVoiceElapsedChange: (Int) -> Unit,
    onVoiceAudioReady: (ByteArray) -> Unit,
    onVoiceTranscriptionFailed: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onImageRoiChange: (Float, Float, Float) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    val context = LocalContext.current
    val currentOnVoiceRecordingStarted by rememberUpdatedState(onVoiceRecordingStarted)
    val currentOnVoiceRecordingStopped by rememberUpdatedState(onVoiceRecordingStopped)
    val currentOnVoiceAudioReady by rememberUpdatedState(onVoiceAudioReady)
    val currentOnVoiceTranscriptionFailed by rememberUpdatedState(onVoiceTranscriptionFailed)
    val currentOnVoiceElapsedChange by rememberUpdatedState(onVoiceElapsedChange)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onImageSelected) },
    )
    var pendingVoiceStart by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val audioStream = remember { ByteArrayOutputStream() }
    var voiceRecorder by remember { mutableStateOf<AudioRecord?>(null) }
    fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun releaseRecorder() {
        runCatching {
            voiceRecorder?.takeIf { it.recordingState == AudioRecord.RECORDSTATE_RECORDING }?.stop()
        }
        runCatching { voiceRecorder?.release() }
        voiceRecorder = null
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            pendingVoiceStart = false
            if (granted) {
                currentOnVoiceTranscriptionFailed("再按住说话")
            } else {
                currentOnVoiceTranscriptionFailed("需要录音权限")
            }
        },
    )

    fun startRecording() {
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            currentOnVoiceTranscriptionFailed("录音启动失败")
            return
        }
        val recorder = createVoiceRecorder(minBufferSize)
        val buffer = ByteArray(minBufferSize)
        audioStream.reset()
        voiceRecorder = recorder
        currentOnVoiceRecordingStarted()
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                recorder.startRecording()
                while (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val bytesRead = recorder.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        audioStream.write(buffer, 0, bytesRead)
                    }
                }
            }.onFailure {
                releaseRecorder()
                currentOnVoiceTranscriptionFailed("录音失败")
            }
        }
    }

    fun startVoiceInput() {
        if (isVoiceRecording || isVoiceTranscribing) return
        if (!hasAudioPermission()) {
            pendingVoiceStart = true
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        startRecording()
    }

    fun stopVoiceInput() {
        val recorder = voiceRecorder
        if (!isVoiceRecording && recorder == null) return
        currentOnVoiceRecordingStopped()
        runCatching {
            recorder?.takeIf { it.recordingState == AudioRecord.RECORDSTATE_RECORDING }?.stop()
        }
        releaseRecorder()
        val audioBytes = audioStream.toByteArray()
        audioStream.reset()
        if (audioBytes.size < MIN_VOICE_BYTES) {
            currentOnVoiceTranscriptionFailed("录音太短，再试一次")
            return
        }
        currentOnVoiceAudioReady(audioBytes)
    }

    DisposableEffect(Unit) {
        onDispose {
            releaseRecorder()
            audioStream.reset()
        }
    }

    LaunchedEffect(isVoiceRecording) {
        if (isVoiceRecording) {
            for (second in 1..MAX_VOICE_SECONDS) {
                delay(1_000)
                currentOnVoiceElapsedChange(second)
                if (second == MAX_VOICE_SECONDS) {
                    stopVoiceInput()
                }
            }
        }
    }

    val canSubmit = text.isNotBlank() || voiceText.isNotBlank() || imagePath.isNotBlank()

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
            isVoiceMode = isVoiceMode,
            isVoiceRecording = isVoiceRecording,
            isVoiceTranscribing = isVoiceTranscribing,
            voiceElapsedSeconds = voiceElapsedSeconds,
            voiceError = voiceError,
            canSubmit = canSubmit,
            theme = theme,
            onTextChange = onTextChange,
            onVoiceModeToggle = onVoiceModeToggle,
            onVoicePressStart = ::startVoiceInput,
            onVoicePressEnd = ::stopVoiceInput,
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
    isVoiceMode: Boolean,
    isVoiceRecording: Boolean,
    isVoiceTranscribing: Boolean,
    voiceElapsedSeconds: Int,
    voiceError: String,
    canSubmit: Boolean,
    theme: DiaryTheme,
    onTextChange: (String) -> Unit,
    onVoiceModeToggle: () -> Unit,
    onVoicePressStart: () -> Unit,
    onVoicePressEnd: () -> Unit,
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
            if (isVoiceMode) {
                VoiceHoldButton(
                    isRecording = isVoiceRecording,
                    isTranscribing = isVoiceTranscribing,
                    elapsedSeconds = voiceElapsedSeconds,
                    errorText = voiceError,
                    theme = theme,
                    onPressStart = onVoicePressStart,
                    onPressEnd = onVoicePressEnd,
                )
            } else {
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
        }
        IconButton(
            onClick = onVoiceModeToggle,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(argb(theme.background).copy(alpha = 0.76f)),
        ) {
            Icon(
                if (isVoiceMode) Icons.Outlined.Keyboard else Icons.Outlined.Mic,
                contentDescription = if (isVoiceMode) "切换键盘输入" else "语音输入",
                tint = argb(theme.muted),
            )
        }
        if (!isVoiceMode) {
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
}

@Composable
private fun VoiceHoldButton(
    isRecording: Boolean,
    isTranscribing: Boolean,
    elapsedSeconds: Int,
    errorText: String,
    theme: DiaryTheme,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
) {
    val label = when {
        isTranscribing -> "正在转录..."
        isRecording -> "正在记录 ${elapsedSeconds.formatVoiceDuration()}"
        errorText.isNotBlank() -> errorText
        else -> "按住 说话"
    }
    val background = when {
        isRecording -> argb(theme.primary).copy(alpha = 0.18f)
        isTranscribing -> argb(theme.accent).copy(alpha = 0.18f)
        else -> argb(theme.background).copy(alpha = 0.58f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .border(1.dp, argb(theme.primary).copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .pointerInput(isRecording, isTranscribing) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!isRecording && !isTranscribing) {
                        onPressStart()
                    }
                    waitForUpOrCancellation()
                    onPressEnd()
                }
            }
            .testTag("record-voice-hold"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isRecording || isTranscribing) argb(theme.primary) else argb(theme.muted),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(name = "记录今天 / 新版", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun RecordScreenPreview() {
    val theme = DiaryThemes.Rose
    DiaryPreviewTheme(theme = theme) {
        RecordScreen(
            text = "今天下午感觉到了一点点胎动。",
            voiceText = "",
            isVoiceMode = false,
            isVoiceRecording = false,
            isVoiceTranscribing = false,
            voiceElapsedSeconds = 0,
            voiceError = "",
            imagePath = "",
            imageRoiScale = 1f,
            imageRoiOffsetX = 0f,
            imageRoiOffsetY = 0f,
            theme = theme,
            onTextChange = {},
            onVoiceModeToggle = {},
            onVoiceRecordingStarted = {},
            onVoiceRecordingStopped = {},
            onVoiceElapsedChange = {},
            onVoiceAudioReady = {},
            onVoiceTranscriptionFailed = {},
            onImageSelected = {},
            onImageRoiChange = { _, _, _ -> },
            onBack = {},
            onSubmit = {},
        )
    }
}

private fun Int.formatVoiceDuration(): String {
    val safeSeconds = coerceAtLeast(0)
    return "%02d:%02d".format(safeSeconds / 60, safeSeconds % 60)
}

@Suppress("DEPRECATION")
private fun createVoiceRecorder(minBufferSize: Int): AudioRecord =
    AudioRecord(
        MediaRecorder.AudioSource.MIC,
        SAMPLE_RATE,
        CHANNEL_CONFIG,
        AUDIO_FORMAT,
        minBufferSize,
    )

private const val MAX_VOICE_SECONDS = 60
private const val SAMPLE_RATE = 16_000
private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
private const val MIN_VOICE_BYTES = SAMPLE_RATE / 2
