package com.cmm.certificates.core.domain

actual fun getAppCapabilities(): AppCapabilities {
    return AppCapabilities(
        canParseXlsx = true,
        canGeneratePdf = true,
        canSendEmails = true,
        canResolveOutputDirectory = true,
        canOpenGeneratedFolders = true,
    )
}
