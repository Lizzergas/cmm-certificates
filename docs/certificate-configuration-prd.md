# Certificate Configuration PRD

## Problem Statement

The certificate workflow is currently tied to a hardcoded XLSX column layout, a hardcoded set of DOCX placeholders, and a hardcoded Conversion screen form. Business changes to certificate fields or spreadsheet structure require code changes and a new app release.

## Solution

Introduce an app-managed, versioned `config.json` that is loaded automatically at startup. The app uses it to:

- define which manual certificate fields appear in the Conversion screen
- define which XLSX-backed fields map to spreadsheet headers
- define which manual field acts as the sequential document number tag

The JSON lifecycle is hidden from normal users. The app first tries to load `config.json`, then falls back to `default.config.json`, and finally to the in-app default configuration if both files fail.

## User Stories

1. As an operator, I want certificate fields to be driven by configuration, so that business changes do not require a new app build.
2. As an operator, I want the app to auto-load configuration without extra clicks, so that the workflow stays simple.
3. As an operator, I want the app to fall back to a safe built-in configuration, so that bad installed config files do not block work.
4. As an operator, I want labels to come from configuration, so that the app shows the wording I entered.
5. As an operator, I want blank labels to fall back to the raw tag, so that nothing is blocked by missing text.
6. As an operator, I want to choose XLSX headers in the configuration editor, so that I set up the mapping once and reuse it.
7. As an operator, I want selected XLSX and DOCX files to auto-refresh while the app is open, so that saving the file immediately updates validation and preview readiness.
8. As an operator, I want active manual fields to stay required by default, so that incomplete certificates are blocked.
9. As an operator, I want missing DOCX tags to disable the matching field and show the existing tooltip/supporting text, so that template mistakes are obvious.
10. As an operator, I want one configured manual field to act as the sequential document number, so that numbering and PDF filenames stay aligned.
11. As an operator, I want preview and generation to use the configured fields and tags, so that DOCX replacement matches the current business rules.
12. As an operator, I want the email workflow to keep working with generated PDFs, so that this overhaul does not break the existing send path.

## Implementation Decisions

- The configuration format is a standalone JSON file.
- Normal users do not manage JSON directly in the UI.
- The installed `config.json` lives alongside packaged resources such as `bazinis_šablonas.docx`.
- A bundled default configuration mirrors the current required certificate behavior.
- Built-in defaults are still provided through the default config, but UI labels now come only from config or raw tags.
- The initial XLSX-backed fields are `vardas` and `pavarde`.
- The config uses `documentNumberTag` instead of a separate `*_start` concept.
- DOCX tag mapping is implicit: field/tag ID `x` maps to `{{x}}`.
- The old fixed first-column timestamp sentinel is removed from certificate parsing.
- XLSX mappings are stored directly in config as `headerName`.
- JVM watches the currently selected XLSX and DOCX files and refreshes them automatically on save.
- Bulk email logic is not redesigned in this phase.

## Testing Decisions

- Tests should verify externally visible behavior through public APIs and use cases.
- Existing replacement, preview, validation, DOCX inspection, and screen tests are updated to the new config-driven model.
- New direct tests cover:
  - configuration validation rules
  - config source precedence and migration
  - config-driven XLSX mapping behavior
- The new suite focuses on behavior, not implementation details or internal state mutation order.

## Out of Scope

- In-app JSON editor
- Arbitrary scripting or expressions
- DOCX loops/conditionals/repeating sections
- Email workflow redesign
- Combining DOCX and config into one bundle format

## Further Notes

- V1 field types are `TEXT`, `NUMBER`, `DATE`, `SELECT`, and `MULTILINE`.
- If a manual field is missing in the DOCX, the field is disabled except for the configured document number field, which remains active because it is still used for filenames.
- If email sending is needed with the config-driven XLSX parser, the configuration must provide the required tags such as `email`.
