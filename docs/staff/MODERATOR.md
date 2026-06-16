# Moderator Guide

Your job: keep players safe and the chat clean. Read [STAFF_INTRO](STAFF_INTRO.md) first.

## Your panel: `/modpanel`
One menu with everything below — reports, punish, freeze, inspect, notes, mod logs,
vanish, staff mode, profiles, whois, IP check, and chat moderation.

## Punishments
| Command | Use |
|---|---|
| `/ban <player> [reason]` | Permanent ban. |
| `/tempban <player> <time> [reason]` | Timed ban (e.g. `7d`, `12h`). |
| `/mute <player> [reason]` | Stop a player chatting. |
| `/tempmute <player> <time>` | Timed mute. |
| `/kick <player> [reason]` | Remove a player now. |
| `/warn <player> <reason>` | Formal warning (tracked). |
| `/unban` `/unmute` `/unwarn` | Reverse the above. |
| `/punish <player>` | Open preset punishment ladder (recommended — keeps punishments consistent). |

**Always give a reason** — players see it and it's logged.

## Investigating
- `/checkpunishments <player>` (`/history`) — full punishment record.
- `/whois <player>` — rank, playtime, punishments, flags at a glance.
- `/invsee <player>` — inspect inventory.
- `/alts <player>` / `/ipcheck <player>` — find linked accounts (handle privately).
- `/note <player> <text>` — leave context for other staff.

## Spy tools (how they actually work)
- `/commandspy` — see **other players'** commands, staff included (commands are only hidden from people
  who lack the `evaulx.commandspy` permission). You still won't see your *own* commands. Sensitive auth
  commands (`/login`, `/register`, `/password`…) stay hidden for everyone. To go back to hiding staff
  commands, set `staff-tools.command-spy.hide-staff-commands: true`.
- `/socialspy` — see **other players'** private `/msg`s. You won't see a conversation you're part of;
  a **third** person with socialspy on has to be online to read it.

## Chat control
- `/mutechat` — freeze global chat during spam/raids.
- `/clearchat` — wipe the chat.
- `/slowchat` — add a cooldown between messages.

## Reports & help
Players use `/report` and `/helpop`. Claim a report before acting, then close it.
Everything is in `/modpanel` or `/staffpanel`.

## Watch out for
- Don't punish on suspicion alone — check logs/inventory first.
- Escalate ban evasion / serious cases to an Admin.
