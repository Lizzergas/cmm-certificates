# Repository Guidelines

## Project Structure & Module Organization
- `composeApp/` is the Kotlin Multiplatform module. Shared code lives in `composeApp/src/commonMain/kotlin`, with platform code in `androidMain`, `iosMain`, and `jvmMain`.
- Feature screens and routes live under `composeApp/src/commonMain/kotlin/com/cmm/certificates/features/` (e.g., `home`, `test`).
- `iosApp/` contains the native iOS app entry point and any SwiftUI code.
- Build scripts and Gradle config live at the repo root (`build.gradle.kts`, `settings.gradle.kts`, `gradle/`).
- Tests (if added) should go in `composeApp/src/commonTest/kotlin` for shared tests, or the relevant platform source set for platform-specific tests.

## Technical Scope & Key Libraries
- UI: Compose Multiplatform with Material 3 and shared resources (`org.jetbrains.compose.*`).
- Navigation: JetBrains Navigation3 with `NavBackStack`, entry providers, and saved state via `kotlinx.serialization` modules in `Navigator.kt`.
- DI: Koin (`appModule`), wired in `App.kt` through `KoinApplication`.
- i18n: `LocalAppLocale` expect/actual with `AppEnvironment` providing a composition local; `customAppLocale` currently defaults to `"lt"` in `LocaleProvider.kt`.
- File access: FileKit (`filekit-core`, dialogs) for cross-platform file dialogs.
- Read `FEATURES.md` for current product scope, placeholder support, and platform limitations.

## Build, Test, and Development Commands
- `./gradlew :composeApp:assembleDebug` builds the Android debug APK.
- `./gradlew :composeApp:run` runs the desktop (JVM) app.
- Open `iosApp/` in Xcode to build and run the iOS app.
- `./gradlew :composeApp:test` runs JVM/common tests (uses Kotlin test).

## Coding Style & Naming Conventions
- Kotlin: 4-space indentation, no tabs. Keep functions small and prefer immutable `val`.
- Naming: `PascalCase` for classes and `@Composable` functions, `camelCase` for methods and properties, `SCREAMING_SNAKE_CASE` for constants.
- Files: one main type per file; match file names to public classes (e.g., `Navigator.kt`).
- Formatting/linting: follow standard Kotlin/Compose style; no project-specific formatter is configured yet.

## Testing Guidelines
- Framework: `kotlin.test` in `commonTest`.
- Naming: test classes `*Test`, test methods should describe behavior (e.g., `returnsDefaultLocaleWhenUnset`).
- Run tests with `./gradlew :composeApp:test`; add platform-specific tests under the appropriate source set when needed.

## Commit & Pull Request Guidelines
- Commit messages follow Conventional Commits style (e.g., `feat: add locale support`, `fix: handle null back stack`).
- PRs should include: a clear description, linked issue (if applicable), testing notes, and UI screenshots for visible changes.

## Security & Configuration Tips
- Keep `local.properties` and any machine-specific SDK paths out of version control.
- Avoid hard-coding credentials; prefer environment variables or local config files.
