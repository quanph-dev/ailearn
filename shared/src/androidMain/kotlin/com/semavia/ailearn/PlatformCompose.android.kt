package com.semavia.ailearn

import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.semavia.ailearn.data.Strings
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
    ensureResourceLanguage(code)
    val currentConfiguration = LocalConfiguration.current
    val localizedConfiguration = remember(context, currentConfiguration, code) {
        Configuration(currentConfiguration).apply {
            val tag = code.trim().replace('_', '-')
            val locale = Locale.forLanguageTag(tag)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                setLocale(locale)
            }
            setLayoutDirection(locale)
        }
    }
    val localizedContext = remember(context, localizedConfiguration) {
        @Suppress("DEPRECATION")
        try {
            context.resources.updateConfiguration(localizedConfiguration, context.resources.displayMetrics)
            context.applicationContext?.resources?.let { appRes ->
                appRes.updateConfiguration(localizedConfiguration, appRes.displayMetrics)
            }
        } catch (_: Throwable) {}
        context.createConfigurationContext(localizedConfiguration)
    }
    val direction = if (Strings.isRtl(code)) {
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
        content()
    }
}

@Composable
actual fun BackActionHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
