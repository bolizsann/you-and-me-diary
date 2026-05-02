package com.youandme.diary.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes

@Composable
fun HomeScreen(
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
