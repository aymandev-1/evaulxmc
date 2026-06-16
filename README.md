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
9. [Complete command reference](#complete-command-reference)
10. [Building from source (developers)](#building-from-source-developers)
11. [Project structure](#project-structure)
12. [Support](#support)

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

### Rank perks & power tools
Purpose-built commands for every rank tier, each gated behind its own permission so you can
attach them to store ranks and staff ranks exactly how you like:

| Tier | Commands |
|------|----------|
| **Staff / Mod** | `/modpanel`, `/clearlag`, `/slowchat`, `/freeze`, `/vanish`, `/staffmode`, `/socialspy`, `/commandspy` |
| **Admin** | `/adminpanel`, `/entitycount`, `/killentities`, `/chunkinfo`, `/maintenance`, `/lockdown` |
| **Builder** | `/builderpanel`, `/buildmode`, `/top`, `/up`, `/builderannounce`, `/head`, `/globalfly` |
| **Developer** | `/developerpanel`, `/devmode`, `/gc`, `/threads`, `/plugininfo`, `/testeffect`, `/devbroadcast`, `/reloadplugin` |
| **Owner** | `/ownerpanel`, `/owneralert`, `/ownerbc`, `/serverfreeze`, `/shutdown`, `/forcechat` |
| **Content Creator** | `/creatorpanel`, `/golive`, `/offair`, `/spotlight`, `/recording`, `/shoutout`, `/cchat`, `/ccgiveaway`, `/milestone`, `/socials`, `/watchparty` |
| **Store ranks** | `/firework`, `/launch`, `/nightvision`, `/hideall`, `/particles`, `/hat`, `/glow`, `/nick` |

### In-game GUIs
EvaulxMC ships **28 menus** built on a single, version-safe GUI engine (auto-resolves
1.8 ↔ modern materials, so the same menus render on 1.8.8 through the latest builds):

| GUI | Opened by | Purpose |
|-----|-----------|---------|
| **Staff Panel** | `/staffpanel` | Central hub: reports, helpop, punish, freeze, inspect, notes, mod logs, grants, vanish, staff mode |
| **Staff Dashboard** | `/staffdashboard` | Live overview with queue counts and a recent-actions feed |
| **Admin Panel** ⭐ | `/adminpanel` | Rank manager, grants, maintenance, lobby protection, server diagnostics, broadcast, clear-lag, lockdown |
| **Owner Panel** ⭐ | `/ownerpanel` | Owner alerts/broadcasts, server freeze, scheduled shutdown, reload, economy, kick-all, lockdown |
| **Mod Panel** ⭐ | `/modpanel` | Reports, punish, freeze, inspect, notes, mod logs, vanish, staff mode, whois, IP check, chat moderation |
| **Builder Panel** ⭐ | `/builderpanel` | Gamemode, fly, build mode, time/weather, speed, workbench, builder announce |
| **Developer Panel** ⭐ | `/developerpanel` | TPS, memory/GC, threads, plugin info, entity/chunk diagnostics, dev mode, test effects, reload |
| **Creator Panel** ⭐ | `/creatorpanel` | Go live, shoutout, giveaway, milestone, socials, recording, watch party, spotlight, profile |
| **Maintenance** | `/maintenance` | Toggle maintenance, manage the allow-list |
| **Staff Sessions** | `/staffsessions` | Active staff sessions and time tracking |
| **Pending Grants** | `/grant` queue | Approve / deny queued rank grants |
| **Player Profile** | `/profile <player>` | Full player dossier: rank, punishments, notes, alts, flags |
| **Punishment History** | profile → history | Paginated punishment record |
| **Reports / HelpOP** | `/reports` | Claim and resolve report & help queues |
| **Punish menu** | `/punish <player>` | Apply punishment presets in two clicks |
| **Grant pickers** | `/grant <player>` | Rank → duration → reason selection flow |
| **Tags menu** | `/tag` | Paginated cosmetic tag catalog |
| **Notes** | `/note <player>` | View and add staff notes |
| **Ranks browser + editor** | `/rank` | Browse ranks and edit prefix/suffix/colour/weight/permissions |
| **Streamer mode** | `/streamermode` | Toggle name-hiding and alias options |
| **Lobby protection** | `/lobbyprotect` | Toggle each protection rule per world |
| **Appeals** | `/appeal` | Review and action ban/mute appeals |

> ⭐ = new. The Admin & Owner panels are permission-gated (`evaulx.admin.panel`, `evaulx.owner.panel`);
> the Owner panel is also reachable from a button inside the Admin panel.

> A complete command and permission list is in [`docs/COMMANDS.md`](docs/COMMANDS.md) and
> [`docs/PERMISSIONS.md`](docs/PERMISSIONS.md) — and inline in the
> [Complete command reference](#complete-command-reference) below.

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
| [`docs/staff/`](docs/staff/README.md) | **Staff handbook** — onboarding + per-role feature guides |
| [`CHANGELOG.md`](CHANGELOG.md) | Version history |

### Staff handbook

Onboarding and role-specific feature guides for your team, in [`docs/staff/`](docs/staff/README.md):

| Guide | Audience |
|-------|----------|
| [Staff Intro](docs/staff/STAFF_INTRO.md) | Everyone — shared baseline, start here |
| [Moderator](docs/staff/MODERATOR.md) | Punishments, investigations, chat control |
| [Admin](docs/staff/ADMIN.md) | Ranks, grants, server controls, oversight |
| [Builder](docs/staff/BUILDER.md) | Build & world tools |
| [Developer](docs/staff/DEVELOPER.md) | Diagnostics & debugging |
| [Content Creator](docs/staff/CONTENT_CREATOR.md) | Streaming, giveaways, shoutouts |
| [Changelog](docs/staff/CHANGELOG.md) | Short, player-facing feature list |

---

## Complete command reference

All **205** commands, grouped by system (the same order they register in). Every command is
permission-gated — see [`docs/PERMISSIONS.md`](docs/PERMISSIONS.md) for the matching nodes and defaults.

<details>
<summary><b>Click to expand the full command list</b></summary>

| Command | Aliases | Description |
|---------|---------|-------------|
| `/evaulxmc` | emc, evaulx | Main EvaulxMC command |
| `/ban` | — | Ban a player |
| `/tempban` | — | Temporarily ban a player |
| `/ipban` | — | IP Ban a player |
| `/unban` | — | Unban a player |
| `/mute` | — | Mute a player |
| `/tempmute` | — | Temporarily mute a player |
| `/unmute` | — | Unmute a player |
| `/kick` | — | Kick a player |
| `/warn` | — | Warn a player |
| `/unwarn` | — | Remove a warning from a player |
| `/blacklist` | — | Blacklist a player |
| `/unblacklist` | — | Unblacklist a player |
| `/checkpunishments` | cp, history | Check punishments of a player |
| `/punish` | — | Open or run punishment presets |
| `/alts` | — | Find alternate accounts linked by IP |
| `/evidence` | setevidence | Attach or clear evidence on a punishment |
| `/rank` | ranks | Manage ranks |
| `/listranks` | ranklist | List ranks |
| `/rankladder` | rankhierarchy | View grouped rank hierarchy |
| `/createrank` | rankcreate | Create a rank |
| `/deleterank` | delrank, rankdelete | Delete a rank |
| `/rankdisplay` | — | Set a rank display name |
| `/rankpermission` | — | Set a rank permission node |
| `/rankprefix` | — | Set a rank prefix |
| `/ranksuffix` | — | Set a rank suffix |
| `/rankcolor` | — | Set a rank color |
| `/ranknamecolor` | — | Set a rank name color |
| `/rankweight` | — | Set a rank weight |
| `/rankdefault` | — | Set the default rank |
| `/rankstaff` | — | Toggle rank staff status |
| `/rankperm` | rankperms | Manage rank permissions |
| `/rankinherit` | rankinherits, rankinheritance | Manage rank inheritance |
| `/rankclone` | copyrank | Clone a rank |
| `/rankreload` | — | Reload ranks |
| `/playerrank` | — | View a player's rank information |
| `/setrank` | — | Set a player's rank |
| `/addrank` | — | Add a rank to a player |
| `/removerank` | — | Remove a rank from a player |
| `/rankinfo` | — | View rank info |
| `/perm` | — | Manage permissions |
| `/grant` | — | Open the grant GUI or grant a temporary/permanent extra rank |
| `/grants` | — | View rank grants for a player |
| `/removegrant` | — | Remove an active rank grant |
| `/disguise` | dis | Disguise as another player |
| `/skin` | nickskin, changeskin | Change your disguise skin |
| `/undisguise` | undis, unnick | Remove your disguise |
| `/disguiseinfo` | — | View disguise info |
| `/realname` | rn | Resolve a disguised player's real name |
| `/nicklist` | disguiselist | List online disguised players |
| `/nickhistory` | disguisehistory | View disguise history |
| `/forcedisguise` | fnick, forcenick | Force-disguise another player |
| `/disguisecooldown` | nickcooldown, dcooldown | Check or reset a player's disguise cooldown |
| `/nickcolor` | disguisecolor, nickcolour | Change your disguise name color while disguised |
| `/gamemode` | gm | Change gamemode |
| `/fly` | — | Toggle fly mode |
| `/vanish` | v | Toggle vanish |
| `/staffmode` | sm, staff | Toggle staff mode |
| `/staffstatus` | sstatus | View your staff status |
| `/staffchat` | sc, staffc | Send or toggle staff chat |
| `/commandspy` | cmdspy, cspy | Toggle command spy |
| `/staffrecover` | smrecover, staffrestore | Recover staff mode inventory and state |
| `/freeze` | — | Freeze or unfreeze a player |
| `/unfreeze` | thaw | Unfreeze a player |
| `/stafflist` | staffonline, slist | Show online staff |
| `/staffpanel` | staffgui, spanel | Open the staff panel |
| `/staffdashboard` | staffdash, sdashboard | Open the staff dashboard |
| `/staffsessions` | stafftime, sessions | View staff session tracking |
| `/maintenance` | maint | Manage maintenance mode |
| `/permaudit` | permissionaudit, auditperms | Audit rank and player permissions |
| `/lookup` | playerlookup | Lookup cached player profile information |
| `/profile` | playerprofile, pprofile | Open a staff player profile |
| `/modlogs` | mlogs, modlog | View moderation logs for a player |
| `/stafflogs` | slogs, actionlogs | Search recent staff action logs |
| `/note` | notes | Manage staff notes |
| `/spawn` | — | Teleport to spawn |
| `/setspawn` | — | Set the server spawn |
| `/back` | — | Return to your previous teleport location |
| `/tppos` | — | Teleport to coordinates |
| `/list` | online, who | List online players by rank |
| `/sudo` | — | Force a player to run a command or chat |
| `/announce` | announcement, announcements | Send or manage announcements |
| `/enchant` | — | Enchant your held item without level limits |
| `/teleport` | tp | Teleport to a player |
| `/teleporthere` | tphere | Teleport a player to you |
| `/teleportall` | tpall | Teleport all players to you |
| `/feed` | — | Feed a player |
| `/heal` | — | Heal a player |
| `/god` | — | Toggle god mode |
| `/speed` | — | Set speed |
| `/invsee` | — | View a player's inventory |
| `/broadcast` | bc | Broadcast a message |
| `/alert` | — | Alert the server |
| `/helpop` | — | Send a message to staff |
| `/report` | — | Report a player |
| `/reports` | staffreports | View recent player reports |
| `/msg` | pm, tell, whisper | Send a private message |
| `/reply` | r | Reply to a private message |
| `/socialspy` | ss | Toggle social spy |
| `/mutechat` | — | Mute the global chat |
| `/clearchat` | cc | Clear the chat |
| `/nametag` | — | Refresh EvaulxMC display names |
| `/chatcolor` | chatcolour | Set your chat color |
| `/namecolor` | namecolour | Set your name color |
| `/tag` | tags, taglist | List, preview, set, clear, or randomize your chat tag |
| `/buildmode` | build, bm | Toggle lobby build bypass mode |
| `/lobbyprotect` | lobbyprotection, protectlobby, lp | Manage and enforce lobby protection |
| `/afk` | away | Toggle your AFK status |
| `/msgtoggle` | msgt, togglemsg | Toggle whether you receive private messages |
| `/ignore` | — | Toggle ignoring private messages from a player |
| `/hat` | — | Wear your held item as a helmet |
| `/kickall` | — | Kick all non-exempt players from the server |
| `/time` | — | Set world time |
| `/weather` | sky | Set world weather |
| `/nick` | nickname | Set or remove your nickname |
| `/seen` | — | Check when a player was last online |
| `/repair` | — | Repair held item or all items in inventory |
| `/more` | — | Fill your held item stack to maximum |
| `/clearinv` | clear, ci | Clear a player's inventory |
| `/appeal` | — | Submit or manage ban/mute appeals |
| `/creator` | contentcreator, creators | Manage content creator profiles and status |
| `/redeemcode` | redeem | Redeem a content creator subscriber code for rewards |
| `/shoutout` | so | Content creator shoutout to a fan |
| `/cchat` | ccchat | Send a message in the content creator private channel |
| `/golive` | live, startstream | Announce you are going live on your stream |
| `/offair` | endstream, stoplive | Announce you have ended your stream |
| `/ccgiveaway` | ccga, creatorga | Run a random giveaway for online players as a content creator |
| `/milestone` | — | Broadcast a content creator milestone to the server |
| `/socials` | — | Broadcast your social links and stream info to the server |
| `/lockdown` | — | Toggle server lockdown to prevent new players joining |
| `/tempfly` | tfly | Grant a player temporary fly for a specified duration |
| `/craft` | workbench, wb | Open a workbench anywhere |
| `/ec` | enderchest | Open your ender chest or view another player's ender chest |
| `/globalfly` | gfly | Toggle fly mode for all online players |
| `/ownerbc` | ownerbroadcast, obc | Send a special owner-styled broadcast with sound |
| `/servermsg` | joinmsg, servermessage | Set or clear a persistent message shown to players on join |
| `/resetrank` | rankreset | Remove all active grants and reset a player to their default rank |
| `/streamermode` | streammode | Toggle streamer mode to hide your real name in chat and tab |
| `/ping` | — | Check a player's connection ping |
| `/ipcheck` | checkip | Check a player's stored IP address and online alts |
| `/playtime` | — | View how long a player has been on the server |
| `/whois` | playerinfo, pinfo | View detailed player info, rank, punishments, and flags |
| `/owneralert` | oalert | Send a dramatic owner alert to all players |
| `/serverfreeze` | sfreeze, freezeall | Freeze or unfreeze all non-staff players server-wide |
| `/forcechat` | fc | Force a player to say something in chat |
| `/devmode` | dev | Toggle developer creative mode with enhanced tools |
| `/reloadplugin` | plreload, evaulxreload | Reload the EvaulxMC plugin configuration |
| `/testeffect` | teffect | Apply a potion effect to yourself for testing |
| `/devbroadcast` | devbc | Broadcast a developer announcement to all players |
| `/builderannounce` | bannounce, ba | Send a builder announcement to all players |
| `/nightvision` | nv, nightvis | Toggle night vision on yourself or another player |
| `/hideall` | hideplayers | Hide or show all other players from your view |
| `/head` | playerhead, skull | Get a player head item |
| `/ccannounce` | creatorannounce | Send a content creator style announcement to all players |
| `/watchparty` | wp | Start or end a watch party event for your stream |
| `/particles` | particle, pe | Toggle a particle effect that follows you |
| `/home` | h | Teleport to one of your saved homes |
| `/sethome` | homeset | Set a home at your current location |
| `/delhome` | deletehome, removehome | Delete a saved home |
| `/homes` | — | List all your saved homes |
| `/warp` | — | Teleport to a server warp |
| `/setwarp` | createwarp | Create or update a server warp |
| `/delwarp` | deletewarp, removewarp | Delete a server warp |
| `/warps` | — | List all available server warps |
| `/tpa` | — | Request to teleport to a player |
| `/tpahere` | — | Request a player to teleport to you |
| `/tpaccept` | tpyes | Accept a pending teleport request |
| `/tpdeny` | tpno | Deny a pending teleport request |
| `/rename` | itemname | Rename your held item |
| `/lore` | itemlore | Edit the lore lines of your held item |
| `/glow` | — | Toggle an enchantment glow effect on your held item |
| `/tps` | — | View server TPS, RAM usage and uptime |
| `/serverinfo` | sinfo, si | View detailed server information |
| `/coins` | balance, bal | View your coin balance or manage another player's coins |
| `/pay` | — | Send coins to another player |
| `/daily` | dailyreward | Claim your daily coin reward |
| `/friend` | friends, f | Manage your friends list |
| `/mail` | — | Send and read offline player mail |
| `/ext` | extinguish | Extinguish a player who is on fire |
| `/near` | nearby | Show nearby players within a radius |
| `/smite` | lightning | Strike a player with lightning |
| `/coords` | pos, position, loc | Display your current coordinates and facing direction |
| `/party` | p | Create and manage a party with other players |
| `/fm` | friendmsg, fmsg | Send a quick message to a friend |
| `/clearlag` | lagclear, cl | Remove ground items and stray entities to reduce lag |
| `/slowchat` | chatslow | Enable or disable a global chat slow mode |
| `/entitycount` | entities, ecount | Show per-world entity counts for lag diagnostics |
| `/killentities` | butcher, removeentities | Remove entities by category across all worlds |
| `/chunkinfo` | chunk | Show diagnostics for the chunk you are standing in |
| `/top` | totop | Teleport to the highest block above you |
| `/up` | ascend | Teleport straight up, placing a platform beneath you |
| `/gc` | garbagecollect | Run garbage collection and report memory usage |
| `/threads` | threadinfo | Show JVM thread statistics |
| `/plugininfo` | pl, plinfo | List plugins or show details for one |
| `/shutdown` | stopserver | Schedule a graceful server shutdown with countdown |
| `/spotlight` | shine | Shine a server-wide spotlight on a player |
| `/recording` | rec | Toggle your content-creator recording status |
| `/firework` | fw | Launch a colourful firework (store perk) |
| `/launch` | rocket | Launch yourself into the air (store perk) |
| `/adminpanel` | apanel, admingui | Open the admin control-panel GUI |
| `/ownerpanel` | opanel, ownergui | Open the owner control-panel GUI |
| `/creatorpanel` | ccpanel, creatorgui | Open the content creator control-panel GUI |
| `/builderpanel` | buildpanel, buildergui | Open the builder control-panel GUI |
| `/developerpanel` | devpanel, devgui | Open the developer control-panel GUI |
| `/modpanel` | modgui, modtools | Open the moderator control-panel GUI |

</details>

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
