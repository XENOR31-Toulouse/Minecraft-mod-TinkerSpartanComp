# Tinkers' Spartan (1.12.2)

An addon that links **Tinkers' Construct** and **Spartan Weaponry**.

Built and tested against **[Tinkers' Antique](https://www.curseforge.com/minecraft/mc-mods/tinkers-antique)**
(the maintained 1.12.2 fork of Tinkers' Construct: same mod id `tconstruct`, same API), with Mantle and
Spartan Weaponry 1.6.1. The API it uses is the same in the original Tinkers' Construct 2.13.

## Weapons: built only from existing Tinkers parts

The Tool Station matches parts **in slot order** and builds the first tool that fits, so every combination
below is unique (among these weapons and against Tinkers' own tools).

| Weapon | Handle | Head | Guard / binding | Where | Spartan properties |
|---|---|---|---|---|---|
| Dagger | tool rod | knife blade | crossguard | Station | backstab, **throwable** |
| Parrying dagger | tool rod | knife blade | hand guard | Station | **parry** (right-click) |
| Katana | tool rod | sword blade | binding | Station | two-handed I, sweep, chest bonus |
| Saber | tool rod | sword blade | tough binding | Station | damage absorb 25%, chest bonus, sweep |
| Spear | tough rod | knife blade | binding | Station | reach |
| Mace | tool rod | hammer head | binding | Station | +50% vs undead |
| Quarterstaff | tough rod | tough rod | binding | Station | two-handed I, 50% sweep |
| Throwing knife | tool rod | knife blade | — | Station | **thrown** ×2 |
| Throwing axe | tool rod | axe head | — | Station | **thrown** ×2 |
| Javelin | tough rod | arrow head | — | Station | **thrown** ×3 |
| Boomerang | tool rod | kama head | — | Station | **thrown**, comes back |
| Pike | tough rod | sword blade | tough binding | Forge | two-handed I, reach II |
| Lance | tough rod | sword blade | large plate | Forge | reach, riding bonus |
| Glaive | tough rod | large sword blade | tough binding | Forge | two-handed I, reach, 50% sweep |
| Greatsword | tough rod | large sword blade | wide guard | Forge | two-handed II, reach, 100% sweep |
| Halberd | tough rod | axe head | tough binding | Forge | two-handed II, reach, shield breach |
| Battleaxe | tough rod | broad axe head | tough binding | Forge | two-handed I, **versatile** (axe) |
| Warhammer | tough rod | pick head | tough binding | Forge | two-handed I, 50% armour piercing |
| Battle hammer | tough rod | hammer head | tough binding | Forge | knockback, nausea |
| War scythe | tough rod | scythe head | binding | Forge | two-handed II, wide sweep, head bonus |

Longsword and rapier are left out: Tinkers already has them. So are bows and crossbows (Tinkers has its own).

### How it works
- The tools implement Spartan's `IWeaponPropertyContainer` and `IBlockingWeapon`: **Spartan itself**
  applies reach, melee bonus/penalty damage and parrying.
- Sweep, armour piercing, shield breach and property callbacks are handled in `SpartanTool`.
- **Throwing** (hold right-click, release): the projectiles subclass Spartan's (`throwing/ThrownTools`),
  reusing its flight, rendering, pickup and boomerang return. On impact, the Tinkers logic applies (traits,
  durability: the weapon becomes "broken" instead of disappearing), plus Spartan's bonuses.
- **Ammo** (`throwing/Ammo`), computed **like Tinkers arrows and shuriken** (`ProjectileCore`): the
  tool's durability is the ammo pool, and count = durability / cost per projectile (12 for the knife,
  26 for the axe, 58 for the javelin). Since durability comes from the parts' materials (head, handle),
  so does the projectile count: **iron → 15 knives / 8 axes / 4 javelins**, flint on wood → 11 knives,
  cobalt → 53 knives. The count shows in the name ("Iron Throwing Knife ×12"), the tooltip and the
  durability bar. Each throw uses one; at 0 the tool stays in the inventory as "empty". A picked-up
  projectile gives its ammo back to an **identical stack** (same materials and modifiers, as with Tinkers
  arrows). Each impact wears the projectile itself: after enough hits, it is lost.

## Tinkers book (Materials and You)
A **"Spartan Arsenal"** section is added to the Tinkers book (English and French):
- an introduction;
- two glossary pages for the Spartan properties;
- a throwing and ammo page and a smeltery page;
- one page per weapon: the tool and its parts laid out as in the Tool Station, a description and its properties.

The files live in `assets/tconspartan/book/`: `index.json`, `sections/spartan.json`, and texts under `<lang>/spartan/`.
`client/SpartanBook` registers them with Tinkers and builds the table of contents. To check that every page
loads without opening the book: `./gradlew runClient -PcheckBook` (results in the log, lines `[book]`).

**Names, descriptions and book texts** all come from one table, `scripts/gen_content.py` (English + French):
edit it, then run `python scripts/gen_content.py .` to regenerate `lang/*.lang` and the book pages.

## Smeltery
- Spartan metal weapons melt down; the amount of metal comes from their crafting recipe.
- **Wear**: a damaged weapon returns metal in proportion to its remaining durability (rounded to the nugget,
  minimum 1 nugget). JEI still shows the amount for a new weapon.

## Textures (generated at runtime, nothing copied from Spartan)
`client/SpartanTextures` reads the `spartanweaponry:items/custom/<weapon>_layer_N` layers and splits them into:
- **handle** (converted to greyscale);
- **head**;
- **guard/binding**: its own layer, coloured by its material. The pixels are chosen by the `guard(...)`,
  `plate(...)` and `handleBelow(...)` parameters in `SpartanWeapon`, in a frame where t = x - y runs along the
  weapon and u = x + y - (size - 1) is the offset from the axis;
- **modifier art** (`client/ModifierArt`): an original design, drawn from each weapon's **actual shape**
  (the weapons differ in resolution: 16, 32 or 64 px, and in silhouette). The code finds the cutting edge,
  the fuller, the tip, the guard, the pommel and the grip, and each modifier has its own spot, so several
  combine cleanly:

  | Where | Modifiers |
  |---|---|
  | Cutting edge | diamond (set with diamond), sharpness (honed quartz edge), fiery (incandescent from base to tip), beheading (ender-tempered tip), fortify, fins |
  | Fuller | haste (redstone rubies), smite (gilded fuller), necrotic (blood veins), shulking (pearls) |
  | Guard | emerald (cut gem), soulbound (nether star) |
  | Pommel | luck (lapis set in gold), blasting |
  | Grip / handle | silk touch (silk-wrapped grip), knockback (brass rings) |
  | Blade base | reinforced (obsidian plates), mending moss, webbed (thread mesh) |
  | Around | glowing (sparkles), bane of arthropods (venom drops) |

  Rules: never paint outside the silhouette, hand-picked 4-tone palettes, shading that follows the original
  pixel's brightness, gem sizes that scale with resolution, 1-pixel lines.

### Hand-drawn textures (for artists)
- Any PNG at `assets/tconspartan/textures/items/<weapon>/<layer>.png` (in the mod **or in a resource pack**)
  **replaces** the generated texture. Layers: `handle`, `head`, `extra`, `mod_<modifier>`. The parts must stay
  in greyscale (Tinkers colours them per material); the `mod_*` overlays are in colour.
- To start from the generated textures: `./gradlew runClient -PdumpTextures` writes them all to
  `run/tconspartan-textures/<weapon>/`. Retouch them with any pixel-art tool (Aseprite, LibreSprite, Krita...),
  then drop them into `src/main/resources/assets/tconspartan/textures/items/` or into a resource pack.

## Development

Requirements: JDK 8 (to compile). Gradle downloads a JDK 25 for itself (RetroFuturaGradle 2.x).

```bash
./gradlew setupDecompWorkspace   # first time only
./gradlew runClient              # copies Spartan into run/mods (coremod), then launches the game
./gradlew build                  # -> build/libs/tconspartan-<version>.jar
```

Adding a weapon: one line in `SpartanWeapon.java` (unique part combination), its entry in
`scripts/gen_models.py`, then `python scripts/gen_models.py <Spartan jar>` and the lang entries.

## Layout
```
src/main/java/tconspartan/
  TConSpartan.java              @Mod, init phases
  ModTools.java                 registers tools and projectiles
  tools/SpartanWeapon.java      weapon table (parts, stats, properties, texture split)
  tools/WeaponDef.java          description builder
  tools/TinkerParts.java        shortcuts to Tinkers parts
  tools/SpartanTool.java        Tinkers tool + Spartan weapon (sweep, throwing, parrying...)
  tools/AttackContext.java      gives Spartan the tool's real damage
  throwing/                     throw types and projectiles
  smeltery/SpartanMelting.java  Spartan weapon melting (+ wear)
  client/SpartanTextures.java   generated sprites (split, modifiers)
  proxy/                        Tool Station GUI, models, renderers
```
