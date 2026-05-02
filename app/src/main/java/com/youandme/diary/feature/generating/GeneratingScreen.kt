package com.youandme.diary.feature.generating

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryTheme

@Composable
fun GeneratingScreen(theme: DiaryTheme) {
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
