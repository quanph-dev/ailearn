package com.semavia.ailearn.core.analytics

import android.os.Build
import android.os.Bundle
import android.util.Log
import com.semavia.ailearn.BuildConfig
import com.semavia.ailearn.AILearnApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.FirebaseDatabase

private const val TAG = "AppAnalytics"

@JvmOverloads
fun logFirebaseEvent(eventName: String, bundle: Bundle? = null) {
    Log.d(TAG, eventName)
    if (eventName.isBlank()) return

    val application = AILearnApplication.instance ?: return
    val firebaseApp = runCatching {
        FirebaseApp.getApps(application).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
    }.onFailure { Log.w(TAG, "Firebase app unavailable") }.getOrNull() ?: return

    if (BuildConfig.DEBUG) {
        runCatching {
            FirebaseDatabase.getInstance(firebaseApp)
                .getReference("events/${Build.MANUFACTURER}_${Build.DEVICE}")
                .push()
                .setValue(eventName)
                .addOnFailureListener { Log.w(TAG, "Debug event write failed") }
        }.onFailure { Log.w(TAG, "Debug event storage unavailable") }
    }

    runCatching { FirebaseAnalytics.getInstance(application).logEvent(eventName, bundle) }
        .onFailure { Log.w(TAG, "Analytics unavailable") }
}

fun logFirebaseEvent(eventName: String, parameters: Map<String, Any>) {
    val bundle = Bundle()
    parameters.forEach { (key, value) ->
        when (value) {
            is String -> bundle.putString(key, value)
            is Int -> bundle.putLong(key, value.toLong())
            is Long -> bundle.putLong(key, value)
            is Double -> bundle.putDouble(key, value)
            is Float -> bundle.putDouble(key, value.toDouble())
        }
    }
    logFirebaseEvent(eventName, bundle.takeUnless(Bundle::isEmpty))
}

/** Catalogued first-flow ad outcomes; technical ads_event records use RTDB only. */
internal fun logFirstFlowAdResult(action: String, placement: String, success: Boolean) {
    val prefix = when {
        placement.startsWith("I_Splash_") && action in setOf("load", "show") -> "spl_ads_inter_$action"
        placement == "N_Splash" && action == "load" -> "spl_ads_nat_pre_splash"
        placement == "N_Splash" && action == "show" -> "spl_ads_nat_show"
        action == "show" && placement in setOf("N_Onboard1_M", "N_Onboard1") -> "onb_ads_nat_show"
        else -> return
    }
    logFirebaseEvent("${prefix}_${if (success) "done" else "fail"}", Bundle().apply {
        putString("placement", placement)
    })
}
