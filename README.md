# Black Hole – a Re:AcademyCraft 1.5.0 add-on (Minecraft 1.20.1, Forge 47.4.10+)

One massive ability, **Black Hole**, in its own category, **Singularity**. A separate mod: it does not change Re:AcademyCraft.

## What it does

Aim anywhere within 40 blocks and press the skill key.

1. **Formation (1.5 s):** a pitch-black sphere opens about 4 blocks above the aimed spot while the pull ramps up.
2. **Active (8 s):** every enemy within **24 blocks** is dragged toward the centre, spiralling inward.
   Anything inside the **event horizon** (3.2 blocks) takes **10 damage 4 times a second**. Arrows and other projectiles are
   pulled in and destroyed at the horizon.
3. **Collapse:** after 8 s, or when you press the key again, it implodes and releases a shockwave (radius 20, damage 18 with
   falloff, strong outward throw).

Safety and balance: you and your allies are never pulled or hurt; only targets the server's PvP / team / protection rules let
you attack are affected (checked through Re:AcademyCraft's safety service). Bosses are pulled, hurt and thrown much less.
**No blocks are changed.** Death, logout or a dimension change end it with no shockwave.
Cost: **7000 CP and 300 overload** (level-5 players have about 12000 CP / 500 overload by default), cooldown **180 s**.
Every number is in `config/blackhole-common.toml` (created on first launch).

## Visuals – zero particles

Drawn entirely as custom 3D geometry on the client: a black sphere, a glowing photon ring that always faces you (a lensing
look), a tilted rotating accretion disk of concentric rings, spiralling infall streams, outward-sweeping gravitational-wave
rings, and on collapse an implosion flash plus an expanding shockwave shell with rings. Near the hole the screen edges darken
and the field of view narrows. The source contains no particle API at all (checked by search).

## Honest status

| Stage | State |
|---|---|
| Code written | Yes (server ability, network, client rendering, screen effect, config) |
| Syntax / API check | Done in a sandbox: parses cleanly, Re:AcademyCraft API types resolve. Minecraft/Forge classes were NOT available there |
| Compiled with Forge | **Not yet** – the GitHub build below is the first real compile |
| Tested in Minecraft | **No** |

If the GitHub build fails, copy the red error text and send it back; most likely a one-line fix to a vanilla name.
Known unknowns that only a real test can settle: how the visuals look and perform; whether a one-skill level-5 category can be
levelled up and learned smoothly in the Ability Developer; and whether the cost and pull strength feel right.

## Build it on GitHub (no Java needed)

1. Sign in at github.com. Click **+** (top right) → **New repository**. Name it `blackhole-addon`, choose **Private**, click **Create repository**.
2. Unzip `blackhole-addon.zip` on your PC. In the new repository click **uploading an existing file** (or **Add file → Upload files**)
   and drag in everything INSIDE the `blackhole-addon` folder (the `src`, `libs`, `gradle` folders and the files next to them). Click **Commit changes**.
3. Click the **Actions** tab. If it asks you to choose a workflow ("Get started with GitHub Actions"), click **set up a workflow yourself**,
   select everything in the editor, delete it, and paste the contents of `.github/workflows/build.yml` from the zip.
   Click **Commit changes…** twice. (Drag-and-drop often skips the hidden `.github` folder, which is why this step may be needed.)
4. A run starts by itself (or press **Run workflow**). The first build takes 10–15 minutes. When it shows a green tick, open the run,
   scroll to **Artifacts**, download **blackhole-mod**, and unzip it: the `.jar` inside is the mod (`blackhole-forge-1.20.1-0.1.0.jar`).
5. If the run is red, open it, click **build**, copy the red error text and send it back.

## Try it (use a separate test instance, not your live server)

1. Make a new Minecraft 1.20.1 instance with Forge 47.4.10 or newer.
2. Put two jars in its `mods` folder: the normal **Re:AcademyCraft 1.5.0** jar from CurseForge, and `blackhole-forge-1.20.1-0.1.0.jar`.
3. Start a creative world. Take the **Singularity Factor** from the Ingredients creative tab and use the Ability Developer machine (see
   the mod's tutorial) to get the category, learn **Black Hole**, and bind it in the skill preset editor.
4. Aim at an open area with mobs around and press the key. Press it again during the 8 s to collapse early.
   On a server, install the same jars on the server **and every client**.

## Files

* `BlackHole.java` – the ability (pull, event horizon, collapse); `BlackHoleMod.java` – registration; `Cfg.java` – all balance numbers
* `net/` – the small network messages; `client/` – the 3D rendering (`FxRenderer`, `Geo`), screen darkening, effect upkeep
* `.github/workflows/build.yml` – the GitHub build
