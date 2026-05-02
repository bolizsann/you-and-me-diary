package com.youandme.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YouAndMeDiaryApp()
        }
    }
}

private enum class AppScreen(val label: String) {
    Home("首页"),
    Record("记录"),
    Generating("生成中"),
    Result("结果"),
    Timeline("时间线"),
    Memory("纪念册"),
    Settings("设置"),
}

@Composable
fun YouAndMeDiaryApp() {
    val entries = remember { MockDiaryRepository.entries }
    var route by rememberSaveable { mutableStateOf(AppScreen.Home.name) }
    var selectedEntryId by rememberSaveable { mutableStateOf(entries.last().id) }
    var selectedSlideIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedNoteIndex by rememberSaveable { mutableStateOf<Int?>(0) }
    var noteMode by rememberSaveable { mutableStateOf("self") }
    var isEditingNote by rememberSaveable { mutableStateOf(false) }
    var sharePreviewVisible by rememberSaveable { mutableStateOf(false) }
    var username by rememberSaveable { mutableStateOf("你") }
    var dueDate by rememberSaveable { mutableStateOf("") }
    var themeId by rememberSaveable { mutableStateOf(DiaryThemes.Rose.id) }
    var recordText by rememberSaveable {
        mutableStateOf("今天下午好像感觉到了一点点胎动，工作有点累，但那一下让我开心了很久。")
    }
    val favoriteIds = remember {
        mutableStateListOf<String>().apply {
            addAll(MockDiaryRepository.defaultFavoriteSlideIds())
        }
    }

    val theme = DiaryThemes.all.firstOrNull { it.id == themeId } ?: DiaryThemes.Rose
    val selectedEntry = entries.firstOrNull { it.id == selectedEntryId } ?: entries.last()
    val selectedSlide = selectedEntry.slides.getOrElse(selectedSlideIndex) { selectedEntry.slides.first() }

    if (route == AppScreen.Generating.name) {
        LaunchedEffect(selectedEntryId, recordText) {
            delay(850)
            route = AppScreen.Result.name
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = argb(theme.primary),
            secondary = argb(theme.accent),
            background = argb(theme.background),
            surface = argb(theme.surface),
            onPrimary = Color.White,
            onSecondary = argb(theme.text),
            onBackground = argb(theme.text),
            onSurface = argb(theme.text),
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = argb(theme.background),
        ) {
            Scaffold(
                containerColor = argb(theme.background),
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(argb(theme.background)),
                ) {
                    when (route) {
                        AppScreen.Record.name -> RecordScreen(
                            text = recordText,
                            theme = theme,
                            onTextChange = { recordText = it },
                            onBack = { route = AppScreen.Home.name },
                            onSubmit = {
                                selectedEntryId = entries.last().id
                                selectedSlideIndex = 0
                                selectedNoteIndex = 0
                                isEditingNote = false
                                sharePreviewVisible = false
                                route = AppScreen.Generating.name
                            },
                        )

                        AppScreen.Generating.name -> GeneratingScreen(theme)

                        AppScreen.Result.name -> ResultScreen(
                            entry = selectedEntry,
                            slide = selectedSlide,
                            slideIndex = selectedSlideIndex,
                            theme = theme,
                            favoriteIds = favoriteIds,
                            selectedNoteIndex = selectedNoteIndex,
                            noteMode = noteMode,
                            isEditingNote = isEditingNote,
                            sharePreviewVisible = sharePreviewVisible,
                            onBack = { route = AppScreen.Home.name },
                            onPreviousSlide = {
                                selectedSlideIndex =
                                    (selectedSlideIndex - 1).floorMod(selectedEntry.slides.size)
                                selectedNoteIndex = 0
                                isEditingNote = false
                                sharePreviewVisible = false
                            },
                            onNextSlide = {
                                selectedSlideIndex =
                                    (selectedSlideIndex + 1).floorMod(selectedEntry.slides.size)
                                selectedNoteIndex = 0
                                isEditingNote = false
                                sharePreviewVisible = false
                            },
                            onSelectNote = {
                                selectedNoteIndex = it
                                isEditingNote = false
                            },
                            onToggleFavorite = {
                                val favoriteId = MockDiaryRepository.favoriteId(selectedEntry.id, selectedSlide.id)
                                if (favoriteIds.contains(favoriteId)) {
                                    favoriteIds.remove(favoriteId)
                                } else {
                                    favoriteIds.add(favoriteId)
                                }
                            },
                            onNoteModeChange = {
                                noteMode = it
                                isEditingNote = false
                            },
                            onToggleEdit = { isEditingNote = !isEditingNote },
                            onToggleSharePreview = { sharePreviewVisible = !sharePreviewVisible },
                        )

                        AppScreen.Timeline.name -> TimelineScreen(
                            entries = entries,
                            selectedEntry = selectedEntry,
                            theme = theme,
                            onBack = { route = AppScreen.Home.name },
                            onSelectEntry = {
                                selectedEntryId = it.id
                                selectedSlideIndex = 0
                                selectedNoteIndex = 0
                            },
                            onOpenResult = { route = AppScreen.Result.name },
                        )

                        AppScreen.Memory.name -> MemoryScreen(
                            entries = entries,
                            favoriteIds = favoriteIds,
                            theme = theme,
                            onBack = { route = AppScreen.Home.name },
                            onOpenSlide = { entry, slideIndex ->
                                selectedEntryId = entry.id
                                selectedSlideIndex = slideIndex
                                selectedNoteIndex = 0
                                sharePreviewVisible = false
                                route = AppScreen.Result.name
                            },
                        )

                        AppScreen.Settings.name -> SettingsScreen(
                            username = username,
                            dueDate = dueDate,
                            themeId = themeId,
                            theme = theme,
                            onUsernameChange = { username = it },
                            onDueDateChange = { dueDate = it },
                            onThemeChange = { themeId = it },
                            onBack = { route = AppScreen.Home.name },
                        )

                        else -> HomeScreen(
                            username = username,
                            theme = theme,
                            onRecord = { route = AppScreen.Record.name },
                            onTimeline = { route = AppScreen.Timeline.name },
                            onMemory = { route = AppScreen.Memory.name },
                            onSettings = { route = AppScreen.Settings.name },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    username: String,
    theme: DiaryTheme,
    onRecord: () -> Unit,
    onTimeline: () -> Unit,
    onMemory: () -> Unit,
    onSettings: () -> Unit,
) {
    DiaryPage(
        theme = theme,
        modifier = Modifier.testTag("home-screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(argb(theme.surface).copy(alpha = 0.76f))
                    .border(BorderStroke(1.dp, argb(theme.muted).copy(alpha = 0.12f)), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text("You & Me Diary", color = argb(theme.muted), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onSettings,
                modifier = Modifier
                    .size(42.dp)
                    .testTag("settings-button"),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = argb(theme.surface).copy(alpha = 0.76f),
                    contentColor = argb(theme.text),
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Text("⚙", fontSize = 17.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .padding(top = 28.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DecorativeHalo(theme)
            Spacer(Modifier.height(18.dp))
            Text(
                text = "${username.ifBlank { "你" }}和小小的 ta。",
                fontSize = 33.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text("今天，轻轻记一页。", color = argb(theme.muted), fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(argb(theme.surface).copy(alpha = 0.68f))
                    .border(BorderStroke(1.dp, argb(theme.muted).copy(alpha = 0.08f)), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = "今天虽然累，但也有一个忽然变软的瞬间。",
                    color = argb(theme.muted),
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                )
            }
        }

        Button(
            onClick = onRecord,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("record-button"),
            colors = ButtonDefaults.buttonColors(containerColor = argb(theme.primary)),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("记录今天", fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        HomeSoftAction("打开时间线", "像日历一样回看那些已经走过来的日子。", theme, onTimeline)
        Spacer(Modifier.height(8.dp))
        HomeSoftAction("打开纪念册", "把真正想留给自己和宝宝的页面收进去。", theme, onMemory)
    }
}

@Composable
private fun DecorativeHalo(theme: DiaryTheme) {
    Box(
        modifier = Modifier
            .size(184.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.94f),
                        argb(theme.primary).copy(alpha = 0.58f),
                        argb(0xFFFFF6EE).copy(alpha = 0.86f),
                        argb(0xFFFFF6EE).copy(alpha = 0.08f),
                    ),
                ),
            ),
    )
}

@Composable
private fun HomeSoftAction(
    title: String,
    subtitle: String,
    theme: DiaryTheme,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(argb(theme.surface).copy(alpha = 0.78f))
            .border(BorderStroke(1.dp, argb(theme.muted).copy(alpha = 0.12f)), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(title, color = argb(theme.text), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = argb(theme.muted), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecordScreen(
    text: String,
    theme: DiaryTheme,
    onTextChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
) {
    DiaryPage(
        title = "记录今天",
        theme = theme,
        onBack = onBack,
        modifier = Modifier.testTag("record-screen"),
    ) {
        Text("把今天发生了什么直接写下来就好。语音和图片今天先作为 mock 入口保留。", color = argb(theme.muted))
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag("record-input"),
            label = { Text("今天的身体、情绪或小事") },
            shape = RoundedCornerShape(18.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlaceholderTool("语音", theme, Modifier.weight(1f))
            PlaceholderTool("图片", theme, Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onSubmit,
            enabled = text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate-button"),
            colors = ButtonDefaults.buttonColors(containerColor = argb(theme.primary)),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("生成今天的日记")
        }
    }
}

@Composable
private fun GeneratingScreen(theme: DiaryTheme) {
    DiaryPage(theme = theme, modifier = Modifier.testTag("generating-screen")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("正在把今天轻轻收起来", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text("mock 数据生成中...", color = argb(theme.muted))
            }
        }
    }
}

@Composable
private fun ResultScreen(
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
    onToggleEdit: () -> Unit,
    onToggleSharePreview: () -> Unit,
) {
    val favoriteId = MockDiaryRepository.favoriteId(entry.id, slide.id)
    val currentNote = selectedNoteIndex?.let { slide.notes.getOrNull(it) }
    var editedText by rememberSaveable(entry.id, slide.id, selectedNoteIndex, noteMode) {
        mutableStateOf(if (noteMode == "self") currentNote?.selfText.orEmpty() else currentNote?.babyText.orEmpty())
    }

    DiaryPage(
        title = entry.dateLabel,
        theme = theme,
        onBack = onBack,
        modifier = Modifier.testTag("result-screen"),
    ) {
        Text("已自动记入时间线", color = argb(theme.muted), fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(entry.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        DiarySlideCard(
            entry = entry,
            slide = slide,
            slideIndex = slideIndex,
            theme = theme,
            selectedNoteIndex = selectedNoteIndex,
            onSelectNote = onSelectNote,
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPreviousSlide) { Text("上一张") }
            Text("${slideIndex + 1} / ${entry.slides.size}", color = argb(theme.muted))
            TextButton(onClick = onNextSlide) { Text("下一张") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineAction(
                label = if (favoriteIds.contains(favoriteId)) "已收藏" else "收藏",
                theme = theme,
                modifier = Modifier
                    .weight(1f)
                    .testTag("favorite-button"),
                onClick = onToggleFavorite,
            )
            OutlineAction(
                label = "分享预览",
                theme = theme,
                modifier = Modifier.weight(1f),
                onClick = onToggleSharePreview,
            )
        }

        Spacer(Modifier.height(12.dp))
        GentleCard(theme = theme) {
            Text("给你的小安慰", color = argb(theme.muted), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(entry.comfortText, fontSize = 15.sp, lineHeight = 23.sp)
        }

        Spacer(Modifier.height(12.dp))
        NotePanel(
            note = currentNote,
            noteMode = noteMode,
            editedText = editedText,
            isEditing = isEditingNote,
            theme = theme,
            onModeChange = onNoteModeChange,
            onEditChange = { editedText = it },
            onToggleEdit = onToggleEdit,
        )

        if (sharePreviewVisible) {
            Spacer(Modifier.height(16.dp))
            SharePreview(entry, slide, currentNote, noteMode, editedText, theme)
        }
    }
}

@Composable
private fun TimelineScreen(
    entries: List<DiaryEntry>,
    selectedEntry: DiaryEntry,
    theme: DiaryTheme,
    onBack: () -> Unit,
    onSelectEntry: (DiaryEntry) -> Unit,
    onOpenResult: () -> Unit,
) {
    var visibleMonthText by rememberSaveable { mutableStateOf(selectedEntry.dateId.take(7)) }
    val visibleMonth = remember(visibleMonthText) { YearMonth.parse(visibleMonthText) }
    val entriesInMonth = entries.filter { it.dateId.startsWith(visibleMonthText) }
    val entryByDay = entriesInMonth.associateBy { it.dateId.takeLast(2).toInt() }
    val displayedEntry = entriesInMonth.firstOrNull { it.id == selectedEntry.id } ?: entriesInMonth.firstOrNull()

    DiaryPage(
        title = "时间线",
        theme = theme,
        onBack = onBack,
        modifier = Modifier.testTag("timeline-screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = {
                visibleMonthText = visibleMonth.minusMonths(1).toString()
            }) {
                Text("上月")
            }
            Text(
                "${visibleMonth.year} 年 ${visibleMonth.monthValue} 月",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = {
                visibleMonthText = visibleMonth.plusMonths(1).toString()
            }) {
                Text("下月")
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = argb(theme.muted),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val leadingEmptyDays = visibleMonth.atDay(1).dayOfWeek.value - 1
        val cells = List(leadingEmptyDays) { null } + (1..visibleMonth.lengthOfMonth()).map { it }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    week.forEach { day ->
                        val entry = day?.let { entryByDay[it] }
                        val isSelected = entry?.id == selectedEntry.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        isSelected -> argb(theme.primary).copy(alpha = 0.22f)
                                        entry != null -> argb(entry.moodColor).copy(alpha = 0.16f)
                                        else -> Color.White.copy(alpha = 0.35f)
                                    },
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) argb(theme.primary) else Color.White.copy(alpha = 0.38f),
                                    ),
                                    RoundedCornerShape(16.dp),
                                )
                                .clickable(enabled = entry != null) { entry?.let(onSelectEntry) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day?.toString().orEmpty(), fontSize = 13.sp)
                                Text(entry?.moodEmoji ?: "", fontSize = 13.sp)
                            }
                        }
                    }
                    repeat(7 - week.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        GentleCard(theme = theme) {
            if (displayedEntry == null) {
                Text("这个月还没有记录", color = argb(theme.muted), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text("切换回有颜色的小圆点月份，或从今天开始写一页。", lineHeight = 24.sp)
            } else {
                Text(displayedEntry.dateLabel, color = argb(theme.muted), fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(displayedEntry.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(displayedEntry.timelineSummary, lineHeight = 24.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onSelectEntry(displayedEntry)
                        onOpenResult()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = argb(theme.primary)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("打开这一天")
                }
            }
        }
    }
}

@Composable
private fun MemoryScreen(
    entries: List<DiaryEntry>,
    favoriteIds: List<String>,
    theme: DiaryTheme,
    onBack: () -> Unit,
    onOpenSlide: (DiaryEntry, Int) -> Unit,
) {
    val favorites = entries.flatMap { entry ->
        entry.slides.mapIndexedNotNull { index, slide ->
            val id = MockDiaryRepository.favoriteId(entry.id, slide.id)
            if (favoriteIds.contains(id)) Triple(entry, index, slide) else null
        }
    }

    DiaryPage(
        title = "纪念册",
        theme = theme,
        onBack = onBack,
        modifier = Modifier.testTag("memory-screen"),
    ) {
        Text("${favorites.size} 页已收藏", color = argb(theme.muted))
        Spacer(Modifier.height(16.dp))
        if (favorites.isEmpty()) {
            GentleCard(theme = theme) {
                Text("还没有收藏。回到结果页，把想留给未来的那一张放进来。", lineHeight = 25.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                favorites.forEach { (entry, index, slide) ->
                    GentleCard(
                        theme = theme,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSlide(entry, index) },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(78.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(argb(slide.gradientStart), argb(slide.gradientEnd)),
                                        ),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(entry.moodEmoji, fontSize = 24.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.dateLabel, color = argb(theme.muted), fontSize = 13.sp)
                                Text(slide.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                Text(
                                    slide.caption,
                                    color = argb(theme.muted),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    username: String,
    dueDate: String,
    themeId: String,
    theme: DiaryTheme,
    onUsernameChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    DiaryPage(
        title = "设置",
        theme = theme,
        onBack = onBack,
        modifier = Modifier.testTag("settings-screen"),
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("用户名") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = dueDate,
            onValueChange = onDueDateChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("预产期，例如 2026-11-08") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text("配色方案", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(DiaryThemes.all) { option ->
                FilterChip(
                    selected = option.id == themeId,
                    onClick = { onThemeChange(option.id) },
                    label = { Text(option.label) },
                )
            }
        }
    }
}

@Composable
private fun DiarySlideCard(
    entry: DiaryEntry,
    slide: DiarySlide,
    slideIndex: Int,
    theme: DiaryTheme,
    selectedNoteIndex: Int?,
    onSelectNote: (Int) -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(292.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(argb(slide.gradientStart), argb(slide.gradientEnd))))
            .testTag("diary-slide-card"),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .fillMaxWidth(0.78f),
        ) {
            Text(entry.moodEmoji, fontSize = 24.sp)
            Spacer(Modifier.height(14.dp))
            Text(
                slide.quote,
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 30.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(slide.caption, color = Color.White.copy(alpha = 0.90f), fontSize = 14.sp, lineHeight = 21.sp)
        }

        Text(
            text = "0${slideIndex + 1}",
            color = Color.White.copy(alpha = 0.45f),
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
        )

        slide.notes.forEachIndexed { index, note ->
            Button(
                onClick = { onSelectNote(index) },
                modifier = Modifier
                    .offset(
                        x = maxWidth * note.x - 18.dp,
                        y = maxHeight * note.y - 18.dp,
                    )
                    .size(34.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedNoteIndex == index) Color.White else Color.White.copy(alpha = 0.82f),
                    contentColor = argb(theme.primary),
                ),
            ) {
                Text("${index + 1}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun NotePanel(
    note: DiaryNote?,
    noteMode: String,
    editedText: String,
    isEditing: Boolean,
    theme: DiaryTheme,
    onModeChange: (String) -> Unit,
    onEditChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
) {
    GentleCard(theme = theme) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(note?.label ?: "图片解释", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            TextButton(onClick = { onModeChange("self") }) {
                Text(if (noteMode == "self") "我*" else "我")
            }
            TextButton(onClick = { onModeChange("baby") }) {
                Text(if (noteMode == "baby") "宝*" else "宝")
            }
            TextButton(onClick = onToggleEdit) {
                Text(if (isEditing) "返回" else "编辑")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (isEditing) {
            OutlinedTextField(
                value = editedText,
                onValueChange = onEditChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                shape = RoundedCornerShape(16.dp),
            )
        } else {
            Text(
                text = editedText.ifBlank { "选择一处注释。" },
                fontSize = 15.sp,
                lineHeight = 23.sp,
            )
        }
    }
}

@Composable
private fun SharePreview(
    entry: DiaryEntry,
    slide: DiarySlide,
    note: DiaryNote?,
    noteMode: String,
    editedText: String,
    theme: DiaryTheme,
) {
    GentleCard(theme = theme, modifier = Modifier.testTag("share-preview")) {
        Text("长图预览", color = argb(theme.muted), fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(argb(slide.gradientStart), argb(slide.gradientEnd)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                slide.quote,
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(28.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(entry.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(if (noteMode == "self") "给自己的解释" else "给宝宝的解释", color = argb(theme.muted))
        Spacer(Modifier.height(6.dp))
        Text(editedText.ifBlank { note?.selfText.orEmpty() }, lineHeight = 25.sp)
        Spacer(Modifier.height(10.dp))
        Text("今天先展示预览占位，后续接入 Bitmap 导出和 Android ShareSheet。", color = argb(theme.muted), fontSize = 13.sp)
    }
}

@Composable
private fun DiaryPage(
    theme: DiaryTheme,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        if (title != null || onBack != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    TextButton(onClick = onBack) { Text("返回") }
                }
                Text(
                    text = title.orEmpty(),
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (onBack == null) TextAlign.Start else TextAlign.Center,
                )
                if (onBack != null) Spacer(Modifier.width(56.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        content()
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun GentleCard(
    theme: DiaryTheme,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = argb(theme.surface).copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun OutlineAction(
    label: String,
    theme: DiaryTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .border(BorderStroke(1.dp, argb(theme.primary).copy(alpha = 0.45f)), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(label, color = argb(theme.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaceholderTool(label: String, theme: DiaryTheme, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(argb(theme.surface).copy(alpha = 0.78f))
            .border(BorderStroke(1.dp, argb(theme.primary).copy(alpha = 0.28f)), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = argb(theme.muted))
    }
}

private fun argb(color: Long): Color = Color(color.toInt())

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other

@Preview(name = "首页 / 日记封面", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun HomeScreenPreview() {
    val theme = DiaryThemes.Rose
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = argb(theme.primary),
            secondary = argb(theme.accent),
            background = argb(theme.background),
            surface = argb(theme.surface),
            onPrimary = Color.White,
            onSecondary = argb(theme.text),
            onBackground = argb(theme.text),
            onSurface = argb(theme.text),
        ),
    ) {
        Surface(color = argb(theme.background)) {
            HomeScreen(
                username = "你",
                theme = theme,
                onRecord = {},
                onTimeline = {},
                onMemory = {},
                onSettings = {},
            )
        }
    }
}
