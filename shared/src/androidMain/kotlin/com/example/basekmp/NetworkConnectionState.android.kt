package com.example.basekmp

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private class AndroidNetworkConnectionState(context: Context) : NetworkConnectionState {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mainHandler = Handler(Looper.getMainLooper())

    override val isOnline: MutableState<Boolean> = mutableStateOf(hasValidatedInternet())

    private var isStarted = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = publishCurrentStatus()

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) = publishCurrentStatus()

        override fun onLost(network: Network) = publishCurrentStatus()
    }

    fun start() {
        if (isStarted) return
        isStarted = true
        refresh()
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        runCatching {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    override fun refresh() {
        isOnline.value = hasValidatedInternet()
    }

    private fun publishCurrentStatus() {
        mainHandler.post {
            if (isStarted) refresh()
        }
    }

    private fun hasValidatedInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

@Composable
actual fun rememberNetworkConnectionState(): NetworkConnectionState {
    val context = LocalContext.current.applicationContext
    val state = remember(context) { AndroidNetworkConnectionState(context) }

    DisposableEffect(state) {
        state.start()
        onDispose(state::stop)
    }

    return state
}
