# Certificate Configuration Plan

## Goal

Move certificate generation from hardcoded XLSX/form/tag behavior to an app-managed configuration system while preserving the working JVM PDF generation flow.

## Implemented Slice

1. Added a versioned certificate configuration model.
2. Added validation for invalid config shape and unsupported references.
3. Added an app-managed configuration repository that auto-loads installed `config.json` and falls back to the bundled default.
4. Added DataStore-backed XLSX header mapping persistence.
5. Refactored XLSX parsing to inspect headers first and map configured field IDs from remembered header selections.
6. Replaced hardcoded certificate replacement building with config-driven manual/XLSX/computed field resolution.
7. Replaced the hardcoded Conversion form with dynamic field rendering.
8. Preserved missing-template-tag UX for dynamic manual fields.
9. Added focused tests for config validation and XLSX header mapping persistence.

## Architecture

### Config Layer

- `CertificateConfiguration`
- `CertificateConfigurationValidator`
- `CertificateConfigurationRepository`

### XLSX Mapping Layer

- `XlsxParser.readFirstSheet(...)`
- `XlsxEntryMapper.mapEntries(...)`
- `XlsxHeaderSelectionStore`

### Presentation Layer

- `ConversionViewModel` now combines:
  - config state
  - dynamic manual values
  - remembered XLSX header mappings
  - DOCX placeholder inspection
  - existing preview/progress/cached-email state
- `ConversionScreen` renders manual fields dynamically and shows an XLSX header-mapping dialog when needed.

### Generation Layer

- `GenerateCertificatesRequest` now carries:
  - loaded configuration
  - manual values map
  - unchanged output/email-adjacent metadata (`docIdStart`, `certificateName`, `feedbackUrl`)
- `BuildCertificateReplacementsUseCase` resolves:
  - manual fields
  - XLSX-backed fields
  - computed template fields
  - sequential document number tag

## Remaining Follow-up Work

1. Expose config-load fallback status in the UI if operators need visibility when installed JSON is invalid.
2. Add optional header auto-matching heuristics beyond exact field ID matches if users request it.
3. Add docs/examples for replacing the packaged `config.json` in customer installations.
4. Decide whether future email recipient mapping should also move into config or stay separate.
5. Add more tests for custom field labels and select/multiline rendering if this area keeps expanding.

## Verification

- `./gradlew :feature:certificate:compileKotlinJvm`
- `./gradlew :feature:certificate:jvmTest`
- `./gradlew :composeApp:jvmTest`
