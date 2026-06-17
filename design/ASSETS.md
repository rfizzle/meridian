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

## Branding masters (`.png` — not glyph-based)

| Asset | `art/` master | Final / derived copies |
|---|---|---|
| Full logo | `art/logo.png` | `site/assets/logo.png` |
| Mod icon (128) | `art/icon-128.png` | `assets/meridian/icon.png` (in-jar), `site/assets/icon.png` |

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
| Enchanting shelf + library blocks (≈40 textures) | **MISSING** | `assets/meridian/textures/block/*.png` — shelves (`hellshelf`, `seashelf`, `deepshelf`, `endshelf`, `sculkshelf`, variants…), `library/`, `ender_library/`, `filtering_shelf/`, `rectifier*`, sight/treasure shelves. Sourced from Zenith (orig. Apotheosis) per README attribution — not glyph-authored |
| Enchanting table GUI | **MISSING** | `assets/meridian/textures/gui/enchanting_table.png` |
| Library GUI | **MISSING** | `assets/meridian/textures/gui/library.png` |
| SGA rune particles (104: a–z × fire/water/sculk/end) | **MISSING** | `assets/meridian/textures/particle/sga_*.png` |

## Not yet created

| Asset | Source | Final asset |
|---|---|---|
| Recipe browser icon (EMI/REI/JEI tab) | `/glyph` | — (planned) |
| Shelf progression icons (per tier) | `/glyph` | — (planned, for tooltips/docs) |
| Website hero background | Gemini | — (planned, `site/`) |
| Open Graph image | Gemini | — (planned, `site/assets/`) |
| Discord embed banner | Gemini | — (planned) |
