# Features and Technical Notes

## Current User Flow
1. Select an XLSX file and a DOCX template on the Conversion screen.
2. Optionally open **Settings -> Certificate configuration** and define XLSX tags, manual fields, select options, and the document number tag.
3. Fill the generated certificate fields on the Conversion screen.
4. Optionally edit a manual field definition inline from the Conversion screen via the Edit action.
5. Click **Preview PDF** or **Convert to PDF**.
6. If required input is missing, the screen keeps the action visible, highlights the exact invalid fields/files, and shows inline error text.
7. Watch progress on the PDF Conversion Progress screen after a valid conversion request.
8. On success, optionally send a preview email or send emails to all recipients.

PDF generation is local on JVM and no longer requires network access. Network access is still relevant for SMTP email delivery.

## Output Behavior
- PDFs are generated as `${docIdStart + index}.pdf`.
- Output folder is auto-created under `./pdf/<sanitized certificate name>`.
- The output folder and `docIdStart` are stored in `PdfConversionProgressStore` for email sending.

## Template Placeholders
DOCX templates now use whatever tags are defined in `config.json` / the certificate configuration editor.

Examples:
- `{{vardas}}`
- `{{pavarde}}`
- `{{data}}`
- `{{dokumento_id}}`

`{{data}}` comes from a manual date field on the Conversion screen. Users can type it in `YYYY-MM-DD` format or choose it via the date picker, and the generated DOCX still receives the same Lithuanian certificate-date string as before.

Conversion-screen validation is now placeholder-aware:
- The selected DOCX template is inspected on JVM to discover which placeholders are present.
- Fields backed by missing template placeholders are disabled and shown with an explanatory tooltip/supporting text.
- `{{dokumento_id}}` remains editable even if absent from the DOCX because it is still used for PDF filenames.

Selected DOCX files are also watched on JVM. Saving the template on disk re-runs placeholder inspection automatically.

## PDF Generation (JVM)
- Replaces placeholders using Apache POI.
- Converts DOCX → PDF with docx4j.
- Stamps background image behind content using PDFBox.
- Keeps only the first page (avoids extra pages).

See `PDF_GEN.md` for the detailed pipeline.

## XLSX Parsing (JVM)
- Parses the first sheet without external XLSX libraries.
- Column mapping is config-driven by XLSX header name, not by fixed column position.
- Stops at the first fully empty row.
- Selected XLSX files are watched on JVM and auto-refresh after save.
- Missing configured headers are shown as file-level validation errors.
- Legacy positional spreadsheet field mapping has been removed from `XlsxMapping.kt`.

## Email Sending
- SMTP configured in **Settings**.
- Authentication is required before sending.
- **Preview email** sends a single email (first PDF attached) and stores the preview email address.
- **Send emails** sends one PDF per XLSX entry.
- Failed sends are cached and can be retried later.
- Cached retries require authenticated SMTP settings and verify that the original PDF attachments still exist.
- Daily email limit `0` means unlimited sending.
- **Clear all** resets SMTP settings, cached retries, and the persisted 24-hour send history.
- With the config-driven XLSX system, recipient email is read from the XLSX tag `email` if that tag is configured.

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
- `feature/certificateconfig`: certificate config schema, persistence, validation, sample XLSX inspection, config editor UI.
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
- `SelectFileIcon`: file selection cards with idle/selected/error states, file name display, and inline error text.
- `PrimaryActionButton`: main CTA with enabled/disabled styling.
- `ProgressIndicatorContent` / `ProgressErrorContent`: shared progress UI.
- `PreviewEmailDialog`: reusable preview email dialog.
- `SignatureEditorDialog`: reusable signature editor dialog.

## Known Gaps
- Android/iOS PDF generation and SMTP sending are not implemented.
- File watching is JVM-only.
- Email compatibility now depends on configuration tags instead of hardcoded spreadsheet columns.
- Some repository defaults still rely on localized default content rather than user-selectable locale persistence.
