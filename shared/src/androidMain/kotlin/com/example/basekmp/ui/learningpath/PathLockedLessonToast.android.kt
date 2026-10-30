package com.example.basekmp.ui.learningpath

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun PathLockedLessonToast(message: String?, eventId: Int) {
    val context = LocalContext.current
    LaunchedEffect(eventId) {
        message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }
}
