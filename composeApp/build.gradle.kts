import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val desktopAppName = "CMM Sertifikatai"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.conveyor)
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

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = desktopAppName
            packageVersion = project.version.toString()
            includeAllModules = true
            description = "CMM certificate generation and email delivery desktop app"
            vendor = "CMM"

            linux {
                packageName = "cmm-sertifikatai"
            }

            macOS {
                packageName = desktopAppName
            }

            windows {
                upgradeUuid = "a2b5eaa2-a40f-4507-886e-fb9db5815121"
                menu = true
                shortcut = true
                menuGroup = desktopAppName
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
