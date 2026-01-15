package com.cmm.certificates.features.home

import androidx.lifecycle.ViewModel
import com.cmm.certificates.getPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val _uiState =
        MutableStateFlow(HomeUiState(greeting = getPlatform().toString() + " From ViewModel"))
    val uiState = _uiState.asStateFlow()
}

data class HomeUiState(
    val greeting: String,
)
