# EvaulxMC Operations

## Rank Presets

Use `/rank presets confirm` to remove all current ranks and install only the approved EvaulxMC rank set.

Approved categories:

- Staff
- Media
- Store
- Hidden
- Default

After changing or resetting ranks, run `/rank cleanup confirm` to remove deleted rank names from stored profiles and grant records.

## Health Checks

Use `/evaulxmc doctor` after config changes. It checks database state, Redis, ProtocolLib, rank categories, missing preset ranks, grant references, tag rank references, disguise rank pools, materials, staff tools, and export folder access.

Use `/permaudit cleanup confirm` after manual rank edits to remove broken inheritance links and duplicate rank permission entries.

## Exports

Use `/evaulxmc export` before larger changes. Exports are saved under `plugins/EvaulxMC/exports` and include config files, custom files, and flatfile data when `database.type` is `FLATFILE`.

For MySQL or MongoDB, back up the external database separately.
