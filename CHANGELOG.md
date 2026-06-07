# Changelog

All notable changes to EvaulxMC are documented here.

## Unreleased (branch `port/modern-1.21`)

### Platform
- Ported from Spigot 1.8.8 to **Paper 1.21** (Java 21). All legacy `Material`/`Enchantment` names
  modernized in code and `config.yml`; action bars rewritten to Adventure; `api-version: '1.21'`.
  See `docs/PORTING_1.21.md` for the full status (note: the disguise NMS layer still needs a 1.21
  rewrite and the whole branch needs live-server QA).

### Added — new features
- **AFK:** `/afk [reason]`, automatic AFK after `afk.auto-minutes` of inactivity, configurable
  enter/leave broadcasts, `evaulx.afk.exempt` to opt out of auto-AFK.

## 1.0.1 — 2026-06-07

### Added
- **Discord player heads:** every player-related webhook embed (punishments, rank changes, grants,
  reports, helpop, staff chat/actions, disguises, frozen logouts, maintenance) now shows the target
  player's Minecraft head as the embed thumbnail. Configurable via `discord.player-head.enabled` and
  `discord.player-head.url` (`{id}`/`{uuid}`/`{name}` placeholders; defaults to mc-heads.net).
- **Punishment IDs are now visible in-game:** the punishment broadcast includes `ID: <id>`, and the
  ban/tempban/blacklist/kick/mute screens shown to the punished player now substitute `{id}` (added to
  the default message templates). Grant IDs were already shown in-game and in Discord.

### Fixed
- **Punishment durations:** the `M` (months) duration unit was unreachable because input was
  lowercased before parsing, so `1M` was silently treated as `1m` (1 minute). Duration parsing now
  preserves case — lowercase `m` = minutes, uppercase `M` = months — and accepts upper/lower case
  for all other units (`s/h/d/w/y`).
- **Flat-file storage corruption:** profile / punishment / rank JSON files are now written
  atomically (temp file + atomic rename, with a safe fallback). This prevents half-written,
  corrupt files if the server crashes mid-write or two async saves race on the same file.
- **MongoDB profile/punishment loading:** reading documents missing `firstJoin`, `lastSeen`,
  `issued`, or `expires` (e.g. externally imported or legacy data) threw an unboxing
  `NullPointerException` and aborted the whole profile load. These reads are now null-safe.

### Changed
- **Login ban checks moved off the main thread.** Ban / IP-ban lookups now run in
  `AsyncPlayerPreLoginEvent` instead of the main-thread `PlayerLoginEvent`, so login database I/O no
  longer stalls the server tick (notably the flat-file IP-ban scan). Maintenance checks stay on the
  main thread because they need the player's permissions/op status.
- Removed a stray, empty `{commands` directory created by a shell that did not expand brace patterns.

### Documentation
- Rewrote `README.md` as a sale-ready overview and added reference docs under `docs/`:
  `COMMANDS.md`, `PERMISSIONS.md`, `CONFIGURATION.md`.

### Known recommendations (not yet changed)
- `RedisSyncManager` rank/grant handlers reload profiles synchronously on the main thread on
  cross-server events; splitting load (async) from apply (sync) would avoid a brief main-thread hit.
  Left as-is because the path is infrequent and the current code is correct.
- Some staff commands (`/checkpunishments`, `/modlogs`, `/lookup`, `/warn`) read from the database on
  the main thread. Fine for MySQL/MongoDB; on the flat-file backend these scan files, so a very large
  history could cause a brief hitch. Infrequent (staff-only), so not refactored.
- Action bars use legacy 1.8 NMS reflection and silently no-op on 1.17+; a modern build should use
  the Spigot `ChatMessageType.ACTION_BAR` API (or Adventure on Paper).

## 1.0.0

- Added the fixed EvaulxMC rank preset set: Owner, Platform-Admin, Admin, Developer, Senior-Mod, Mod, Builder, Youtuber, Twitch, Evaulx, VIP, Default.
- Added saved rank categories: Staff, Media, Store, Hidden, Default.
- Added rank cleanup tooling for deleted rank references in profiles and grants.
- Added `/evaulxmc doctor` checks for ranks, categories, grants, Redis, exports, and config references.
- Added `/evaulxmc export` for zipped plugin data exports.
- Simplified Redis config to `enabled`, `ip`, and `port`.
