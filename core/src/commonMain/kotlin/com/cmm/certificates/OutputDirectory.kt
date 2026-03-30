package com.cmm.certificates

expect object OutputDirectory {
    fun resolve(path: String): String
    fun ensureExists(path: String): Boolean
    fun canWrite(path: String): Boolean
}

private const val DEFAULT_OUTPUT_PATH = "pdf/"

fun preferredDefaultOutputDirectory(installationDirectoryPath: String?): String {
    val installationPath = installationDirectoryPath
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.takeIf(OutputDirectory::canWrite)
    return installationPath ?: OutputDirectory.resolve(DEFAULT_OUTPUT_PATH)
}

fun shouldResetLegacyInstallOutputDirectory(
    configuredOutputDirectory: String,
    installationDirectoryPath: String?,
): Boolean {
    if (configuredOutputDirectory.isBlank() || installationDirectoryPath.isNullOrBlank()) return false

    val normalizedConfigured = runCatching { OutputDirectory.resolve(configuredOutputDirectory) }.getOrNull()
    val normalizedInstallation = runCatching { OutputDirectory.resolve(installationDirectoryPath) }.getOrNull()

    return normalizedConfigured != null &&
        normalizedConfigured == normalizedInstallation &&
        !OutputDirectory.canWrite(normalizedConfigured)
}
