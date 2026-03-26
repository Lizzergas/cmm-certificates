package com.cmm.certificates.core.domain

data class AppCapabilities(
    val canParseXlsx: Boolean,
    val canGeneratePdf: Boolean,
    val canSendEmails: Boolean,
    val canResolveOutputDirectory: Boolean,
    val canOpenGeneratedFolders: Boolean,
) {
    val canRunConversion: Boolean
        get() = canParseXlsx && canGeneratePdf && canResolveOutputDirectory
}

interface PlatformCapabilityProvider {
    val capabilities: AppCapabilities
}

expect fun getAppCapabilities(): AppCapabilities
