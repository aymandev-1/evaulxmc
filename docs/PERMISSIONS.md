# EvaulxMC — Permission Reference

All permission nodes as declared in `plugin.yml`. **Default** is the node's default value:

- `op` — granted to server operators only (the default for most nodes)
- `true` — granted to everyone
- `false` — granted to no one unless explicitly assigned

`config.yml` has `permissions.op-bypass: true`, so operators bypass permission checks by default.

---

## Wildcards

| Node | Default | Description |
|------|---------|-------------|
| `evaulx.*` | op | All EvaulxMC permissions |
| `evaulx.admin` | op | Access EvaulxMC admin commands |
| `evaulxmc.rank.*` | op | All EvaulxMC rank identity nodes (`evaulxmc.rank.<rank>`) |
| `evaulx.rank` | op | View and manage ranks (parent of `evaulx.rank.*`) |

`evaulxmc.rank.<rank>` identity nodes exist for each preset rank, e.g. `evaulxmc.rank.owner`,
`evaulxmc.rank.admin`, `evaulxmc.rank.mod`, `evaulxmc.rank.vip`, etc.

---

## Punishments

| Node | Default | Description |
|------|---------|-------------|
| `evaulx.ban` | op | Ban players |
| `evaulx.tempban` | op | Temporarily ban players |
| `evaulx.ipban` | op | IP-ban players |
| `evaulx.unban` | op | Unban players |
| `evaulx.mute` | op | Mute players |
| `evaulx.tempmute` | op | Temporarily mute players |
| `evaulx.unmute` | op | Unmute players |
| `evaulx.kick` | op | Kick players |
| `evaulx.warn` | op | Warn players |
| `evaulx.unwarn` | op | Remove warnings |
| `evaulx.blacklist` | op | Blacklist players |
| `evaulx.checkpunishments` | op | View punishment history |
| `evaulx.punish` | op | Use punishment presets |

---

## Ranks, permissions & grants

| Node | Default | Description |
|------|---------|-------------|
| `evaulx.rank.add` | op | Add extra ranks |
| `evaulx.rank.category` | op | Set saved rank categories |
| `evaulx.rank.cleanup` | op | Clean deleted rank references |
| `evaulx.rank.clone` | op | Clone ranks |
| `evaulx.rank.create` | op | Create ranks |
| `evaulx.rank.default` | op | Set the default rank |
| `evaulx.rank.delete` | op | Delete ranks |
| `evaulx.rank.edit` | op | Edit rank display fields and weight |
| `evaulx.rank.hidden` | op | Toggle hidden rank status |
| `evaulx.rank.inherit` | op | Manage rank inheritance |
| `evaulx.rank.perm` | op | Manage rank permissions |
| `evaulx.rank.player` | op | View player rank information |
| `evaulx.rank.reload` | op | Reload ranks |
| `evaulx.rank.remove` | op | Remove extra ranks |
| `evaulx.rank.set` | op | Set primary ranks |
| `evaulx.rank.staff` | op | Toggle staff rank status |
| `evaulx.perm` | op | Manage permissions |
| `evaulx.permaudit` | op | Audit rank and player permissions |
| `evaulx.grant` | op | Grant ranks |
| `evaulx.grant.approve` | op | Approve and deny grant requests |
| `evaulx.grant.bypass-approval` | op | Bypass grant approval requirements |
| `evaulx.grant.remove` | op | Remove grants |
| `evaulx.grants` | op | View grants |
| `evaulx.grant.template.vip` | op | Use VIP grant templates |
| `evaulx.grant.template.media` | op | Use media grant templates |
| `evaulx.grant.template.staff` | op | Use staff grant templates |

---

## Staff tools

| Node | Default | Description |
|------|---------|-------------|
| `evaulx.staff` | op | Receive staff alerts |
| `evaulx.staffmode` | op | Toggle staff mode for yourself |
| `evaulx.staffmode.autojoin` | op | Auto-enter staff mode on join (if enabled) |
| `evaulx.staffmode.others` | op | Toggle staff mode for other players |
| `evaulx.staffchat` | op | Send and receive staff chat |
| `evaulx.staffstatus` | op | View current staff status |
| `evaulx.staffrecover` | op | Recover staff-mode inventory/state |
| `evaulx.staffpanel` | op | Open the staff panel |
| `evaulx.staffdashboard` | op | Open the staff dashboard |
| `evaulx.staffsessions` | op | View staff session tracking |
| `evaulx.stafflist` | op | View online staff |
| `evaulx.stafflogs` | op | Search recent staff action logs |
| `evaulx.modlogs` | op | View moderation logs |
| `evaulx.notes` | op | Manage and view staff notes |
| `evaulx.vanish` | op | Toggle vanish for yourself |
| `evaulx.vanish.others` | op | Toggle vanish for other players |
| `evaulx.vanish.see` | op | See vanished staff |
| `evaulx.freeze` | op | Freeze and unfreeze players |
| `evaulx.commandspy` | op | Toggle command spy |
| `evaulx.commandspy.exempt` | false | Hide your commands from command spy |
| `evaulx.socialspy` | op | Toggle social spy |
| `evaulx.maintenance` | op | Manage maintenance mode |
| `evaulx.maintenance.bypass` | op | Join while maintenance mode is enabled |
| `evaulx.lookup` | op | Lookup cached player profile info |
| `evaulx.profile` | op | Open staff player profiles |
| `evaulx.reports` | op | View and receive player reports |
| `evaulx.helpop.receive` | op | Receive helpop messages |

---

## Disguises / nicks

| Node | Default | Description |
|------|---------|-------------|
| `evaulx.disguise` | op | Use disguise commands |
| `evaulx.disguise.admin` | op | Manage disguise settings, cache, debug, refresh, status |
| `evaulx.disguise.skin` | op | Choose a disguise skin |
| `evaulx.disguise.rank` | op | Choose a disguise rank |
| `evaulx.disguise.others` | op | Force undisguise other players |
| `evaulx.disguise.staff` | op | Receive disguise staff alerts |
| `evaulx.disguise.bypass-name-check` | op | Bypass duplicate disguise name checks |
| `evaulx.disguise.bypass-blacklist` | op | Bypass disguise blacklist checks |
| `evaulx.disguise.cooldown.bypass` | op | Bypass disguise cooldowns |
| `evaulx.skin` | op | Change your active disguise skin |
| `evaulx.nick` | op | Use the `/nick` alias |
| `evaulx.nick.skin` | op | Choose a skin with `/nick` |
| `evaulx.nick.rank` | op | Choose a rank with `/nick` |
| `evaulx.nick.cooldown.bypass` | op | Bypass nick cooldowns |
| `evaulx.nicklist` | op | List online disguised players |
| `evaulx.nickhistory` | op | View disguise history |
| `evaulx.realname` | op | Resolve disguised player names |

---

## Chat & cosmetics

| Node | Default | Description |
|------|---------|-------------|
| `evaulx.chat.bypass` | op | Bypass chat mute and moderation filters |
| `evaulx.chat.links` | op | Send links when link blocking is enabled |
| `evaulx.chat.spy` | op | See ranged chat from any distance |
| `evaulx.chatcolor` | **true** | Set your chat colour |
| `evaulx.chatcolor.others` | op | Set other players' chat colours |
| `evaulx.namecolor` | **true** | Set your name colour |
| `evaulx.namecolor.others` | op | Set other players' name colours |
| `evaulx.mutechat` | op | Mute global chat |
| `evaulx.clearchat` | op | Clear global chat |
| `evaulx.nametag` | op | Refresh nametags |
| `evaulx.tag` | **true** | List and set public tags |
| `evaulx.tag.all` | op | Use every configured tag |
| `evaulx.tag.custom` | op | Use custom tag text |
| `evaulx.tag.others` | op | Set other players' tags |
| `evaulx.tag.<name>` | op | Per-tag nodes: `owner`, `admin`, `mod`, `helper`, `vip`, `mvp`, `legend`, `media`, `streamer`, `yt`, `builder`, `partner`, `veteran`, `champion`, `grinder`, `seasonal` |

---

## Essentials & utility

| Node | Default | Description |
|------|---------|-------------|
| `evaulx.fly` | op | Toggle flight |
| `evaulx.god` | op | Toggle god mode |
| `evaulx.heal` | op | Heal players |
| `evaulx.feed` | op | Feed players |
| `evaulx.speed` | op | Set fly or walk speed |
| `evaulx.gamemode` | op | Change gamemode |
| `evaulx.enchant` | op | Enchant held item without level limits |
| `evaulx.invsee` | op | Inspect player inventories |
| `evaulx.spawn` | **true** | Teleport to spawn |
| `evaulx.setspawn` | op | Set server spawn |
| `evaulx.back` | op | Teleport to previous location |
| `evaulx.tp` | op | Teleport players |
| `evaulx.tpall` | op | Teleport all players |
| `evaulx.tppos` | op | Teleport to coordinates |
| `evaulx.tppos.others` | op | Teleport other players to coordinates |
| `evaulx.list` | **true** | List online players |
| `evaulx.sudo` | op | Force players to run commands or chat |
| `evaulx.broadcast` | op | Broadcast messages |
| `evaulx.alert` | op | Send alert messages |
| `evaulx.announce` | op | Send announcements |
| `evaulx.announce.manage` | op | Manage automatic announcements |
| `evaulx.buildmode` | op | Toggle build mode in protected lobbies |
| `evaulx.buildmode.others` | op | Toggle build mode for other players |
| `evaulx.lobbyprotection` | op | Manage lobby protection |
| `evaulx.protection.bypass` | op | Bypass lobby protection |
| `evaulx.warp` | **true** | Use and list warps |
| `evaulx.warp.admin` | op | Create and delete warps |
| `evaulx.home` | **true** | Set, delete and teleport to your homes |
| `evaulx.homes.<n>` | — | Grant a rank `n` homes (e.g. `evaulx.homes.5`) |
| `evaulx.homes.unlimited` | op | Bypass the home limit |
| `evaulx.afk` | **true** | Toggle your AFK status |
| `evaulx.afk.exempt` | op | Never be marked AFK automatically |
