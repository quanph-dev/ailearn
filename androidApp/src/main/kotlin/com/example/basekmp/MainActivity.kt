package com.example.basekmp

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AndroidBaseAppViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val savedLanguage = LanguagePrefs.get(newBase)
        if (savedLanguage.isBlank()) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale.forLanguageTag(savedLanguage)
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(
                viewModel = appViewModel,
                onLanguageApplied = {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(appViewModel.uiState.value.languageCode)
                    )
                },
                onExitApp = { finish() }
            )
        }
    }
}
