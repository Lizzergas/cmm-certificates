# Email Sending Guide

This document explains how SMTP settings, authentication, and email delivery are wired in this repo.

## Overview

- Users configure SMTP settings in the Settings screen.
- Auth is verified via `testConnection()`.
- After PDFs are generated, the Success state shows a "Send emails" button.
- Clicking it opens the Email progress screen and sends each generated PDF to the matching XLSX
  entry.

## User Flow

1) Go to Settings (currently opened by clicking the logo on the home screen).
2) Enter SMTP host, port, username, password, and transport.
3) Click **Authenticate** (must succeed to enable email sending).
4) Generate PDFs from the Conversion screen.
5) On the Success screen, click **Send emails**.

## SMTP Settings

Location:

- UI: `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/settings/SettingsScreen.kt`
- State:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/settings/SmtpSettingsStore.kt`

Fields:

- Host, Port, Username, Password, Transport.
- Email subject and body are configured here as well.
- Username is used as the sender address.
- Transport options: `SMTP`, `SMTPS`, `SMTP_TLS` (see `SmtpTransport.kt`).

Auth:

- `SmtpSettingsStore.authenticate()` runs `SmtpClient.testConnection()`.
- On success: `isAuthenticated = true`.
- On failure: `errorMessage` is set and auth is reset.

## Email Delivery

Location:

- Email progress screen:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/ui/EmailProgressScreen.kt`
- Email sending logic:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/ui/EmailSenderViewModel.kt`
- SMTP client interface:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/data/email/SmtpClient.kt`
- JVM implementation:
  `composeApp/src/jvmMain/kotlin/com/cmm/certificates/data/email/SmtpClient.jvm.kt`

Behavior:

- Subject: taken from Settings (fallback: `Pažyma`).
- Body: taken from Settings (fallback: `Certificate attached.`).
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

The output directory and `docIdStart` are stored in `ConversionProgressStore` when PDFs are
generated.

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

- SMTP settings and email subject/body are persisted with DataStore Preferences.
- Stored settings are loaded on app start and an authentication attempt is made automatically.

## UI and Navigation

- Settings screen entry:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/settings/SettingsEntryProvider.kt`
- Email progress screen entry:
  `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/emailsending/ui/EmailEntryProvider.kt`
- Navigation wiring: `composeApp/src/commonMain/kotlin/com/cmm/certificates/Navigator.kt`
- "Send emails" button enablement:
    - Located in
      `composeApp/src/commonMain/kotlin/com/cmm/certificates/feature/pdfconversion/ui/PdfConversionProgressScreen.kt`.
    - Enabled only when `SmtpSettingsStore.state.isAuthenticated == true`.

## Troubleshooting

- UI freezes during send: ensure sending is on `Dispatchers.IO` (already implemented).
- "Missing email address" error: check `primaryEmail` in the XLSX.
- "Attachment not found" error: verify output directory and that PDFs exist.

## Extending

- To support Android/iOS:
    - Implement `SmtpClient` with a platform mailer or API.
    - Provide secure storage for credentials.
- To customize subject/body per recipient:
    - Update `EmailSenderViewModel` constants or build per entry.
