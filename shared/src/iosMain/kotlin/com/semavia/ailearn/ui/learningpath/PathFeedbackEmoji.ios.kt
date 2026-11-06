package com.semavia.ailearn.ui.learningpath

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
actual fun PathFeedbackEmoji(isCorrect: Boolean, modifier: Modifier) {
    Box(modifier = modifier.size(34.dp), contentAlignment = Alignment.Center) {
        Text(text = if (isCorrect) "🥳" else "😭", fontSize = 22.sp)
    }
}
