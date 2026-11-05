package com.example.basekmp.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Unified Semantic Design System Colors.
 * Semavia's light-first teal and coral system with semantic surfaces and feedback colors.
 */
@Immutable
data class AppColors(
    val isDark: Boolean = false,

    // Background & Surfaces
    val background: Color = SurfaceMint,
    val surface: Color = Color.White,
    val surfaceVariant: Color = OptionSurface,
    val surfaceCard: Color = Color.White,
    val surfaceSelected: Color = OptionSurfaceSelected,

    // Brand Primary & Gradients
    val primary: Color = BrandPrimary,
    val primaryGradientStart: Color = BrandPrimary,
    val primaryGradientEnd: Color = BrandPrimaryPressed,
    val secondary: Color = BrandPrimaryPressed,
    val secondarySelected: Color = BrandPrimaryPressed,

    // Accent & Highlights
    val accentGold: Color = Gold,
    val accentPink: Color = BrandAccent,

    // Status Feedback
    val success: Color = CorrectGreen,
    val successBg: Color = CorrectGreenBg,
    val warning: Color = ScoreYellow,
    val error: Color = ScoreRed,
    val errorBg: Color = ScoreRedPale,

    // Typography & Content
    val textPrimary: Color = TextPrimary,
    val textSecondary: Color = TextSecondary,
    val textTertiary: Color = TextSecondary,
    val textOnPrimary: Color = Color.White,

    // Borders & Dividers
    val border: Color = DividerColor,
    val borderActive: Color = BrandPrimary,
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
    background = Color(0xFF10211F),
    surface = Color(0xFF172B28),
    surfaceVariant = Color(0xFF203733),
    surfaceCard = Color(0xFF172B28),
    surfaceSelected = Color(0xFF25443E),
    primary = Color(0xFF54B7A7),
    primaryGradientStart = Color(0xFF54B7A7),
    primaryGradientEnd = Color(0xFF2F9185),
    secondary = Color(0xFF54B7A7),
    secondarySelected = Color(0xFF2F9185),
    accentPink = Color(0xFFFF907D),
    textPrimary = Color.White,
    textSecondary = Color(0xFFB5C9C4),
    textTertiary = Color(0xFFA9C8C0),
    border = Color(0xFF35514B),
    divider = Color(0xFF35514B),
    borderActive = Color(0xFF54B7A7),
    navBarBackground = Color(0xFF142522)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
