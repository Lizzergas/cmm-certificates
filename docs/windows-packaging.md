# Windows Packaging

## Primary release path

The primary Windows release path is **Compose Desktop + jpackage + WiX**.

- Gradle packaging config lives in `composeApp/build.gradle.kts`.
- GitHub Actions release automation lives in `.github/workflows/windows-msi-on-tag.yml`.
- Release builds are triggered from git tags that match `vMAJOR.MINOR.BUILD`.

This path currently produces:

- `.msi`
- `.exe`

## Why this is the primary path

This setup is the most direct and maintainable fit for the current project:

- already integrated into the repository workflow
- uses the Compose Desktop packaging DSL the app already depends on
- produces Windows-native installers suitable for business users
- supports icons, shortcuts, menu entries, upgrade UUIDs, EULA, and signing

## Configurable packaging knobs

Defaults are stored in `gradle.properties` so installer branding can be changed
without rewriting the Gradle logic:

- `appDisplayName`
- `appDescription`
- `appVendor`
- `appLegalOwner`
- `windowsInstallationPath`
- `windowsPerUserInstall`

The Compose Desktop packaging block then applies those values to:

- package name and version
- installer description/vendor/copyright
- EULA file
- Windows install directory chooser
- per-user vs machine-wide install mode
- Start menu group
- desktop shortcut
- Windows icon

## GitHub Actions alignment

The Windows workflow now does the following on tag builds:

1. Derives app version from the git tag.
2. Runs JVM and Android host verification tests.
3. Builds release installers for the current Windows runner.
4. Optionally signs the produced installers if signing secrets are configured.
5. Uploads both MSI and EXE artifacts.

Optional signing secrets:

- `WINDOWS_SIGNING_CERT_BASE64`
- `WINDOWS_SIGNING_CERT_PASSWORD`
- `WINDOWS_SIGNING_TIMESTAMP_URL`

If those secrets are absent, the workflow still builds unsigned installers.

## Recommended Windows release checklist

Before a customer-facing release:

1. Verify `gradle.properties` branding values.
2. Replace the icon if the brand changes.
3. Review the legal files in `composeApp/packaging/resources/common/`.
4. Configure code-signing secrets in GitHub.
5. Create and push a release tag.
