# Features and Technical Notes

## Current User Flow
- Choose a source XLSX file, a DOCX template, and an output folder.
- The app parses the XLSX into registration entries.
- Clicking "Convert to PDF" (currently) generates DOCX files in the output folder for each entry.

## Template Placeholders
- Templates are plain DOCX files containing placeholders like `{{full_name}}`.
- Each entry replaces `{{full_name}}` with `name + surname`.
- Output files are written as `entry_{index}.docx`.

## XLSX Parsing (JVM)
- Parses the first sheet of an XLSX file (no external XLSX library).
- Column mapping is positional (A-H) and does not rely on column names.
- Parsing stops at the first empty date cell or fully empty row.
- Timestamps support two formats:
  - String: `MM/dd/yyyy HH:mm:ss`
  - Excel serial: numeric date/time values (1900 system with leap-year bug adjustment)

## DOCX Templating (JVM)
- Uses Apache POI (XWPF) to replace text across paragraphs, tables, headers, and footers.
- Handles placeholders that are split across multiple runs while preserving styling.
- The template is loaded into memory once per batch to avoid re-reading the file.

## Platform Support
- JVM: full XLSX parsing and DOCX templating are enabled.
- Android/iOS: XLSX parsing and DOCX templating throw `UnsupportedOperationException` for now.

## UI Components
- `SelectionCard` renders file selection cards with idle/selected states and full-card ripple.
- `PrimaryActionButton` handles enabled/disabled styles for the main action.

## Known Gaps
- "Convert to PDF" currently outputs DOCX files only.
- Placeholder coverage is minimal (only `{{full_name}}`).
- No error banner or progress UI; logging is used for diagnostics.
