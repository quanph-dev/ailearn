package com.semavia.ailearn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

interface NetworkConnectionState {
    val isOnline: State<Boolean>

    fun refresh()
}

@Composable
expect fun rememberNetworkConnectionState(): NetworkConnectionState

private object IosNetworkConnectionBridge {
    val isOnline: MutableState<Boolean> = mutableStateOf(false)
    var refreshHandler: (() -> Unit)? = null
}

/** Called by the iOS host whenever Network.framework reports a path update. */
fun updateIosNetworkConnection(isOnline: Boolean) {
    IosNetworkConnectionBridge.isOnline.value = isOnline
}

/** Installs the iOS host callback used by the shared Try again button. */
fun setIosNetworkRefreshHandler(handler: (() -> Unit)?) {
    IosNetworkConnectionBridge.refreshHandler = handler
}

internal val iosNetworkConnectionState: State<Boolean>
    get() = IosNetworkConnectionBridge.isOnline

internal fun requestIosNetworkRefresh() {
    IosNetworkConnectionBridge.refreshHandler?.invoke()
}
