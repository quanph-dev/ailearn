package com.example.basekmp.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Unified Semantic Design System Colors.
 * Matching authentic Engla design system: Light-first, clean whites, soft blue highlights.
 */
@Immutable
data class AppColors(
    val isDark: Boolean = false,

    // Background & Surfaces
    val background: Color = Color.White,
    val surface: Color = Color.White,
    val surfaceVariant: Color = OptionSurface,
    val surfaceCard: Color = Color.White,
    val surfaceSelected: Color = OptionSurfaceSelected,

    // Brand Primary & Gradients
    val primary: Color = Blue,
    val primaryGradientStart: Color = Blue,
    val primaryGradientEnd: Color = BlueSelected,
    val secondary: Color = BlueSelected,
    val secondarySelected: Color = BlueSelected,

    // Accent & Highlights
    val accentGold: Color = Gold,
    val accentPink: Color = ProgressPink,

    // Status Feedback
    val success: Color = CorrectGreen,
    val successBg: Color = CorrectGreenBg,
    val warning: Color = ScoreYellow,
    val error: Color = ScoreRed,
    val errorBg: Color = ScoreRedPale,

    // Typography & Content
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = ChipOutlineText,
    val textOnPrimary: Color = Color.White,

    // Borders & Dividers
    val border: Color = DividerColor,
    val borderActive: Color = Blue,
    val divider: Color = DividerColor,

    // Mascots & Floating Glows
    val robotGlow: Color = TutorHeroGlow,
    val navBarBackground: Color = NavBarBg
) {
    val primaryBrush: Brush
        get() = Brush.horizontalGradient(listOf(primaryGradientStart, primaryGradientEnd))

    val surfaceBrush: Brush
        get() = Brush.verticalGradient(
            0.0f to Color.White,
            0.42f to GradientBottom,
            1.0f to Color.White
        )
}

val LightAppColors = AppColors(isDark = false)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF14161B),
    surface = Color(0xFF1C1F26),
    surfaceVariant = Color(0xFF252932),
    surfaceCard = Color(0xFF1C1F26),
    surfaceSelected = Color(0xFF2B3242),
    primary = Blue,
    primaryGradientStart = Blue,
    primaryGradientEnd = BlueSelected,
    secondary = BlueSelected,
    secondarySelected = BlueSelected,
    textPrimary = Color.White,
    textSecondary = Color(0xFFA0A3AB),
    border = Color(0xFF2E333D),
    divider = Color(0xFF2E333D),
    navBarBackground = Color(0xFF151821)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
