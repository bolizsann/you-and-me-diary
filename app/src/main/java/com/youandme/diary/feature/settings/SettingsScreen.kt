package com.youandme.diary.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.OutlineAction
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes

@Composable
fun SettingsScreen(
    username: String,
    dueDate: String,
    themeId: String,
    theme: DiaryTheme,
    onUsernameChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onClearLocalData: () -> Unit,
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-username-input"),
            label = { Text("用户名") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = dueDate,
            onValueChange = onDueDateChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-due-date-input"),
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
        Spacer(Modifier.height(24.dp))
        Text("开发工具", fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        OutlineAction(
            label = "清空本地测试数据",
            theme = theme,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clear-local-data-button"),
            onClick = onClearLocalData,
        )
    }
}
