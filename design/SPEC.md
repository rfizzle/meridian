# Meridian — Feature Spec

Minecraft 1.21.1 Fabric mod. Enchanting overhaul.

**Asset philosophy:** Custom pixel-art textures for mod-specific visuals (authored through Concord's glyph pipeline — `/glyph`, the `mc-textures` skill, concord `design/DESIGN-SYSTEM.md` §8 — with `.glyph` sources kept beside the masters; the XP-tome item ladder ships its `.glyph` sources under `art/tome/`). The shelf and block textures are likewise original pixel art authored through the glyph pipeline — no asset is Zenith/Apotheosis-derived. Sounds stay **fully vanilla** — Meridian ships no custom `SoundEvent`s and no `sounds.json`; every cue (enchanting hum, anvil use, sculk bloom, library interaction) maps to a vanilla `SoundEvent`. The audio direction — why the soundscape stays vanilla — lives in [`DESIGN.md`](DESIGN.md). Custom particles are used for mod-specific table ambiance: four element-themed enchant-glyph particle families (`enchant_fire`, `enchant_water`, `enchant_sculk`, `enchant_end`) whose textures are per-letter Standard Galactic Alphabet rune sprites (`sga_a_*` … `sga_z_*`) drawn in element palettes, plus vanilla `ParticleTypes.ENCHANT` / `SCULK_SOUL` where vanilla already reads correctly.

---

## 1. Stat-Driven Enchanting Table

Meridian replaces vanilla's single bookshelf "power" value with five independent stats gathered from surrounding shelf blocks. The vanilla `EnchantingTableBlock` menu provider is replaced (via `EnchantmentTableBlockMixin` on `getMenuProvider()`) with `MeridianEnchantmentMenu`; the block itself is unchanged.

### The Five Stats

| Stat | Type | Range after aggregation | Role |
|---|---|---|---|
| **Eterna** | float | floored at 0, capped per shelf tier | Drives the XP-level cost of each enchant slot. Slot 2 cost = `round(eterna * 2)`, capped at `enchantingTable.maxEterna` (default 50, so a level cap of 100). Replaces vanilla's 0–30 level range with 0–100 |
| **Quanta** | float | clamped [0, 100] | Upper bound of the random power roll. Higher Quanta = more variance |
| **Arcana** | float | clamped [0, 100] | Biases enchantment selection toward rarer enchantments via weighted rarity buckets |
| **Rectification** | float | clamped [0, 100] | Truncates Quanta's negative variance. At 100 the power factor is always ≥ 0 |
| **Clues** | int | clamped [0, 3] | Number of enchantments revealed in each slot's preview tooltip |

Two additional pieces of context travel with the stats but are not "stats":
- **`blacklist`** — `Set<ResourceKey<Enchantment>>`, the union of all `BlacklistSource` block entities in range. Enchantments in this set never roll.
- **`treasureAllowed`** — boolean, true if any `TreasureFlagSource` block entity is in range. Unlocks treasure-tagged enchantments (Mending, Frost Walker, Soul Speed).

These eight fields are carried by the `StatCollection` record (`com.rfizzle.meridian.api.StatCollection`), whose `EMPTY` constant is `(0, 0, 0, 0, 0, 0, {}, false)`.

### Shelf Scan

`EnchantingStatRegistry.gatherStats(level, tablePos)` iterates the **15 fixed offsets** from vanilla's `EnchantingTableBlock.BOOKSHELF_OFFSETS` — there is no configurable radius. For each offset:

1. A **line-of-sight check** mirrors vanilla `isValidBookShelf`: the midpoint block (`offset.x / 2`, `offset.y`, `offset.z / 2`, integer division) must be in `BlockTags.ENCHANTMENT_POWER_TRANSMITTER`. If the transmitter slot is obstructed, that shelf contributes **zero** stats and no blacklist/treasure context.
2. Stats are resolved by `IEnchantingStatProvider.getStats()` if the block implements the interface, otherwise by datapack lookup (`enchanting_stats/*.json`).
3. Block entities at the offset are queried for `BlacklistSource` (merged) and `TreasureFlagSource` (OR'd).

**Eterna aggregation is a step-ladder** (not a simple sum): shelves are grouped by their `maxEterna` value, the groups are processed in ascending `maxEterna` order, each group's eterna is added to a running total, and the running total is capped at that group's `maxEterna` before moving on. Groups with `maxEterna ≤ 0` add their eterna directly with no cap. The result is floored at 0. Quanta, arcana, rectification, and clues are summed then clamped to their ranges.

**Vanilla fallback:** A vanilla enchantment-power block with no datapack entry contributes `VANILLA_FALLBACK = (maxEterna 15, eterna 1, quanta 0, arcana 0, rectification 0, clues 0)`. The `#minecraft:enchantment_power_provider` tag carries the same `(maxEterna 15, eterna 1)` entry in data.

### Enchantment Selection Math

Selection lives in `RealEnchantmentHelper`. The constants below are exact.

**Per-slot XP cost** (`getCost`, three slots indexed 0–2):
```
clamped  = clamp(eterna, 0, enchantingTable.maxEterna)   // default cap 50
level    = round(clamped * 2)                             // LEVELS_PER_ETERNA, default cap 100
slot 2   = level                                          // deterministic
slot 1   = max(1, round(level * uniform(0.6, 0.8)))       // 60–80% of slot 2
slot 0   = max(1, round(level * uniform(0.2, 0.4)))       // 20–40% of slot 2
```

**Power roll** (`getQuantaFactor` + power application):
```
g = clamp(nextGaussian() / 3.0, -1, 1)
if g < (rectification / 100) - 1:
    g = uniform((rectification / 100) - 1, 1)             // rectification truncates the low tail
quantaFactor   = quanta * g / 100
effectivePower = clamp(round(level * (1 + quantaFactor)), 1, 200)
```

**Candidate pool:** an enchantment is eligible when it is in `EnchantmentTags.IN_ENCHANTING_TABLE` (or `EnchantmentTags.TREASURE` when `treasureAllowed`), not in the blacklist, applicable to the item (or the item is a Book), and `enabled` in config. For each candidate the highest level whose `[minPower, maxPower]` window contains `effectivePower` is taken.

**Arcana rarity picking:** the arcana value maps to an `Arcana` tier by threshold ladder (`EMPTY` 0, `LITTLE` 10, `FEW` 20, `SOME` 30, `LESS` 40, `MEDIUM` 50, `MORE` 60, `VALUE` 70, `EXTRA` 80, `ALMOST` 90, `MAX` 99). Each tier supplies four weights for the rarity buckets `[common, uncommon, rare, very_rare]` — e.g. `EMPTY = [10, 5, 2, 1]` (favors common), `MAX = [1, 2, 5, 10]` (favors rare). At arcana thresholds **0, 33, 66** a guaranteed pick runs; then a probabilistic loop continues `while rand.nextInt(max(50, round(scaledLevel * 1.15))) <= scaledLevel`, picking one enchantment, removing all enchantments incompatible with it, and halving `scaledLevel` each iteration.

**`EnchantableItem` hook:** if the input item implements `com.rfizzle.meridian.api.EnchantableItem`, its `selectEnchantments(...)` post-processes the built list.

**Clue building:** one enchantment from the slot's selection pool is the "primary" (the one that will apply). When `clues > 0`, the primary plus additional random picks (up to `clues` total) are sent to the client. If the pool empties mid-draw, an `exhaustedList` flag tells the client no more clues are possible at higher stats.

### Menu / GUI

`MeridianEnchantmentMenu` has slot 0 (input item or book), slot 1 (lapis), and three vanilla read-only preview slots. On `slotsChanged()` it gathers stats at the table position and applies **baseline bonuses** before computing previews:
- `quanta += 15` (then clamp to [0, 100])
- `arcana += itemEnchantmentValue / 2.0` (then clamp)
- `clues += 1` (then clamp to [0, 3])

These baselines let recipe and selection thresholds ported from Zenith stay reachable. Eterna, rectification, blacklist, and treasure are untouched.

Click-time validation (`MeridianEnchantmentLogic`): the button id must be in [0, 3), the input slot non-empty, the cost > 0, lapis count ≥ `buttonId + 1`, and player XP level ≥ `max(buttonId + 1, cost)` — all waived for players with infinite materials (creative). On success: lapis shrinks by `buttonId + 1`, the player loses `cost` XP levels, and the enchanted item replaces the input. `globalMinEnchantability` (default 1) is the minimum enchantability the input needs to be enchantable.

Stats and clues stream to the client via `StatsPayload` (eterna/quanta/arcana/rectification/clues/maxEterna/blacklist/treasure + projected crafting result) and per-slot `CluesPayload`. The client `MeridianEnchantmentScreen` and `EnchantingInfoScreen` render the stat readout and clue list.

### Implementation Notes
- The menu provider swap is the only mixin touching the enchanting table block; the block, blockstate, and model stay vanilla.
- Eterna's step-ladder is the reason higher-tier shelves carry a high `maxEterna` and modest `eterna`: a tier raises the ceiling, and the eterna fills toward it.
- `enchantingTable.maxEterna` is validated to [1, 100]; `globalMinEnchantability` to [0, 100].

---

## 2. Shelf Blocks

25 themed shelf blocks plus two special blacklist/treasure shelves and two library blocks, all registered through `MeridianShelves` / `MeridianRegistry`. Stat contributions are datapack-driven (`data/meridian/enchanting_stats/<id>.json`), so operators retune any block without touching the jar — applied with the vanilla `/reload`, not `/meridian reload`. Values below are `(maxEterna, eterna, quanta, arcana, rectification, clues)`; omitted axes are 0.

### Roster and Stat Contributions

**Starter (wood / stone tier)**
| Block ID | maxEterna | eterna | quanta | arcana | rect | clues | Notes |
|---|---|---|---|---|---|---|---|
| (vanilla `bookshelf`) | 15 | 1 | — | — | — | — | via `#minecraft:enchantment_power_provider` |
| `meridian:stoneshelf` | 0 | -1.5 | 0 | -7.5 | — | — | |
| `meridian:beeshelf` | 0 | -15 | 100 | 0 | — | — | extreme Quanta, negative eterna |
| `meridian:melonshelf` | 0 | -1 | -10 | 0 | — | — | Quanta antidote |
| `meridian:dormant_deepshelf` | 15 | 1 | 0 | 0 | — | — | crafts into `deepshelf` |

**Early (Nether / Ocean)**
| Block ID | maxEterna | eterna | quanta | arcana | rect | clues |
|---|---|---|---|---|---|---|
| `meridian:hellshelf` | 22.5 | 1.5 | 3 | 0 | — | — |
| `meridian:seashelf` | 22.5 | 1.5 | 0 | 2 | — | — |

**Mid (infused / upgraded Nether & Ocean)**
| Block ID | maxEterna | eterna | quanta | arcana | rect | clues |
|---|---|---|---|---|---|---|
| `meridian:infused_hellshelf` | 27 | 1.75 | 1.75 | 0 | — | — |
| `meridian:glowing_hellshelf` | 30 | 2 | 2 | 4 | — | — |
| `meridian:blazing_hellshelf` | 30 | 4 | 5 | 0 | — | -1 |
| `meridian:infused_seashelf` | 27 | 1.75 | 0 | 1.75 | — | — |
| `meridian:crystal_seashelf` | 30 | 2 | 4 | 2 | — | — |
| `meridian:heart_seashelf` | 30 | 3 | 0 | 10 | -5 | — |

**Late (Deep Dark / Sculk)**
| Block ID | maxEterna | eterna | quanta | arcana | rect | clues |
|---|---|---|---|---|---|---|
| `meridian:deepshelf` | 35 | 2.5 | 5 | 5 | — | — |
| `meridian:echoing_deepshelf` | 37.5 | 2.5 | 0 | 15 | — | — |
| `meridian:soul_touched_deepshelf` | 37.5 | 2.5 | 15 | 0 | — | — |
| `meridian:echoing_sculkshelf` | 40 | 5 | 5 | 15 | — | 1 |
| `meridian:soul_touched_sculkshelf` | 40 | 5 | 15 | 5 | 5 | — |

**End**
| Block ID | maxEterna | eterna | quanta | arcana | rect | clues |
|---|---|---|---|---|---|---|
| `meridian:endshelf` | 45 | 2.5 | 5 | 5 | — | — |
| `meridian:pearl_endshelf` | 45 | 5 | 7.5 | 7.5 | — | — |
| `meridian:draconic_endshelf` | 50 | 10 | 0 | 0 | — | — |

The Draconic Endshelf's `maxEterna 50` is the only way to reach Eterna 50.

**Utility — Clues (`sightshelf` family)**
| Block ID | clues | Other stats |
|---|---|---|
| `meridian:sightshelf` | 1 | none |
| `meridian:sightshelf_t2` | 2 | none |

**Utility — Rectification (`rectifier` family)**
| Block ID | rectification | Other stats |
|---|---|---|
| `meridian:rectifier` | 10 | none |
| `meridian:rectifier_t2` | 15 | none |
| `meridian:rectifier_t3` | 25 | none |

Sightshelves and rectifiers grant no Eterna/Quanta/Arcana and are deliberately **excluded from the `c:bookshelves` tag** — they are pure modifiers, not power sources.

**Misc data-driven providers** (not Meridian blocks, contributed via `enchanting_stats`):
- `#meridian:basic_skulls` tag → quanta 5
- `minecraft:wither_skeleton_skull` / `..._wall_skull` → quanta 10
- `minecraft:amethyst_cluster` → rectification 1.5

### Special Shelves

**Filtering Shelf (`meridian:filtering_shelf`)** — `FilteringShelfBlock extends ChiseledBookShelfBlock implements IEnchantingStatProvider`, BE `FilteringShelfBlockEntity implements Container, BlacklistSource`. A 6-slot chiseled-bookshelf-style block: right-clicking a face maps the hit position to a slot index via `ShelfSlotMapping.computeSlot(horizontal, dy)` (top row slots 0–2, bottom row 3–5). Only **single-enchantment enchanted books** may be inserted (rejected at `canPlaceItem`). Each stored book's enchantment is added to the blacklist returned by `getEnchantmentBlacklist()`, suppressing those enchantments from the table's roll. Base stats are `(maxEterna 15, eterna 1)`; each stored book adds **+0.5 eterna and +1.0 arcana** (up to 6 books → +3.0 eterna, +6.0 arcana).

**Treasure Shelf (`meridian:treasure_shelf`)** — `TreasureShelfBlock extends Block implements EntityBlock, IEnchantingStatProvider`, BE `TreasureShelfBlockEntity implements TreasureFlagSource` (pure marker, no methods). Contributes no stats; its presence in range flips `treasureAllowed`, unlocking treasure enchantments. The FAQ documents the `allowTreasureWithoutShelf` config escape hatch.

**Sculk Shelves (`echoing_sculkshelf`, `soul_touched_sculkshelf`)** — `SculkShelfBlock extends EnchantingShelfBlock`, random-ticking. On `animateTick`, with probability `shelves.sculkShelfShriekerChance` (default 0.02) it plays `SoundEvents.SCULK_CATALYST_BLOOM` (volume 2.0, pitch 0.6–1.0); with probability `shelves.sculkParticleChance` (default 0.05) it spawns `ParticleTypes.SCULK_SOUL` above the block.

### Particle Themes

`ParticleTheme` assigns each shelf family a table-ambiance particle: `ENCHANT` (vanilla), `ENCHANT_FIRE`, `ENCHANT_WATER`, `ENCHANT_END`, `ENCHANT_SCULK` (custom `ModParticles`). Nether shelves use fire, ocean shelves water, end shelves end, deep/sculk shelves sculk. `IEnchantingStatProvider.spawnTableParticle()` emits these from the shelf toward the table.

### Implementation Notes
- Most shelves are plain `EnchantingShelfBlock`; only sculk, filtering, and treasure shelves have custom classes/BEs.
- Material/strength varies by tier (wood 0.75, stone baseline 1.75, deep 2.5, sculk 3.5, end 4.5–5.0); stone+ shelves require the correct tool to drop.
- Shelf textures are original pixel art authored via `/glyph`, not Zenith/Apotheosis-derived.

---

## 3. Enchanting-Table Crafting

The enchanting table doubles as a stat-gated crafting station. When the table's gathered stats satisfy a recipe whose ingredient matches the input slot, the third preview slot (`CRAFTING_SLOT = 2`) shows the crafted result instead of an enchantment; clicking it spends 3 lapis + the recipe's XP cost and produces the result.

### Recipe Types
Two recipe types register through `EnchantingRecipeRegistry`:
- **`meridian:enchanting`** (`EnchantingRecipe`) — standard stat-gated transmutation.
- **`meridian:keep_nbt_enchanting`** (`KeepNbtEnchantingRecipe`) — copies the input's `DataComponents.ENCHANTMENTS` onto the result (used for the library upgrade so stored books survive).

### Recipe Schema
```json
{
  "type": "meridian:enchanting",
  "input": { "item": "minecraft:dragon_breath" },
  "requirements":     { "eterna": 40, "quanta": 15, "arcana": 60 },
  "max_requirements": { "eterna": -1, "quanta": 25, "arcana": -1 },
  "display_level": 5,
  "xp_cost": 0,
  "result": { "id": "meridian:infused_breath", "count": 3 }
}
```
- `requirements` — minimum eterna/quanta/arcana (each optional, default 0).
- `max_requirements` — optional upper bounds; `-1` on an axis means unbounded (`StatRequirements.NO_MAX`). A recipe matches only when stats fall in `[min, max]` on every axis.
- `display_level` — optional UI level badge.
- `xp_cost` — optional; when 0 the effective cost is `max(1, round(requirements.eterna))`.
- `result` — output stack (count default 1).

`EnchantingRecipeRegistry.findMatch` sorts candidate recipes by `requirements.eterna()` descending and returns the **first** whose ingredient and stat window both match, so the hardest-to-reach recipe wins ties.

### Shipped Recipes
`data/meridian/recipe/enchanting/`:

| Recipe | Input → Result | eterna / quanta / arcana (min) | Notes |
|---|---|---|---|
| `deepshelf` | dormant_deepshelf → deepshelf | 30 / 40 / 40 | |
| `infused_hellshelf` | hellshelf → infused_hellshelf | 22.5 / 30 / 0 | display 5 |
| `infused_seashelf` | seashelf → infused_seashelf | 22.5 / 15 / 10 | display 5 |
| `ender_library` | library → ender_library | 50 / 45 / 100 (max 50/50/100) | keep_nbt — preserves books |
| `infused_breath` | dragon_breath → infused_breath ×3 | 40 / 15 / 60 (quanta max 25) | end-tier material |
| `budding_amethyst` | amethyst_block → budding_amethyst | 30 / 30 / 50 (quanta max 50) | |
| `improved_scrap_tome` | scrap_tome → improved_scrap_tome ×4 | 22.5 / 25 / 35 (quanta max 50) | |
| `extraction_tome` | improved_scrap_tome → extraction_tome ×4 | 30 / 25 / 45 (quanta max 75) | |
| `xp_tome_t1` | dormant_xp_tome → xp_tome_t1 | 10 / 25 / 25 | |
| `xp_tome_t2` | dormant_xp_tome → xp_tome_t2 | 30 / 25 / 25 | |
| `xp_tome_t3` | dormant_xp_tome → xp_tome_t3 | 50 / 25 / 25 | |
| `golden_apple` | apple → golden_apple | 25 / 20 / 15 (quanta max 40) | |
| `enchanted_golden_apple` | golden_apple → enchanted_golden_apple | 45 / 40 / 75 | |
| `golden_carrot` | carrot → golden_carrot | 10 / 10 / 0 (quanta max 30) | |
| `honey_to_xp_t1/t2/t3` | honey_bottle → experience_bottle | 10 / 25 / 25 (t1) + higher tiers | |
| `echo_shard_duplication` | echo_shard → echo_shard ×4 | 35 / 50 / 50 | |
| `heart_of_the_sea` | nautilus_shell → heart_of_the_sea | 35 / 30 / 60 | |
| `totem_of_undying` | emerald_block → totem_of_undying | 50 / 45 / 85 | |
| `tempered_core` | dormant_core → tempered_core | 45 / 0 / 50 | xp_cost 45; ignites the dragon-dropped core (§7) |

### Recipe Modules
Each recipe carries an optional `"module"` field (`RecipeModule`, read from the codec) grouping it for the config gate:
- **`core`** (untagged default) — shelf upgrades, tomes, XP conversions; always on, cannot be switched off.
- **`duplication`** — vanilla-item duplication (totem of undying, echo shard, golden apples, golden carrot, heart of the sea, budding amethyst); gated by `tableCrafting.allowDuplication` (default true).
- **`everfeast`** — Everfeast rations and the Everfull Flask; gated by `everfeast.enabled` (default true).

A disabled module's recipes vanish from the table and from the EMI/REI/JEI browser; the gate reads the server's config on the server and the synced config on the client, so a dedicated server's toggles govern its clients. The codec is strict — an unknown `"module"` string fails that recipe at load rather than falling back to `core`.

### Implementation Notes
- Multi-item recipe input handling: on craft, excess input beyond the consumed amount is returned to inventory or dropped; `setChanged()` re-triggers the shelf scan.
- The crafting result is surfaced in `StatsPayload` so the client can render the projected output in slot 2 before the click.

---

## 4. Enchantment Library

A two-tier storage block (`EnchantmentLibraryBlock` + `EnchantmentLibraryBlockEntity`, subclassed by `BasicLibraryBlockEntity` and `EnderLibraryBlockEntity`) that pools enchanted books into a per-enchantment point bank and dispenses books deterministically for XP. The Basic → Ender upgrade is the `meridian:keep_nbt_enchanting` recipe (§3), which preserves the stored data.

### Tiers
| Tier | Block ID | Max stored level | Max points/enchantment |
|---|---|---|---|
| Basic | `meridian:library` | 16 | 32,768 (`2^15`) |
| Ender | `meridian:ender_library` | 31 | 1,073,741,824 (`2^30`) |

### Point Bank
Each deposited book of level *L* adds `points(L) = 2^(L-1)` to that enchantment's pool (level ≤ 0 contributes 0). The BE keeps two parallel maps per enchantment:
- **`points`** — accumulated pool, saturating at the tier's max points.
- **`maxLevels`** — the highest single-book level ever deposited, clamped to the tier's max level.

### Extraction
`canExtract(enchantment, target)` requires all three:
1. `target > currentLevel` and `target ≤ tier max level`,
2. `maxLevels[enchantment] ≥ target` — you must have deposited at least one book of that level (you cannot grind Sharpness I books into a Sharpness V),
3. `points[enchantment] ≥ points(target) − points(currentLevel)`.

The cost in points is `points(target) − points(currentLevel)`. There is **no XP cost** — extraction is entirely point-driven. A normal click extracts `currentLevel + 1`; a shift-click extracts the highest affordable level (clamped to `maxLevels`). Extraction is **menu-only** (`EnchantmentLibraryMenu`).

### Hopper Automation
Books may be **inserted** by hopper (other items rejected; multi-stacks are pushed one book at a time to `depositBookSilent`). When a pool is saturated, further points are silently voided so pipes never jam. `library.ioRateLimitTicks` (default 0 = no throttle) sets an insert cooldown. **Extraction is permanently disabled via hopper** — the library is read-only to automation on the way out.

### Implementation Notes
- The `STORED_XP` data component is shared between Basic and Ender (identical codec).
- The Ender upgrade does not auto-scale existing entries to the higher ceiling; pre-upgrade pools stay where they are and only new deposits push past the old 32,768 cap.
- `MeridianAPI.getStoredPoints(level, pos, enchKey)` returns `OptionalInt` of the pool, empty when the block is not a library.

---

## 5. Salvage Tomes

Three salvage tomes move enchantments between items at the anvil — input item on the left, tome on the right. Dispatch is handled by the anvil handler chain (§7).

| Tome | Item ID | Recovers | Source item | XP cost (config key) |
|---|---|---|---|---|
| **Scrap Tome** | `meridian:scrap_tome` | one random enchantment | destroyed | 3 (`tomes.scrapTomeXpCost`) |
| **Improved Scrap Tome** | `meridian:improved_scrap_tome` | all enchantments | destroyed | 5 (`tomes.improvedScrapTomeXpCost`) |
| **Extraction Tome** | `meridian:extraction_tome` | all enchantments | preserved (takes durability damage) | 10 (`tomes.extractionTomeXpCost`) |

### Behavior
- **Scrap Tome:** outputs one enchanted book carrying one enchantment chosen by a **deterministic seeded RNG** — the seed combines the player's enchantment seed with the candidate enchantments' resource-location hashes, and candidates are sorted by ID string, so the same player + same item yields the same pick (re-rolling requires changing the loadout). One tome consumed. Source item destroyed by the vanilla anvil take.
- **Improved Scrap Tome:** outputs one enchanted book with every enchantment at its original level (no RNG). One tome consumed; source destroyed.
- **Extraction Tome:** outputs one enchanted book with all enchantments; the source item is **preserved**, fully unenchanted, and damaged by `tomes.extractionTomeItemDamage` (default 50 durability), clamped so the item never breaks (`min(damage + delta, maxDamage − 1)`). Non-damageable inputs are unenchanted without damage. The stripped item is restored to the left slot by the `AnvilMenuMixin#onTake` tail. One tome consumed.

### Extraction Tome — Repair Path
A separate handler (`ExtractionTomeFuelSlotRepairHandler`, registered after the extraction handler so extraction wins on enchanted items) fires when the left item is a **damaged, unenchanted, damageable** item and the right is an Extraction Tome. It repairs `floor(maxDamage * tomes.extractionTomeRepairPercent)` (default 0.25 → 25% of max durability), declines if the result would be a no-op, consumes one tome, and costs 10 XP levels.

### Implementation Notes
- All three tome anvil paths run through the same `AnvilHandler`/`AnvilResult` contract (§7).
- If the left item carries Curse of Sealing, the anvil result is blanked before any handler runs (`AnvilMenuMixin`), locking the item against tome extraction.

---

## 6. XP Tomes

A tiered consumable that banks experience levels. Crafted at the enchanting table from a Dormant XP Tome (§3); the dormant form is inert fluff (tooltip only). The `.glyph` sources for the four-item ladder live in `art/tome/`.

| Item ID | Display | Capacity (levels) |
|---|---|---|
| `meridian:dormant_xp_tome` | Dormant XP Tome | — (inert) |
| `meridian:xp_tome_t1` | XP Tome I | 10 |
| `meridian:xp_tome_t2` | XP Tome II | 30 |
| `meridian:xp_tome_t3` | XP Tome III | 50 |

### Behavior
- **Right-click:** deposit 1 level from the player into the tome (if not full and the player has ≥ 1 level).
- **Shift-right-click:** withdraw 1 level from the tome to the player (if not empty).
- Stored amount lives in the `meridian:stored_xp` integer data component (network-synced).
- The item's durability bar shows the fill fraction in lime green (HSV hue 0.4).
- On a failed action, an action-bar message displays `message.meridian.xp_tome.full` / `.empty`.

---

## 7. Anvil Upgrades

Meridian extends the vanilla anvil through a handler chain rather than replacing the menu. `AnvilMenuMixin` injects on `AnvilMenu#createResult` (RETURN) and on `onTake`; `AnvilDispatcher` runs the registered `AnvilHandler`s in order and the **first** handler that returns a non-empty `AnvilResult(output, xpCost, rightConsumed, leftReplacement)` claims the slot pair. If the left item has Curse of Sealing, the result is blanked and all handlers are skipped.

Registration order (`MeridianAnvilHandlers`): Prismatic Web → Iron Block Repair → Scrap Tome → Improved Scrap Tome → Extraction Tome → Extraction Tome Repair → Tempered Core.

### Prismatic Web (`meridian:prismatic_web`)
Left = any enchanted item, right = Prismatic Web. Strips all `#minecraft:curse` enchantments (non-curses preserved); declines if there are no curses. Consumes 1 web, costs `anvil.prismaticWebLevelCost` (default 30) XP levels. Gated by `anvil.prismaticWebRemovesCurses` (default true).

### Iron Block Anvil Repair
Left = a damaged anvil (Damaged → Chipped → Anvil), right = an iron block (exact item match, not a tag — ingots/nuggets/other blocks are excluded). Repairs the anvil one tier, copies any `ENCHANTMENTS` component to the output, consumes 1 iron block, and costs 1 XP level (`XP_COST_LEVELS = 1`). Pristine anvils decline. Gated by `anvil.ironBlockRepairsAnvil` (default true).

### Tempered Core (`meridian:tempered_core`)
Left = any damageable, not-already-unbreakable item, right = a Tempered Core. Marks the left item permanently unbreakable (`minecraft:unbreakable`): remaining damage is healed to full and the item stops taking damage entirely, while item type, enchantments, name, and every other component are preserved — any Mending or Unbreaking simply goes inert (nothing is stripped or refunded). Consumes exactly 1 core per click regardless of stack size and costs `anvil.temperedCoreLevelCost` (default 10) XP levels — the real gate is obtaining the core. Declines (leaving vanilla and the rest of the chain free) when either slot is empty, the right slot is not a Tempered Core, the left is already unbreakable (one core per item), or the left has no durability to protect. Gated by `anvil.temperedCoreEnabled` (default true).

**Dormant Core source.** The Ender Dragon drops one Dormant Core per kill (first and every respawn) as a free item entity — spawned by `DragonLootHandler.dropDormantCore` from `EnderDragonMixin`, injected at the terminal frame of `EnderDragon#tickDeath()` (`dragonDeathTime == 200`) because the dragon never routes through `LivingEntity.die()` and rolls no loot table. The Dormant Core is ignited into a Tempered Core at an end-tier table via the `tempered_core` enchanting recipe (§3): Eterna 45, Arcana 50, `xp_cost` 45.

### Implementation Notes
- `leftReplacement` (used by the Extraction Tome) restores a stripped/damaged copy to slot 0 after vanilla's `onTake` clears it.
- The salvage-tome anvil paths (§5) are handlers in this same chain.

---

## 8. Enchantments

131 original enchantments, defined as JSON in `data/meridian/enchantment/` (131 files) on top of vanilla's data-driven `EnchantmentEffectComponents`, with custom Java handlers where vanilla components are insufficient. Names, IDs, weights, costs, and effects are original to Meridian.

### JSON Structure
Each file is a standard 1.21.1 enchantment definition: `description` (translation key), `supported_items` / `primary_items` (item tags), optional `exclusive_set`, `weight`, `max_level`, `min_cost` / `max_cost` (`{base, per_level_above_first}`), `anvil_cost`, `slots`, and an `effects` map. Pure-data enchantments (≈29) carry vanilla effect codecs (e.g. `minecraft:attributes`). The remaining ≈83 carry custom behavior driven by Java event handlers.

### Categories and Handlers
| Category | Example enchantments | Handler |
|---|---|---|
| Combat | Siphon, Keen Edge, Tempo, Blight, Decay, Nightfall, Final Gambit, Cleave, Soul Tax, Quell, Pummel, Repulse, Rally, Bloodrage, Retribution | `EnchantmentEffectHandler` |
| Ranged | Detonation, Stormcall, Resonance, Gale Shot, Permafrost, Ricochet, True Flight, Glacial Lance | `ProjectileEnchantmentHandler` |
| Tools | Excavate (3×3), Prospect (vein), Bounty, Furrow, Terrasculpt | `ToolEnchantmentHandler`, `ArmorTickHandler` (Terrasculpt) |
| Mobility / Armor | Alacrity, Clamber, Vault, Slipstream, Cinderwalk, Diminish, Colossus, Luminance, Gravitas, Abyss Ward, Premonition, Antidote, Steadfast | `ArmorTickHandler`, `EnchantmentEffectHandler` |
| Mounts | Gallop, Trample, Skybound, Saddleguard, Wavestride, Endurance | `MountedEnchantmentHandler`, `SaddleguardMixin`, `WavestrideMixin`, `EnduranceMixin` |
| Elytra | Ironwing, Impact Ward | data + handlers |
| Shield | Retribution, Pummel, Fortify | `EnchantmentEffectHandler`, `ShieldFortifyMixin` |
| Utility | Mason's Reach, Aurify, Tether | `AurifyHandler`, `TetherHandler` |

`EnchantmentEffects` holds ~75 `ResourceKey<Enchantment>` constants and helpers (`getEnchantmentLevel`, `getEquippedLevel`).

### Exclusive Sets
Ten exclusive-set tags constrain mutually exclusive picks: `exclusive_set/mining`, `/size`, `/arrow_impact`, `/glass_cannon`, `/aspect`, `/mending`, `/axe`, `/loot_bonus`, `/mobility`, `/trophy`.

### Implementation Notes
- The `EnchantableItem` API hook (§1) is the supported path for items to post-process selection.
- `AbstractArrowMixin`, `TetherMixin`, `TemptGoalMixin`, `SheepMixin`, `AntidoteMixin`, `PlayerMixin`, `ItemMixin`, `ItemStackMixin`, `CreeperEntityAccessor` support specific enchantment effects.

---

## 9. Per-Enchantment Overrides

Operators can override `enabled`, `maxLevel`, `maxLootLevel`, `levelCap`, and the min/max power functions for **any** enchantment — vanilla or modded — via the `enchantmentOverrides` map in `config/meridian.json`. Disabling an enchantment removes it from the table, loot, and tooltips without deleting it from existing items.

### Override Entry
Keys are enchantment IDs (e.g. `minecraft:sharpness`, `meridian:siphon`). Each entry: `enabled` (default true), `maxLevel` / `maxLootLevel` / `levelCap` (default -1 = vanilla), and optional `minPowerFunction` / `maxPowerFunction` objects.

### Power Functions
`PowerFunctionConfig.type` is one of:
- `"default"` — vanilla `getMinCost`/`getMaxCost`. The default min function extrapolates beyond the vanilla max level with a `1.6^(level − max)` slope; the default max function returns the 200 power ceiling.
- `"linear"` — `base + perLevel * level`.
- `"fixed"` — constant `value`.

Invalid `type` resets to `"default"` with a warning. `maxLevel`/`maxLootLevel`/`levelCap` are clamped to [1, 127] when set.

### Registry and Sync
`EnchantmentInfoRegistry` builds the effective `EnchantmentInfo` per enchantment (merging overrides over vanilla defaults) on server start, on datapack `/reload`, and on `/meridian reload`. It keeps two maps — keyed by `ResourceKey` (API) and by `Enchantment` instance (mixin access). The server pushes `EnchantmentInfoPayload` to clients immediately after each rebuild so tooltips and the table match. `EnchantmentMaxLevelMixin` applies the override to `Enchantment.getMaxLevel()`; `EnchantmentHelperMixin` filters disabled enchantments (and Curse of Sealing) out of loot selection.

### `EnchantmentInfo`
Record `(ench, maxLevel, maxLootLevel, levelCap, maxPower, minPower, enabled)` with getters `getMaxLevel()` / `getMaxLootLevel()` (each `min(levelCap, …)` when capped) and `getMinPower(level)` / `getMaxPower(level)`.

---

## 10. Warden Loot

Wardens drop Warden Tendrils (`meridian:warden_tendril`), the key material for Sculk-tier shelves. Two loot pools are injected into the vanilla warden loot table via `WardenLootHandler`, gated on `source.isBuiltin()` (datapacks that fully replace the table opt out).

- **Guaranteed pool:** drops 1 tendril with probability `warden.tendrilDropChance` (default 1.0 = guaranteed). Condition `WardenPoolCondition(DROP_CHANCE)`.
- **Looting pool:** drops a second tendril with probability `lootingLevel * warden.tendrilLootingBonus` (default 0.10 → 10% per level: I 10%, II 20%, III 30%, IV 40%). Condition `WardenPoolCondition(LOOTING_BONUS)`, looting read from `LootContextParams.ATTACKING_ENTITY`. No attacker = 0%.

Both conditions read config live (reload-safe) and clamp the chance to [0, 1]. `meridian:infused_breath` (crafted from Dragon's Breath, §3) and the tendril are plain materials with tooltip-only behavior.

---

## Configuration

`config/meridian.json` is generated with defaults on first launch. All values hot-reload via `/meridian reload` (server-authoritative, synced to clients). The config is a **nested object** (sections, not a flat key list); shelf stat contributions live in datapacks, not here. The file carries a `configVersion` (currently 4) with a forward-migration hook.

### `enchantingTable`
| Key | Type | Default | Range / Notes |
|---|---|---|---|
| `allowTreasureWithoutShelf` | bool | false | Allow treasure enchantments without a Treasure Shelf |
| `maxEterna` | int | 50 | 1–100; Eterna stat cap |
| `globalMinEnchantability` | int | 1 | 0–100; minimum enchantability to be enchantable |

### `tableCrafting`
| Key | Type | Default | Range / Notes |
|---|---|---|---|
| `allowDuplication` | bool | true | Whether the vanilla-item duplication recipes (`"module": "duplication"`) are available at the table; already-crafted items are unaffected |

### `shelves`
| Key | Type | Default | Range |
|---|---|---|---|
| `sculkShelfShriekerChance` | double | 0.02 | 0.0–1.0 |
| `sculkParticleChance` | double | 0.05 | 0.0–1.0 |

### `anvil`
| Key | Type | Default | Notes |
|---|---|---|---|
| `prismaticWebRemovesCurses` | bool | true | |
| `prismaticWebLevelCost` | int | 30 | XP levels per curse removal |
| `ironBlockRepairsAnvil` | bool | true | |
| `temperedCoreLevelCost` | int | 10 | XP levels for the Tempered Core anvil upgrade |
| `temperedCoreEnabled` | bool | true | Whether a Tempered Core may be applied at the anvil to make gear unbreakable (§7) |

### `library`
| Key | Type | Default | Notes |
|---|---|---|---|
| `ioRateLimitTicks` | int | 0 | Hopper insert throttle; 0 = no limit |

### `tomes`
| Key | Type | Default | Notes |
|---|---|---|---|
| `scrapTomeXpCost` | int | 3 | |
| `improvedScrapTomeXpCost` | int | 5 | |
| `extractionTomeXpCost` | int | 10 | |
| `extractionTomeItemDamage` | int | 50 | durability damage to preserved item |
| `extractionTomeRepairPercent` | double | 0.25 | 0.0–1.0; repair-path durability restored |

### `everfeast`
| Key | Type | Default | Range / Notes |
|---|---|---|---|
| `enabled` | bool | true | Whether the Everfeast ration and Everfull Flask recipes (`"module": "everfeast"`) are available at the table; existing items keep working when off |
| `bites` | int | 128 | 1–4096; bites a newly-infused Everfeast ration is created with (existing rations keep theirs) |

### `warden`
| Key | Type | Default | Notes |
|---|---|---|---|
| `tendrilDropChance` | double | 1.0 | 0.0–1.0 |
| `tendrilLootingBonus` | double | 0.10 | 0.0–1.0; per Looting level |

### `combat`
| Key | Type | Default | Notes |
|---|---|---|---|
| `sunderAffectsPlayers` | bool | false | Whether Sunder may knock equipment off player victims (mobs always eligible) |
| `seekerTargetsPlayers` | bool | false | Whether Seeker bolts may lock onto player targets (mobs always eligible) |
| `harpoonAffectsPlayers` | bool | false | Whether Harpoon may drag player victims toward the thrower (mobs always eligible) |

### `display` (client-facing tooltip behavior)
| Key | Type | Default | Notes |
|---|---|---|---|
| `showBookTooltips` | bool | true | Enchantment tooltips on books |
| `overLeveledColor` | string | `"#FF6600"` | Hex (`#RRGGBB`); over-leveled enchantment color |
| `enableInlineEnchDescs` | bool | false | Inline enchantment descriptions in tooltips |

### `enchantmentOverrides`
Map of enchantment ID → override entry (`enabled`, `maxLevel`, `maxLootLevel`, `levelCap`, `minPowerFunction`, `maxPowerFunction`). See §9. Default `{}`.

---

## Commands

| Command | Permission | Description |
|---|---|---|
| `/meridian reload` | Op level 2 | Re-reads `config/meridian.json`, rebuilds `EnchantmentInfo`, fires `MeridianReloadCallback`, and syncs updated values to all connected players |

`/meridian reload` is the **only** command Meridian registers. The permission node for permission mods is `meridian.command.reload`. Localization keys: `command.meridian.reload.ok`, `command.meridian.reload.error`. Shelf stat changes use the vanilla `/reload` instead (datapack-driven).

---

## Public API

Everything under `com.rfizzle.meridian.api` is stable across patch/minor versions and conforms to the [Concord API Standard v1](https://github.com/rfizzle/concord/blob/master/API-STANDARD.md). Consume as a soft dependency: `modCompileOnly "maven.modrinth:meridian:<version>"` and guard with `FabricLoader.isModLoaded("meridian")`.

| Type | Purpose |
|---|---|
| `MeridianAPI` | Static read-only facade: `gatherStats(Level, BlockPos)`, `getEnchantmentInfo(Holder<Enchantment>)`, `getAllEnchantmentInfo()`, `getStoredPoints(Level, BlockPos, ResourceKey<Enchantment>)` |
| `StatCollection` | Aggregated shelf stats: `eterna`, `quanta`, `arcana`, `rectification`, `clues`, `maxEterna`, `blacklist`, `treasureAllowed`; `EMPTY` constant |
| `EnchantmentInfo` | Per-enchantment config: effective max level, max loot level, level cap, min/max power functions, enabled flag |
| `MeridianReloadCallback` | Fabric event fired server-side at the end of `/meridian reload` (listener exceptions isolated) |
| `IEnchantingStatProvider` | Implement on a block to contribute stats and table particles to the shelf scan |
| `BlacklistSource` | Implement on a shelf block entity to blacklist enchantments (`getEnchantmentBlacklist()`) |
| `TreasureFlagSource` | Marker interface on a shelf block entity to unlock treasure enchantments |
| `EnchantableItem` | Implement on an item to post-process the table's enchantment selection (`selectEnchantments(...)`) |

---

## Compatibility

### Required
- Minecraft 1.21.1
- Fabric Loader ≥ 0.16.10
- Fabric API (0.116.1+1.21.1 or newer)
- Java 21

Must be installed on **both client and server** — a client missing the mod desyncs on the first table interaction. Quilt users run it via Quilted Fabric API unchanged.

### Optional Integrations
- **EMI / REI / JEI** — two recipe categories each: an **Infusions** category (enchanting-table crafting recipes, with stat requirements, extracted by `TableCraftingDisplayExtractor`) and an **Enchantments** browser category (per-enchantment max level, exclusive sets, treasure flag, enabled state, per-level power windows, compatible items, extracted by `EnchantmentBrowserExtractor`), plus one info page per block-keyed `enchanting_stats` entry. Both categories use the vanilla enchanting table as workstation.
- **Jade / WTHIT** — enchanting-table tooltip showing the gathered stats (server provider computes via `EnchantingStatRegistry`), and an enchantment-library tooltip showing stored contents.
- **ModMenu** — config screen entry (`ModMenuIntegration`).

All optional dependencies are `suggests` in `fabric.mod.json`; the mod runs without any of them.

---

## Sound Design

Every cue is **vanilla**: Meridian ships no custom `SoundEvent`s and no `sounds.json`, mapping each interaction to an existing vanilla sound. The audio direction — why the mod stays vanilla-foley — lives in [`DESIGN.md`](DESIGN.md); this table owns the trigger-to-sound mapping.

| Feature | Vanilla Sound |
|---|---|
| Sculk shelf ambiance | `minecraft:block.sculk_catalyst.bloom` (`SoundEvents.SCULK_CATALYST_BLOOM`, vol 2.0, pitch 0.6–1.0) |
| Anvil tome/web/repair operations | vanilla anvil use sounds |
| Enchanting / table interaction | vanilla enchanting table sounds |
| Library deposit / extract | vanilla container sounds |

### Particles

Mod-specific table ambiance uses four custom element-themed enchant-glyph particle families; functional/organic effects stay vanilla.

| Effect | Particle | Texture |
|---|---|---|
| Nether-shelf table glyphs | `meridian:enchant_fire` | `sga_<a–z>_fire.png` (per-letter SGA runes, fire palette) |
| Ocean-shelf table glyphs | `meridian:enchant_water` | `sga_<a–z>_water.png` |
| Deep/Sculk-shelf table glyphs | `meridian:enchant_sculk` | `sga_<a–z>_sculk.png` |
| End-shelf table glyphs | `meridian:enchant_end` | `sga_<a–z>_end.png` |
| Baseline / default shelf glyphs | `minecraft:enchant` | Vanilla |
| Sculk shelf bloom | `minecraft:sculk_soul` | Vanilla |

---

## Localization

All user-facing text uses translation keys in `assets/meridian/lang/en_us.json`.

| Pattern | Example | Used for |
|---|---|---|
| `enchantment.meridian.*` | `enchantment.meridian.cleave` | Enchantment names |
| `item.meridian.*` | `item.meridian.xp_tome_t1` | Item names |
| `block.meridian.*` | `block.meridian.hellshelf` | Block names |
| `info.meridian.*` | `info.meridian.xp_tome.stored` | Item tooltip lines |
| `message.meridian.*` | `message.meridian.xp_tome.full` | Action-bar messages |
| `config.meridian.*` | `config.meridian.max_eterna` | Config screen labels |
| `command.meridian.reload.*` | `command.meridian.reload.ok` | Command feedback |

---

## Advancement Tree

26 advancements (`data/meridian/advancement/`) guide progression from the first shelf to Eterna 50. The tree is **usage-triggered** — each advancement fires when the player actually *uses* a system (takes the anvil output, deposits or extracts from a library, hits a stat threshold at the table), not merely when the block is crafted. Branches cover shelf tiers, the five stats, the library, the tomes, the anvil, and the End-game: `root`, `stone_tier`, `tier_three`, `stable_enchanting`, `high_quanta`, `high_arcana`, `high_clues`, `high_rectification`, `all_seeing`, `treasure_seeker`, `library`, `library_deposit`, `library_extract`, `ender_library`, `curator`, `tome_apprentice`, `tome_master`, `tome_salvage`, `web_spinner`, `tempered_core`, `curse_strip`, `filtering_blacklist`, `warden_tendril`, `sculk_mastery`, `infused_breath`, `apotheosis`.

---

## HUD

Meridian has **no persistent HUD element** and does not occupy a slot in the Concord shared HUD priority order. Enchanting is table-interaction-based with no always-on player stat; all information surfaces through the table screen, Jade/WTHIT overlays on enchanting blocks, and the EMI/REI/JEI browser.

---

## Testing Strategy

### Unit Tests (`src/test/`)
JUnit, no Minecraft runtime. Cover config parse/serialize round-trips, enchanting math (cost, quanta/rectification power roll, arcana picking), shelf stat aggregation (step-ladder), library point math and extraction gates, tome/anvil handler logic, recipe matching, and command reload routing (`MeridianCommand.runReload` is split out for headless testing).

### Gametests (`src/gametest/`, Fabric Gametest API)
Registered in `fabric.mod.json`. Cover registry presence, shelf roster and scan, menu end-to-end, prismatic web, library deposit/extract/persist/hopper, tome registry and anvil flows, XP tome, crafting button, specialty materials, advancement codec, config + reload-callback reload, and the enchantment roster (exclusive-set enforcement, config-disable, attribute/status effects, edge cases, selection, tags).
