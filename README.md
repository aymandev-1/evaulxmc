# EvaulxMC — Core Manager for your Minecraft Server

EvaulxMC is an all-in-one **core/essentials plugin** for Spigot/Paper servers and networks.
It bundles ranks & permissions, a full punishment system, staff tools, chat management,
cosmetic tags, nametags/tab-list, disguises/nicks, GUIs, Discord integration, and
multi-server synchronisation into a single jar with **no hard dependencies**.

- **Version:** 1.0.0
- **Platform:** Spigot / Paper **1.8.8** (API target `1.8.8-R0.1-SNAPSHOT`)
- **Java:** 8+
- **Storage:** MongoDB · MySQL (HikariCP) · Flat-file (JSON) — switchable in `config.yml`
- **Network sync:** Redis (optional, for multi-server / proxy setups)

---

## Table of contents

1. [Feature overview](#feature-overview)
2. [Requirements](#requirements)
3. [Installation (server owners)](#installation-server-owners)
4. [First-run configuration](#first-run-configuration)
5. [Storage backends](#storage-backends)
6. [Multi-server / network setup](#multi-server--network-setup)
7. [Integrations](#integrations)
8. [Reference documentation](#reference-documentation)
9. [Building from source (developers)](#building-from-source-developers)
10. [Project structure](#project-structure)
11. [Support](#support)

---

## Feature overview

### Ranks & permissions
- Weighted rank ladder with prefix, suffix, colour, name-colour, display name, category.
- Rank inheritance, per-rank permission nodes, default rank, staff flag, hidden ranks.
- Built-in permission engine (no Vault/LuckPerms required) with optional Vault & LuckPerms hooks.
- Temporary & permanent **grants** of extra ranks, with an optional approval queue and grant templates.
- Rank backups/rollback (`/rank backup`, `/rank rollback`), YAML import/export, and a cleanup tool
  for orphaned rank references.

### Punishments
- `ban`, `tempban`, `ipban`, `mute`, `tempmute`, `kick`, `warn`, `blacklist` and their reversals.
- Punishment history (`/checkpunishments`), silent punishments, evidence URLs, appeal status, staff/internal notes.
- Configurable ban/mute/kick/blacklist screens, warn thresholds with an automatic escalation action.
- Preset punishment ladders driven by `/punish` (first/second/third offence).

### Staff tools
- Staff mode (item kit, vanish, recovery snapshots, auto-enable on join).
- Vanish, freeze, staff chat, command spy, social spy, staff list, staff sessions/time tracking.
- Staff panel & staff dashboard GUIs, player profiles, mod logs, action logs (JSONL), staff notes.
- Reports & helpop queues with cooldowns and clickable alerts.
- Maintenance mode with bypass permission / allow-list.

### Chat
- Configurable chat format with tags, prefixes, colours, suffixes and PlaceholderAPI support.
- Anti-spam (cooldown + repeat blocking), caps filter, word filter, link blocking, mention highlighting.
- Local/ranged chat, global chat mute, clear chat.

### Cosmetics & display
- Cosmetic tag catalog with categories, rarities, per-tag permissions and a paginated GUI.
- Chat colour & name colour selectors.
- Nametags via scoreboard teams + tab-list formatting (`/nametag status`, `/nametag reload`).

### Disguises / nicks
- Dependency-free disguises using reflected 1.8 packets with a Bukkit hide/show fallback.
- Optional ProtocolLib hook for cleaner entity refreshes.
- Mojang skin fetching with caching (`plugins/EvaulxMC/skin-cache.json`), rank pools, random names,
  name blacklist/patterns, history, and a built-in `/disguise test` diagnostic.

### Essentials & utility
- Teleport suite (`tp`, `tphere`, `tpall`, `tppos`, `back`, `spawn`, `setspawn`).
- `fly`, `god`, `heal`, `feed`, `speed`, `gamemode`, `enchant`, `invsee`, private messages (`msg`/`reply`), `sudo`.
- Broadcasts, announcements, auto-tips, lobby protection (region-aware), build mode.
- Admin tooling: `/evaulxmc doctor`, `/evaulxmc setup [quick]`, `/evaulxmc export`,
  `/evaulxmc restore <file.zip> confirm`, `/permaudit cleanup confirm`, `/perm debug <player> <node>`.

> A complete command and permission list is in [`docs/COMMANDS.md`](docs/COMMANDS.md) and
> [`docs/PERMISSIONS.md`](docs/PERMISSIONS.md).

---

## Requirements

| Component | Requirement |
|-----------|-------------|
| Server software | Spigot or Paper **1.8.8** |
| Java | **8** or newer |
| Database | None required (flat-file default). MongoDB or MySQL optional. |
| Redis | Optional — only for multi-server sync |

**Optional soft-dependencies** (auto-detected, never required):
`PlaceholderAPI`, `Vault`, `LuckPerms`, `ProtocolLib`, `EvaulxMCHub`.

---

## Installation (server owners)

1. Stop your server.
2. Drop the **shaded** `EvaulxMC-1.0.0.jar` (from `target/`) into the `plugins/` folder.
   It already contains the MongoDB, Redis and Hikari drivers relocated under `dev.evaulx.libs.*`.
3. Start the server once to generate `plugins/EvaulxMC/config.yml` and data folders, then stop it.
4. Edit `plugins/EvaulxMC/config.yml` (see below).
5. Start the server again.

On first start the plugin prints its banner and reports the active database and any
configuration warnings (missing default rank, invalid Redis settings, orphaned grants).
Run `/evaulxmc doctor` at any time for a health check.

---

## First-run configuration

The defaults work out of the box with flat-file storage. The settings most servers change first:

```yaml
database:
  type: FLATFILE            # FLATFILE | MYSQL | MONGODB

server:
  name: "EvaulxMC"
  server-id: "hub"          # unique per server when using Redis
  network: false

chat:
  format: "{tag}{prefix}{namecolor}{player}{suffix}&7: {message}"

permissions:
  default-rank: "default"   # the rank new players receive
```

> ⚠️ Set a **default rank** before opening the server. If none is configured the console
> warns: *"No default rank is loaded."* Run `/rank presets confirm` to install the built-in
> 12-rank ladder (`Owner`, `Platform-Admin`, `Admin`, `Developer`, `Senior-Mod`, `Mod`,
> `Builder`, `Youtuber`, `Twitch`, `Evaulx`, `VIP`, `Default`), or build your own with
> `/createrank` and `/rankdefault`.

Full configuration reference: [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md).

---

## Storage backends

Set `database.type` in `config.yml`.

### Flat-file (default)
JSON files under `plugins/EvaulxMC/data/`. No setup required. Writes are atomic
(temp-file + rename) to prevent corruption on crash or concurrent save. Best for single servers.

### MySQL
```yaml
database:
  type: MYSQL
  mysql:
    host: localhost
    port: 3306
    database: evaulxmc
    username: root
    password: ""
    pool-size: 10
```
Tables (`evaulx_players`, `evaulx_punishments`, `evaulx_ranks`) and any missing columns
are created automatically. Recommended for networks sharing one database.

### MongoDB
```yaml
database:
  type: MONGODB
  mongodb:
    host: localhost
    port: 27017
    database: evaulxmc
    username: ""
    password: ""
    auth-database: admin
```

---

## Multi-server / network setup

For BungeeCord/Velocity networks, share **one** MySQL or MongoDB database across all servers
and enable Redis for live sync (staff chat, punishments, ranks, grants, vanish, disguises):

```yaml
redis:
  enabled: true
  ip: <redis-host>
  port: 6379

server:
  network: true
  server-id: "lobby-1"   # MUST be unique on every server
```

Each server ignores its own published messages and applies events from the others.

---

## Integrations

| Plugin | What it adds | Required? |
|--------|--------------|-----------|
| **PlaceholderAPI** | Exposes EvaulxMC placeholders for other plugins | No |
| **Vault** | Registers EvaulxMC ranks/permissions/prefix/suffix as a provider | No |
| **LuckPerms** | Optional one-time rank import on startup | No |
| **ProtocolLib** | Cleaner disguise/skin entity refreshes | No |
| **EvaulxMCHub** | Hands nametag/display refreshes to your hub plugin | No |

All integrations are detected at runtime; none of them are required to run.

---

## Reference documentation

| Document | Contents |
|----------|----------|
| [`docs/COMMANDS.md`](docs/COMMANDS.md) | Every command, alias and description |
| [`docs/PERMISSIONS.md`](docs/PERMISSIONS.md) | Every permission node and default |
| [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md) | Section-by-section `config.yml` guide |
| [`CHANGELOG.md`](CHANGELOG.md) | Version history |

---

## Building from source (developers)

### Requirements
- **Java 8** (JDK 1.8) — JDK 8+ works for compilation (target is 8).
- The bundled Maven wrapper (`mvnw` / `mvnw.cmd`) — no separate Maven install needed.
- Spigot 1.8.8 API in your local Maven repo (Spigot is not on Maven Central).

### Step 1 — install the Spigot 1.8.8 API
```bash
mkdir spigot && cd spigot
curl -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
java -jar BuildTools.jar --rev 1.8.8     # installs spigot-api into ~/.m2
```

### Step 2 — build
```bash
./mvnw clean package        # Linux/macOS
mvnw.cmd clean package      # Windows
```
The shaded, server-ready jar is written to `target/EvaulxMC-1.0.0.jar`.

### IntelliJ
Open the `EvaulxMC/` folder; IntelliJ imports the `pom.xml` automatically. Set the Project SDK
to Java 8. A **Build EvaulxMC** run configuration is included.

---

## Project structure

```
src/main/java/dev/evaulx/core/
├── EvaulxCore.java              Main plugin class (bootstrap, command/listener registration)
├── chat/                        ChatManager (formatting, filters, moderation)
├── commands/
│   ├── essential/               General commands (fly, gm, heal, msg, tp, tags, etc.)
│   ├── punishment/              Ban, mute, kick, warn, blacklist, presets
│   ├── rank/                    Rank & permission & grant management
│   └── staff/                   Vanish, staffmode, disguise, socialspy, panels, logs
├── database/
│   ├── flatfile/                JSON-based storage (atomic writes)
│   ├── mongo/                   MongoDB storage
│   ├── mysql/                   MySQL/HikariCP storage
│   └── redis/                   Redis pub/sub (multi-server)
├── discord/                     Discord webhook integration
├── disguise/                    Disguise manager (packets + skin cache)
├── gui/                         GUI manager and menus
├── hooks/                       PlaceholderAPI, Vault, ProtocolLib, EvaulxMCHub
├── listeners/                   Event listeners (join/quit, chat, login, staff, lobby)
├── managers/                    Player, Rank, Punishment, Grant, Note, Tips, Message, Config-migration
├── models/                      Rank, Punishment, PlayerProfile, Grant, PlayerNote
├── nametags/                    Scoreboard-team nametag system
├── network/                     RedisSyncManager (cross-server events)
├── staff/                       StaffRequestManager (sessions, freeze, reports, helpop)
└── utils/                       CC, TimeUtil, TaskUtil, PlayerUtil, ActionBarUtil, DisplayUtil
```

---

## Support

- Website: https://evaulx.dev
- For server owners: keep `config.yml` and the `data/` folder backed up before upgrades.
  Use `/evaulxmc export` for a full data snapshot and `/rank backup` before bulk rank edits.

© EvaulxMC. All rights reserved.
