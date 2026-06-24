# Meridian — Asset Manifest

> Where every committed asset lives: its source under `art/` (a re-renderable
> `.glyph` for pixel art, or a `.png` master for logos) and the final file it
> ships as. **`MISSING`** in the glyph column flags a pixel asset that has no
> `.glyph` source yet — a candidate for the glyph pipeline (concord
> [`design/DESIGN-SYSTEM.md`](../../concord/design/DESIGN-SYSTEM.md) §8).
> [`DESIGN.md`](DESIGN.md) covers *why* each asset exists; this file covers *where* it lives.
>
> Final paths are under `src/main/resources/` unless noted. A separate report sweeps
> the resource tree for any final asset lacking a `.glyph` source.

## Branding masters

| Asset | Source | Final / derived copies |
|---|---|---|
| Full logo | `art/logo.png` — `.png` master (not glyph-based) | `site/assets/logo.png` |
| Mod icon | `art/glyphs/astrolabe_logo.glyph` → `art/icon-128.png` (128 native), `art/icon-512.png` (512 store master) | `assets/meridian/icon.png` (256, in-jar Mod Menu + EMI/REI/JEI category tabs), `site/assets/icon.png` (256, favicon/apple-touch/nav) |

## In-game pixel art

`art/glyphs/` holds every glyph's `.glyph` source; rendered `.png`/`@16x.png` previews there are gitignored review artifacts (the shipped masters live under `assets/`).

| Asset | `.glyph` source | Final asset |
|---|---|---|
| XP Tome | `art/glyphs/xp_tome.glyph` | `assets/meridian/textures/item/tome/xp_tome.png` |
| XP Tome T1 | `art/glyphs/xp_tome_t1.glyph` | `assets/meridian/textures/item/tome/xp_tome_t1.png` |
| XP Tome T2 | `art/glyphs/xp_tome_t2.glyph` | `assets/meridian/textures/item/tome/xp_tome_t2.png` |
| XP Tome T3 | `art/glyphs/xp_tome_t3.glyph` | `assets/meridian/textures/item/tome/xp_tome_t3.png` |
| Dormant XP Tome | `art/glyphs/dormant_xp_tome.glyph` | `assets/meridian/textures/item/tome/dormant_xp_tome.png` |
| Scrap Tome | `art/glyphs/scrap_tome.glyph` | `assets/meridian/textures/item/tome/scrap_tome.png` |
| Improved Scrap Tome | `art/glyphs/improved_scrap_tome.glyph` | `assets/meridian/textures/item/tome/improved_scrap_tome.png` |
| Extraction Tome | `art/glyphs/extraction_tome.glyph` | `assets/meridian/textures/item/tome/extraction_tome.png` |
| Infused Breath (animated, 8 frames) | `art/glyphs/infused_breath.glyph` | `assets/meridian/textures/item/infused_breath.png` (+ `.png.mcmeta`) |
| Prismatic Web | `art/glyphs/prismatic_web.glyph` | `assets/meridian/textures/item/prismatic_web.png` |
| Warden Tendril | `art/glyphs/warden_tendril.glyph` | `assets/meridian/textures/item/warden_tendril.png` |
| Sea-family shelves (`seashelf`, `crystal_seashelf`, `heart_seashelf`, `infused_seashelf`) + shared `prismarine_shelf_top` | `art/glyphs/shelves/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/{seashelf,crystal_seashelf,heart_seashelf,infused_seashelf,prismarine_shelf_top}.png` |
| Nether-family shelves (`hellshelf`, `glowing_hellshelf`, `blazing_hellshelf` [animated], `infused_hellshelf`) + shared `nether_brick_shelf_top` | `art/glyphs/shelves/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/{hellshelf,glowing_hellshelf,blazing_hellshelf,infused_hellshelf,nether_brick_shelf_top}.png` |
| Deepslate-family shelves (`deepshelf`, `dormant_deepshelf`, `echoing_deepshelf` [animated], `soul_touched_deepshelf` [animated]) + shared `deepslate_shelf_top` | `art/glyphs/shelves/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/{deepshelf,dormant_deepshelf,echoing_deepshelf,soul_touched_deepshelf,deepslate_shelf_top}.png` |
| End-family shelves (`endshelf`, `pearl_endshelf`, `draconic_endshelf` [animated]) + shared `end_stone_shelf_top` | `art/glyphs/shelves/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/{endshelf,pearl_endshelf,draconic_endshelf,end_stone_shelf_top}.png` |
| Sculk-family shelves (`echoing_sculkshelf` [animated], `soul_touched_sculkshelf` [animated]) + shared `sculkshelf_top` | `art/glyphs/shelves/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/{echoing_sculkshelf,soul_touched_sculkshelf,sculkshelf_top}.png` |
| Single shelves (`beeshelf`, `melonshelf`, `stoneshelf`) + custom tops | `art/glyphs/shelves/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/{beeshelf,melonshelf,stoneshelf,honeycomb_shelf_top,melon_shelf_top,stone_shelf_top}.png` |
| Device shelves — rectifier `t1/t2/t3`, sight `t1/t2`, treasure (sides + tops) | `art/glyphs/shelves/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/{rectifier,rectifier_t2,rectifier_t3,sight_side,sightshelf_t2,treasure_shelf_side}.png` + `{rectifier_t2_top,rectifier_t3_top,sight_top,sightshelf_t2_top,treasure_shelf_top}.png` (rectifier t1 reuses `prismarine_shelf_top`) |
| Enchantment library (`library/`, `ender_library/` — 5 faces each; `books` is a UV atlas mapped to the book stacks) | `art/glyphs/shelves/{library,ender_library}/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/{library,ender_library}/{side,side2,top,bottom,books}.png` |
| Filtering shelf (`filtering_shelf/` — `side`/`top` faces + `empty`/`occupied` slot atlases, vanilla chiseled-bookshelf UVs) | `art/glyphs/shelves/filtering_shelf/*.glyph` + `shelfgen.py` | `assets/meridian/textures/block/filtering_shelf/{side,top,empty,occupied}.png` |
| Enchanting table GUI | `art/gui/guigen.py` (stitched atlas compositor) | `assets/meridian/textures/gui/enchanting_table.png` |
| Library GUI | `art/gui/guigen.py` (stitched atlas compositor) | `assets/meridian/textures/gui/library.png` |
| SGA rune particles (104: a–z × fire/water/sculk/end) | `art/glyphs/sga/*.glyph` (26 shapes) + `render-sga.py` | `assets/meridian/textures/particle/sga_*.png` |

## Not yet created

| Asset | Source | Final asset |
|---|---|---|
| Shelf progression icons (per tier) | `/glyph` | — (planned, for tooltips/docs) |
| Website hero background | Gemini | — (planned, `site/`) |
| Open Graph image | Gemini | — (planned, `site/assets/`) |
| Discord embed banner | Gemini | — (planned) |
