# Architecture

## Overview

This repository is a multi-module Kotlin Multiplatform app centered around one JVM-first workflow:

1. Parse participant rows from XLSX.
2. Fill a DOCX certificate template.
3. Convert DOCX to PDF.
4. Send generated PDFs by email.
5. Cache failed sends and retry them later.

The current module graph is:

- `androidApp` - Android application shell
- `composeApp` - thin KMP app shell (`App.kt`, `Navigator.kt`, DI aggregation, desktop/iOS entry points)
- `core` - shared resources, theme, UI primitives, logging, i18n, platform abstractions, and most `expect/actual`
- `feature/settings` - settings, SMTP auth, templates, signature editor
- `feature/certificate` - conversion form, XLSX parsing, DOCX/PDF generation pipeline
- `feature/pdfconversion` - conversion progress and preview email flow
- `feature/emailsending` - bulk email sending, retry, progress, and caches

## Feature Structure

Each feature module is organized under `feature/<name>/src/commonMain/kotlin/...` with these subpackages:

- `data/` - adapters, stores, repository implementations
- `domain/` - models, repository contracts, ports, and use cases
- `presentation/` - screens, view models, components, and navigation entry providers
- `di/` - Koin module wiring

Current features:

- `feature/certificate/` - conversion input flow
- `feature/pdfconversion/` - conversion progress and success flow
- `feature/emailsending/` - bulk email send and retry flow
- `feature/settings/` - SMTP, email template, and certificate defaults

## Layer Responsibilities

### Presentation

Presentation code lives in `feature/**/presentation`.

- Screens collect state with Compose and delegate actions to view models.
- View models coordinate user actions through use cases and repositories.
- Navigation entry providers live in `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/*/presentation`.
- Feature-local components live in `feature/**/presentation/components` when they are not shared.

Examples:

- `feature/certificate/src/commonMain/kotlin/com/cmm/certificates/feature/certificate/presentation/ConversionScreen.kt`
- `feature/pdfconversion/src/commonMain/kotlin/com/cmm/certificates/feature/pdfconversion/presentation/PdfConversionProgressScreen.kt`
- `feature/emailsending/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/presentation/EmailProgressScreen.kt`
- `feature/settings/src/commonMain/kotlin/com/cmm/certificates/feature/settings/presentation/SettingsScreen.kt`

### Domain

Domain code lives in `feature/**/domain` and `core/src/commonMain/kotlin/com/cmm/certificates/core/domain`.

- Repository interfaces and feature ports are defined here.
- Workflow use cases live here.
- Domain models are shared across presentation and data.
- Capability and connectivity abstractions are exposed through core/domain interfaces.

Examples:

- `feature/certificate/src/commonMain/kotlin/com/cmm/certificates/feature/certificate/domain/usecase/GenerateCertificatesUseCase.kt`
- `feature/emailsending/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/domain/usecase/SendEmailRequestsUseCase.kt`
- `core/src/commonMain/kotlin/com/cmm/certificates/core/domain/AppCapabilities.kt`
- `core/src/commonMain/kotlin/com/cmm/certificates/core/domain/ConnectivityMonitor.kt`

### Data

Data code lives in feature modules, `core`, and platform source sets owned by those modules.

- Repository implementations and stores live here.
- Platform adapters for DOCX, XLSX, SMTP, file system, and connectivity live here.
- DataStore-backed persistence is encapsulated in stores and repository implementations.

Examples:

- `feature/settings/src/commonMain/kotlin/com/cmm/certificates/feature/settings/data/SettingsRepositoryImpl.kt`
- `feature/emailsending/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/data/EmailProgressRepositoryImpl.kt`
- `feature/emailsending/src/jvmMain/kotlin/com/cmm/certificates/data/email/SmtpClient.jvm.kt`
- `feature/certificate/src/jvmMain/kotlin/com/cmm/certificates/data/xlsx/XlsxParser.jvm.kt`

## Workflow Design

### Conversion Flow

- `ConversionViewModel` builds input state from form values, selected files, parsed XLSX rows, settings, connectivity, and cached-email metadata.
- `ParseRegistrationsUseCase` delegates XLSX parsing through the `RegistrationParser` port.
- `GenerateCertificatesUseCase` delegates template loading, PDF generation, and output directory handling through ports.
- Progress is persisted in `PdfConversionProgressRepository` so later screens can continue from generated output metadata.

### Email Flow

- `SendGeneratedEmailsUseCase` prepares one email request per generated PDF.
- `SendEmailRequestsUseCase` enforces SMTP auth, connectivity, daily limits, attachment existence, caching, and stop reasons.
- Failed sends are persisted through `CachedEmailStore` and retried through `RetryCachedEmailsUseCase`.
- `SendPreviewEmailUseCase` reuses the same gateway abstraction for a single-message send.

### Settings Flow

- `SettingsRepository` is the source of truth for SMTP settings, templates, signature HTML, and certificate defaults.
- `SettingsStore` persists settings through DataStore.
- `SettingsViewModel` maps repository state into presentation state and capability-aware actions.

## Navigation

Navigation is centralized in `composeApp/src/commonMain/kotlin/com/cmm/certificates/Navigator.kt` and uses Navigation3 keys plus feature entry providers.

Routes are declared per feature in presentation navigation packages:

- `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/certificate/presentation/ConversionEntryProvider.kt`
- `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/pdfconversion/presentation/ProgressEntryProvider.kt`
- `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/presentation/EmailEntryProvider.kt`
- `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/settings/presentation/SettingsEntryProvider.kt`

## Capabilities and Platform Support

The app now exposes platform support through `AppCapabilities` instead of relying only on runtime `UnsupportedOperationException`s.

- JVM supports XLSX parsing, DOCX to PDF conversion, SMTP sending, output directory resolution, and opening generated folders.
- Android and iOS currently expose unsupported capability states for the JVM-only parts of the workflow.

The UI uses these capabilities to disable unsupported actions and explain why they are unavailable.

## DI

Koin aggregation is configured in `composeApp/src/commonMain/kotlin/com/cmm/certificates/core/di`.

- `appModule` includes shared infrastructure plus feature modules.
- Each feature module wires its own presentation, domain, and data dependencies.
- `core` provides connectivity and capability abstractions shared across features.

## Testing

The repository now has both Android-host/JVM-shell tests and module-local JVM tests:

- `composeApp/src/commonTest/kotlin` and `composeApp/src/jvmTest/kotlin` still cover app-shell and integration-oriented cases.
- Module-local JVM/platform tests live beside their owning modules as the split continues.

Preferred testing style:

- use fakes instead of mocks when practical
- keep business-rule tests deterministic
- use JVM-specific tests only for code that genuinely depends on JVM-only APIs or runtime behavior
