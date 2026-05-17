# Meridian — Config Reference

Annotated reference for `config/meridian.json`. This is the operator-facing tuning surface; stat values per block live in datapack JSON under `data/meridian/enchanting_stats/` and are not covered here.

## Location & lifecycle

- **Path:** `<instance>/config/meridian.json` — resolved via `FabricLoader.getConfigDir()`.
- **First launch:** the mod writes the defaults shown below and loads them. Delete the file and restart to regenerate.
- **Hot reload:** `/meridian reload` (permission level 2) rereads the file. Any runtime caches (Warden loot modifier, anvil/tome handlers, shelf particle gates, tooltip coloring) pick up the new values on the next interaction — no restart required.
- **Validation:** every field is clamped on load. Out-of-range values are rewritten in memory and logged as `clamped <field> from <old> to <new>`; the file on disk is left untouched unless a schema migration ran.
- **Missing fields:** any top-level section or field absent from the file is filled from defaults. Partial configs are valid.

## Defaults (full file)

```json
{
  "configVersion": 1,
  "enchantingTable": {
    "allowTreasureWithoutShelf": false,
    "maxEterna": 50,
    "showLevelIndicator": true,
    "globalMinEnchantability": 1
  },
  "shelves": {
    "sculkShelfShriekerChance": 0.02,
    "sculkParticleChance": 0.05
  },
  "anvil": {
    "prismaticWebRemovesCurses": true,
    "prismaticWebLevelCost": 30,
    "ironBlockRepairsAnvil": true
  },
  "library": {
    "ioRateLimitTicks": 0
  },
  "tomes": {
    "scrapTomeXpCost": 3,
    "improvedScrapTomeXpCost": 5,
    "extractionTomeXpCost": 10,
    "extractionTomeItemDamage": 50,
    "extractionTomeRepairPercent": 0.25
  },
  "warden": {
    "tendrilDropChance": 1.0,
    "tendrilLootingBonus": 0.10
  },
  "display": {
    "showBookTooltips": true,
    "overLeveledColor": "#FF6600",
    "enableInlineEnchDescs": false
  },
  "enchantmentOverrides": {}
}
```

## `configVersion` *(int, default `1`)*

Schema marker. Do not edit by hand. The mod bumps this only for renames, removals, or semantic changes — purely additive schema growth (new sections or fields) stays at `1` and is handled by the defaults-fill pass. When `CURRENT_VERSION` is raised, the load path runs `migrate()`, rewrites the file, and logs `Migrated config from version X to Y`.

## `enchantingTable`

Controls the table itself — cap on Eterna aggregation, treasure-shelf gating, and the client-side stat readout.

### `allowTreasureWithoutShelf` *(boolean, default `false`)*

When `false`, treasure-only enchantments (`#minecraft:treasure` — Mending, Soulbound, Frost Walker, etc.) only become selectable when a **Treasure Shelf** is in range of the table. When `true`, treasure enchants may roll from any table, ignoring the treasure-shelf gate entirely.

Example — vanilla-lite server that skips the treasure-shelf progression:
```json
"enchantingTable": { "allowTreasureWithoutShelf": true }
```

### `maxEterna` *(int, default `50`)*

Hard cap on the aggregated Eterna across all in-range shelves, regardless of individual shelf `maxEterna` contributions. The table also clamps each shelf's `maxEterna` contribution to this value at aggregation time.

- **Clamp:** `[1, 100]` — values outside this range are corrected and warned.
- **Effect of raising:** enables slot-2 costs above 30 (vanilla's hard ceiling); useful if your datapack ships shelves with boosted `maxEterna`.
- **Effect of lowering:** caps the table at sub-vanilla power — combine with a wood-tier-only shelf roster for early-game progression servers.

Example — early-game cap:
```json
"enchantingTable": { "maxEterna": 30 }
```

### `showLevelIndicator` *(boolean, default `true`)*

Toggles the one-line stat readout (`E: 50  Q: 12  A: 5  R: 10  C: 2`) rendered below the three enchant slots in the enchanting screen. Set `false` for a cleaner UI at the cost of feedback while tuning shelf placement.

### `globalMinEnchantability` *(int, default `1`)*

Minimum enchantability value for all items. When an item's `getEnchantmentValue()` returns less than this value, the mod overrides it via a mixin on `Item`. This makes items that are normally unenchantable (shields, shears, flint-and-steel, etc.) enchantable at the table with an enchantability of at least this value.

- **Clamp:** `[0, 100]`.
- **Setting to 0:** disables the override entirely — only items that natively return a positive enchantability can be enchanted.
- **Setting to 1 (default):** mirrors Apothic-Enchanting's behavior — every item becomes enchantable. Items with existing enchantability (swords, armor, etc.) keep their native values since the mixin only fires when the base return is 0.
- **Setting higher (e.g. 15):** all previously-unenchantable items behave as if they had the enchantability of iron-tier equipment, making it much easier to roll multiple enchantments on them.

Example — disable to restore vanilla enchantability:
```json
"enchantingTable": { "globalMinEnchantability": 0 }
```

## `shelves`

Behavior specific to shelf blocks — sculk-tier mob interaction, client particle throttling.

### `sculkShelfShriekerChance` *(double, default `0.02`)*

Probability [0.0–1.0] that a **Sculk Shelf** summons a shrieker event when a player passes within activation range. `0.0` disables the warden-summon path entirely; `1.0` triggers it every tick the player is within range (not recommended).

- **Clamp:** unit (0.0–1.0).
- **Typical tuning:** `0.005`–`0.05` for a noticeable but non-obnoxious rate on an SMP.

### `sculkParticleChance` *(double, default `0.05`)*

Probability [0.0–1.0] per client animation tick that a sculk-themed shelf (Sculk Shelf, Soulshelf) emits a particle. Lowering reduces visual noise in dense shelf rooms; raising makes sculk shelves more distinct. This is a client-side visual toggle only — it has no gameplay effect.

- **Clamp:** unit (0.0–1.0).

## `anvil`

Custom anvil handlers (Prismatic Web, iron-block repair). Tomes have their own section.

### `prismaticWebRemovesCurses` *(boolean, default `true`)*

Master gate for the Prismatic Web curse-stripping handler. When `false`, placing a Prismatic Web in the right slot of an anvil with a cursed item on the left produces no result — the handler declines and falls through to any later anvil handlers (currently none that match this input shape). Non-curse enchantments are always preserved.

### `prismaticWebLevelCost` *(int, default `30`)*

XP level cost to strip all curses from one item using one Prismatic Web. Consumes one web per application.

- **Clamp:** non-negative (0 or higher).
- **Setting to 0:** free curse removal — balance implication: Curse of Binding and Curse of Vanishing become trivial nuisances instead of mild strategic constraints.

### `ironBlockRepairsAnvil` *(boolean, default `true`)*

Enables the iron-block anvil repair path: placing a Damaged Anvil or Chipped Anvil (as a BlockItem) on the left and an Iron Block on the right in an anvil produces the next tier up (damaged → chipped → normal) for a flat 1-level XP cost, consuming one iron block. When `false`, the handler declines and players must re-craft anvils from scratch.

## `library`

Enchantment Library throttling. Tier caps (Basic `maxLevel=16`, Ender `maxLevel=31`) and point-pool limits are **code constants**, not config — they drive the `points(level) = 2^(level-1)` math and are baked into on-disk NBT. Changing them mid-save would corrupt stored books.

### `ioRateLimitTicks` *(int, default `0`)*

Minimum game-ticks between hopper-driven book inserts into a single library block entity. `0` disables the throttle (hoppers feed at vanilla cadence — typically 8 ticks per transfer). Values above `0` cause the `Storage<ItemVariant>` adapter to drop inserts that arrive sooner than the rate limit.

- **Clamp:** non-negative.
- **Use case:** set to `20` or higher on large SMPs where players pipe hundreds of enchanted books per minute and the constant sync traffic becomes a bandwidth concern.

Example — throttle to one insert per second:
```json
"library": { "ioRateLimitTicks": 20 }
```

## `tomes`

Scrap, Improved Scrap, and Extraction tome handler tuning.

### `scrapTomeXpCost` *(int, default `3`)*

XP level cost for the **Scrap Tome** path. Scrap Tome + enchanted item in an anvil produces an enchanted book with one random enchantment from the source item; the source item is destroyed and the tome consumed.

- **Clamp:** non-negative.

### `improvedScrapTomeXpCost` *(int, default `5`)*

XP level cost for the **Improved Scrap Tome** path. Same shape as Scrap Tome, but the output book carries **all** enchantments from the source item.

- **Clamp:** non-negative.

### `extractionTomeXpCost` *(int, default `10`)*

XP level cost for the **Extraction Tome** path. Produces a book with all enchantments of the source item; the source item is returned unenchanted, damaged by `extractionTomeItemDamage`. Also gates the anvil-fuel-slot repair side-path (see `extractionTomeRepairPercent`).

- **Clamp:** non-negative.

### `extractionTomeItemDamage` *(int, default `50`)*

Raw durability points deducted from the source item when using the Extraction Tome. Durability is clamped so the item always survives with at least 1 HP remaining — a 1-durability sword stays at 1.

- **Clamp:** non-negative.
- **Setting to 0:** lossless extraction — balance consideration for a hardcore server that still wants the progression ladder without the durability cost.

### `extractionTomeRepairPercent` *(double, default `0.25`)*

Fraction [0.0–1.0] of max durability restored when an **Extraction Tome** is consumed in the anvil fuel slot against a damaged item with no right-hand input. Applied as `floor(repairPercent * maxDurability)` per consumption.

- **Clamp:** unit (0.0–1.0).
- **Setting to 0.0:** disables the repair side-path — tomes are then usable only for extraction.
- **Setting to 1.0:** one tome fully repairs any item. Probably too strong for a standard server.

## `warden`

Controls the Warden loot modifier that ships `warden_tendril` (the sculk-tier shelf material).

### `tendrilDropChance` *(double, default `1.0`)*

Probability [0.0–1.0] that a Warden drop-event yields one guaranteed `warden_tendril`. Evaluated at roll time, so reloading config after mutation updates subsequent Warden kills. At `1.0` every Warden drops at least one tendril; at `0.0` this pool never fires (the looting-bonus pool still can).

- **Clamp:** unit (0.0–1.0).

### `tendrilLootingBonus` *(double, default `0.10`)*

Per-level-of-Looting probability [0.0–1.0] that a Warden drop-event yields a **second** `warden_tendril` from the bonus pool (`LootItemRandomChanceWithLootingCondition(0.0, bonus)`). With the default `0.10`, Looting III gives a ~30% chance of a second drop; Looting IV gives ~40%.

- **Clamp:** unit (0.0–1.0).

## `display`

Client-only tooltip tweaks. Values are read client-side every tick; changes apply immediately on reload.

### `showBookTooltips` *(boolean, default `true`)*

When `false`, the per-level enchantment lines on stored-book tooltips (e.g. "Sharpness V" under a book's name) are suppressed. Useful in inventories full of library-extracted books where the noise becomes overwhelming; the book's stored data is unchanged — only the hover text is.

### `overLeveledColor` *(string, default `"#FF6600"`)*

Hex color applied to enchantment names whose level exceeds the vanilla maximum (e.g. Sharpness VII when vanilla caps at V). The color is injected via a mixin on `Enchantment.getFullname()`, so it applies everywhere the enchantment name is rendered — tooltips, anvil UI, library screens, chat hover text, and any mod that calls `getFullname()`. Curses are excluded and always render in vanilla red. Must match the regex `^#[0-9A-Fa-f]{6}$`; invalid strings are replaced with `#FF6600` on load and warned.

- **Accepted:** `"#FF6600"`, `"#ff6600"`, `"#AABBCC"`.
- **Rejected:** `"FF6600"` (missing hash), `"#F60"` (3-digit), `"orange"` (named), `"#FF66001"` (7-digit).

Example — electric cyan instead of the default orange:
```json
"display": { "overLeveledColor": "#00E5FF" }
```

### `enableInlineEnchDescs` *(boolean, default `false`)*

When `true`, each enchantment line in an item's tooltip (e.g. "Sharpness V") is followed by the enchantment's description text rendered in dark gray (pulled from the `enchantment.meridian.<id>.desc` lang keys). When `false`, only the enchantment name and level are shown — vanilla behavior.

Example — enable inline descriptions:
```json
"display": { "enableInlineEnchDescs": true }
```

## `enchantmentOverrides`

Per-enchantment configuration overrides. Keys are fully-qualified enchantment IDs (e.g. `"minecraft:sharpness"`, `"meridian:tempo"`). Each entry is an object with optional fields; use `-1` on numeric fields to keep the vanilla default.

These overrides are merged with vanilla defaults at server start and after datapack reload. The resulting `EnchantmentInfo` records are synced to clients via `EnchantmentInfoPayload` on join and reload, and enforced at the `Enchantment` class level via a mixin on `getMaxLevel()`.

### Entry format

```json
"enchantmentOverrides": {
  "minecraft:sharpness": {
    "enabled": true,
    "maxLevel": 10,
    "maxLootLevel": 7,
    "levelCap": 10
  },
  "meridian:snare": {
    "enabled": false
  }
}
```

### `enabled` *(boolean, default `true`)*

Master toggle for the enchantment. When `false`:

- The enchantment does not appear in the enchanting table.
- The enchantment does not roll in loot tables or villager trades.
- Existing items keep their enchantment data (no data loss), but the enchantment has no effect while disabled.
- The enchantment is hidden from tooltip display and the enchantment glint is suppressed if it is the item's only enchantment.
- The Enchantment Library blocks extraction and new deposits of the disabled enchantment.
- The Enchanting Info Screen omits it from the browser list.

The `enabled` state is synced to clients via `EnchantmentInfoPayload` on join and after `/meridian reload`. No server restart is required to toggle an enchantment on or off.

Example — disable economy-warping enchantments:
```json
"enchantmentOverrides": {
  "meridian:snare": { "enabled": false },
  "meridian:aurify": { "enabled": false },
  "meridian:plunder": { "enabled": false }
}
```

### `maxLevel` *(int, default `-1`)*

Maximum level the enchantment can reach at the enchanting table. Overrides the vanilla `Enchantment.getMaxLevel()` return value for the selection algorithm's power-window loop. `-1` means "use the vanilla definition's `maxLevel`".

- **Clamp (when not -1):** `[1, 127]`.
- **Mixin enforcement:** the `EnchantmentMixin` on `Enchantment.getMaxLevel()` returns this value globally — not just during table selection, but also for any code path that calls `getMaxLevel()` (anvil combining, loot table generation via other mods, etc.).

### `maxLootLevel` *(int, default `-1`)*

Maximum level the enchantment can appear at in loot tables and villager trades. Separate from `maxLevel` so operators can allow level 10 at the table while capping random loot at level 7. `-1` falls back to the vanilla `maxLevel`.

- **Clamp (when not -1):** `[1, 127]`.

### `levelCap` *(int, default `-1`)*

Hard ceiling applied after both `maxLevel` and `maxLootLevel`. If set, the effective max level is `min(levelCap, maxLevel)` and the effective max loot level is `min(levelCap, maxLootLevel)`. Useful as a server-wide safety net — e.g. set `levelCap: 5` to prevent any enchantment from exceeding level V regardless of per-enchant `maxLevel` settings. `-1` disables the cap.

- **Clamp (when not -1):** `[1, 127]`.

### `minPowerFunction` *(object, optional)*

Custom power function overriding the enchantment's minimum power curve. Controls the lowest enchanting-power value at which each level of the enchantment becomes selectable. When absent or `null`, vanilla behavior applies.

Supported `type` values:
- `"default"` — vanilla behavior (min-cost extrapolation from the enchantment definition). This is the fallback when an invalid type is supplied.
- `"linear"` — `base + perLevel * level`. Uses the `base` and `perLevel` fields.
- `"fixed"` — constant `value` regardless of level. Uses the `value` field.

Validation: `type` must be one of `default`, `linear`, `fixed`; invalid values are reset to `"default"` and warned.

### `maxPowerFunction` *(object, optional)*

Custom power function overriding the enchantment's maximum power curve. Controls the highest enchanting-power value at which each level of the enchantment remains selectable. When absent or `null`, vanilla behavior applies (200 ceiling or max-cost extrapolation).

Same `type` values and validation as `minPowerFunction`.

Example — raise Sharpness to 10 at the table, cap loot at 7, hard-cap at 10:
```json
"enchantmentOverrides": {
  "minecraft:sharpness": { "maxLevel": 10, "maxLootLevel": 7, "levelCap": 10 }
}
```

Example — allow Mending to roll up to level 3 at the table (for faster repair):
```json
"enchantmentOverrides": {
  "minecraft:mending": { "maxLevel": 3, "maxLootLevel": -1, "levelCap": -1 }
}
```

Example — custom power curve for Sharpness (linear min, fixed max):
```json
"enchantmentOverrides": {
  "minecraft:sharpness": {
    "maxLevel": 10,
    "minPowerFunction": { "type": "linear", "base": 1, "perLevel": 11 },
    "maxPowerFunction": { "type": "linear", "base": 21, "perLevel": 11 }
  }
}
```

## Validation summary

| Field | Rule |
|---|---|
| `enchantingTable.maxEterna` | clamp to `[1, 100]` |
| `enchantingTable.globalMinEnchantability` | clamp to `[0, 100]` |
| `shelves.sculkShelfShriekerChance` | `clampUnit` (0–1) |
| `shelves.sculkParticleChance` | `clampUnit` (0–1) |
| `anvil.prismaticWebLevelCost` | `clampNonNegative` |
| `library.ioRateLimitTicks` | `clampNonNegative` |
| `tomes.scrapTomeXpCost` | `clampNonNegative` |
| `tomes.improvedScrapTomeXpCost` | `clampNonNegative` |
| `tomes.extractionTomeXpCost` | `clampNonNegative` |
| `tomes.extractionTomeItemDamage` | `clampNonNegative` |
| `tomes.extractionTomeRepairPercent` | `clampUnit` (0–1) |
| `warden.tendrilDropChance` | `clampUnit` (0–1) |
| `warden.tendrilLootingBonus` | `clampUnit` (0–1) |
| `display.overLeveledColor` | regex `^#[0-9A-Fa-f]{6}$`; on mismatch, fall back to `#FF6600` |
| `enchantmentOverrides.*.enabled` | boolean; defaults to `true` when absent |
| `enchantmentOverrides.*.maxLevel` | skip if `-1`; otherwise clamp to `[1, 127]` |
| `enchantmentOverrides.*.maxLootLevel` | skip if `-1`; otherwise clamp to `[1, 127]` |
| `enchantmentOverrides.*.levelCap` | skip if `-1`; otherwise clamp to `[1, 127]` |

All other fields are typed but not clamped — booleans accept only `true`/`false` (malformed JSON falls back to the default for that field and logs).

## Forward compatibility

New top-level sections or fields are additive — existing `configVersion: 1` files pick them up automatically via the defaults-fill pass, no manual action required. Dead stubs are **not** shipped ahead of time. If a schema-breaking rename lands later, `configVersion` gets bumped and a per-version migration runs on first load after the update.
