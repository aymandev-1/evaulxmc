# EvaulxMC — Command Reference

All commands and aliases as registered in `plugin.yml`. Permission nodes are listed in
[`PERMISSIONS.md`](PERMISSIONS.md). Most management commands also offer in-game tab-completion.

> Notation: `<required>` `[optional]`. Aliases are shown in parentheses.

---

## Core

| Command | Aliases | Description |
|---------|---------|-------------|
| `/evaulxmc` | `emc`, `evaulx` | Main command. Sub-commands include `doctor` (health check), `setup [quick]`, `export`, `restore <file.zip> confirm`, and `reload`. |

---

## Punishments

| Command | Description |
|---------|-------------|
| `/ban <player> <reason>` | Permanently ban a player |
| `/tempban <player> <duration> <reason>` | Temporarily ban (e.g. `7d`, `1M`, `12h`) |
| `/ipban <player> <reason>` | IP-ban a player |
| `/unban <player>` | Remove a ban |
| `/mute <player> <reason>` | Permanently mute |
| `/tempmute <player> <duration> <reason>` | Temporarily mute |
| `/unmute <player>` | Remove a mute |
| `/kick <player> <reason>` | Kick a player |
| `/warn <player> <reason>` | Warn a player (threshold auto-escalates) |
| `/unwarn <player>` | Remove a warning |
| `/blacklist <player> <reason>` | Blacklist (permanent, non-appealable) |
| `/unblacklist <player>` | Remove a blacklist |
| `/checkpunishments <player>` (`cp`, `history`) | View punishment history & edit metadata |
| `/punish [player]` | Open/run punishment presets |

**Duration units:** `s` seconds, `m` minutes, `h` hours, `d` days, `w` weeks, `M` months, `y` years.
Use `perm`/`permanent`/`-1` for permanent. (Note: lowercase `m` = minutes, uppercase `M` = months.)

**Punishment metadata** (via `/checkpunishments`):
```text
/checkpunishments meta <player> <id> evidence <url|none>
/checkpunishments meta <player> <id> appeal <status>
/checkpunishments meta <player> <id> staffnote <note|none>
/checkpunishments meta <player> <id> internalnote <note|none>
```

---

## Ranks, permissions & grants

| Command | Aliases | Description |
|---------|---------|-------------|
| `/rank` | `ranks` | Manage ranks (parent command) |
| `/listranks` | `ranklist` | List ranks |
| `/rankladder` | `rankhierarchy` | View grouped rank hierarchy |
| `/createrank` | `rankcreate` | Create a rank |
| `/deleterank` | `delrank`, `rankdelete` | Delete a rank |
| `/rankdisplay` | | Set a rank display name |
| `/rankpermission` | | Set a rank permission node |
| `/rankprefix` | | Set a rank prefix |
| `/ranksuffix` | | Set a rank suffix |
| `/rankcolor` | | Set a rank colour |
| `/ranknamecolor` | | Set a rank name colour |
| `/rankweight` | | Set a rank weight |
| `/rankdefault` | | Set the default rank |
| `/rankstaff` | | Toggle rank staff status |
| `/rankperm` | `rankperms` | Manage rank permissions (`add/remove/list/clear`) |
| `/rankinherit` | `rankinherits`, `rankinheritance` | Manage rank inheritance |
| `/rankclone` | `copyrank` | Clone a rank |
| `/rankreload` | | Reload ranks |
| `/playerrank` | | View a player's rank information |
| `/setrank <player> <rank>` | | Set a player's primary rank |
| `/addrank <player> <rank>` | | Add an extra rank to a player |
| `/removerank <player> <rank>` | | Remove an extra rank |
| `/rankinfo <rank>` | | View rank info |
| `/perm` | | Manage permissions (`debug <player> <node>` available) |
| `/grant [player]` | | Open the grant GUI or grant a temporary/permanent extra rank |
| `/grants <player>` | | View rank grants for a player |
| `/removegrant <player>` | | Remove an active rank grant |

Additional `/rank` sub-commands include `presets confirm`, `category`, `backup`, `rollback <file> confirm`,
`export [file.yml]`, and `cleanup confirm`. Rank imports are read from `plugins/EvaulxMC/imports/`.

---

## Disguises / nicks

| Command | Aliases | Description |
|---------|---------|-------------|
| `/disguise <name>` | `dis`, `nick` | Disguise as another player |
| `/skin <skin>` | `nickskin`, `changeskin` | Change your disguise skin |
| `/undisguise` | `undis`, `unnick` | Remove your disguise |
| `/disguiseinfo` | | View disguise info |
| `/realname <player>` | `rn` | Resolve a disguised player's real name |
| `/nicklist` | `disguiselist` | List online disguised players |
| `/nickhistory` | `disguisehistory` | View disguise history |

Diagnostics: `/disguise test <skin> [rank]`, `/disguise status`, `/disguise refresh <player>`.

---

## Staff tools

| Command | Aliases | Description |
|---------|---------|-------------|
| `/staffmode` | `sm`, `staff` | Toggle staff mode |
| `/staffstatus` | `sstatus` | View your staff status |
| `/staffchat` | `sc`, `staffc` | Send or toggle staff chat |
| `/vanish` | `v` | Toggle vanish |
| `/commandspy` | `cmdspy`, `cspy` | Toggle command spy |
| `/socialspy` | `ss` | Toggle social spy |
| `/staffrecover` | `smrecover`, `staffrestore` | Recover staff-mode inventory/state |
| `/freeze <player>` | | Freeze a player |
| `/unfreeze <player>` | `thaw` | Unfreeze a player |
| `/stafflist` | `staffonline`, `slist` | Show online staff |
| `/staffpanel` | `staffgui`, `spanel` | Open the staff panel GUI |
| `/staffdashboard` | `staffdash`, `sdashboard` | Open the staff dashboard |
| `/staffsessions` | `stafftime`, `sessions` | View staff session tracking |
| `/maintenance` | `maint` | Manage maintenance mode |
| `/permaudit` | `permissionaudit`, `auditperms` | Audit rank/player permissions (`cleanup confirm`) |
| `/lookup <player>` | `playerlookup` | Lookup cached player profile info |
| `/profile <player>` | `playerprofile`, `pprofile` | Open a staff player profile |
| `/modlogs <player>` | `mlogs`, `modlog` | View moderation logs for a player |
| `/stafflogs` | `slogs`, `actionlogs` | Search recent staff action logs |
| `/note <player>` | `notes` | Manage staff notes |
| `/report <player>` | | Report a player |
| `/reports` | `staffreports` | View recent player reports |
| `/helpop <message>` | | Send a message to staff |

---

## Essentials & utility

| Command | Aliases | Description |
|---------|---------|-------------|
| `/gamemode <mode>` | `gm` | Change gamemode |
| `/fly [player]` | | Toggle flight |
| `/god` | | Toggle god mode |
| `/heal [player]` | | Heal a player |
| `/feed [player]` | | Feed a player |
| `/speed <amount>` | | Set fly/walk speed |
| `/enchant <ench> [level]` | | Enchant held item without level limits |
| `/invsee <player>` | | View a player's inventory |
| `/spawn` | | Teleport to spawn |
| `/setspawn` | | Set the server spawn |
| `/back` | | Return to your previous teleport location |
| `/teleport <player>` | `tp` | Teleport to a player |
| `/teleporthere <player>` | `tphere` | Teleport a player to you |
| `/teleportall` | `tpall` | Teleport all players to you |
| `/tppos <x> <y> <z>` | | Teleport to coordinates |
| `/list` | `online`, `who` | List online players by rank |
| `/sudo <player> <command>` | | Force a player to run a command or chat |
| `/msg <player> <message>` | `pm`, `tell`, `whisper` | Private message |
| `/reply <message>` | `r` | Reply to a private message |
| `/broadcast <message>` | `bc` | Broadcast a message |
| `/alert <message>` | | Alert the server |
| `/announce` | `announcement`, `announcements` | Send or manage announcements |
| `/mutechat` | | Mute the global chat |
| `/clearchat` | `cc` | Clear the chat |
| `/chatcolor <color>` | `chatcolour` | Set your chat colour |
| `/namecolor <color>` | `namecolour` | Set your name colour |
| `/tag` | `tags`, `taglist` | List, preview, set, clear or randomise your chat tag |
| `/nametag` | | Refresh EvaulxMC display names (`status`, `reload`) |
| `/buildmode` | `build`, `bm` | Toggle lobby build bypass mode |
| `/lobbyprotect` | `lobbyprotection`, `protectlobby`, `lp` | Manage and enforce lobby protection |

### Warps, homes & AFK

| Command | Aliases | Description |
|---------|---------|-------------|
| `/warp <name>` | | Teleport to a warp |
| `/warps` | `warplist` | List warps |
| `/setwarp <name>` | | Create or update a warp (admin) |
| `/delwarp <name>` | `deletewarp`, `removewarp` | Delete a warp (admin) |
| `/home [name]` | | Teleport to one of your homes |
| `/homes` | `homelist` | List your homes |
| `/sethome [name]` | | Set a home (default name `home`) |
| `/delhome <name>` | `deletehome`, `removehome` | Delete one of your homes |
| `/afk [reason]` | `away` | Toggle your AFK status |

Home limits: everyone gets `homes.default-limit` (config); grant a rank `evaulx.homes.<n>` for `n`
homes, or `evaulx.homes.unlimited` to remove the cap. `/warp` and `/home` set your `/back` location.
AFK is also triggered automatically after `afk.auto-minutes` of inactivity (config).

### `/tag` quick reference
```text
/tag list
/tag search <text>
/tag preview <tag>
/tag info <tag>
/tag set <tag> [player]
/tag random [player]
/tag clear [player]
```
