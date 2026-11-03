package com.example.basekmp.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun FirstOpenIntroArtwork(
    page: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
)

@Composable
expect fun isReducedMotionEnabled(): Boolean
