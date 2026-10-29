package com.example.basekmp

import android.content.Context

internal object LanguagePrefs {
    private const val PREFS_NAME = "lang_prefs"
    private const val KEY_LANG = "lang_code"

    fun get(context: Context): String = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_LANG, "").orEmpty()

    fun set(context: Context, code: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANG, code)
            .apply()
    }
}
