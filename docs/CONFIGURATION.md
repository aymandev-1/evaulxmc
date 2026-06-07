# EvaulxMC — Configuration Guide

This guide walks through every section of `plugins/EvaulxMC/config.yml`.
The file is created on first start and is automatically migrated forward by version
(`config-version`) when you upgrade, so your settings are preserved across updates.

All message strings support `&` colour codes.

---

## `database`
Selects and configures storage. `type` is `FLATFILE`, `MYSQL`, or `MONGODB`.

```yaml
database:
  type: FLATFILE
  mongodb: { host, port, database, username, password, auth-database }
  mysql:   { host, port, database, username, password, pool-size }
  flatfile:
    path: data        # stored inside plugins/EvaulxMC only; external paths are ignored
```
See [`README.md`](../README.md#storage-backends) for backend trade-offs.

---

## `redis`
Cross-server pub/sub for networks. Disabled by default.
```yaml
redis: { enabled: false, ip: localhost, port: 6379 }
```
When enabled, give each server a unique `server.server-id`.

---

## `hooks`
Toggles for optional integrations.
```yaml
hooks:
  vault: { enabled: true }
  luckperms: { import-on-startup: false }   # one-time rank import
```

---

## `server`
Identity and network role.
```yaml
server:
  name: "EvaulxMC"
  network: false
  server-id: "hub"      # unique per server when using Redis
  proxy: false
```

---

## `hub-hook`
Hands display/nametag refreshes to an `EvaulxMCHub` plugin if present. Leave defaults if
you do not run that plugin. Supports method-based or command-based refresh.

---

## `spawn`
Spawn location. Set it in-game with `/setspawn` rather than editing by hand.

---

## `chat`
Chat formatting and moderation.
```yaml
chat:
  format: "{tag}{prefix}{namecolor}{player}{suffix}&7: {message}"
  range: -1                 # -1 = global; a positive number = local radius in blocks
  filter-enabled: false
  filtered-words: [ ... ]
  anti-spam: { enabled, cooldown-seconds, block-repeat, cooldown-message, repeat-message }
  caps: { enabled, min-length, max-percent }
  links: { block, allowed-domains, block-message }
  mentions: { enabled, color }
```
Placeholders in `format`: `{tag}`, `{prefix}`, `{namecolor}`, `{player}`, `{suffix}`, `{message}`.

---

## `tags`
Cosmetic tag system used by `/tag`.
```yaml
tags:
  require-per-tag-permission: true
  show-locked: true
  custom-max-raw-length: 48
  custom-max-visible-length: 16
  gui: { title: "&8Tags {page}/{pages}" }
  catalog:
    <id>: { display, category, rarity, description, permission, ranks: [...], order }
  available: [ ... ]        # legacy public tags available to anyone with evaulx.tag
```
Each catalog entry can be gated by a `permission` (e.g. `evaulx.tag.vip`) and/or by `ranks`.

---

## `nametags`
Display name, scoreboard team prefix/suffix, and tab-list formatting.
```yaml
nametags:
  enabled: true
  format: "{tag}{prefix}{namecolor}{player}{suffix}"
  scoreboard: { enabled, prefix-format, suffix-format }
  tab-list: { enabled, format, max-visible-length }   # 1.8 tab names are limited to 16 chars
```

---

## `lobby-protection`
Region-aware lobby protection. Empty `worlds` protects **all** loaded worlds. Staff bypass with
`/buildmode` or `evaulx.protection.bypass`. Each `prevent-*` flag toggles a specific protection
(block break/place, interaction, pistons, fire, liquids, growth, explosions, mobs, damage,
projectiles, item drops/pickup, hunger, portals, weather). `void-rescue` teleports players who
fall below a Y level back to spawn.

---

## `grant-approvals`, `rank-backups`, `exports`, `grant-templates`
```yaml
grant-approvals: { enabled: false }     # optional approval queue for /grant
rank-backups:    { keep: 25 }           # 0 = keep all
exports:         { keep: 10 }           # /evaulxmc export retention; 0 = keep all
grant-templates:
  <id>: { display-name, material, rank, duration, reason, permission, order }
```

---

## `maintenance`
```yaml
maintenance:
  enabled: false
  reason: "Server maintenance"
  bypass-permission: "evaulx.maintenance.bypass"
  kick-online-players: true
  allowed-players: []
  kick-message: "..."
```

---

## `first-join`
Automation on a player's first join: assign a rank/tag, send a message, run console commands
(`{player}` is substituted).

---

## `disguise`
The largest section. Key options:
- `enabled`, `safe-mode`, `packet-refresh`, `self-refresh`, `require-real-skin`
- `persist-on-relog`, `auto-undisguise-on-quit`
- `skin-cache-ttl-minutes`, `max-skins`, timing delays (`skin-load-delay-ticks`, etc.)
- `requires-permission`, `prevent-duplicate-names`, `prevent-online-player-names`, `cooldown-seconds`
- `rank-pools` — per-rank allowed skins/names and which ranks may use the pool
- `restrictions` — pool-only enforcement and rank-permission gating
- `blacklist` — blocked names, reserved names, regex patterns
- `gui` — full GUI layout/material customisation
- `skins`, `random-names` — global pools

See `disguise.protocollib.enabled` to toggle the ProtocolLib refresh hook.

---

## `staff-tools`
Staff behaviour and the staff-mode item kit.
- `staff-permission`, report/helpop cooldowns, `clickable-alerts`, `staff-chat-format`
- `command-spy` — enable, hide staff commands, blocked-commands (login/register/password), format
- `staffmode-items` — the hotbar kit (teleport compass, random teleport, inspect, vanish, reports)
- `staffmode` — auto-vanish, recovery snapshots, auto-enable on join
- `staff-join-leave-alerts`, `staff-list.network`, `action-log` (JSONL), `grants.expiry-reminder-minutes`,
  `sessions.max-recent`

---

## `profile-persistence`
Controls which player toggles survive reconnects/restarts:
```yaml
profile-persistence:
  vanish: true
  god: true
  social-spy: true
  msg-toggle: true
  staff-mode: false
  disguise: false
```

---

## `gui`
Titles, slots and materials for every menu (staff panel, reports, helpop, punish, notes, ranks,
rank editor, lobby protection, dashboard, maintenance, sessions, pending grants, player profile,
grant duration/reason pickers). Fully re-themeable.

---

## `tips`, `announcements`, `discord`, `join-quit`
```yaml
tips: { enabled, interval (seconds), messages: [...] }
announcements: { broadcast-format, announcement-format }
discord:
  enabled: false
  username: "EvaulxMC"
  player-head:                 # shows the player's MC head as the embed thumbnail
    enabled: true
    url: "https://mc-heads.net/avatar/{id}/100"   # {id}=UUID if known else name; {uuid}, {name} also work
  webhooks: { punishments, rank-changes, helpop, reports, staff-chat, staff-actions, system, security }
join-quit: { enabled, join-message, quit-message, first-join-message }
```

---

## `punishments` & `punishment-presets`
```yaml
punishments:
  default-appeal-status: "not-submitted"
  ban-message / mute-message / kick-message / blacklist-message: "..."   # support {reason} {duration} {id} {punisher}
  warn-threshold: 5
  warn-threshold-action: "tempban {player} 1d Auto-banned for reaching warn limit"

punishment-presets:
  <id>: { display-name, material, lore: [...], first, second, third }   # offence ladder for /punish
```

---

## `permissions`
```yaml
permissions:
  default-rank: "default"
  op-bypass: true        # operators bypass permission checks
```
