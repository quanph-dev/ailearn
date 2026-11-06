package com.semavia.ailearn.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.plus_jakarta_bold
import ailearn.shared.generated.resources.plus_jakarta_extrabold
import ailearn.shared.generated.resources.plus_jakarta_medium
import ailearn.shared.generated.resources.plus_jakarta_regular
import ailearn.shared.generated.resources.plus_jakarta_semibold
import ailearn.shared.generated.resources.inter_bold
import ailearn.shared.generated.resources.inter_regular
import ailearn.shared.generated.resources.inter_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun getPlusJakartaFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.plus_jakarta_regular, FontWeight.Normal),
        Font(Res.font.plus_jakarta_medium, FontWeight.Medium),
        Font(Res.font.plus_jakarta_semibold, FontWeight.SemiBold),
        Font(Res.font.plus_jakarta_bold, FontWeight.Bold),
        Font(Res.font.plus_jakarta_extrabold, FontWeight.ExtraBold)
    )
}

@Composable
fun getInterFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold)
    )
}

@Composable
fun getAppTypography(): Typography {
    val fontFamily = getPlusJakartaFontFamily()
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = fontFamily)
    )
}

@Composable
fun getScreenTitleStyle(): TextStyle {
    return TextStyle(
        fontFamily = getPlusJakartaFontFamily(),
        fontSize = 27.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun getOptionLabelStyle(): TextStyle {
    return TextStyle(
        fontFamily = getPlusJakartaFontFamily(),
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )
}
