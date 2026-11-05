package com.example.basekmp.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.compose.resources.stringResource

object Strings {
    val languages: List<UiLanguage> = listOf(
        UiLanguage("de", "Deutsch", "🇩🇪"),
        UiLanguage("en", "English", "🇬🇧"),
        UiLanguage("es", "Español", "🇪🇸"),
        UiLanguage("fr", "Français", "🇫🇷"),
        UiLanguage("it", "Italiano", "🇮🇹"),
        UiLanguage("pt", "Português", "🇵🇹"),
        UiLanguage("vi", "Tiếng Việt", "🇻🇳"),
        UiLanguage("tr", "Türkçe", "🇹🇷"),
        UiLanguage("ru", "Русский", "🇷🇺"),
        UiLanguage("ar", "العربية", "🇸🇦", rtl = true),
        UiLanguage("ja", "日本語", "🇯🇵"),
        UiLanguage("zh", "中文", "🇨🇳"),
        UiLanguage("ko", "한국어", "🇰🇷"),
        UiLanguage("th", "ไทย", "🇹🇭"),
        UiLanguage("ur", "اردو", "🇵🇰", rtl = true),
        UiLanguage("hi", "हिन्दी", "🇮🇳")
    )

    var language: MutableState<String> = mutableStateOf("en")

    fun getLanguage(): String = language.value

    fun select(code: String) {
        val target = languages.firstOrNull { it.code == code }?.code ?: "en"
        language.value = target
    }

    fun isRtl(): Boolean = isRtl(language.value)

    fun isRtl(code: String): Boolean =
        languages.firstOrNull { it.code == code.lowercase() }?.rtl ?: false

    @Composable
    fun tr(en: String): String {
        val resource = uiStringResourceByEnglish[en] ?: return en
        return stringResource(resource)
    }

    @Composable
    fun trf(en: String, vararg args: Any?): String =
        formatString(tr(en), args)

    internal fun hasTranslationResource(en: String): Boolean =
        en in uiStringResourceByEnglish
}
