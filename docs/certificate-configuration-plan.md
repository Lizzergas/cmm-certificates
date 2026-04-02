# Certificate Configuration Plan

## Goal

Move certificate generation from hardcoded XLSX/form/tag behavior to an app-managed configuration system while preserving the working JVM PDF generation flow.

## Current Implementation

1. Added a dedicated `feature:certificateconfig` module.
2. Replaced the old schema with a smaller tag-based config model.
3. Made `config.json` the startup source of truth, with fallback to `default.config.json` and then code defaults.
4. Added config editing UI in Settings.
5. Added inline manual-field config editing from the Conversion screen.
6. Removed computed fields and positional XLSX column assumptions.
7. Switched XLSX mapping to config-defined header names.
8. Preserved placeholder-aware field disabling for missing DOCX tags.
9. Added JVM file watching for selected XLSX and DOCX files, with automatic refresh and notifications.

## Architecture

### Config Layer

- `CertificateConfiguration`
- `CertificateConfigurationValidator`
- `CertificateConfigurationRepository`
- `ActiveCertificateConfigStore`

### XLSX Mapping Layer

- `XlsxParser.readFirstSheet(...)`
- `XlsxEntryMapper.mapEntries(...)`
- config-defined `headerName` values inside `xlsxFields`

### Presentation Layer

- `ConversionViewModel` now combines:
  - config state
  - dynamic manual values
  - DOCX placeholder inspection
  - inline manual-field editing state
  - refresh notifications
  - existing preview/progress/cached-email state
- `ConversionScreen` renders manual fields dynamically and allows inline config edits.
- `CertificateConfigScreen` owns the full config editor.

### Generation Layer

- `GenerateCertificatesRequest` now carries:
  - loaded configuration
  - manual values map
  - unchanged output/email-adjacent metadata (`docIdStart`, `certificateName`, `feedbackUrl`)
- `BuildCertificateReplacementsUseCase` resolves:
  - manual fields
  - XLSX-backed fields
  - sequential document number tag

## Remaining Follow-up Work

1. Add more focused tests for watcher-driven refresh success/failure paths.
2. Decide whether email-related tags should become first-class config concepts or stay convention-based.
3. Add import/export UI only if real distribution needs appear.
4. Polish the config editor copy and move more inline UI strings into resources if desired.

## Verification

- `./gradlew :feature:certificateconfig:jvmTest`
- `./gradlew :feature:certificate:jvmTest`
- `./gradlew :composeApp:jvmTest`
- `./gradlew :composeApp:test`
