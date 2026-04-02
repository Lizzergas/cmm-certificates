import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")

    android {
        namespace = "com.cmm.certificates.feature.certificate"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.feature.certificateconfig)
            implementation(projects.feature.settings)
        }

        jvmMain.dependencies {
            implementation(libs.poi.ooxml)
            implementation(libs.docx4j.jaxb.referenceimpl)
            implementation(libs.docx4j.export.fo)
            implementation(libs.pdfbox)
            implementation(libs.icepdf.core)
            implementation(libs.icepdf.viewer)
            implementation(libs.log4j.core)
        }

        jvmTest.dependencies {
            implementation(libs.junit)
            implementation("org.jetbrains.compose.ui:ui-test-junit4:1.10.3")
            implementation(compose.desktop.currentOs)
        }
    }
}
