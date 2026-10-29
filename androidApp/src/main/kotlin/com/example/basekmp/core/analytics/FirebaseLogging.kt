package com.example.basekmp.core.analytics

import android.os.Build
import android.os.Bundle
import android.util.Log
import com.example.basekmp.BuildConfig
import com.example.basekmp.BaseKmpApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase

private const val TAG = "AppAnalytics"

@JvmOverloads
fun logFirebaseEvent(eventName: String, bundle: Bundle? = null) {
    Log.d(TAG, eventName)
    if (eventName.isBlank() || !BuildConfig.FIREBASE_CONFIGURED) return

    val application = BaseKmpApplication.instance ?: return
    val firebaseReady = try {
        FirebaseApp.getApps(application).any { it.name == FirebaseApp.DEFAULT_APP_NAME }
    } catch (failure: Exception) {
        failure.printStackTrace()
        false
    }
    if (!firebaseReady) return

    if (BuildConfig.DEBUG) {
        try {
            FirebaseDatabase.getInstance()
                .getReference("events/${Build.MANUFACTURER}_${Build.DEVICE}")
                .push()
                .setValue(eventName)
        } catch (failure: Exception) {
            failure.printStackTrace()
        }
    }

    try {
        FirebaseAnalytics.getInstance(application).logEvent(eventName, bundle)
    } catch (failure: Exception) {
        failure.printStackTrace()
    }
}

fun logAdsEvent(eventName: String, placement: String, bundle: Bundle? = null) {
    if (placement.isBlank()) return
    val eventBundle = Bundle(bundle ?: Bundle()).apply {
        putString("placement", placement)
    }
    logFirebaseEvent(eventName, eventBundle)
}
