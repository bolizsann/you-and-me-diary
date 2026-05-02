package com.youandme.diary.feature.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.PlaceholderTool
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryTheme

@Composable
fun RecordScreen(
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
