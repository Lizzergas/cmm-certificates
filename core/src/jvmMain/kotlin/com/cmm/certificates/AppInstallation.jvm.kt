package com.cmm.certificates

import java.io.File

actual object AppInstallation {
    actual fun installedResourcePath(fileName: String): String? {
        val candidates = listOfNotNull(
            resourcesDirectory()?.resolve(fileName),
            installationDirectory()?.resolve(fileName),
            projectPackagingResource(fileName),
            File(System.getProperty("user.dir"), fileName),
        )

        return candidates.firstOrNull { it.isFile }?.absolutePath
    }

    actual fun installationDirectoryPath(): String? {
        return installationDirectory()?.absolutePath
    }

    actual fun preferredOutputBaseDirectoryPath(): String? {
        return packagedInstallationDirectory()?.takeIf(OutputDirectory::canWrite)
    }

    private fun installationDirectory(): File? {
        return packagedInstallationDirectory()?.let(::File)
            ?: System.getProperty("app.dir")
                ?.takeIf(String::isNotBlank)
                ?.let(::File)
                ?.takeIf(File::isDirectory)
            ?: File(System.getProperty("user.dir")).takeIf(File::isDirectory)
    }

    private fun packagedInstallationDirectory(): String? {
        return resourcesDirectory()?.parentFile?.takeIf(File::isDirectory)?.absolutePath
            ?: System.getProperty("app.dir")
                ?.takeIf(String::isNotBlank)
                ?.let(::File)
                ?.takeIf(File::isDirectory)
                ?.absolutePath
    }

    private fun resourcesDirectory(): File? {
        return System.getProperty("compose.application.resources.dir")
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
            ?.takeIf(File::isDirectory)
    }

    private fun projectPackagingResource(fileName: String): File {
        return File(System.getProperty("user.dir"), "composeApp/packaging/resources/common/$fileName")
    }
}
