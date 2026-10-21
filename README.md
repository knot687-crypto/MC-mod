# VANTA

Internal ghost client for **Minecraft 1.21.11**. Combat only, client side, humanised by default.
Fabric mod for the normal path, Java agent injector for the runtime path, one shared core.

```
53 java files · 4,108 lines · 13 combat modules · 3 visual modules · 1 premium menu
```

![version](https://img.shields.io/badge/minecraft-1.21.11-6b7cff) ![loader](https://img.shields.io/badge/fabric--loader-0.19.5+-3fd9ff)

---

## What it is

An **internal** client: it runs inside the game process with direct access to the client
classes, not an external overlay reading memory. Two ways in:

| Path | How | Hooks |
| --- | --- | --- |
| **Fabric mod** | drop `vanta-client-1.0.0.jar` in `.minecraft/mods` | Mixin (`@Inject`) |
| **Injector** | attach `vanta-injector-1.0.0.jar` to a running instance | ASM retransform at runtime |

Both land on the same core (`dev.vanta.Vanta`), so a feature behaves identically whichever
door it came through. Everything is client side: no server, no packet spam, no teleports.

---

## The menu

`RSHIFT` opens a horizontal type-kit window — header, tab rail, module grid, live settings
column, status footer. It is drawn entirely with rounded-rectangle primitives, so it stays
crisp at any GUI scale.

- **Header** — gradient mark, `VANTA` wordmark with letter-spaced caps, `GHOST SUITE · 1.21.11`,
  search pill with caret, macOS-style window dots.
- **Tab rail** — `Combat / Visuals / Client`, each with a drawn icon and module count; the
  active pill and its accent rail slide between tabs.
- **Module grid** — two columns of cards. Hover raises the card, enables slide the accent bar in,
  every switch has its own spring animation. Descriptions clip to the card, cards clip to the list.
- **Settings column** — hover a card and its settings appear live: toggles, gradient sliders with
  a glowing knob, mode cyclers with drawn chevrons, and a bind chip that captures the next key.
- **Footer** — hints, fps, ping, active count, accent name.
- Scroll rails are gradient thumbs, not scrollbars.

### Palette

| Token | Value | Use |
| --- | --- | --- |
| `WINDOW_TOP` | `#0C1017` | window gradient top |
| `WINDOW_BOTTOM` | `#090C11` | window gradient bottom |
| `HEADER` | `#10151E` | header / footer plate |
| `CARD` | `#141A25` | module card |
| `CARD_HOVER` | `#1A2231` | card hover |
| `CARD_ACTIVE` | `#1C2537` | enabled card |
| `TEXT` | `#E9EDF7` | primary |
| `TEXT_DIM` | `#8A93AA` | secondary |
| `TEXT_FAINT` | `#59627A` | meta |
| accent A | `#7C5CFF` | violet (default) |
| accent B | `#3FD9FF` | cyan |

Accent presets: **Violet, Ember, Mint, Rose, Ice** (`Interface → Accent`).

---

## Features

### Combat

| Module | What it does |
| --- | --- |
| **KillAura** | Range 2–6, CPS band, FOV gate, sort by distance/angle/health, visible-only, mobs, weapon-only, hold-click mode. Rotations are GCD-snapped to the mouse grid and step at a configurable fraction, only swinging once inside the aim tolerance. |
| **AutoClicker** | CPS band with gaussian spacing, hold-click, entity-only, cooldown gate, weapon-only. Swings through the real interaction manager so crits/wtaps still fire. |
| **AimAssist** | Soft pull, never a snap: strength × smoothness, gaussian noise, FOV, visible check, optional horizontal-only. |
| **TriggerBot** | Swings at whatever the crosshair already touches, with its own CPS band, range gate and miss chance. |
| **Reach** | Pushes the `entity_interaction_range` / `block_interaction_range` attributes; restores the saved base values on disable. |
| **HitBox** | Client-side box expansion per player/mob with a chance roll and jitter, so clicks land where the model looks. |
| **Velocity** | Scales incoming knockback horizontally and vertically, with chance and ground-only gates. Applied at `setVelocityClient`, so vanilla never sees the original. |
| **Criticals** | Packet mode spoofs one falling tick before the hit, optionally dropping sprint for N ticks; jump mode presses jump instead. |
| **WTap** | Drops sprint on hit and restores it after a configurable hold, sending the real sprint packets. |
| **JumpReset** | Auto-jumps the tick after knockback, with delay and chance. |
| **AutoTotem** | Swaps a totem into the offhand under a health threshold, with a delay floor and optional combat check. |
| **AutoShield** | Raises the offhand shield when a target enters range, with a delay floor. |
| **AntiBot** | Tab-list and invisibility heuristics; every targeting query runs through it. |

### Visuals

| Module | What it does |
| --- | --- |
| **Watermark** | Badge, fps/ping/cps line, right-hand module list sorted by width. |
| **TargetHud** | Name, distance and a smoothed health bar under the crosshair. |
| **HitMarker** | Four ticks that flash and fade on every landed hit. |

### Client

| Module | What it does |
| --- | --- |
| **Interface** | Menu bind (default `RSHIFT`) and accent palette. |

Every combat module is humanised: timing is randomised per click, rotations are snapped to
mouse pixels, nothing fires on a fixed beat, and every target query is gated by FOV, range and
visibility.

---

## Build

Requires **JDK 21**. The wrapper is committed.

```bash
./gradlew build
```

Outputs:

```
client/build/libs/vanta-client-1.0.0.jar      # fabric mod (internal client)
injector/build/libs/vanta-injector-1.0.0.jar  # java agent + attach CLI, ASM shaded
```

`client/build.gradle` compiles against Loom 1.14.10, Yarn `1.21.11+build.6`, Fabric Loader
`0.19.5`, Fabric API `0.141.6+1.21.11`. Loom 1.14 remaps mixin targets natively, so there is
no annotation processor and no refmap file.

### Dev run

```bash
./gradlew :client:runClient
```

---

## Install

### A — as a mod

1. Fabric Loader 0.19.5+ for 1.21.11, Fabric API installed.
2. Drop `vanta-client-1.0.0.jar` into `.minecraft/mods`.
3. Launch, press `RSHIFT`.

### B — injected

Both jars side by side, then:

```bash
java -jar vanta-injector-1.0.0.jar --list          # see running 1.21.11 instances
java -jar vanta-injector-1.0.0.jar                 # auto-detect, attach, load client
java -jar vanta-injector-1.0.0.jar --pid 4821 --client vanta-client-1.0.0.jar
```

`--list` prints pid and command for every Fabric 1.21.11 process it can see. Attach needs the
same user account as the game, and the game must run on a **JDK** (the attach API ships with
`jdk.attach`, not a JRE). A log goes to `vanta-inject.log` in the working directory.

#### How the injector works

1. `VirtualMachine.attach(pid)` → `loadAgent(agentJar, "client=<path>;log=<path>")`.
2. `GameLoader` finds the class loader the render thread actually uses (Knot on Fabric).
3. `Resolver` wraps fabric-loader's `MappingResolver` and converts the Yarn names in the hook
   table to the runtime (intermediary) names — nothing is hardcoded.
4. The client jar is opened in a `URLClassLoader` parented to the game loader, so Minecraft
   types resolve while client classes stay isolated.
5. `Hooks` retransforms four classes with straight-line ASM insertions:

| Target | Insert | Hook |
| --- | --- | --- |
| `MinecraftClient#tick` | entry | `Bridge.onTick()` |
| `InGameHud#render` | before each `RETURN` | `Bridge.onHud(context)` |
| `Entity#setVelocityClient` | argument replace | `Bridge.filterVelocity(self, velocity)` |
| `ClientPlayerInteractionManager#attackEntity` | entry | `Bridge.onAttack(player, target)` |

Nothing branches, so `COMPUTE_MAXS` is enough and the original stack map frames stay valid.
`Bridge` is the only shared class and it references nothing but `java.base` — the game loader
can link it on the hot path without touching client classes.

On the Fabric path the same four points are hooked with Mixin (see `dev/vanta/mixin`), including
hitbox expansion on `Entity#getBoundingBox` (mod path only).

---

## Keys

| Key | Action |
| --- | --- |
| `RSHIFT` | open / close the menu |
| click a card | toggle the module |
| hover a card | load its settings in the right column |
| drag the header | move the window |
| scroll | module list / settings list |
| bind chip | click, then press a key (`ESC` clears) |
| type in the search pill | filters across every category |

---

## Config

`config/vanta/config.json`, written on toggle and at shutdown:

```json
{
  "modules": {
    "KillAura": {
      "enabled": true,
      "key": -1,
      "settings": {
        "Range": 3.1,
        "Min CPS": 9,
        "Max CPS": 13,
        "Sort": "Distance",
        "Rotations": true
      }
    }
  }
}
```

Unknown or corrupt keys are ignored; a bad file boots clean rather than failing.

---

## Project map

```
client/
  src/main/java/dev/vanta/
    Vanta.java                  client root, frame + hook funnel
    core/                       modules, settings, config, targeting, rotations, stats
    modules/combat/             13 combat modules
    modules/visual/             watermark, target hud, hit marker
    modules/client/             interface (bind + accent)
    gui/ClickGui.java           the menu
    render/                     Render2D primitives + Theme tokens
    util/                       angles, input, clicker, packets, inventory, combat
    mixin/                      4 mixin classes, 5 injection points
    agent/Injected.java         bootstrap used by the injector path
  src/main/resources/           fabric.mod.json, vanta.mixins.json, icon

injector/
  src/main/java/dev/vanta/injector/
    Main.java                   attach CLI (+ --list)
    Agent.java                  premain / agentmain
    GameLoader.java             render-thread class loader discovery
    Resolver.java               yarn -> runtime mappings
    Hooks.java                  ASM transformer + retransform
    Log.java
  src/main/java/dev/vanta/agent/
    Bridge.java                 loader-neutral hook contract
```

---

## Technical notes

- **Reach** is attribute based: the server still enforces its own interaction range, so values
  past the server's limit are rejected there, not here.
- **HitBox** is client side only. It changes what you can click, not what the server accepts.
- **Velocity** returns the scaled vector instead of writing it, so the caller (mixin or bridge)
  applies it once. No double application, no stale state.
- **Criticals** sends one spoofed position tick and lets the normal movement packet correct it.
- **AntiBot** uses tab-list presence, which is the cheapest reliable NPC tell on practice servers.
- Module exceptions cannot take down the render or tick thread on the injected path (`Injected`
  guards every bridge call and rate-limits the log).
- The agent path needs a Fabric runtime: it resolves every target through fabric-loader's
  mapping API, and the client jar ships in intermediary names. Vanilla (obfuscated) instances
  are not a supported target.
- **HitBox and reach mixing**: hitbox expansion is mod-path only; the injection path covers tick,
  HUD, knockback and attack.

### Verified in this environment

| Check | Result |
| --- | --- |
| `./gradlew clean build` (both modules) | pass |
| Mixin targets remapped by Loom | pass — `method="method_1574"` in the built class |
| Client jar excludes `Bridge` (compileOnly) | pass |
| Injector jar: manifest, Bridge, shaded ASM | pass |
| Agent harness (stub JVM, `-javaagent`, fake game classes) | pass — 4/4 hooks installed, `ticks=3 huds=1 attacks=1 velocities=1`, velocity replacement applied by the JVM (`Vec3d[2,3,4] → Vec3d[1,3,2]`) |

Not verified here: an actual Minecraft session (no GPU/display in the build environment), so the
visual pass and in-game behaviour still need a real client run.

---

## License

MIT — see `LICENSE`.
