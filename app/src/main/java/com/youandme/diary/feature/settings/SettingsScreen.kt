package com.youandme.diary.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youandme.diary.core.designsystem.DiaryPage
import com.youandme.diary.core.designsystem.DiaryPreviewTheme
import com.youandme.diary.core.designsystem.OutlineAction
import com.youandme.diary.core.designsystem.argb
import com.youandme.diary.domain.model.DiaryTheme
import com.youandme.diary.domain.model.DiaryThemes
import com.youandme.diary.domain.model.GenerationModes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    username: String,
    dueDate: String,
    themeId: String,
    theme: DiaryTheme,
    generationMode: String,
    onUsernameChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onGenerationModeChange: (String) -> Unit,
    onClearLocalData: () -> Unit,
    onBack: () -> Unit,
) {
    var themeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var usernameField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(username, selection = TextRange(username.length)))
    }
    var dueDateField by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val formattedDueDate = dueDate.formatDueDateInput()
        mutableStateOf(TextFieldValue(formattedDueDate, selection = TextRange(formattedDueDate.length)))
    }
    val selectedTheme = DiaryThemes.all.firstOrNull { it.id == themeId } ?: DiaryThemes.Rose
    val isDueDateError = dueDateField.text.length == DueDateTextMaxLength && !dueDateField.text.isValidDueDate()

    LaunchedEffect(username) {
        if (username != usernameField.text) {
            usernameField = TextFieldValue(username, selection = TextRange(username.length))
        }
    }

    LaunchedEffect(dueDate) {
        val formattedDueDate = dueDate.formatDueDateInput()
        if (formattedDueDate != dueDateField.text) {
            dueDateField = TextFieldValue(formattedDueDate, selection = TextRange(formattedDueDate.length))
        }
    }

    DiaryPage(
        title = "设置",
        theme = theme,
        onBack = onBack,
        modifier = Modifier.testTag("settings-screen"),
    ) {
        SettingsSectionTitle("基础")
        Spacer(Modifier.height(SettingsTitleContentGap))
        OutlinedTextField(
            value = usernameField,
            onValueChange = { value ->
                usernameField = value
                onUsernameChange(value.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-username-input"),
            label = { Text("用户名") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = dueDateField,
            onValueChange = { value ->
                val text = value.text.formatDueDateInput()
                val selection = TextRange(text.length)
                dueDateField = value.copy(text = text, selection = selection)
                onDueDateChange(text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-due-date-input"),
            label = { Text("预产期") },
            placeholder = { Text("MM/DD/YYYY") },
            isError = isDueDateError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        if (isDueDateError) {
            Spacer(Modifier.height(6.dp))
            Text("请使用 MM/DD/YYYY，例如 06/01/2026", color = argb(theme.muted), fontSize = 12.sp)
        }
        Spacer(Modifier.height(SettingsSectionGap))
        SettingsSectionTitle("界面")
        Spacer(Modifier.height(SettingsTitleContentGap))
        ExposedDropdownMenuBox(
            expanded = themeMenuExpanded,
            onExpandedChange = { themeMenuExpanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selectedTheme.label,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                label = { Text("当前配色") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeMenuExpanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = argb(theme.surface).copy(alpha = 0.62f),
                    unfocusedContainerColor = argb(theme.surface).copy(alpha = 0.50f),
                    focusedBorderColor = argb(theme.primary).copy(alpha = 0.42f),
                    unfocusedBorderColor = argb(theme.muted).copy(alpha = 0.16f),
                    focusedLabelColor = argb(theme.muted),
                    unfocusedLabelColor = argb(theme.muted),
                ),
                shape = RoundedCornerShape(18.dp),
            )
            ExposedDropdownMenu(
                expanded = themeMenuExpanded,
                onDismissRequest = { themeMenuExpanded = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = argb(theme.surface).copy(alpha = 0.96f),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, argb(theme.muted).copy(alpha = 0.12f)),
            ) {
                DiaryThemes.all.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onThemeChange(option.id)
                            themeMenuExpanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(SettingsSectionGap))
        SettingsSectionTitle("开发工具")
        Spacer(Modifier.height(SettingsTitleContentGap))
        GenerationModeControl(
            generationMode = generationMode,
            theme = theme,
            onGenerationModeChange = onGenerationModeChange,
        )
        Spacer(Modifier.height(12.dp))
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

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun GenerationModeControl(
    generationMode: String,
    theme: DiaryTheme,
    onGenerationModeChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(argb(theme.surface).copy(alpha = 0.62f))
            .border(BorderStroke(1.dp, argb(theme.muted).copy(alpha = 0.12f)), RoundedCornerShape(16.dp))
            .padding(4.dp)
            .testTag("generation-mode-control"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GenerationModeOption(
            label = "Offline",
            selected = generationMode == GenerationModes.Offline,
            theme = theme,
            modifier = Modifier
                .weight(1f)
                .testTag("generation-mode-offline"),
            onClick = { onGenerationModeChange(GenerationModes.Offline) },
        )
        GenerationModeOption(
            label = "Online",
            selected = generationMode == GenerationModes.Online,
            theme = theme,
            modifier = Modifier
                .weight(1f)
                .testTag("generation-mode-online"),
            onClick = { onGenerationModeChange(GenerationModes.Online) },
        )
    }
}

@Composable
private fun GenerationModeOption(
    label: String,
    selected: Boolean,
    theme: DiaryTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) argb(theme.primary).copy(alpha = 0.18f) else Color.Transparent),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = label,
            color = if (selected) argb(theme.text) else argb(theme.muted),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private val SettingsTitleContentGap = 10.dp
private val SettingsSectionGap = 24.dp
private const val DueDateDigitsMaxLength = 8
private const val DueDateTextMaxLength = 10
private val DueDatePattern = Regex("""\d{2}/\d{2}/\d{4}""")
private val DueDateFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("MM/dd/uuuu")
    .withResolverStyle(ResolverStyle.STRICT)

private fun String.isValidDueDate(): Boolean =
    DueDatePattern.matches(this) && runCatching {
        LocalDate.parse(this, DueDateFormatter)
    }.isSuccess

private fun String.formatDueDateInput(): String {
    runCatching {
        LocalDate.parse(this)
    }.getOrNull()?.let { return it.format(DueDateFormatter) }

    val digits = filter(Char::isDigit).take(DueDateDigitsMaxLength)
    return buildString {
        digits.forEachIndexed { index, digit ->
            if (index == 2 || index == 4) append('/')
            append(digit)
        }
    }
}

@Preview(name = "设置", showBackground = true, widthDp = 430, heightDp = 880)
@Composable
private fun SettingsScreenPreview() {
    val theme = DiaryThemes.Rose
    DiaryPreviewTheme(theme = theme) {
        SettingsScreen(
            username = "你",
            dueDate = "",
            themeId = theme.id,
            theme = theme,
            generationMode = GenerationModes.Offline,
            onUsernameChange = {},
            onDueDateChange = {},
            onThemeChange = {},
            onGenerationModeChange = {},
            onClearLocalData = {},
            onBack = {},
        )
    }
}
