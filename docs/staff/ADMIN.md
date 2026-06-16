# Admin Guide

You manage ranks, staff, and server-wide settings. Read [STAFF_INTRO](STAFF_INTRO.md)
and [MODERATOR](MODERATOR.md) first — you have every mod tool plus the below.

## Your panel: `/adminpanel`
Rank manager, grants, player profiles, maintenance, lobby protection, reports,
server diagnostics, and a shortcut to the owner panel (if permitted).

## Ranks & grants
| Command | Use |
|---|---|
| `/rank` / `/ranks` | Manage the rank ladder. |
| `/createrank` `/deleterank` `/setrank <player> <rank>` | Core rank ops. |
| `/grant <player> <rank> [time]` | Give a temporary or permanent extra rank. |
| `/grants <player>` / `/removegrant` | View / remove active grants. |
| `/resetrank <player>` | Wipe grants and reset to default rank. |
| `/rankreload` | Reload ranks after editing. |

Grants can require approval — check the **Pending Grants** queue in the panel.

## Server controls
| Command | Use |
|---|---|
| `/maintenance` | Put the server in maintenance (only allowed players join). |
| `/lockdown` | Block new joins instantly (raids/emergencies). |
| `/lobbyprotect` | Manage lobby protection rules. |
| `/announce` / `/broadcast` | Server-wide messages. |
| `/serverinfo` `/tps` | Health and performance at a glance. |

## Staff oversight
- `/staffsessions` — who's been on staff duty and for how long.
- `/stafflogs` / `/modlogs <player>` — audit staff actions.
- `/permaudit` — review rank & player permissions.

## Watch out for
- Rank edits affect everyone with that rank — use `/rank backup` before big changes.
- Maintenance and lockdown are powerful; announce before using on a busy server.
