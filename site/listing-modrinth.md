# Meridian — Enchanting Overhaul

**_Chart your enchantments._**

![Meridian logo](https://raw.githubusercontent.com/rfizzle/meridian/master/art/logo.png)

**Also on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/meridian-enchanting-overhaul)
and [GitHub Releases](https://github.com/rfizzle/meridian/releases).**
Visit the [website](https://meridian.rfizzle.com) for the full feature
list, config reference, and command guide.

---

Meridian is a **ground-up rewrite of Minecraft's enchanting system** for
**Minecraft 1.21.1 (Fabric)**. It replaces the single-stat enchanting table
with a five-axis progression system, adds 113 original enchantments, introduces
25+ biome-themed shelf blocks, and gives you real tools for managing your
enchantment collection — libraries, salvage tomes, and anvil upgrades included.

**A full system, not a handful of tweaks.** Inspired by
[Apotheosis](https://modrinth.com/mod/apotheosis) and
[Zenith](https://modrinth.com/mod/zenith), rebuilt from scratch for modern
Fabric.

## At a glance

- Minecraft **1.21.1**, **Fabric** loader (0.16.10+), **Fabric API** required.
- Install on the **server** and every **client** — Meridian ships custom GUIs
  and themed enchanting particles.
- Every enchantment and feature is **individually configurable** through Mod
  Menu / Cloth Config or `config/meridian.json` — hot-reload with
  `/meridian reload`.
- MIT licensed.

## Features

### Five-Stat Enchanting

The vanilla enchanting table's single "bookshelf power" is gone. In its place, five independent stats — each contributed by the blocks you build around your table — control every aspect of the enchanting process.

| Stat | What it does |
|---|---|
| **Eterna** | Drives the maximum enchanting level — two levels per Eterna point, scaling up to 100, far beyond vanilla's 30 |
| **Quanta** | Increases the upper bound of randomness — higher risk, higher reward |
| **Arcana** | Biases selection toward rarer enchantments and increases the number of enchantments applied |
| **Rectification** | Tames Quanta's chaos, narrowing the variance for more predictable results |
| **Clues** | Reveals additional enchantments in the tooltip preview before you commit |

Building your enchanting setup is no longer about stacking 15 bookshelves. It's about choosing which stats to push and how far.

### 25+ Themed Shelf Blocks

Every shelf contributes a unique combination of stats, and they're organized into progression tiers themed around Minecraft's dimensions.

**Organic Shelves** — Early-game options crafted from renewable resources.
- Beeshelf, Melonshelf

**Nether Shelves** — Fire-themed shelves with escalating Eterna.
- Hellshelf → Infused Hellshelf → Glowing Hellshelf → Blazing Hellshelf

**Ocean Shelves** — Water-themed shelves that boost Arcana.
- Seashelf → Infused Seashelf → Crystalline Seashelf → Heart-Forged Seashelf

**End Shelves** — The highest Eterna values in the game. Draconic Endshelf is the only way to hit Eterna 50.
- Endshelf → Pearlescent Endshelf → Draconic Endshelf

**Deep Dark Shelves** — Sculk-infused shelves with unique particle effects. Sculk variants have a small chance of spawning a Shrieker.
- Dormant Deepshelf → Deepshelf → Echoing Deepshelf → Soul-Touched Deepshelf
- Echoing Sculkshelf, Soul-Touched Sculkshelf

**Utility Shelves** — Shelves that modify specific stats or unlock special behaviors.
- Hellshelf of Sight / Hellshelf of Masterful Sight — Boost Clues
- Shelf of Seabound / Hellbound / End-Fused Rectification — Boost Rectification
- Filtering Shelf — Blacklist specific enchantments from your table
- Deepshelf of Arcane Treasures — Unlock treasure-tier enchantments (like Mending) at the table

Each shelf tier displays themed enchanting particles — fire, water, end, and sculk.

### 113 Original Enchantments

Meridian adds 113 new enchantments across every equipment slot and playstyle. Every enchantment is fully data-driven and can be individually configured or disabled by server operators.

#### Combat
**Tempo** — Increases attack speed. **Keen Edge** — Chance to deal a burst of bonus damage. **Cleave** — Strikes hit nearby enemies in an arc. **Siphon** — Drains life from targets on hit. **Final Gambit** — Sacrifice your weapon for devastating burst damage. **Soul Tax** — Spend XP to amplify strikes. **Blight** — Poisons targets. **Decay** — Inflicts Wither. **Nightfall** — Blinds targets with Darkness.

#### Ranged
**Detonation** — Arrows explode on impact. **Stormcall** — Arrows summon lightning. **Resonance** — Arrows emit a damaging sonic shockwave. **Gale Shot** — Wind burst knockback on impact. **Permafrost** — Freezes water and chills targets. **Ricochet** — Arrows bounce off surfaces. **True Flight** — Flat trajectory, ignoring gravity.

#### Tools
**Excavate** — Mine a 3x3 area in one swing. **Prospect** — Break an ore and shatter the entire connected vein. **Bounty** — Harvesting crops reaps and replants nearby mature ones. **Furrow** — Tilling extends to surrounding blocks. **Terrasculpt** — Walking converts rough terrain into natural growth.

#### Mobility
**Alacrity** — Movement speed boost. **Vault** — Higher jumps. **Clamber** — Automatic step-up. **Slipstream** — Dolphin-speed swimming. **Cinderwalk** — Solidifies lava underfoot into temporary obsidian. **Diminish** — Shrink yourself with auto step-up. **Colossus** — Grow larger at the cost of speed.

#### Defense & Utility
**Abyss Ward** — Saves you from the void once per life. **Premonition** — Hostile mobs glow through walls. **Luminance** — Permanent night vision. **Gravitas** — Nearby items drift toward you. **Repulse** — Attackers get knocked back. **Rally** — Regeneration surge at critical health. **Bloodrage** — Taking damage fuels a frenzy. **Antidote** — Harmful potion effects expire faster. **Spellguard** — Reduces magic damage.

#### Mounts
**Gallop** — Faster mount speed. **Trample** — Mount damages creatures it runs through. **Skybound** — Higher mount jumps, less fall damage. **Saddleguard** — Reduced rider damage.

#### Elytra
**Ironwing** — Reduces all damage while gliding. **Impact Ward** — Reduces kinetic collision damage.

#### Shield
**Retribution** — Chance to reflect blocked damage. **Pummel** — Bonus damage while a shield is equipped. **Fortify** — Blocking costs less durability.

#### Specialty
**Tether** — Item stays in your inventory on death. **Snare** — Slain creatures may drop their spawn egg. **Plunder** — Double loot drops. **Aurify** — Transmute blocks into gold. **Beckon** — Farm animals are drawn to your held hoe. **Prismatic** — Sheared wool drops as a random color. **Renewal** — Sheep instantly regrow wool after shearing.

...and more, including dedicated Trident, Mace, and Curse enchantments.

### Enchanting Table Crafting

The enchanting table doubles as a **stat-gated crafting station**. When your table's stats meet the requirements, the third enchant slot transforms into a crafting recipe. Spend XP to craft items without leaving the table.

Craftable items include:
- **Shelf upgrades** — Infused Hellshelf, Infused Seashelf, Deepshelf, and more
- **Key materials** — Infused Breath (from Dragon's Breath), Echo Shards, Budding Amethyst
- **Tome progression** — Scrap Tome → Improved Scrap Tome → Extraction Tome
- **Library upgrades** — Basic Library → Library of Alexandria (preserves stored books)
- **Rare items** — Enchanted Golden Apple, Heart of the Sea, Totem of Undying, Golden Apple, Golden Carrot
- **XP conversion** — Honey to Experience Bottles (three tiers)

Recipes are viewable in EMI, REI, and JEI under the **"Infusions"** crafting category, with each stat requirement shown as a color-coded bar (hover for exact values). A companion **"Enchantments"** browser category lists every enchantment with its max level, exclusive sets, treasure flag, and per-level power windows.

### Enchantment Libraries

A dedicated storage system for pooling enchanted books. Drop books into the library and it tracks **per-enchantment point banks** with an exponential cost curve.

**Basic Library** — Stores enchantments up to level 16.

**Library of Alexandria** — Stores enchantments up to level 31. Upgrading preserves all stored enchantments.

The library tracks both accumulated points and the highest level you've ever deposited. You can't grind thousands of Sharpness I books to pull Sharpness V — you need to deposit at least one Sharpness V first. Hopper automation is supported for bulk deposits.

### Salvage Tomes

Three tiers of enchantment recovery, used in the anvil:

| Tome | Result | Item fate | XP Cost |
|---|---|---|---|
| **Scrap Tome** | One random enchantment | Destroyed | 3 levels |
| **Improved Scrap Tome** | All enchantments | Destroyed | 5 levels |
| **Extraction Tome** | All enchantments | Preserved (takes durability damage) | 10 levels |

All costs are configurable.

### XP Tomes

A ladder of consumables that bank experience levels for later. Right-click to deposit one level into the tome; shift-right-click to withdraw one back to yourself. A lime durability bar shows how full the tome is. Each tier is crafted at the enchanting table from a Dormant XP Tome, with higher tiers gated behind higher Eterna.

| Tome | Capacity |
|---|---|
| **XP Tome I** | 10 levels |
| **XP Tome II** | 30 levels |
| **XP Tome III** | 50 levels |

The Dormant XP Tome itself stores nothing — it's the crafting base for the active tiers.

### Anvil Upgrades

**Prismatic Web** — Place it in the anvil with a cursed item to strip all curses while keeping every other enchantment. Costs 30 XP levels (configurable).

**Iron Block Repair** — Place an Iron Block in the anvil with a damaged anvil to repair it by one tier (Damaged → Slightly Damaged → Undamaged).

### Warden Loot

Wardens now drop **Warden Tendrils** — a crafting material required for sculk-tier shelves. One guaranteed drop, plus a 10% bonus chance per Looting level.

### Progression

Meridian includes an advancement tree that guides you from your first shelf all the way to Apotheosis — reaching Eterna 50 with a Draconic Endshelf. Twenty-six advancements cover shelf crafting, library building, tome mastery, Warden hunting, and more.

## Commands

Operator commands: `/meridian reload` — reload `config/meridian.json` from disk,
rebuild enchantment info, and re-sync to all connected players (op level 2).
Full reference:
[meridian.rfizzle.com/commands.html](https://meridian.rfizzle.com/commands.html)

## Optional integrations

Meridian detects and integrates with these mods when present. **None are
bundled** — install whichever you already use.

- [Mod Menu](https://modrinth.com/mod/modmenu) — config screen entry
- [Cloth Config](https://modrinth.com/mod/cloth-config) — settings GUI
- [Jade](https://modrinth.com/mod/jade) / [WTHIT](https://modrinth.com/mod/wthit)
  — shelf stat tooltips and library contents via probe
- [EMI](https://modrinth.com/mod/emi) / [REI](https://modrinth.com/mod/rei) /
  [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) — enchanting table
  recipes and library mechanics under the "Infusions" category, plus an
  "Enchantments" browser (max level, exclusive sets, treasure flag, power windows)

## Requirements

- Minecraft **1.21.1**
- Fabric Loader **0.16.10+**
- **Fabric API** — Meridian will not load without it
- Java **21+**
- Works on **dedicated servers and singleplayer**

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for 1.21.1.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) into your `mods/`
   folder — Meridian requires it.
3. Download Meridian from this Modrinth page (via the Modrinth App, Prism
   Launcher's Modrinth tab, or a manual jar drop) and place it into `mods/`
   as well — on both server and client.
4. *(Optional)* Add Mod Menu and Cloth Config for the in-game settings screen.

Config generates at `config/meridian.json` on first launch.

## Links

- **Website:** <https://meridian.rfizzle.com>
- **GitHub Releases (canonical downloads):** <https://github.com/rfizzle/meridian/releases>
- **CurseForge:** <https://www.curseforge.com/minecraft/mc-mods/meridian-enchanting-overhaul>
- **GitHub:** <https://github.com/rfizzle/meridian>
- **Report an issue:** <https://github.com/rfizzle/meridian/issues>
- **Changelog:** <https://meridian.rfizzle.com/changelog.html>

## Companion mods

Meridian is part of [Concord](https://github.com/rfizzle/concord) — a
modular collection of system overhauls. Install any, combine all:

- [Mercantile](https://mercantile.rfizzle.com) — Every villager remembers.
- [Tribulation](https://tribulation.rfizzle.com) — Survive what comes next.
- [Prosperity](https://prosperity.rfizzle.com) — Every chest, yours to discover.

## License & credits

Licensed under the [MIT License](https://github.com/rfizzle/meridian/blob/master/LICENSE).
© 2026 rfizzle. Meridian is not affiliated with Mojang Studios or
Microsoft.
