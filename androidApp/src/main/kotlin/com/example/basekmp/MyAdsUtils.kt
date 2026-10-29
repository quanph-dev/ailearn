package com.example.basekmp

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ads.pro.manager.AdsManager

/** Entry point for future ad setup. Base startup deliberately has no consent UI or ad requests. */
object MyAdsUtils {
    private const val INIT_TIMEOUT_MS = 15_000L
    private val lock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val callbacks = mutableListOf<(Boolean) -> Unit>()

    private var isInitializing = false
    private var isInitialized = false
    private var operationId = 0L
    private var terminalOperationId = -1L
    private var lifecycleOwner: LifecycleOwner? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var timeoutAction: Runnable? = null

    fun isAdsReady(): Boolean = synchronized(lock) { isInitialized }

    fun initializeAfterConsent(
        activity: Activity,
        consentGranted: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        dispatchToMain {
            if (!consentGranted || activity.isFinishing || activity.isDestroyed) {
                onComplete(false)
                return@dispatchToMain
            }

            var startOperationId = -1L
            val alreadyInitialized = synchronized(lock) {
                when {
                    isInitialized -> true

                    isInitializing -> {
                        callbacks += onComplete
                        false
                    }

                    else -> {
                        operationId += 1
                        terminalOperationId = -1L
                        isInitializing = true
                        callbacks += onComplete
                        startOperationId = operationId
                        false
                    }
                }
            }
            if (alreadyInitialized) {
                onComplete(true)
            } else if (startOperationId >= 0L) {
                startInitialization(activity, startOperationId)
            }
        }
    }

    private fun startInitialization(activity: Activity, currentOperationId: Long) {
        val owner = activity as? LifecycleOwner
        if (owner != null) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    finish(currentOperationId, success = false)
                }
            }
            synchronized(lock) {
                lifecycleOwner = owner
                lifecycleObserver = observer
            }
            owner.lifecycle.addObserver(observer)
            if (!isOperationPending(currentOperationId)) return
        }

        val timeout = Runnable { finish(currentOperationId, success = false) }
        val shouldScheduleTimeout = synchronized(lock) {
            if (operationId == currentOperationId && isInitializing && terminalOperationId != currentOperationId) {
                timeoutAction = timeout
                true
            } else {
                false
            }
        }
        if (!shouldScheduleTimeout) return
        mainHandler.postDelayed(timeout, INIT_TIMEOUT_MS)

        try {
            AdsManager.initAds(activity) {
                mainHandler.post { finish(currentOperationId, success = true) }
            }
        } catch (_: Exception) {
            finish(currentOperationId, success = false)
        }
    }

    private fun finish(currentOperationId: Long, success: Boolean) {
        var callbacksToNotify: List<(Boolean) -> Unit> = emptyList()
        synchronized(lock) {
            if (success) isInitialized = true
            val activeOperationId = operationId
            when {
                currentOperationId == activeOperationId && terminalOperationId == activeOperationId -> return
                currentOperationId != activeOperationId && (!success || !isInitializing) -> return
            }
            terminalOperationId = activeOperationId
            isInitializing = false
            callbacksToNotify = callbacks.toList()
            callbacks.clear()
            timeoutAction?.let(mainHandler::removeCallbacks)
            timeoutAction = null
            lifecycleObserver?.let { observer -> lifecycleOwner?.lifecycle?.removeObserver(observer) }
            lifecycleObserver = null
            lifecycleOwner = null
        }
        callbacksToNotify.forEach { callback -> runCatching { callback(success) } }
    }

    private fun isOperationPending(currentOperationId: Long): Boolean = synchronized(lock) {
        operationId == currentOperationId &&
            isInitializing &&
            terminalOperationId != currentOperationId
    }

    private fun dispatchToMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }
}
