package com.semavia.ailearn.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized Design System Dimension Tokens.
 * Ensures consistent padding, corner radii, and minimum touch target sizes across the app.
 */
data class AppDimens(
    // Spacing & Padding
    val spacingXs: Dp = 4.dp,
    val spacingSm: Dp = 8.dp,
    val spacingMd: Dp = 12.dp,
    val spacingLg: Dp = 16.dp,
    val spacingXl: Dp = 20.dp,
    val spacing2Xl: Dp = 24.dp,
    val spacing3Xl: Dp = 32.dp,
    val spacing4Xl: Dp = 40.dp,

    // Corner Radii
    val cornerXs: Dp = 8.dp,
    val cornerSm: Dp = 12.dp,
    val cornerMd: Dp = 16.dp,
    val cornerLg: Dp = 20.dp,
    val cornerXl: Dp = 24.dp,
    val cornerPill: Dp = 28.dp,
    val cornerFull: Dp = 999.dp,

    // Component Dimensions
    val minTouchTarget: Dp = 48.dp,
    val primaryButtonHeight: Dp = 56.dp,
    val secondaryButtonHeight: Dp = 48.dp,
    val chipHeight: Dp = 36.dp,
    val bottomNavHeight: Dp = 72.dp,
    val topBarHeight: Dp = 56.dp,
    val avatarSm: Dp = 36.dp,
    val avatarMd: Dp = 48.dp,
    val avatarLg: Dp = 72.dp,
    val iconSm: Dp = 20.dp,
    val iconMd: Dp = 24.dp,
    val iconLg: Dp = 32.dp
)

val LocalAppDimens = staticCompositionLocalOf { AppDimens() }
