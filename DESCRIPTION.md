# Afterlight

> **BETA — TESTING IN PROGRESS**
>
> Afterlight is in beta. Core features are complete and the mod is stable
> enough for small-group multiplayer testing, but it has not yet had wide
> exposure. Use backups on any world that matters and please report any
> bugs you hit.

When you die in Minecraft, instead of seeing the respawn screen, you become
a ghost — translucent, intangible, and unable to interact with the world.
You float around the death scene as your friends scramble to revive you
with a Revival Beacon.

---

## What it does

- **No respawn screen.** Death drops your inventory and XP, then converts
  you into a ghost in-place — you don't get teleported to spawn.
- **Two ghost forms** (configurable):
  - **Transparent** — others see a translucent version of you with a
    `[Ghost]` prefix above your name.
  - **Invisible** — fully hidden from other players.
- **Server-wide default + per-player override.** Each player can pick
  their preferred form via `/afterlight mode <transparent|invisible>`,
  or fall back to whatever the server admin sets as the default.
- **Revival Beacon** — a craftable item another player can use on you to
  bring you back to life. Single-use, consumed after a successful revive.
  Spawns a poof of cyan particle rings on the revived player.
- **Combat-aware.** While a ghost you cannot:
  - Take damage (mobs ignore you, projectiles pass through your body)
  - Push or be pushed by other entities
  - Break blocks, attack, use items, or pick up XP and items
  - Leave sprint particles or stuck arrows behind on your body
- **Death broadcast.** When a player dies, every player sees a red
  `<name> has been killed!` message. On revival, a green
  `<name> has been revived!` message.
- **240-block kill sound.** A wither-spawn alert plays for every player
  within 240 blocks of a death — even outside normal sound range — so
  nearby players know something happened.
- **Persistent.** Ghost state survives relogs and server restarts. Quit
  and rejoin while a ghost? You're still a ghost.
- **Totem of Undying compatible.** Holding a totem still saves you from
  fatal damage — vanilla totem behavior is preserved.
- **Health bar shows 0.** While in ghost form your client UI displays
  empty hearts so you know you're not in your normal body.

---

## Commands

All `/afterlight` subcommands are OP-only.

| Command | Effect |
|---|---|
| `/afterlight` | Toggle yourself into / out of ghost mode |
| `/afterlight <player>` | Toggle another player |
| `/afterlight visible [player]` | Flip a current ghost between transparent and invisible |
| `/afterlight mode <transparent\|invisible> [player]` | Set a player's preferred death-form (persists) |
| `/afterlight default <transparent\|invisible>` | Set the server-wide default death-form |

---

## Requirements

- Minecraft **1.21.11**
- **Fabric Loader** ≥ 0.18.4
- **Fabric API**
- **AStudioLib** 1.1.0 (shared library)
- Java 21

---

## Known beta caveats

- Tested primarily on macOS in dev environments and small multiplayer
  setups. Larger servers may surface issues we haven't seen.
- The Revival Beacon currently has no recipe / loot drop wired up yet —
  beacons need to be given via `/give` for testing.
- Behavior with other mods that hook the death pipeline (alternative
  totems, custom respawn systems, gravestones, etc.) has not been
  validated and may conflict.
- Save format for `afterlight_config.json` and `afterlight_ghosts.dat`
  may still change between beta builds.

If you hit a bug or weird interaction, a short repro plus your
`logs/latest.log` is the most useful thing you can send.
