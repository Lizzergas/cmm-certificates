import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.time.Year

val desktopAppName = providers.gradleProperty("appDisplayName").orElse("CMM Sertifikatai").get()
val desktopAppDescription = providers.gradleProperty("appDescription")
    .orElse("Certificate generation, PDF preparation, and email delivery.")
    .get()
val desktopAppVendor = providers.gradleProperty("appVendor").orElse("CMM").get()
val desktopAppLegalOwner = providers.gradleProperty("appLegalOwner").orElse(desktopAppVendor).get()
val desktopAppCommitHash = providers.gradleProperty("appCommitHash")
    .orElse(providers.environmentVariable("GITHUB_SHA"))
    .map { it.take(7) }
    .orElse("dev")
    .get()
val windowsInstallationPath = providers.gradleProperty("windowsInstallationPath")
    .orElse("CMM/CMM Sertifikatai")
    .get()
val windowsPerUserInstall = providers.gradleProperty("windowsPerUserInstall")
    .map(String::toBoolean)
    .orElse(false)
val windowsInstallerIcon = project.file("packaging/icons/cmm_logo.ico")
val isWindowsHost = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
val shouldValidateWindowsInstallerIcon = gradle.startParameter.taskNames.any { taskName ->
    val simpleName = taskName.substringAfterLast(':')
    simpleName.matches(Regex("package.*(Exe|Msi)")) ||
        (isWindowsHost && simpleName.contains("DistributionForCurrentOS"))
}

if (shouldValidateWindowsInstallerIcon) {
    requireValidWindowsIcon(windowsInstallerIcon)
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sentry)
}

kotlin {
    android {
        namespace = "com.cmm.certificates.composeapp"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        withHostTest {
            isIncludeAndroidResources = true
        }
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.feature.settings)
            implementation(projects.feature.certificate)
            implementation(projects.feature.certificateconfig)
            implementation(projects.feature.emailsending)
            implementation(projects.feature.pdfconversion)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        jvmTest.dependencies {
            implementation(libs.poi.ooxml)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.cmm.certificates.MainKt"
        jvmArgs += listOf(
            "-Dapp.version=${project.version}",
            "-Dapp.commitHash=$desktopAppCommitHash",
        )

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            appResourcesRootDir.set(project.layout.projectDirectory.dir("packaging/resources"))
            packageName = desktopAppName
            packageVersion = project.version.toString()
            includeAllModules = true
            description = desktopAppDescription
            vendor = desktopAppVendor
            copyright = "Copyright (c) ${Year.now().value} $desktopAppLegalOwner. All rights reserved."
            licenseFile.set(project.file("packaging/resources/common/EULA.txt"))

            linux {
                packageName = "pazymejimu-konverteris"
            }

            macOS {
                packageName = desktopAppName
            }

            windows {
                upgradeUuid = "a2b5eaa2-a40f-4507-886e-fb9db5815121"
                dirChooser = true
                perUserInstall = windowsPerUserInstall.get()
                installationPath = windowsInstallationPath
                menu = true
                shortcut = true
                menuGroup = desktopAppVendor
                iconFile.set(windowsInstallerIcon)
            }
        }
    }
}

tasks.register("test") {
    group = "verification"
    description = "Runs Compose app Android host tests."
    dependsOn(tasks.named("testAndroidHostTest"))
}

fun requireValidWindowsIcon(file: File) {
    require(file.isFile) {
        "Missing Windows installer icon at ${file.absolutePath}. Provide a real .ico file."
    }
    require(isValidWindowsIcon(file)) {
        "Invalid Windows installer icon at ${file.absolutePath}. Expected a real .ico file, not a renamed PNG or another format."
    }
}

fun isValidWindowsIcon(file: File): Boolean {
    if (!file.isFile) return false

    return runCatching {
        file.inputStream().use { input ->
            val header = ByteArray(4)
            input.read(header) == header.size &&
                header[0] == 0.toByte() &&
                header[1] == 0.toByte() &&
                header[2] == 1.toByte() &&
                header[3] == 0.toByte()
        }
    }.getOrDefault(false)
}
