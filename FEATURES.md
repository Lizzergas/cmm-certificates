# Features and Technical Notes

## Current User Flow
1. Select an XLSX file and a DOCX template on the Conversion screen.
2. Fill certificate fields (IDs, hours, name, lecturer, etc.).
3. Click **Convert to PDF**.
4. Watch progress on the PDF Conversion Progress screen.
5. On success, optionally send a preview email or send emails to all recipients.

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

## Email Signature Editor
- The Settings screen provides an **Edit signature** dialog instead of a raw HTML text area.
- Supports **Builder** mode (font, size, bold/italic, line height, color, and line list).
- Supports **HTML** mode (paste/edit full HTML) with validation.
- Supports **Preview** mode (builder-based preview; custom HTML falls back to a message).
- The editor is **ephemeral**: changes are only saved when the user clicks **Save**.
- Stored value remains a single string: `email.signatureHtml`.

## Platform Support
- JVM: full XLSX parsing + DOCX templating + email sending.
- Android/iOS: XLSX parsing, DOCX templating, and SMTP are not implemented yet.

## UI Components
- `SelectFileIcon`: file selection cards (idle/selected states).
- `PrimaryActionButton`: main CTA with enabled/disabled styling.
- `ProgressIndicatorContent` / `ProgressErrorContent`: shared progress UI.
- `PreviewEmailDialog`: reusable preview email dialog.
- `SignatureEditorDialog`: reusable signature editor dialog.

## Known Gaps
- Android/iOS PDF generation and SMTP sending are not implemented.
- Limited placeholder set (see above).
