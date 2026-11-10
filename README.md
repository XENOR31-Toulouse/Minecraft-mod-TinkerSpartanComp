# Tinkers' Spartan (1.12.2)

An addon that links **Tinkers' Construct** and **Spartan Weaponry**.

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
- **Ammo** (`throwing/Ammo`), as in Spartan: a newly built tool holds **16 throwing knives**, **8 throwing axes**
  or **4 javelins** (the count shows in the name: "Iron Throwing Knife ×12"). Each throw uses one; a picked-up
  projectile goes back into **its** stack (identified by a UUID) and passes on the wear it took. If that wear
  exceeds the stack's remaining durability, only that projectile is lost, not the whole stack.

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
- **modifier overlays**: a dot in the modifier's colour on the head, one position per modifier (21
  modifiers, files `models/item/modifiers/*.json`).

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
