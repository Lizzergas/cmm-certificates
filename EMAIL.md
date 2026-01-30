# Email Sending Guide

This document explains how SMTP settings, authentication, and email delivery are wired in this repo.

## Overview

- Users configure SMTP settings in the Settings screen.
- Auth is verified via `SmtpClient.testConnection()`.
- After PDFs are generated, the Success state shows **Send emails** and **Send preview email**.
- Sending emails delivers one PDF per XLSX entry.
- Preview email sends a single email (optionally attaching the first PDF) and stores the address.

## User Flow

1) Go to Settings (opened by clicking the logo on the home screen).
2) Enter SMTP host, port, username, password, and transport.
3) Click **Authenticate** (must succeed to enable email sending).
4) Generate PDFs from the Conversion screen.
5) On the Success screen:
   - Click **Send preview email** to send a single preview email.
   - Click **Send emails** to send all generated PDFs.

## SMTP Settings

Location:

- UI: `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/settings/ui/SettingsScreen.kt`
- Persistence: `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/settings/data/SettingsStore.kt`
- Repository: `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/settings/data/SettingsRepositoryImpl.kt`

Fields:

- Host, Port, Username, Password, Transport.
- Email subject, body, and signature are configured in Settings.
- Preview email address is stored in Settings.
- Username is used as the sender address.
- Transport options: `SMTP`, `SMTPS`, `SMTP_TLS` (see `SmtpTransport.kt`).

Auth:

- `SettingsRepository.authenticate()` runs `SmtpClient.testConnection()`.
- On success: `isAuthenticated = true`.
- On failure: `errorMessage` is set and auth is reset.

## Email Delivery

Location:

- Email progress screen:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/ui/EmailProgressScreen.kt`
- Email sending logic:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/ui/EmailSenderViewModel.kt`
- Preview email use case:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/core/usecase/SendPreviewEmailUseCase.kt`
- SMTP client interface:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/data/email/SmtpClient.kt`
- JVM implementation:
  `composeApp/src/jvmMain/kotlin/com/cmm/certificates/data/email/SmtpClient.jvm.kt`

Behavior:

- Subject/body/signature: taken from Settings.
- One email per XLSX entry.
- Attachment: `${docIdStart + index}.pdf`.
- Recipient: `primaryEmail` from XLSX entry.
- Sender: SMTP `username`.

Mapping between entries and files:

```
docIdStart = user input (e.g., 123)
entries[0] -> 123.pdf
entries[1] -> 124.pdf
...
```

The output directory and `docIdStart` are stored in `PdfConversionProgressStore` when PDFs are
generated.

## Preview Email

- Triggered from the PDF conversion success screen.
- Uses `SendPreviewEmailUseCase`.
- Stores the preview email address in Settings for reuse.
- Attaches the first generated PDF when invoked from the conversion screen.

## Dependencies

- Simple Java Mail (JVM only): `org.simplejavamail:simple-java-mail`.
  - Version set in `gradle/libs.versions.toml`.
  - Added in `composeApp/build.gradle.kts` under `jvmMain.dependencies`.
- Simple Java Mail uses Jakarta Mail for transport.
- DataStore Preferences (KMP): `androidx.datastore:datastore` and
  `androidx.datastore:datastore-preferences`.

## Platform Support

- JVM: supported (Simple Java Mail implementation).
- Android/iOS: not supported yet (throws `UnsupportedOperationException`).
  - See `composeApp/src/androidMain/.../SmtpClient.android.kt` and
    `composeApp/src/iosMain/.../SmtpClient.ios.kt`.

## Persistence

- SMTP settings and email template fields are persisted with DataStore Preferences.
- Stored settings are loaded on app start and an authentication attempt is made automatically.

## UI and Navigation

- Settings screen entry:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/settings/ui/SettingsEntryProvider.kt`
- Email progress screen entry:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/ui/EmailEntryProvider.kt`
- PDF progress screen entry:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/pdfconversion/ui/ProgressEntryProvider.kt`
- Navigation wiring: `composeApp/src/commonMain/kotlin/com/cmm/certificates/Navigator.kt`

## Troubleshooting

- UI freezes during send: ensure sending is on `Dispatchers.IO` (already implemented).
- "Missing email address" error: check `primaryEmail` in the XLSX.
- "Attachment not found" error: verify output directory and that PDFs exist.

## Extending

- To support Android/iOS:
  - Implement `SmtpClient` with a platform mailer or API.
  - Provide secure storage for credentials.
- To customize subject/body per recipient:
  - Update `EmailSenderViewModel` or build per entry.
