# Certificate Configuration Guide

## Overview

The certificate flow is now driven by a user-editable configuration system instead of hardcoded XLSX columns and a fixed Conversion form.

The system is split into two main areas:

- `feature:certificateconfig`: config model, validation, persistence, editor UI, sample XLSX header inspection
- `feature:certificate`: conversion UI, XLSX parsing, DOCX inspection, PDF generation, runtime refresh/watch logic

The app lets users define:

- XLSX-backed tags
- manual input fields shown in the Conversion screen
- which manual tag is used as the document number

DOCX templates consume tags directly in `{{tag}}` format.

## Config Files

The packaged resources now contain:

- `default.config.json`: immutable fallback configuration
- `config.json`: editable active configuration file
- `bazinis_šablonas.docx`: packaged template

### Load precedence on app launch

On startup the app loads configuration in this order:

1. `config.json`
2. `default.config.json`
3. hardcoded in-app default

When `config.json` is loaded successfully, the app mirrors it into DataStore for runtime state.

Important:

- `config.json` is the startup source of truth.
- If the user edits `config.json` while the app is closed, the next launch picks it up.
- On JVM, installed/runtime resource locations are preferred over the workspace packaging directory.

### Save behavior

Saving configuration from the app does all of the following:

1. validates the draft
2. writes JSON to `config.json`
3. mirrors the same JSON into DataStore
4. refreshes the in-memory config state

## Current Config Schema

The current schema is tag-based and intentionally small.

```json
{
  "version": 1,
  "id": "default-certificate",
  "documentNumberTag": "dokumento_id",
  "xlsxFields": [
    {
      "tag": "vardas",
      "label": "Vardas",
      "headerName": "Vardas"
    },
    {
      "tag": "pavarde",
      "label": "Pavardė",
      "headerName": "Pavardė"
    }
  ],
  "manualFields": [
    {
      "tag": "dokumento_id",
      "label": "Pažymėjimo numeris",
      "type": "NUMBER"
    },
    {
      "tag": "akreditacijos_tipas",
      "label": "Renginio tipas",
      "type": "SELECT",
      "options": ["paskaitoje", "seminare"]
    }
  ]
}
```

### Field groups

`xlsxFields`

- define tags filled from XLSX rows
- each field stores the XLSX header name directly in config
- there is no runtime header mapping modal anymore

`manualFields`

- define fields rendered on the Conversion screen
- supported types:
  - `TEXT`
  - `NUMBER`
  - `DATE`
  - `SELECT`
  - `MULTILINE`

`documentNumberTag`

- must point to a `NUMBER` manual field
- used for sequential PDF file naming and `{{tag}}` replacement

### Removed features

The following earlier ideas are no longer part of the implementation:

- computed fields
- `optionsSource`
- runtime XLSX header-mapping modal in Conversion
- hardcoded label resolution from `strings.xml`

## Label Rules

Config labels are now plain user-entered strings.

The app does not localize config-defined labels.

Display behavior:

- if `label` is present and non-blank, it is shown
- if `label` is blank, the raw tag is shown

Validation messages also use:

- the config label when present
- otherwise the raw tag

## Config Editor Screen

Settings now includes a **Certificate configuration** button that opens the config editor.

The editor supports:

- adding/removing XLSX tags
- adding/removing manual fields
- editing tags, labels, types, default values, and select options
- selecting a sample XLSX file to inspect headers
- binding XLSX tags to headers once and saving the mapping into config
- selecting the document number tag
- resetting back to defaults

The editor writes to `config.json` on save.

## Conversion Screen

The Conversion screen is now config-driven.

### Manual field rendering

- manual fields are generated from `manualFields`
- field labels come only from config or fallback to the raw tag
- missing DOCX tags still disable manual fields and show the existing tooltip/supporting text behavior

### Inline config editing

Manual fields in the Conversion screen now include an Edit action.

From the Conversion screen the user can open a modal and edit:

- tag
- label
- type
- default value
- select options

This reuses the same manual field form logic as the full config screen.

### Select fields

- select fields still open their option menu directly from the Conversion screen
- the extra dropdown arrow icon was removed to avoid colliding with the Edit icon
- the field itself remains the anchor for fast selection

### Date fields

- date fields still use the date picker for values
- the Edit icon is aligned with the displayed date value instead of the label row

## DOCX Placeholder Rules

DOCX templates use placeholders like `{{tag}}`.

Rules:

- manual field tags can disable their own UI if the tag is absent from the selected DOCX
- `documentNumberTag` remains active even if missing from DOCX because it still drives output file naming
- XLSX tags are not rendered as UI controls, so they are validated through parsing and output behavior rather than field disablement

## XLSX Parsing Rules

The XLSX parser now follows config only.

It no longer uses hardcoded positional spreadsheet assumptions from columns A-H.

Current behavior:

- reads the first sheet
- resolves configured `headerName` values from `xlsxFields`
- fills `fieldValues[tag]`
- creates `RegistrationEntry` from config-driven field values only
- stops at the first fully empty row
- no longer uses the old timestamp/first-column sentinel

### Legacy field removal

Old hardcoded mapping for these spreadsheet positions was removed from `XlsxMapping.kt`:

- email column fallback
- name/surname positional fallback
- institution/event/payment/publicity positional mapping

`RegistrationEntry` is now populated from config-driven tags only.

Current compatibility note:

- `primaryEmail` is taken from the XLSX tag `email` when present
- `name` is taken from tag `vardas`
- `surname` is taken from tag `pavarde`

If users want email sending to work with the new config system, the configuration must include the needed tags.

## Runtime File Watching

Selected `XLSX` and `DOCX` files are now watched for changes on JVM.

### Watched files

- currently selected XLSX file
- currently selected DOCX template

### What happens on save/change

When the watched file changes:

- `XLSX`: the app re-inspects headers and reparses rows
- `DOCX`: the app re-inspects placeholders

The refresh is debounced to avoid multiple reloads during a single editor save.

### User feedback

When auto-refresh succeeds:

- a snackbar announces success

When auto-refresh fails:

- the file error is shown on the file card (`SelectFileIcon`)
- a snackbar announces failure

### Failure behavior

For watcher-triggered refreshes:

- last successful parsed/template state is preserved when possible
- file-level validation error still updates to show that the refresh failed

This avoids wiping out the whole screen during transient save failures.

### Platform scope

- JVM: active file watching via `WatchService`
- Android/iOS: no-op watcher implementation

## Validation Summary

Configuration validation checks:

- unique tags across XLSX and manual fields
- valid tag format
- non-empty XLSX `headerName`
- `documentNumberTag` points to a `NUMBER` manual field
- `SELECT` fields have at least one option

Conversion validation checks:

- XLSX selected
- DOCX selected
- XLSX headers exist for configured XLSX tags
- parsed entries exist
- enabled manual fields are filled
- date values use `YYYY-MM-DD`

## Migration Notes

Older installed config formats are migrated when possible.

Current migration behavior:

- old `id`-based field definitions are converted to the current `tag`-based schema
- migration result is validated before use
- migration context is surfaced through the config repository state

## Module Layout

- `feature:certificateconfig`
  - config schema
  - config validation
  - config persistence
  - sample XLSX header inspection
  - config editor UI
- `feature:certificate`
  - conversion form
  - XLSX row mapping
  - DOCX inspection and generation
  - runtime auto-refresh watchers
- `feature:settings`
  - settings shell and navigation entry into the config editor

## Verification Commands

Useful verification commands for this feature set:

- `./gradlew :feature:certificateconfig:jvmTest`
- `./gradlew :feature:certificate:jvmTest`
- `./gradlew :composeApp:jvmTest`
- `./gradlew :composeApp:test`
