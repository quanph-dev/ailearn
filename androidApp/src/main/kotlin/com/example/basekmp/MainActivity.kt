package com.example.basekmp

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.CompositionLocalProvider
import com.example.basekmp.data.platformInitializePlanCache
import com.example.basekmp.data.platformInitializeShadowingCache
import com.example.basekmp.platformInitializeAppInstallEpochDay
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AndroidBaseAppViewModel by viewModels()
    private lateinit var adsHost: AndroidAppAdsHost
    private var pauseWasAds = false

    override fun attachBaseContext(newBase: Context) {
        val savedLanguage = LanguagePrefs.get(newBase)
        if (savedLanguage.isBlank()) {
            super.attachBaseContext(newBase)
            return
        }
        applyPlatformLanguage(savedLanguage)
        val tag = savedLanguage.trim().replace('_', '-')
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(newBase.resources.configuration).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                setLocale(locale)
            }
            setLayoutDirection(locale)
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adsHost = AndroidAppAdsHost(this)
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                lifecycleScope.launch { adsHost.onAppForegrounded() }
            }
        })
        platformInitializeAppInstallEpochDay(applicationContext)
        platformInitializePlanCache(applicationContext)
        platformInitializeShadowingCache(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        hideNavigationBar()
        setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides this@MainActivity) {
                App(
                    viewModel = appViewModel,
                    adsHost = adsHost,
                    onExitApp = { finish() }
                )
            }
        }
    }

    override fun onPause() {
        pauseWasAds = if (::adsHost.isInitialized) adsHost.onAppPaused() else false
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (::adsHost.isInitialized && !pauseWasAds && !isChangingConfigurations) {
            adsHost.onAppBackgrounded()
        }
    }

    override fun onDestroy() {
        if (::adsHost.isInitialized) adsHost.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    private fun hideNavigationBar() {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
    }
}
