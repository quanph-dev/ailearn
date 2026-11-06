package com.example.basekmp

import com.proxglobal.ads.AdsUtils
import com.proxglobal.ads.application.ProxApplication
import com.google.ads.pro.manager.AdsManager
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp
import android.util.Log

@HiltAndroidApp
class AILearnApplication : ProxApplication() {
    override fun onCreate() {
        instance = this
        AdsUtils.setKeyRemoteConfig("config_ads_v${BuildConfig.VERSION_CODE}")
        // Debug builds use src/debug test placements; release keeps the production Remote Config.
        AdsManager.enableLoadRemoteConfig(!BuildConfig.DEBUG)
        AdsUtils.disableOpenAds()
        super.onCreate()
        AdsUtils.addStyleNative(101, R.layout.layout_ads_custom_native_101)
        AdsUtils.addStyleNative(102, R.layout.layout_ads_custom_native_102)
        AdsUtils.addStyleNative(103, R.layout.layout_ads_custom_native_103)
        AdsUtils.addStyleNative(104, R.layout.layout_ads_custom_native_104)
        AdsUtils.addStyleNative(105, R.layout.layout_ads_custom_native_105)
        AdsUtils.addStyleNative(106, R.layout.layout_ads_custom_native_106)
        AdsUtils.addStyleNative(107, R.layout.layout_ads_custom_native_107)
        runCatching { FirebaseApp.initializeApp(this) }
            .onFailure { Log.w("FirebaseBootstrap", "Google Services Firebase initialization failed", it) }
    }

    companion object {
        @Volatile
        var instance: AILearnApplication? = null
            private set
    }
}
