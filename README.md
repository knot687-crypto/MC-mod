# Vanta

Internal ghost client for **Minecraft Java 1.21.11**.

No Fabric. No Forge. No mods folder. A Java agent attaches to the running game,
remaps the client's classes into the game's obfuscated namespace and patches five call
sites in vanilla code. Everything runs inside the Minecraft process.

---

## How it works

```
vanta-1.0.0.jar
├── dev/vanta/agent/        agent, mappings, remapping loader, ASM hooks
├── dev/vanta/**            the client (compiled against Yarn, shipped named)
├── vanta/mappings.bin      Yarn -> obfuscated table generated from 1.21.11
└── org/objectweb/asm/**    shaded ASM

premain / agentmain
  1. parse vanta/mappings.bin                     10,275 classes, 96,465 members
  2. find the loader that owns the game
  3. install a retransform-capable transformer   (warm, non re-entrant)
  4. patch gfj (MinecraftClient#tick)
           giq (InGameHud#render)
           hio (ClientPlayerInteractionManager#attackEntity)
           cgk (Entity#setVelocityClient, Entity#getBoundingBox)
  5. load dev.vanta.Bootstrap through ClientLoader
  6. ClientLoader remaps every client class from Yarn names to the game's
     obfuscated names and defines it inside the game JVM
```

`ClientLoader` is the piece a mod loader normally provides. Client sources are written
against readable Yarn names (`MinecraftClient.getInstance()`, `player.getPos()`) and are
translated on load; member lookups walk the real class hierarchy so overrides and
inherited calls resolve correctly.

The five hooks talk to the client through `dev.vanta.agent.Bridge`, a class that
references nothing but `java.base`. Nothing in the patched game code can pull a client
class into the game's loader.

## Features

**Combat** — KillAura (switch / single, rotation modes, CPS, range, walls, teams),
AutoClicker (gaussian jitter, block-hit, inventory checks), AimAssist (GCD-snapped,
smoothing, vertical/horizontal factors), TriggerBot (delay, only-weapon, crits),
Reach (entity + block, restored on disable), HitBox, Velocity (horizontal/vertical,
chance, explosion), Criticals (packet/jump), WTap, JumpReset, AutoTotem (health/hole,
offhand restore), AutoShield, AntiBot (tab-list, ping, sleeping, duplicate names).

**Visual** — Watermark/ArrayList with accent gradient, TargetHud with health and
distance, HitMarker.

**Client** — Interface (RSHIFT menu bind, five accent palettes), JSON config written to
`<game dir>/config/vanta/config.json`.

All combat values are randomised where it matters: click intervals, rotation steps,
hit chance, box growth. Nothing fires on a fixed tick.

## The menu

A horizontal, type-kit style window: gradient header with mark, wordmark, search pill and
window dots; sliding tab rail with icons and module counts; two-column card grid with
spring-animated switches and clipped descriptions; live settings column with gradient
sliders, mode cyclers and bind capture; gradient scroll thumb; stats footer.

Everything is drawn through `Render2D` — rounded rects, vertical gradients, outlines,
soft shadows and letter-spaced caps built from `DrawContext` scanline fills, so there are
no textures to load and it scales with GUI scale.

## Build

Requires **JDK 21**. Gradle wrapper is committed.

```bash
./gradlew build
```

Output:

| Path | What |
| --- | --- |
| `injector/build/libs/vanta-1.0.0.jar` | everything: agent + client + mappings + ASM |
| `client/build/devlibs/vanta-client-1.0.0-dev.jar` | client classes alone (Yarn names) |

Loom is used **only** to provide the Yarn-named 1.21.11 classes for compilation.
Nothing from Fabric Loader or Fabric API is compiled against, linked or shipped.

## Inject

The jar is both the CLI and the agent. Minecraft must already be running on a **JDK**
(not a JRE), and the CLI has to run on the same JVM/account.

```bash
# list running 1.21.11 clients
java -jar vanta-1.0.0.jar --list

# attach (auto-detects, or pass --pid)
java -jar vanta-1.0.0.jar --pid 4821

# or at launch
java -javaagent:vanta-1.0.0.jar -cp ... net.minecraft.client.main.Main ...
```

Press **RSHIFT** for the menu. The agent logs to stdout and to `vanta-inject.log`; the log
lists every hook it bound and every class it patched.

## Verification

`tools/Verify.java` runs offline against the real obfuscated 1.21.11 jar and checks,
in order:

1. all 48 client classes remap without error,
2. every remapped class / field / method reference exists in the game jar (363 class and
   329 member references),
3. every method that overrides a parent method was actually renamed by the remapper,
4. each remapped class is defined in a JVM, which forces HotSpot to verify it,
5. each hook target exists, patches, passes ASM data-flow verification and is accepted by
   the JVM verifier.

```bash
java -cp injector/build/libs/vanta-1.0.0.jar tools/Verify.java \
     client/build/devlibs/vanta-client-1.0.0-dev.jar \
     ~/.gradle/caches/fabric-loom/1.21.11/minecraft-client.jar
```

Last run:

```
mappings: 10275 classes, 96465 members
remapped 48/48 client classes
linked 48/48 remapped classes in the JVM
hooks: [MinecraftClient#tick -> gfj#x()V, InGameHud#render -> giq#a(Lgir;Lgez;)V,
        ClientPlayerInteractionManager#attackEntity -> hio#a(Lddm;Lcgk;)V,
        Entity#setVelocityClient -> cgk#j(Lftm;)V,
        Entity#getBoundingBox -> cgk#dj()Lfth;]
  patched + verified gfj / giq / hio / cgk
RESULT: all checks passed
```

The agent was also run in a real JVM with the 1.21.11 jar on the classpath: it bound all
five hooks, retransformed all four classes and reported `vanta is live`.

## Regenerating mappings

`client/src/main/resources/vanta/mappings.bin` was produced by `tools/MappingGen.java`
from the Yarn `mergedv2` mappings and the official obfuscated client jar:

```bash
java tools/MappingGen.java mappings.tiny minecraft-client.jar \
     client/src/main/resources/vanta/mappings.bin
```

Re-run it when the game version or Yarn build changes; nothing else in the client needs
to know.

## Notes and limits

- **Vanilla 1.21.11 only.** The mapping table is generated from the official obfuscated
  jar. Launchers that rewrite the jar with their own names (lunar, badlion) are not
  covered.
- Reach, HitBox and Velocity are client-side; servers with their own reach/knockback
  checks can still reject them.
- The client was verified against the real bytecode as described above, but this
  environment has no display, so the menu has not been looked at in a live game. First
  launch with the HUD and menu open is the remaining visual check.

## Layout

```
client/src/main/java/dev/vanta/
  Bootstrap.java        injected entry point, wires Bridge
  Vanta.java            client root, frame hooks
  agent/Bridge.java     hook seam (java.base only)
  core/                 modules, settings, config, targeting, rotations, stats
  modules/combat/       13 combat modules
  modules/visual/       watermark, target hud, hit marker
  render/               theme, Render2D
  gui/ClickGui.java     the menu
injector/src/main/java/dev/vanta/
  agent/Agent.java      premain / agentmain
  agent/Mappings.java   runtime mapping table, hierarchy-aware lookups
  agent/ClientLoader.java  remapping class loader
  agent/Hooks.java      target resolution + ASM patch rules
  injector/Main.java    attach CLI
tools/                  mapping generator, verifier
```
