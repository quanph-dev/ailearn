package com.example.basekmp.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

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

    fun isRtl(): Boolean {
        return languages.firstOrNull { it.code == language.value }?.rtl ?: false
    }

    fun tr(en: String): String {
        return trIn(language.value, en)
    }

    fun trf(en: String, vararg args: Any?): String {
        return trfIn(language.value, en, *args)
    }

    fun trIn(lang: String, en: String): String {
        if (lang == "en") return en
        val langMap = Translations.byLanguage[lang]
        val translated = langMap?.get(en)
            ?: Translations.learningPathDialogOverrides[lang]?.get(en)
            ?: Translations.videoShadowingOverrides[lang]?.get(en)
        if (translated != null) return translated
        return en
    }
}
