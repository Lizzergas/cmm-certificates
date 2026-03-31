import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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
                iconFile.set(project.file("packaging/icons/cmm_logo.ico"))
            }
        }
    }
}

tasks.register("test") {
    group = "verification"
    description = "Runs Compose app Android host tests."
    dependsOn(tasks.named("testAndroidHostTest"))
}
