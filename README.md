# Meridian

**A complete enchanting overhaul for Minecraft 1.21.1 Fabric.**

Meridian replaces the vanilla enchanting table with a stat-driven system featuring five independent stats, 25+ themed shelf blocks, a two-tier enchantment library, salvage tomes, anvil upgrades, and 75 original enchantments — built on vanilla's data-driven `EnchantmentEffectComponents` with custom Java handlers where needed.

---

## Features

### Stat-Driven Enchanting Table

The vanilla enchanting table's single "power" value is replaced by five independent stats, each influencing a different aspect of the enchanting process:

| Stat | Effect |
|------|--------|
| **Eterna** | Maximum enchanting level (scales 0–50, replacing vanilla's 0–30 cap) |
| **Quanta** | Upper bound of the random power roll — higher means more variance |
| **Arcana** | Biases selection toward rarer, more obscure enchantments |
| **Rectification** | Reduces Quanta's negative variance for more consistent results |
| **Clues** | Reveals additional enchantments in the preview tooltip |

Stats are contributed by nearby shelf blocks and are fully data-driven — server operators can retune every block's contribution via datapack JSON without touching the jar.

### Shelf Blocks

25 themed shelves organized into progression tiers:

- **Starter** — Vanilla Bookshelves, Stoneshelf, Beeshelf, Melonshelf, Dormant Deepshelf
- **Early** — Hellshelf, Seashelf (Nether and Ocean materials)
- **Mid** — Infused and upgraded variants (Infused Hellshelf, Glowing Hellshelf, Blazing Hellshelf, Infused Seashelf, Crystal Seashelf, Heart Seashelf)
- **Late** — Deepshelf, Echoing Deepshelf, Soul-Touched Deepshelf, Echoing Sculkshelf, Soul-Touched Sculkshelf
- **End** — Endshelf, Pearl Endshelf, Draconic Endshelf (the only way to reach Eterna 50)
- **Utility** — Sightshelf (bonus Clues), Rectifier (bonus Rectification), Filtering Shelf (blacklist specific enchantments), Treasure Shelf (unlocks treasure enchantments like Mending)

Higher-tier shelves are crafted at the enchanting table itself using stat-gated recipes — building your shelf collection *is* the progression.

### Enchantment-Table Crafting

The enchanting table doubles as a stat-gated crafting station. When the table's stats meet a recipe's thresholds, the third enchant slot is replaced with the crafting result — click it to spend XP and craft the item. Recipes include:

- Tier-2 and tier-3 shelf upgrades
- Infused Breath (from Dragon's Breath — key material for end-tier shelves)
- Tome upgrades (Scrap → Improved Scrap → Extraction)
- Basic Library → Ender Library upgrade
- Budding Amethyst, Experience Bottles, Echo Shards, and more

### Enchantment Library

A two-tier storage block that pools enchanted books into a per-enchantment point bank, then dispenses them deterministically for XP:

- **Basic Library** — stores enchantments up to level 16
- **Ender Library** — stores enchantments up to level 31 (upgrade preserves stored books)

Points follow an exponential curve: `2^(level - 1)` per book deposited. The library tracks both accumulated points *and* the highest level ever deposited per enchantment — you can't grind thousands of Sharpness I books to pull a Sharpness V without first depositing at least one Sharpness V book.

Supports hopper automation for bulk book deposits. Extraction is menu-only.

### Salvage Tomes

Three tomes for moving enchantments between items, each with a different cost/reward tradeoff:

| Tome | Enchantments Recovered | Source Item | XP Cost |
|------|----------------------|-------------|---------|
| **Scrap Tome** | One (random) | Destroyed | 3 levels |
| **Improved Scrap Tome** | All | Destroyed | 5 levels |
| **Extraction Tome** | All | Preserved (takes durability damage) | 10 levels |

Tomes are used in the anvil: place the enchanted item on the left, the tome on the right.

### Anvil Upgrades

- **Prismatic Web** — Strips all curses from an item (30 levels, 1 web consumed). Non-curse enchantments are preserved.
- **Iron Block Repair** — Repairs a Damaged or Chipped Anvil by one tier (1 iron block consumed).

### 75 Enchantments

A roster of 75 original enchantments spanning combat, tools, mobility, mounts, and more. Implemented as data-driven JSON definitions with custom Java handlers where vanilla effect components aren't sufficient.

Highlights include:

- **Combat** — Siphon, Keen Edge, Tempo, Blight, Decay, Nightfall, Final Gambit, Cleave, Soul Tax
- **Ranged** — Detonation, Stormcall, Resonance, Gale Shot, Permafrost, Ricochet, True Flight
- **Tools** — Excavate (3x3 mining), Prospect (vein mining), Bounty, Furrow, Terrasculpt
- **Mobility** — Alacrity, Clamber, Vault, Slipstream, Cinderwalk, Diminish, Colossus
- **Utility** — Mason's Reach, Luminance, Abyss Ward, Premonition, Gravitas, Steadfast
- **Mounts** — Gallop, Trample, Skybound, Saddleguard
- **Elytra** — Ironwing, Impact Ward
- **Shield** — Retribution, Pummel, Fortify

### Warden Loot

Wardens drop Warden Tendrils (1 guaranteed, +10% per Looting level for a second), the key material for crafting Sculk-tier shelves.

### Integrations

First-class recipe and tooltip adapters ship at launch for:

- **EMI**, **REI**, and **JEI** — enchanting-table crafting recipes, library mechanics
- **Jade** — shelf stat tooltips, library contents

### Advancement Tree

18 advancements guide players through the progression, from picking up their first shelf to reaching Eterna 50.

### Per-Enchantment Overrides

Server operators can override `maxLevel`, `maxLootLevel`, `levelCap`, and `enabled` for any enchantment (vanilla or modded) via the config file. Disabling an enchantment removes it from the table, loot, and tooltips without deleting data from existing items. Changes sync to clients automatically.

---

## Installation

### Requirements

- Minecraft **1.21.1**
- Fabric Loader **0.16.10+**
- Fabric API **0.116.1+1.21.1** or newer
- Java **21**

### Setup

Drop the jar into the `mods/` directory on both server and client. The mod must be present on **both sides** — a client missing the mod will desync on the first table interaction.

Quilt users can run the mod via Quilted Fabric API with no changes.

---

## Configuration

The mod generates `config/meridian.json` on first launch with sensible defaults. Every value can be tuned without a restart using `/meridian reload`.

See the full annotated reference: **[Configuration Guide](docs/CONFIG.md)**

---

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/meridian reload` | 2 | Reload config from disk |

---

## Building from Source

```sh
./gradlew build          # produces build/libs/meridian-<version>.jar
./gradlew test           # runs unit tests
./gradlew runDatagen     # regenerates src/main/generated/
```

---

## Credits & Attribution

Meridian is a clean-room 1.21.1 Fabric rewrite. The enchanting module concepts (stat-driven table, shelf blocks, enchantment library, anvil interactions, and tome system) are inspired by [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) by Shadows_of_Fire and its Fabric port [Zenith](https://www.curseforge.com/minecraft/mc-mods/zenith) by bageldotjpg. All code is a fresh 1.21.1 rewrite — no source was copied. Zenith's stat schema, shelf roster, recipe shapes, and texture pipeline were the reference for the enchanting table subsystem.

All 75 enchantments are original to Meridian — names, IDs, weights, costs, effect definitions, and description text are authored fresh.

---

## License

- **Code:** MIT
- **Textures:** Sourced from Zenith (originally Apotheosis).
- **Enchantment data (75 of 75):** Original to Meridian.
