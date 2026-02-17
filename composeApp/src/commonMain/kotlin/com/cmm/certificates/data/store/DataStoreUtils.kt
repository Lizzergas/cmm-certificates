package com.cmm.certificates.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.io.IOException

fun Preferences.stringOrDefault(
    key: Preferences.Key<String>,
    default: String,
): String = this[key] ?: default

inline fun <reified T : Enum<T>> Preferences.enumOrDefault(
    key: Preferences.Key<String>,
    default: T,
): T {
    val raw = this[key] ?: return default
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
}

fun Preferences.intOrDefault(
    key: Preferences.Key<Int>,
    default: Int,
): Int= this[key] ?: default

fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { e ->
        if (e is IOException) {
            emit(emptyPreferences())
        } else {
            throw e
        }
    }
