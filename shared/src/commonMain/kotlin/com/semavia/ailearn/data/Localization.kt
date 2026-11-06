package com.semavia.ailearn.data

import androidx.compose.runtime.Composable

@Composable
fun tr(en: String): String = Strings.tr(en)

@Composable
fun trf(en: String, vararg args: Any?): String = formatString(Strings.tr(en), args)

internal fun formatString(pattern: String, args: Array<out Any?>): String {
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
