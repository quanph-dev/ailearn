package com.example.basekmp

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

actual object LocalAppLocale {
    private val appLocale = staticCompositionLocalOf { Locale.getDefault().toLanguageTag() }

    actual val current: String
        @Composable get() = appLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> =
        appLocale provides (value ?: Locale.getDefault().toLanguageTag())
}

@Composable
actual fun PlatformLocaleProvider(code: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val currentConfiguration = LocalConfiguration.current
    val localizedConfiguration = remember(context, currentConfiguration, code) {
        Configuration(currentConfiguration).apply {
            val locale = Locale.forLanguageTag(code)
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }
    val localizedContext = remember(context, localizedConfiguration) {
        context.createConfigurationContext(localizedConfiguration)
    }
    val direction = if (code.startsWith("ar", ignoreCase = true)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalAppLocale provides code,
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
        LocalLayoutDirection provides direction
    ) {
        key(code) { content() }
    }
}

@Composable
actual fun BackActionHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
