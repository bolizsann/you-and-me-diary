package com.youandme.diary.feature.generating

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.DiaryPreviewTheme
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes

@Composable
fun GeneratingScreen(
    theme: DiaryTheme,
) {
    val blinkTransition = rememberInfiniteTransition(label = "generating-blink")
    val blinkAlpha by blinkTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 980),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "generating-blink-alpha",
    )
    DiaryPage(theme = theme, modifier = Modifier.testTag("generating-screen")) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "正在把这一刻收录起来...",
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "情绪接收中...",
                    modifier = Modifier
                        .alpha(blinkAlpha)
                        .testTag("generating-blink-text"),
                    color = argb(theme.muted),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(name = "生成中", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun GeneratingScreenPreview() {
    val theme = DiaryThemes.Rose
    DiaryPreviewTheme(theme = theme) {
        GeneratingScreen(theme = theme)
    }
}
