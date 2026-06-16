# Developer Guide

You keep the server healthy and debug issues. Powerful tools — use them carefully on
the live server. Read [STAFF_INTRO](STAFF_INTRO.md) first.

## Your panel: `/developerpanel`
Diagnostics and dev tools in one menu — TPS, memory/GC, threads, plugin info,
entity/chunk diagnostics, dev mode, test effects, and reload.

## Diagnostics
| Command | Use |
|---|---|
| `/tps` | Server tick rate, RAM, uptime. |
| `/serverinfo` | Detailed server/version info. |
| `/gc` | Run garbage collection + report memory. |
| `/threads` | JVM thread statistics. |
| `/plugininfo` (`/pl`) | List plugins / inspect one. |
| `/entitycount` | Per-world entity counts (lag hunting). |
| `/chunkinfo` | Diagnostics for your current chunk. |

## Dev actions
| Command | Use |
|---|---|
| `/devmode` | Creative + fly + night vision for debugging. |
| `/testeffect <effect> [duration] [amp]` | Apply a potion effect to yourself. |
| `/devbroadcast <message>` | Developer-styled announcement. |
| `/clearlag` | Remove ground items & stray entities. |
| `/reloadplugin` | Reload EvaulxMC config/managers. |

## Watch out for
- `/reloadplugin` and `/clearlag` affect the **whole** server — warn staff first.
- Prefer a restart over repeated reloads if something behaves oddly.
- Test effects/dev mode on yourself only; don't disrupt players.
