package com.example.basekmp.data

fun tr(en: String): String = Strings.tr(en)

fun trf(en: String, vararg args: Any?): String {
    val translated = Strings.tr(en)
    return formatString(translated, args)
}

fun trIn(lang: String, en: String): String = Strings.trIn(lang, en)

fun trfIn(lang: String, en: String, vararg args: Any?): String {
    val translated = Strings.trIn(lang, en)
    return formatString(translated, args)
}

private fun formatString(pattern: String, args: Array<out Any?>): String {
    var result = pattern
    for (i in args.indices) {
        val index = i + 1
        val value = args[i]?.toString() ?: ""
        result = result.replace("%$index\$s", value)
            .replace("%$index\$d", value)
            .replace("%$index\$f", value)
    }
    for (arg in args) {
        val value = arg?.toString() ?: ""
        if (result.contains("%s")) {
            result = result.replaceFirst("%s", value)
        } else if (result.contains("%d")) {
            result = result.replaceFirst("%d", value)
        }
    }
    return result
}
