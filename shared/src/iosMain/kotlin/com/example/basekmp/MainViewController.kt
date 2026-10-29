package com.example.basekmp

import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel

fun MainViewController() = ComposeUIViewController {
    val appViewModel: BaseAppViewModel = viewModel { BaseAppViewModel(IosAppRepository()) }
    App(viewModel = appViewModel)
}
