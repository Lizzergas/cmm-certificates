# Features and Technical Notes

## Current User Flow
1. Select an XLSX file and a DOCX template on the Conversion screen.
2. Fill certificate fields (IDs, hours, name, lecturer, etc.).
3. Click **Convert to PDF**.
4. Watch progress on the PDF Conversion Progress screen.
5. On success, optionally send a preview email or send emails to all recipients.

PDF generation is local on JVM and no longer requires network access. Network access is still relevant for SMTP email delivery.

## Output Behavior
- PDFs are generated as `${docIdStart + index}.pdf`.
- Output folder is auto-created under `./pdf/<sanitized certificate name>`.
- The output folder and `docIdStart` are stored in `PdfConversionProgressStore` for email sending.

## Template Placeholders
DOCX templates use placeholders like:
- `{{vardas_pavarde}}`
- `{{data}}`
- `{{akreditacijos_id}}`
- `{{dokumento_id}}`
- `{{akreditacijos_tipas}}`
- `{{akreditacijos_valandos}}`
- `{{sertifikato_pavadinimas}}`
- `{{destytojas}}`
- `{{destytojo_tipas}}`

## PDF Generation (JVM)
- Replaces placeholders using Apache POI.
- Converts DOCX → PDF with docx4j.
- Stamps background image behind content using PDFBox.
- Keeps only the first page (avoids extra pages).

See `PDF_GEN.md` for the detailed pipeline.

## XLSX Parsing (JVM)
- Parses the first sheet without external XLSX libraries.
- Column mapping is positional (A–H), not header-based.
- Stops at first empty date cell or fully empty row.
- Timestamp formats:
  - String: `MM/dd/yyyy HH:mm:ss`
  - Excel serial (1900 system with leap-year bug adjustment)

## Email Sending
- SMTP configured in **Settings**.
- Authentication is required before sending.
- **Preview email** sends a single email (first PDF attached) and stores the preview email address.
- **Send emails** sends one PDF per XLSX entry.
- Failed sends are cached and can be retried later.
- Cached retries require authenticated SMTP settings and verify that the original PDF attachments still exist.
- Daily email limit `0` means unlimited sending.
- **Clear all** resets SMTP settings, cached retries, and the persisted 24-hour send history.

## Email Signature Editor
- The Settings screen provides an **Edit signature** dialog instead of a raw HTML text area.
- Supports **Builder** mode (font, size, bold/italic, line height, color, and line list).
- Supports **HTML** mode (paste/edit full HTML) with validation.
- Supports **Preview** mode (builder-based preview; custom HTML falls back to a message).
- The editor is **ephemeral**: changes are only saved when the user clicks **Save**.
- Stored value remains a single string: `email.signatureHtml`.

## Platform Support
- JVM: full XLSX parsing + DOCX templating + PDF generation + email sending.
- Android/iOS: UI now exposes unsupported capability states instead of relying on runtime-only failures for JVM-only features.

## Module Layout
- `androidApp`: Android app shell only.
- `composeApp`: shared app shell, DI aggregation, navigation root, desktop/iOS entry points.
- `core`: shared resources, theme, UI primitives, platform abstractions, expect/actual code.
- `feature/settings`: settings state, persistence, SMTP auth, signature editor.
- `feature/certificate`: conversion form, XLSX parser integration, DOCX/PDF generation pipeline.
- `feature/pdfconversion`: conversion progress and preview-email flow.
- `feature/emailsending`: send, retry, caches, and email progress flow.

## Platform Matrix

| Capability | JVM | Android | iOS |
|-----------|-----|---------|-----|
| XLSX parsing | Yes | Not yet | Not yet |
| DOCX -> PDF generation | Yes | Not yet | Not yet |
| Preview email | Yes | Not yet | Not yet |
| Bulk email sending | Yes | Not yet | Not yet |
| Cached retry sending | Yes | Not yet | Not yet |
| Open output folder | Yes | Not yet | Not yet |

## UI Components
- `SelectFileIcon`: file selection cards (idle/selected states).
- `PrimaryActionButton`: main CTA with enabled/disabled styling.
- `ProgressIndicatorContent` / `ProgressErrorContent`: shared progress UI.
- `PreviewEmailDialog`: reusable preview email dialog.
- `SignatureEditorDialog`: reusable signature editor dialog.

## Known Gaps
- Android/iOS PDF generation and SMTP sending are not implemented.
- Limited placeholder set (see above).
- Some repository defaults still rely on localized default content rather than user-selectable locale persistence.
