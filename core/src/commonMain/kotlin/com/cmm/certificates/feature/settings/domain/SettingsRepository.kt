package com.cmm.certificates.feature.settings.domain

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val state: StateFlow<SettingsState>
    fun setHost(value: String)
    fun setPort(value: String)
    fun setUsername(value: String)
    fun setPassword(value: String)
    fun setTransport(value: SmtpTransport)
    fun setSubject(value: String)
    fun setBody(value: String)
    fun setSignatureHtml(value: String)
    fun setAccreditedTypeOptions(value: String)
    fun setOutputDirectory(value: String)
    fun setPreviewEmail(value: String)
    fun setDailyLimit(value: Int)
    fun setThemeMode(value: AppThemeMode)
    fun setUseInAppPdfPreview(value: Boolean)
    suspend fun save()
    suspend fun authenticate(): Boolean
    suspend fun resetAndClear()
}
