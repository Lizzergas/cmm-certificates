package com.cmm.certificates

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppInstallationJvmTest {

    @Test
    fun installedResourcePath_findsPackagedLegalFilesFromWorkspaceRoot() {
        withUserDir(File(System.getProperty("user.dir"))) {
            val path = AppInstallation.installedResourcePath("EULA.txt")

            assertNotNull(path)
            assertTrue(path.normalizedPath().endsWith("packaging/resources/common/EULA.txt"))
        }
    }

    @Test
    fun installedResourcePath_findsPackagedLegalFilesFromComposeAppDirectory() {
        val currentDirectory = File(System.getProperty("user.dir"))
        val composeAppDirectory = if (File(currentDirectory, "packaging/resources/common").isDirectory) {
            currentDirectory
        } else {
            File(currentDirectory, "composeApp")
        }

        withUserDir(composeAppDirectory) {
            val path = AppInstallation.installedResourcePath("LICENSE.txt")

            assertNotNull(path)
            assertTrue(path.normalizedPath().endsWith("packaging/resources/common/LICENSE.txt"))
        }
    }

    private fun String.normalizedPath(): String = replace('\\', '/')

    private fun withUserDir(directory: File, block: () -> Unit) {
        val previousUserDir = System.getProperty("user.dir")
        System.setProperty("user.dir", directory.absolutePath)
        try {
            block()
        } finally {
            System.setProperty("user.dir", previousUserDir)
        }
    }
}
