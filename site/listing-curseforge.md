![Meridian Logo](https://raw.githubusercontent.com/rfizzle/meridian/master/art/logo.png)

# Meridian — A Complete Enchanting Overhaul

Meridian is a **ground-up rewrite of Minecraft's enchanting system** for Fabric 1.21.1. It replaces the single-stat enchanting table with a five-axis progression system, adds 75 original enchantments, introduces 25+ biome-themed shelf blocks, and gives you real tools for managing your enchantment collection — libraries, salvage tomes, and anvil upgrades included.

Inspired by [Apotheosis](https://www.curseforge.com/minecraft/mc-mods/apotheosis) and [Zenith](https://www.curseforge.com/minecraft/mc-mods/zenith), rebuilt from scratch for modern Fabric.

---

## Five-Stat Enchanting

The vanilla enchanting table's single "bookshelf power" is gone. In its place, five independent stats — each contributed by the blocks you build around your table — control every aspect of the enchanting process.

| Stat | What it does |
|---|---|
| **Eterna** | Sets the maximum enchanting level (scales up to 50, far beyond vanilla's 30) |
| **Quanta** | Increases the upper bound of randomness — higher risk, higher reward |
| **Arcana** | Biases selection toward rarer enchantments and increases the number of enchantments applied |
| **Rectification** | Tames Quanta's chaos, narrowing the variance for more predictable results |
| **Clues** | Reveals additional enchantments in the tooltip preview before you commit |

Building your enchanting setup is no longer about stacking 15 bookshelves. It's about choosing which stats to push and how far.

---

## 25+ Themed Shelf Blocks

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
- Treasure Shelf — Unlock treasure-tier enchantments (like Mending) at the table

Each shelf tier displays themed enchanting particles — fire, water, end, and sculk.

---

## 75 Original Enchantments

Meridian adds 75 new enchantments across every equipment slot and playstyle. Every enchantment is fully data-driven and can be individually configured or disabled by server operators.

### Combat
**Tempo** — Increases attack speed. **Keen Edge** — Chance to deal a burst of bonus damage. **Cleave** — Strikes hit nearby enemies in an arc. **Siphon** — Drains life from targets on hit. **Final Gambit** — Sacrifice your weapon for devastating burst damage. **Soul Tax** — Spend XP to amplify strikes. **Blight** — Poisons targets. **Decay** — Inflicts Wither. **Nightfall** — Blinds targets with Darkness.

### Ranged
**Detonation** — Arrows explode on impact. **Stormcall** — Arrows summon lightning. **Resonance** — Arrows emit a damaging sonic shockwave. **Gale Shot** — Wind burst knockback on impact. **Permafrost** — Freezes water and chills targets. **Ricochet** — Arrows bounce off surfaces. **True Flight** — Flat trajectory, ignoring gravity.

### Tools
**Excavate** — Mine a 3x3 area in one swing. **Prospect** — Break an ore and shatter the entire connected vein. **Bounty** — Harvesting crops reaps and replants nearby mature ones. **Furrow** — Tilling extends to surrounding blocks. **Terrasculpt** — Walking converts rough terrain into natural growth.

### Mobility
**Alacrity** — Movement speed boost. **Vault** — Higher jumps. **Clamber** — Automatic step-up. **Slipstream** — Dolphin-speed swimming. **Cinderwalk** — Solidifies lava underfoot into temporary obsidian. **Diminish** — Shrink yourself with auto step-up. **Colossus** — Grow larger at the cost of speed.

### Defense & Utility
**Abyss Ward** — Saves you from the void once per life. **Premonition** — Hostile mobs glow through walls. **Luminance** — Permanent night vision. **Gravitas** — Nearby items drift toward you. **Repulse** — Attackers get knocked back. **Rally** — Regeneration surge at critical health. **Bloodrage** — Taking damage fuels a frenzy. **Antidote** — Harmful potion effects expire faster. **Spellguard** — Reduces magic damage.

### Mounts
**Gallop** — Faster mount speed. **Trample** — Mount damages creatures it runs through. **Skybound** — Higher mount jumps, less fall damage. **Saddleguard** — Reduced rider damage.

### Elytra
**Ironwing** — Reduces all damage while gliding. **Impact Ward** — Reduces kinetic collision damage.

### Shield
**Retribution** — Chance to reflect blocked damage. **Pummel** — Bonus damage while a shield is equipped. **Fortify** — Blocking costs less durability.

### Specialty
**Tether** — Item stays in your inventory on death. **Snare** — Slain creatures may drop their spawn egg. **Plunder** — Double loot drops. **Aurify** — Transmute blocks into gold. **Beckon** — Farm animals are drawn to your held hoe. **Prismatic** — Sheared wool drops as a random color. **Renewal** — Sheep instantly regrow wool after shearing.

...and more, including dedicated Trident, Mace, and Curse enchantments.

---

## Enchanting Table Crafting

The enchanting table doubles as a **stat-gated crafting station**. When your table's stats meet the requirements, the third enchant slot transforms into a crafting recipe. Spend XP to craft items without leaving the table.

Craftable items include:
- **Shelf upgrades** — Infused Hellshelf, Infused Seashelf, Deepshelf, and more
- **Key materials** — Infused Breath (from Dragon's Breath), Echo Shards, Budding Amethyst
- **Tome progression** — Scrap Tome → Improved Scrap Tome → Extraction Tome
- **Library upgrades** — Basic Library → Ender Library (preserves stored books)
- **Rare items** — Enchanted Golden Apple, Heart of the Sea, Totem of Undying, Golden Apple, Golden Carrot
- **XP conversion** — Honey to Experience Bottles (three tiers)

Recipes are viewable in EMI, REI, and JEI under the "Infusions" category.

---

## Enchantment Libraries

A dedicated storage system for pooling enchanted books. Drop books into the library and it tracks **per-enchantment point banks** with an exponential cost curve.

### Basic Library
- Stores enchantments up to level 16.

### Ender Library
- Stores enchantments up to level 31. Upgrading preserves all stored enchantments.

The library tracks both accumulated points and the highest level you've ever deposited. You can't grind thousands of Sharpness I books to pull Sharpness V — you need to deposit at least one Sharpness V first. Hopper automation is supported for bulk deposits.

---

## Salvage Tomes

Three tiers of enchantment recovery, used in the anvil:

| Tome | Result | Item fate | XP Cost |
|---|---|---|---|
| **Scrap Tome** | One random enchantment | Destroyed | 3 levels |
| **Improved Scrap Tome** | All enchantments | Destroyed | 5 levels |
| **Extraction Tome** | All enchantments | Preserved (takes durability damage) | 10 levels |

All costs are configurable.

---

## Anvil Upgrades

**Prismatic Web** — Place it in the anvil with a cursed item to strip all curses while keeping every other enchantment. Costs 30 XP levels (configurable).

**Iron Block Repair** — Place an Iron Block in the anvil with a damaged anvil to repair it by one tier (Damaged → Slightly Damaged → Undamaged).

---

## Warden Loot

Wardens now drop **Warden Tendrils** — a crafting material required for sculk-tier shelves. One guaranteed drop, plus a 10% bonus chance per Looting level.

---

## Compatibility

Meridian ships with first-class integration for:
- **EMI / REI / JEI** — Enchanting table recipes and library mechanics
- **Jade / WTHIT** — Shelf stat tooltips and library contents via probe
- **ModMenu + Cloth Config** — In-game configuration screen
- **Trinkets** — Wearable item compatibility

---

## Configuration

Everything is tunable through `config/meridian.json`:

- Stat caps and enchanting behavior
- Per-enchantment overrides (max level, loot level, enable/disable)
- Tome XP costs and durability damage
- Warden drop rates
- Sculk shelf shrieker/particle chances
- Anvil feature toggles
- Display and tooltip settings

Use `/meridian reload` (op level 2) to apply changes without restarting the server. Config syncs automatically to all connected clients.

---

## Progression

Meridian includes an advancement tree that guides you from your first shelf all the way to Apotheosis — reaching Eterna 50 with a Draconic Endshelf. Eighteen advancements cover shelf crafting, library building, tome mastery, Warden hunting, and more.

---

## Links

- [Documentation](https://meridian.rfizzle.com) — Full enchantment list, shelf stats, commands, and config reference
- [Source Code](https://github.com/rfizzle/meridian) — MIT licensed

---

## Screenshots

> Screenshots coming soon — see below for what to capture.

<!-- SCREENSHOT CHECKLIST (remove this section after adding images)

Recommended screenshots, roughly in this order:

1. **Enchanting Table UI** — Show the overhauled GUI with all five stat bars
   visible (Eterna, Quanta, Arcana, Rectification, Clues). Include a tool in
   the slot with enchantment clues showing. This is the single most important
   screenshot — it immediately communicates what the mod does.

2. **Full Shelf Setup** — A top-down or angled shot of a well-built enchanting
   room with mixed shelf types. Ideally show at least three tiers (e.g., Nether,
   Ocean, and End shelves together) so the visual variety is obvious. Themed
   particles should be visible.

3. **Nether Shelf Progression** — A focused shot showing the four Hellshelf
   tiers side by side (Hellshelf → Infused → Glowing → Blazing) with their
   fire-themed particles. Good for showing the upgrade path.

4. **End Shelf Setup** — A dramatic shot of Draconic Endshelves around a table,
   ideally in an End City or purpur-themed build. End particles visible.

5. **Sculk Shelf Setup** — Echoing and Soul-Touched Sculkshelves around a
   table in a Deep Dark setting. Sculk particles and the ambient Shrieker
   risk make this visually compelling.

6. **Enchanting Table Crafting** — The infusion UI in action, showing the third
   slot recipe (e.g., crafting an Infused Hellshelf or Extraction Tome). Include
   the XP cost display.

7. **Enchantment Library** — Both Basic and Ender Library blocks placed, with
   the library GUI open showing stored enchantments and their point banks.

8. **Tome Usage** — An anvil screen showing an Extraction Tome being applied to
   a fully-enchanted tool, with the result preview visible.

9. **Prismatic Web** — Anvil screen showing curse removal from a cursed item.

10. **Enchantments in Action** — 2-3 action shots showing flashy enchantments:
    - Excavate (3x3 mining)
    - Detonation or Stormcall (explosive/lightning arrows)
    - Cinderwalk (obsidian forming over lava)

11. **EMI/REI/JEI Integration** — The "Infusions" recipe category showing
    available enchanting table recipes.

12. **Advancement Tree** — The full Meridian advancement tab in the
    advancements screen.

Tips:
- Use a shader pack for dramatic lighting (BSL, Complementary, etc.)
- 1920x1080 minimum resolution
- F1 to hide HUD for environmental shots, keep HUD for UI shots
- Nighttime or underground shots work well for particle visibility
- Consider a short GIF or video for Cinderwalk, Detonation, or Stormcall

-->
