package com.youandme.diary.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.youandme.diary.domain.model.DiaryTheme

@Composable
fun DiaryPreviewTheme(
    theme: DiaryTheme,
    content: @Composable () -> Unit,
) {
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
            content()
        }
    }
}
