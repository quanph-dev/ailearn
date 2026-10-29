package com.example.basekmp

import com.proxglobal.ads.AdsUtils
import com.proxglobal.ads.application.ProxApplication
import dagger.hilt.android.HiltAndroidApp
import com.google.firebase.FirebaseApp

@HiltAndroidApp
class BaseKmpApplication : ProxApplication() {
    override fun onCreate() {
        instance = this
        AdsUtils.setKeyRemoteConfig("config_ads_v${BuildConfig.VERSION_CODE}")
        AdsUtils.disableOpenAds()
        super.onCreate()
        if (BuildConfig.FIREBASE_CONFIGURED) {
            runCatching {
                if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this)
            }
        }
    }

    companion object {
        @Volatile
        var instance: BaseKmpApplication? = null
            private set
    }
}
