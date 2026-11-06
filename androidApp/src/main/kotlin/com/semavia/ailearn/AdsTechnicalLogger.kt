package com.semavia.ailearn

import com.semavia.ailearn.BuildConfig
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

/** Technical ads lifecycle messages stay out of Firebase Analytics. */
internal object AdsTechnicalLogger {
    private const val TAG = "ads_event"
    private val allowedActions = setOf("load", "show")
    private val allowedStatuses = setOf("start", "success", "fail")

    fun log(action: String, idAds: String, status: String, reason: String? = null) {
        if (action !in allowedActions || status !in allowedStatuses || idAds.isBlank()) return
        val message = buildString {
            append("ads_event action=").append(action)
            append(" id_ads=").append(idAds)
            append(" status=").append(status)
            if (status == "fail" && !reason.isNullOrBlank()) {
                append(" reason=").append(reason.replace(Regex("[\\r\\n\\s]+"), "_").take(80))
            }
        }
        Log.d(TAG, message)
        if (!BuildConfig.DEBUG) return

        val application = AILearnApplication.instance ?: return
        val firebaseReady = runCatching {
            FirebaseApp.getApps(application).any { it.name == FirebaseApp.DEFAULT_APP_NAME }
        }.getOrDefault(false)
        if (!firebaseReady) return

        runCatching {
            val firebaseApp = FirebaseApp.getApps(application)
                .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                ?: return
            FirebaseDatabase.getInstance(firebaseApp)
                .getReference("events/${Build.MANUFACTURER}_${Build.DEVICE}")
                .push()
                .setValue(message)
                .addOnFailureListener { Log.w(TAG, "RTDB debug logging unavailable") }
        }.onFailure { failure ->
            Log.w(TAG, "RTDB debug logging unavailable", failure)
        }
    }
}
