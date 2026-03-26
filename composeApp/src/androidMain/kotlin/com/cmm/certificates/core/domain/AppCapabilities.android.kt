package com.cmm.certificates.core.domain

actual fun getAppCapabilities(): AppCapabilities {
    return AppCapabilities(
        canParseXlsx = false,
        canGeneratePdf = false,
        canSendEmails = false,
        canResolveOutputDirectory = false,
        canOpenGeneratedFolders = false,
    )
}
