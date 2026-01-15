package com.cmm.certificates

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object HomeScreenRoute : NavKey

@Serializable
data class TestScreenRoute(val message: String) : NavKey
