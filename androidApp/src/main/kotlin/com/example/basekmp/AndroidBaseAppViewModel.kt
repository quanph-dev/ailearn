package com.example.basekmp

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AndroidBaseAppViewModel @Inject constructor(
    repository: AndroidAppRepository
) : BaseAppViewModel(repository)
