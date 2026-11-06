package com.semavia.ailearn

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.State

private class IosNetworkConnectionState : NetworkConnectionState {
    override val isOnline: State<Boolean> = iosNetworkConnectionState

    override fun refresh() {
        requestIosNetworkRefresh()
    }
}

@Composable
actual fun rememberNetworkConnectionState(): NetworkConnectionState {
    return remember { IosNetworkConnectionState() }
}
