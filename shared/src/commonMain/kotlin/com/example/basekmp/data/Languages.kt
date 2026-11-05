package com.example.basekmp.data

import kotlinx.serialization.Serializable

@Serializable
data class Language(
    val code: String,
    val name: String,
    val flag: String,
    val englishName: String,
    val speechTag: String
) {
    val displayName: String get() = name
}

object Languages {
    fun resolveNativeLanguage(saved: String, restored: String?, system: String): String =
        saved.takeIf { code -> native.any { it.code == code } }
            ?: restored?.takeIf { code -> native.any { it.code == code } }
            ?: defaultNativeLanguageCode(system)

    val learnable: List<Language> = listOf(
        Language("en", "English", "🇬🇧", "English", "en-US"),
        Language("es", "Spanish", "🇪🇸", "Spanish", "es-ES"),
        Language("fr", "French", "🇫🇷", "French", "fr-FR"),
        Language("ar", "Arabic", "🇸🇦", "Arabic", "ar"),
        Language("ur", "Urdu", "🇵🇰", "Urdu", "ur"),
        Language("hi", "Hindi", "🇮🇳", "Hindi", "hi-IN"),
        Language("zh", "Chinese", "🇨🇳", "Chinese (Mandarin)", "zh-CN"),
        Language("ja", "Japanese", "🇯🇵", "Japanese", "ja-JP"),
        Language("ko", "Korean", "🇰🇷", "Korean", "ko-KR"),
        Language("it", "Italian", "🇮🇹", "Italian", "it-IT"),
        Language("de", "German", "🇩🇪", "German", "de-DE"),
        Language("pt", "Portuguese", "🇧🇷", "Portuguese (Brazilian)", "pt-BR"),
        Language("other", "Other", "🌐", "English", "en-US")
    )

    val native: List<Language> = listOf(
        Language("ur", "Urdu", "🇵🇰", "Urdu", "ur"),
        Language("hi", "Hindi", "🇮🇳", "Hindi", "hi-IN"),
        Language("ar", "Arabic", "🇸🇦", "Arabic", "ar"),
        Language("zh", "Chinese", "🇨🇳", "Chinese (Mandarin)", "zh-CN"),
        Language("es", "Spanish", "🇪🇸", "Spanish", "es-ES"),
        Language("fr", "French", "🇫🇷", "French", "fr-FR"),
        Language("tr", "Turkish", "🇹🇷", "Turkish", "tr-TR"),
        Language("en", "English", "🇬🇧", "English", "en-US"),
        Language("pt", "Portuguese", "🇧🇷", "Portuguese (Brazilian)", "pt-BR"),
        Language("de", "German", "🇩🇪", "German", "de-DE"),
        Language("ja", "Japanese", "🇯🇵", "Japanese", "ja-JP"),
        Language("ko", "Korean", "🇰🇷", "Korean", "ko-KR"),
        Language("ru", "Russian", "🇷🇺", "Russian", "ru-RU"),
        Language("it", "Italian", "🇮🇹", "Italian", "it-IT"),
        Language("vi", "Vietnamese", "🇻🇳", "Vietnamese", "vi-VN"),
        Language("th", "Thai", "🇹🇭", "Thai", "th-TH")
    )

    fun defaultNativeLanguageCode(systemLanguage: String): String {
        return nativeOptionsFor(systemLanguage).firstOrNull()?.code ?: "en"
    }

    /**
     * Native language choices follow a popularity order, with a supported device
     * language promoted to the first row. If the device language is unsupported,
     * the popularity order is shown unchanged.
     */
    fun nativeOptionsFor(systemLanguage: String): List<Language> {
        val popularityOrder = listOf(
            "en", "es", "zh", "hi", "ar", "fr", "pt", "ru",
            "de", "ja", "ko", "it", "tr", "vi", "ur", "th"
        )
        val rank = popularityOrder.withIndex().associate { (index, code) -> code to index }
        val popular = native.sortedBy { rank[it.code] ?: Int.MAX_VALUE }
        val systemCode = systemLanguage.trim().lowercase().replace('_', '-').substringBefore('-')
        val systemOption = popular.firstOrNull { it.code == systemCode } ?: return popular
        return listOf(systemOption) + popular.filterNot { it.code == systemOption.code }
    }

    private val highlightedSurveyCodes = listOf("en", "es", "it", "de", "pt")
    val surveyOptions: List<Language> = highlightedSurveyCodes.map { code ->
        learnable.first { it.code == code }
    } + learnable.filter { it.code !in highlightedSurveyCodes && it.code != "other" }

    fun learn(name: String): Language {
        return learnable.firstOrNull { it.name.equals(name, ignoreCase = true) || it.englishName.equals(name, ignoreCase = true) || it.code.equals(name, ignoreCase = true) }
            ?: Language("en", "English", "🇬🇧", "English", "en-US")
    }

    fun nativeFor(name: String): Language {
        return native.firstOrNull { it.name.equals(name, ignoreCase = true) || it.englishName.equals(name, ignoreCase = true) || it.code.equals(name, ignoreCase = true) }
            ?: Language("en", "English", "🇬🇧", "English", "en-US")
    }
}
