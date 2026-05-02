package com.youandme.diary.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.youandme.diary.domain.model.DiaryTheme

@Composable
fun OutlineAction(
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
fun PlaceholderTool(label: String, theme: DiaryTheme, modifier: Modifier = Modifier) {
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
