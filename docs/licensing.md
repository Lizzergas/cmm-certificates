# Licensing Strategy

This repository is set up for a **binary-only commercial license** model:

- you keep ownership of the source code and product IP
- customers receive usage rights only
- no IP transfer happens unless a separate signed assignment explicitly says so

## Files added to support this model

- `composeApp/packaging/resources/common/LICENSE.txt` - proprietary repository/source license
- `composeApp/packaging/resources/common/EULA.txt` - customer-facing binary-use terms
- `composeApp/packaging/resources/common/NOTICE.txt` - ownership and trademark notice
- `composeApp/packaging/resources/common/THIRD_PARTY_NOTICES.txt` - working third-party notice inventory

These files live directly in the packaged desktop resources directory so desktop
builds can expose them after installation.

## Contract position this repo assumes

The legal documents in the repo assume the following commercial position:

- the software is licensed, not sold
- ownership remains with the Licensor
- customer receives a limited, non-exclusive, non-transferable right to use the software
- no source code rights are granted unless separately agreed in writing
- no assignment or work-made-for-hire transfer applies unless separately signed

## Important business note

The repo files help reinforce ownership, but the customer contract still matters
most. Avoid customer contract language that says:

- the deliverables are assigned to the customer
- all IP created under the project belongs to the customer
- the work is work made for hire in full

If you need customer-specific exceptions, put them in a separate written
agreement and keep the assignment narrowly scoped.

## Third-party notices

`composeApp/packaging/resources/common/THIRD_PARTY_NOTICES.txt` is intentionally a working inventory, not a final
compliance bundle. Before external distribution:

1. generate the final dependency graph
2. verify bundled versions
3. collect required third-party license texts
4. update the notice file to match the shipping installer

## Installer behavior

The Windows installer uses `composeApp/packaging/resources/common/EULA.txt` as its installer license file, while the
installed application bundle also carries:

- `composeApp/packaging/resources/common/LICENSE.txt`
- `composeApp/packaging/resources/common/EULA.txt`
- `composeApp/packaging/resources/common/NOTICE.txt`
- `composeApp/packaging/resources/common/THIRD_PARTY_NOTICES.txt`

The Settings screen exposes those documents when running on JVM desktop builds.
