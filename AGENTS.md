# Repository Guidelines

## Project Structure & Module Organization
- `androidApp/` is the Android application module with `src/main` entry point code, manifest, and Android resources.
- `composeApp/` is the thin Kotlin Multiplatform app shell. It owns `App.kt`, `Navigator.kt`, Koin aggregation, the desktop entry point, and the iOS `MainViewController`.
- `core/` is the shared KMP library for resources, theme, UI primitives, platform abstractions, logging, i18n, and shared `expect/actual` implementations.
- Feature code lives in dedicated modules under `feature/` (`settings`, `certificate`, `pdfconversion`, `emailsending`), typically split into `data`, `domain`, `presentation`, and `di` packages.
- `iosApp/` contains the native iOS app entry point and any SwiftUI code.
- Build scripts and Gradle config live at the repo root (`build.gradle.kts`, `settings.gradle.kts`, `gradle/`).
- App-shell shared tests live in `composeApp/src/commonTest/kotlin`; JVM-specific tests live in `composeApp/src/jvmTest/kotlin`.

## Technical Scope & Key Libraries
- UI: Compose Multiplatform with Material 3 and shared resources (`org.jetbrains.compose.*`).
- Navigation: JetBrains Navigation3 with `NavBackStack`, entry providers, and saved state via `kotlinx.serialization` modules in `Navigator.kt`.
- DI: Koin (`appModule`), wired in `App.kt` through `KoinApplication`.
- i18n: `LocalAppLocale` expect/actual with `AppEnvironment` providing a composition local; `customAppLocale` currently defaults to `"lt"` in `LocaleProvider.kt`.
- File access: FileKit (`filekit-core`, dialogs) for cross-platform file dialogs.
- Read `FEATURES.md` for current product scope, placeholder support, and platform limitations.

## Build, Test, and Development Commands
- `./gradlew :androidApp:assembleDebug` builds the Android debug APK.
- `./gradlew :composeApp:run` runs the desktop (JVM) app.
- Open `iosApp/` in Xcode to build and run the iOS app.
- `./gradlew :composeApp:test` runs the composeApp Android host tests.
- `./gradlew :composeApp:jvmTest` runs JVM-specific tests.

## Coding Style & Naming Conventions
- Kotlin: 4-space indentation, no tabs. Keep functions small and prefer immutable `val`.
- Naming: `PascalCase` for classes and `@Composable` functions, `camelCase` for methods and properties, `SCREAMING_SNAKE_CASE` for constants.
- Files: one main type per file; match file names to public classes (e.g., `Navigator.kt`).
- Formatting/linting: follow standard Kotlin/Compose style; no project-specific formatter is configured yet.

## Testing Guidelines
- Framework: `kotlin.test` in `commonTest`.
- Naming: test classes `*Test`, test methods should describe behavior (e.g., `returnsDefaultLocaleWhenUnset`).
- Run tests with `./gradlew :composeApp:test` and `./gradlew :composeApp:jvmTest`; add platform-specific tests under the appropriate module/source set when needed.

## Commit & Pull Request Guidelines
- Commit messages follow Conventional Commits style (e.g., `feat: add locale support`, `fix: handle null back stack`).
- PRs should include: a clear description, linked issue (if applicable), testing notes, and UI screenshots for visible changes.

## Security & Configuration Tips
- Keep `local.properties` and any machine-specific SDK paths out of version control.
- Avoid hard-coding credentials; prefer environment variables or local config files.
