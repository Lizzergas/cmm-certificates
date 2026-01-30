# Design System and UI Architecture

This document summarizes how UI, state, and design tokens work in the app.

## App Structure (High Level)
- **Home / Conversion**: select XLSX + DOCX, fill certificate fields, start conversion.
- **PDF Conversion Progress**: shows progress, success info, preview email dialog, and actions.
- **Email Progress**: sends one email per entry with generated PDFs.
- **Settings**: SMTP settings, email template, and certificate defaults.

## Architecture (Feature Pattern)
Each feature follows a simple layering model:
- **UI**: composables and view models in `feature/**/ui`.
- **Domain**: interfaces in `feature/**/domain` (repositories).
- **Data**: stores / repository implementations in `feature/**/data`.

State is exposed as `StateFlow` from repositories or view models and mapped into UI state
in view models using `stateIn(...)` with `SharingStarted.WhileSubscribed`.

### State Management
- **Settings**: `SettingsRepository` is the source of truth and persists via `SettingsStore`.
- **Conversion**: `ConversionViewModel` composes UI state from:
  - `ConversionFormState`
  - `ConversionFilesState`
  - parsed XLSX entries
  - settings + network availability
- **Progress**: `PdfConversionProgressRepository` and `EmailProgressRepository` drive progress
  screens; UI derives mode from repository state.

### Use Cases
- `ClearAllDataUseCase` clears settings and progress stores.
- `SendPreviewEmailUseCase` sends a single preview email (optionally attaches the first PDF)
  and persists the preview email address.

## Design Tokens
Design values live in `core/theme/Dimens.kt` and are used throughout UI code.

### Grid Spacing
- **Grid.x1 = 2.dp**, **Grid.x2 = 4.dp**, **Grid.x3 = 6.dp**, ...
- Use `Grid` tokens for padding, gaps, sizes, and elevations.

### Stroke
- `Stroke.thin = 1.dp`

### Colors
- Use only `MaterialTheme.colorScheme` values in UI.
- `tertiary`/`tertiaryContainer` are used for success emphasis (green in theme).

## Common UI Components
- `PrimaryActionButton`: main CTA button with enabled/disabled styling.
- `SelectFileIcon`: file pick card with selected state.
- `ProgressIndicatorContent` / `ProgressErrorContent`: shared progress screens.
- `PreviewEmailDialog`: reusable preview email dialog with sending/success states.
- `ClearableOutlinedTextField`: shared text input with clear icon.

## Resources & Strings
- Use Compose Multiplatform resources (`Res.*`) for strings and assets.
- String formatting uses `stringResource(res, args...)` (no manual `%d` replacements).

## File Picking
- `rememberFilePickerLauncher()` in `core/ui/FilePickerLauncher.kt` wraps FileKit.

## Navigation
- Navigation uses JetBrains Navigation3 with entry providers in each feature.
- Routes are `NavKey` objects serialized via `SerializersModule`.

