package com.cmm.certificates

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TestViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TestUiState(greeting = Greeting().greet() + " From ViewModel"))
    val uiState = _uiState.asStateFlow()
}

data class TestUiState(
    val greeting: String
)
