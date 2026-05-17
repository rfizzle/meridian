# Meridian — Manual QA Playthrough

End-to-end checklist for verifying a Meridian build before tagging a release. Walks one tester from a fresh world to every MVP system: shelf tiers, stat readout, Eterna 50, library upgrade, all three tomes, Warden tendril, `infused_breath`, foreign-enchant rolls, and the integration tooltips. Treat each top-level section as a gate — if a check fails, file the regression before moving on.

> **Mode:** Creative for tier-up shortcuts is fine; flip to Survival for the XP-cost gates so cost rejection paths get exercised. The Warden section requires Survival and a real Ancient City.

## 0. Pre-flight

- [ ] Server jar `meridian-<version>.jar` placed in `mods/`; client jar matches **byte-for-byte** (the mod is `environment: "*"` — version skew desyncs on first table interaction).
- [ ] `config/meridian.json` is generated on first launch with `configVersion: 1`. Delete it and reboot to confirm regeneration.
- [ ] Server log shows `Meridian initialized` and **no** `clamped …` warnings on the default file.
- [ ] `/meridian reload` (op) replies with the `command.meridian.reload.ok` translation.

## 1. Shelf roster — craft every tier

Goal: confirm every shelf class registers, renders, drops itself, and feeds the stat scanner. Run in Creative.

### 1.1 Wood tier
- [ ] `beeshelf`, `melonshelf` craftable from their Zenith recipe shapes.
- [ ] Block place + break + pickup roundtrip — drops itself.

### 1.2 Stone tier (Nether / Ocean / End / Deep)
- [ ] Each shelf in DESIGN.md § "Stone-tier shelves" places, animates with the right particle theme (`ENCHANT_FIRE` for hellshelves, `ENCHANT_WATER` for seashelves, `ENCHANT_END` for endshelves, `ENCHANT_SCULK` for deepshelves).
- [ ] `blazing_hellshelf` plays its animated texture (`.mcmeta` preserved).
- [ ] `stoneshelf` shows its negative-Eterna stat in the table readout (it's intentionally weak).

### 1.3 Sculk tier
- [ ] `echoing_sculkshelf`, `soul_touched_sculkshelf` placeable; particle gate respects `config.shelves.sculkParticleChance`.

### 1.4 Utility
- [ ] `sightshelf` adds `+1 C`; `sightshelf_t2` adds `+2 C`. Two `sightshelf_t2` in range → readout caps at `C: 3` (T-2.2.3 clamp).
- [ ] `rectifier` / `rectifier_t2` / `rectifier_t3` add `R: 10/15/20` respectively.

### 1.5 Special (BE-backed)
- [ ] `filtering_shelf` — chiseled-bookshelf-style cursor targeting; insert an enchanted book → stat scanner blacklists that enchantment for the table; remove → blacklist shrinks.
- [ ] `treasure_shelf` — placement alone flips `treasureAllowed = true`. Verify by removing it from range and watching `#minecraft:treasure` enchants vanish from the preview list.

### 1.6 Vanilla bookshelf parity
- [ ] A vanilla bookshelf still contributes (via `vanilla_provider.json` → `#minecraft:enchantment_power_provider` → `maxEterna: 15, eterna: 1`).

## 2. Enchanting table — Eterna 50

Goal: hit the design's headline number and prove the stat readout reflects placement. Stay in Creative for shelf juggling.

- [ ] Place a Meridian table; open it. The `MeridianEnchantmentScreen` shows the one-line stat readout `E: 0  Q: 0  A: 0  R: 0  C: 0` below the three slots (toggleable via `config.enchantingTable.showLevelIndicator`).
- [ ] Place 15 vanilla bookshelves around the table → readout reads `E: 15`.
- [ ] Replace with 1 `draconic_endshelf` plus enough `endshelf` / `pearl_endshelf` to push `E: 50`. Confirm the readout caps at `E: 50` (default `config.enchantingTable.maxEterna`).
- [ ] Block one shelf's line-of-sight with a stone block at the midpoint offset → that shelf drops out (T-2.2.2). Remove the stone → it returns.
- [ ] Slot a diamond sword + 3 lapis. Slot 0 cost ≤ slot 1 ≤ slot 2 (monotonic ordering from T-2.4.1).
- [ ] Roll an enchant — XP and lapis decrement correctly; the item gains an `ItemEnchantments` component matching one of the preview slots.
- [ ] Lower `config.enchantingTable.maxEterna` to `30`, `/meridian reload`, reopen the table → readout caps at `E: 30`.

## 3. Library — Basic → Ender upgrade

Goal: deposit, extract, shift-extract, and the `keep_nbt_enchanting` upgrade path that preserves stored books across tier change.

### 3.1 Basic library
- [ ] Craft `library` (vanilla-shape recipe, ships as `data/meridian/recipe/library.json`). Place it.
- [ ] Open the Library UI — three slots visible (deposit, extract, scratch).
- [ ] Deposit Sharpness I × 32 → `points[Sharpness]` totals `32 × 1 = 32`; `maxLevels[Sharpness] = 1`.
- [ ] Try to extract Sharpness V — denied (`maxLevels` gate, T-4.4.2).
- [ ] Deposit one Sharpness V book → `maxLevels[Sharpness] = 5`, `points` += 16 (= 48 total). Now extract Sharpness V — **denied** still, `points(5) − points(0) = 16` requires ≥ 16 *plus* the deposited V got absorbed; verify the Zenith semantics (deposit increments both maps, then extract checks gates).
- [ ] Deposit enough Sharpness IV books to push past `points(5) = 16` → extraction succeeds; output book has Sharpness V; pool decrements by `16 − points(curLvl)`.
- [ ] Shift-click on Sharpness with `points = 256` → output is the max level affordable (T-4.3.3 formula: `1 + log₂(256) = 9`, clamped to `maxLevels[Sharpness]` = 5).
- [ ] Hopper feeding: place a hopper above the library with a stack of enchanted books → all books absorb; non-book items (e.g. iron ingot) are rejected (T-4.5.1).
- [ ] Set `config.library.ioRateLimitTicks: 20`, `/meridian reload` → second hopper insert within 20 ticks is dropped (T-4.5.3).

### 3.2 Ender upgrade
- [ ] Place a Meridian table next to the Basic Library. Slot the Library item into the table; surround with shelves to satisfy the `ender_library` recipe's stat requirements (see `data/meridian/recipe/enchanting/ender_library.json`).
- [ ] The table's 4th-row crafting result shows the Ender Library output with its XP cost badge (T-5.3.4).
- [ ] Click the row → server-side `keep_nbt_enchanting` handler fires; output is an `ender_library` BlockItem whose NBT preserves `Points` + `Levels` from the basic library (T-4.6.2).
- [ ] Place the Ender Library; reopen — pool contents are intact and `maxLevel` cap is now 31.
- [ ] Confirm encumbrance: deposit until `points` saturates `maxPoints` (Basic: `32_768`, Ender: `1_073_741_824`); excess deposits are silently voided, hoppers never jam.

## 4. Anvil tweaks

### 4.1 Prismatic Web (curses)
- [ ] Craft a `prismatic_web` (Zenith-shape recipe, ships under `data/meridian/recipe/prismatic_web.json`).
- [ ] Place a sword with Curse of Vanishing + Sharpness 3 in slot A, web in slot B → output keeps Sharpness 3, drops Curse of Vanishing; cost = `config.anvil.prismaticWebLevelCost` (default 30); 1 web consumed.
- [ ] Try the same on a sword with no curses → handler declines, no output.
- [ ] Set `config.anvil.prismaticWebRemovesCurses: false`, reload → handler declines on cursed input.

### 4.2 Iron block anvil repair
- [ ] Damaged Anvil + Iron Block → Chipped Anvil (1 level, 1 iron block); enchantments on the anvil stack (rare) preserved (T-4.2.1).
- [ ] Chipped Anvil + Iron Block → normal Anvil.
- [ ] Normal Anvil + Iron Block → declines.
- [ ] Iron *ingot* (not block) in slot B → declines.
- [ ] Set `config.anvil.ironBlockRepairsAnvil: false`, reload → declines for damaged inputs.

## 5. Tomes — all three paths

Run in Survival so the XP gates exercise.

### 5.1 Scrap Tome
- [ ] Craft `scrap_tome` (Zenith-shape recipe, ships at `data/meridian/recipe/scrap_tome.json`).
- [ ] Enchanted sword (Sharpness 4 + Looting 2) + scrap tome → output is an enchanted book with **one** of the two enchants (seeded by world random); sword destroyed; cost = `config.tomes.scrapTomeXpCost` (default 3).
- [ ] Unenchanted sword + tome → declines.

### 5.2 Improved Scrap Tome
- [ ] Craft via the table: input `scrap_tome` against an `improved_scrap_tome` recipe (ships at `data/meridian/recipe/enchanting/improved_scrap_tome.json`); requires the table's `infused_breath`-tier stat block.
- [ ] Sharpness 4 + Looting 2 + Mending sword + improved scrap tome → output book carries **all 3** enchantments; sword destroyed; cost = `config.tomes.improvedScrapTomeXpCost` (default 5).

### 5.3 Extraction Tome
- [ ] Craft via the table (`data/meridian/recipe/enchanting/extraction_tome.json`).
- [ ] Sharpness 5 + Looting 3 + Unbreaking 3 sword + extraction tome → output book has all 3; sword returned **unenchanted** with `config.tomes.extractionTomeItemDamage` (default 50) durability deducted; cost = `config.tomes.extractionTomeXpCost` (default 10).
- [ ] Damage-clamp: a 1-durability sword stays at 1 after extraction (T-5.2.3).
- [ ] **Anvil-fuel-slot repair (T-5.2.4):** damaged sword in slot A, extraction tome in the materials slot, no right-hand item → tome consumed, durability restored by `config.tomes.extractionTomeRepairPercent * maxDurability` (default 25%); same XP cost as standard extraction.

## 6. Warden — `warden_tendril` loot

Requires a real Ancient City and Survival mode. Skip without a generated city.

- [ ] Lure a Warden out, kill it. Loot drops include **at least one** `warden_tendril` (Pool A, default `config.warden.tendrilDropChance: 1.0`).
- [ ] Kill a second Warden with a Looting III sword → ~30% chance of a **second** tendril (Pool B, `config.warden.tendrilLootingBonus * looting_level`).
- [ ] Set `config.warden.tendrilDropChance: 0.0`, reload → next Warden drops 0 base tendrils, looting still rolls (T-5.4.4).

## 7. `infused_breath` + sculk-tier crafts

- [ ] Craft `infused_breath` via the table (`data/meridian/recipe/enchanting/infused_breath.json`) using `dragon_breath` as input. Confirm it can **only** be obtained this way (no crafting-table recipe exists).
- [ ] Use `infused_breath` to craft `infused_hellshelf` (`infused_hellshelf.json` table recipe).
- [ ] Use `infused_breath` to craft `infused_seashelf` (`infused_seashelf.json` table recipe).
- [ ] Use `infused_breath` to craft `deepshelf` (`deepshelf.json` table recipe).
- [ ] Place all three; readout aggregates Eterna correctly (`infused_hellshelf` = `+2 E, maxEterna 30`).

## 8. Foreign-enchant rolls — Mending + Soulbound

### 8.1 Mending
- [ ] With a Treasure Shelf in range, roll the table 50× on a netherite pickaxe → Mending appears at least once (treasure-shelf gate respected).
- [ ] Remove the Treasure Shelf → Mending stops appearing in previews.
- [ ] Set `config.enchantingTable.allowTreasureWithoutShelf: true`, reload → Mending appears even without the treasure shelf.

### 8.2 Soulbound (yigd)
- [ ] **Skip if [You're in Grave Danger](https://modrinth.com/mod/yigd) is not installed in this dev environment.**
- [ ] With yigd present and a Treasure Shelf in range, roll on a chestplate → Soulbound appears (per-enchantment override raised it from `weight: 0`).

## 9. Authored enchants (Zenith ports)

- [ ] Roll the table on a chestplate enough times to land **Icy Thorns**. Get hit by a zombie → zombie has Slowness applied (`minecraft:post_attack` enchanted-victim → affected-attacker chain).
- [ ] Apply **Shield Bash** to a shield (the enchant should appear in the preview when a shield is in slot A; the bundled tag override adds shields to `#minecraft:enchantable/weapon`). Hit a mob with the shield in mainhand → bonus damage; shield takes durability per hit.

## 10. Integrations smoke

### 10.1 EMI / REI / JEI
- [ ] With **EMI** loaded, open the EMI panel and search "Meridian". Two categories appear: "Meridian — Shelves", "Meridian — Tomes". Each `meridian:enchanting` and `keep_nbt_enchanting` recipe has its inputs, output, stat requirements, and XP cost rendered.
- [ ] With **REI** loaded (EMI absent), the same recipes show in REI's category panel via the shared `compat/common/TableCraftingDisplay` source of truth.
- [ ] With **JEI** loaded, JEI plugin entry-point fires; recipes render.

### 10.2 Jade
- [ ] With **Jade** loaded, look at a Meridian table → probe shows the 5 stats (`E/Q/A/R/C`) computed via `EnchantingStatRegistry.gatherStats`.
- [ ] Look at a Library BE → probe shows `Basic Library — N enchants stored` (or `Ender Library — N enchants stored`); per-enchant point detail only inside the Library UI (not in the world probe).

## 11. Advancement tree

Open the advancement screen (`L` by default) and verify the **Meridian** tab is populated. As the playthrough progresses, expect to unlock:

- [ ] `root` — granted on inventory-pickup of any Meridian shelf.
- [ ] `stone_tier` — after crafting `hellshelf` / `seashelf` / `dormant_deepshelf`.
- [ ] `tier_three` — after crafting `infused_hellshelf` / `infused_seashelf` / `deepshelf`.
- [ ] `library` — after crafting a basic `library`.
- [ ] `ender_library` — after the upgrade in §3.2.
- [ ] `tome_apprentice` — after crafting a `scrap_tome`.
- [ ] `tome_master` — after crafting an `extraction_tome`.
- [ ] `warden_tendril` — after picking one up in §6.
- [ ] `infused_breath` — after crafting one in §7.
- [ ] `apotheosis` — after the table reads `E: 50` in §2.

## 12. Tooltips

- [ ] Apply Sharpness 7 (or any over-leveled enchant) to a sword via /enchant → tooltip line is recolored using `config.display.overLeveledColor` (default `#FF6600`).
- [ ] Vanilla-cap-respecting Sharpness 5 → uses vanilla coloring.
- [ ] Set `config.display.overLeveledColor: "not-a-hex"`, reload → falls back to `#FF6600`, log warns (T-1.3.3).
- [ ] Stored-book tooltip: hover an enchanted book in inventory → per-level lines visible.
- [ ] Set `config.display.showBookTooltips: false`, reload, hover the same book → per-level lines suppressed.

## 13. Server reload sweep

- [ ] Edit `config/meridian.json` while the server is running (e.g. flip `enchantingTable.maxEterna` to `25`).
- [ ] `/meridian reload` (perm 2) → `command.meridian.reload.ok` reply; **no server restart**.
- [ ] Reopen the table → readout reflects the new cap.
- [ ] Kill another Warden after mutating `warden.tendrilDropChance` → drops reflect the new value (handlers read at roll time, not registration).

## 14. Crash / log clean

- [ ] After all of the above, server log contains zero `Caused by` stacks attributable to Meridian.
- [ ] No `clamped …` warnings beyond intentionally out-of-range tests.
- [ ] No `unresolved enchantment key` warnings unless a datapack was uninstalled mid-save (acceptable in that case).

---

When every box ticks green, the build is release-ready. File any failures against the relevant Story / Task in `TODO.md` before tagging.
