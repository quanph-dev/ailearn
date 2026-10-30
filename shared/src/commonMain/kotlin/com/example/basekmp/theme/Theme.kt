package com.example.basekmp.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Accessor object for global Design System Tokens.
 */
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current

    val dimens: AppDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalAppDimens.current
}

val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    secondary = BlueSelected,
    surface = Color.White,
    onSurface = TextPrimary,
    background = Color.White,
    onBackground = TextPrimary,
    surfaceVariant = OptionSurface,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor
)

@Composable
fun EnglishLearningAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val appColors = LightAppColors
    val appDimens = AppDimens()
    val typography = getAppTypography()

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppDimens provides appDimens,
        LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = getPlusJakartaFontFamily())
    ) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = typography,
            content = content
        )
    }
}
