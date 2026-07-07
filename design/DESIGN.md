# Meridian — Design Specification

> Enchanting Overhaul for Minecraft 1.21.1 Fabric

---

## 1. Brand Identity

### Narrative

Meridian transforms Minecraft's enchanting table from a flat, luck-based mechanic into a deep five-axis progression system. The name evokes celestial navigation — charting a course through arcane knowledge. The visual language draws from **astral cartography**, **runic stonework**, and **cosmic energy** — an enchanting table reimagined as a portal to the stars.

### Tagline

*"Chart your enchantments."*

### Logo Description

**Full Logo (`art/logo.png`):** A stone archway frames a glowing compass rose set against a star field. The compass has an eight-pointed design with a golden central star. Runic symbols (enchanting table glyphs) fill the background on dark blue-violet brickwork. Constellation lines and galaxy swirls weave through the scene. Below, the word "MERIDIAN" appears in a blocky pixel font on a stone tablet, with "MINECRAFT ENCHANTING OVERHAUL" as a subtitle. A crescent moon crowns the arch.

**Icon (`art/glyphs/astrolabe_logo.glyph`):** A circular astrolabe — a dark stone bezel rings a deep blue-violet night-sky dial; an eight-point compass rose in lavender and purple radiates from a glowing golden central star, with faint stars dusting the dial and a blue rim-glow haloing the bezel. No text. This single glyph is the source for every icon surface: the in-jar mod icon (`assets/meridian/icon.png`, shown in Mod Menu) and the website favicon/nav (`site/assets/icon.png`).

### Color Palette

| Role | Color | Hex | Usage |
|------|-------|-----|-------|
| Primary | Deep Violet | `#1a0a3e` | Backgrounds, dark surfaces |
| Secondary | Cosmic Blue | `#2a1a6e` | Mid-tones, card backgrounds |
| Accent 1 | Arcane Purple | `#7B2FBE` | Glows, highlights, interactive elements |
| Accent 2 | Enchant Gold | `#DAA520` | Titles, stars, key accents |
| Bright | Celestial Gold | `#FFD700` | Hover states, emphasis |
| Glow | Cyan Spark | `#00BFFF` | Particle effects, sparkles |
| Text Primary | Bone | `#e8e0d4` | Body text |
| Text Secondary | Ash | `#a89f93` | Muted text, descriptions |
| Text Tertiary | Smoke | `#6b6359` | Disabled, placeholder |
| Surface Base | Obsidian | `#0a0a0a` | Page backgrounds |
| Surface Card | Dark Stone | `#1a1a1a` | Cards, panels |
| Surface Elevated | Stone | `#222222` | Elevated surfaces, hover cards |

### Typography

- **Headings:** Pixel/blocky display font in gradient (`#DAA520` → `#FFD700`)
- **Body:** Monospace stack: SF Mono, Cascadia Code, Fira Code, Consolas
- **Website gradient animation:** `gold-pulse` keyframes (4s ease-in-out, brightness 1→1.15)

### Audio

Meridian's soundscape stays vanilla. Its cues are organic foley — the enchanting hum, the anvil clang, the sculk bloom, book pages — which vanilla already renders convincingly; synthesizing them would only make them feel fake. A custom synthesized cue (via the `/sfx` pipeline, concord `design/DESIGN-SYSTEM.md` §9) earns its place only where a sound needs its own identity, and none currently do. [`SPEC.md`](SPEC.md) owns the trigger-to-sound mapping.

---

## 2. HUD

Meridian occupies **no HUD slot**, by design — enchanting is table-interaction-based, with no persistent ambient stat that warrants an always-on element (concord [`HUD-STANDARD.md`](../../concord/HUD-STANDARD.md) §1: "opting out is conformant"). Eterna, Quanta, Arcana, Rectification, clues, shelf stats, and library contents surface through the enchanting-table screen, Jade/WTHIT overlays on enchanting blocks, and the EMI/REI/JEI recipe browser. [`SPEC.md`](SPEC.md) owns when and where each of these appears.

---

## 3. Assets

The full asset manifest — every `.glyph` source under `art/`, the final resource/site path it ships as, and what is still `MISSING` a glyph source — lives in [`ASSETS.md`](ASSETS.md). This document owns the *why and the look* of each asset family; the manifest owns the *where*.

---

## 4. Generation Prompts

### Gemini Prompts (Logos / High-Res Art)

**Open Graph / Social Card:**
```
Pixel art style, 1200x630 banner image for a Minecraft mod called "Meridian".
Center the logo: a stone archway framing a glowing eight-pointed compass rose
with a golden star center, set against a deep blue-violet cosmic background
with constellation lines and runic symbols. The word "MERIDIAN" in blocky
pixel font below. Dark violet (#1a0a3e) background. Subtle cyan and gold
particle effects. Style consistent with the existing Meridian logo.
```

**Website Hero Background:**
```
Pixel art tileable background texture, 1920x600. Dark blue-violet brickwork
(#1a0a3e to #2a1a6e gradient) with subtle enchanting table rune glyphs
scattered across the surface. Faint constellation lines connecting small
cyan (#00BFFF) star points. Very subtle — this is a background behind text.
Minecraft pixel art style, 16-pixel grid aligned.
```

**Discord Banner:**
```
Pixel art banner, 1280x640. The Meridian compass rose icon centered on a
dark violet (#1a0a3e) background. Soft purple-blue glow radiating from center.
Faint runic symbols in the background. "Meridian" in gold pixel font below
the icon. "Enchanting Overhaul" subtitle in lighter text. Clean, minimal.
```

### Glyph Specs (In-Game Pixel Art)

In-game pixel art — HUD/UI glyphs, recipe-browser icons, item textures, and
tier-icon sets — is authored through Concord's glyph pipeline: write the
ASCII-grid `.glyph` spec, then render it deterministically with `/glyph` (the
`mc-textures` skill is the craft reference). Every PNG master commits its
`.glyph` source beside it in `art/glyphs/`, so each texture re-renders from its
spec rather than being hand-patched. Design at the target size with hard pixels,
a limited palette, and an `ink` (#0a0a0a) 1px outline so the glyph reads against
any background. The normative spec is concord's `design/DESIGN-SYSTEM.md` §8.
The specs below seed that work.

**Recipe Browser Icon (EMI/REI/JEI Tab):**
```
Theme: Enchanting / arcane navigation
Subject: Compass rose with golden center star
Style: Minecraft item icon, pixel art
Size: 32x32
Colors: Deep violet background, gold (#DAA520) star, purple (#7B2FBE) compass points,
        cyan (#00BFFF) sparkle accents
Notes: Must read clearly at 16x16 downscale. No text. Single centered motif.
```

**Shelf Tier Icons (batch of 5):**
```
Theme: Enchanting shelf progression
Subject: Five 16x16 icons representing shelf tiers:
  1. Basic (wooden bookshelf) — warm brown, simple
  2. Nether (hellshelf) — orange/red glow, netherbrick texture
  3. Deep (deepshelf) — dark teal/sculk, echo particles
  4. End (endshelf) — pale yellow/purple, end stone texture
  5. Treasure (special) — gold with enchant glint
Style: Minecraft item icons, pixel art, consistent set
Size: 16x16 each
Colors: Each uses its biome-appropriate sub-palette while keeping gold (#DAA520) as
        the unifying accent
```

---

## 5. Image References

| Image | Reference Source | Notes |
|-------|----------------|-------|
| Compass rose motif | Meridian-Icon.png | Eight-pointed star, stone arch frame |
| Runic background | Meridian-Logo.png background | Enchanting table glyphs on brickwork |
| Color temperature | Meridian-Logo.png | Deep violet → cosmic blue gradient |
| Glow style | Meridian-Icon.png outer glow | Purple-blue radial, not hard-edged |
| Pixel art item style | `assets/meridian/textures/item/tome/xp_tome.png` | Tome sprite — sets the in-game item pixel density |
| Shelf textures | `src/main/resources/assets/meridian/textures/block/` | Reference for shelf tier icon colors |
| Constellation overlay | Meridian-Logo.png background | Thin cyan lines connecting star points |
| Stone texture | Meridian-Icon.png arch | Dark gray stone with vine/tendril wrapping |

---

## 6. Website & Listing Brand Notes

How the Meridian brand lands on its web and store surfaces. The page and listing copy themselves are content, not brand — they live in `site/` and render through the shared Concord template; this section carries only the brand direction.

- **Domain:** `meridian.rfizzle.com`. The site is built from `site/` (JSON page content plus the `site.json` theme) by the shared Concord template and deployed by the `site.yml` Actions workflow.
- **Accent usage:** gold (`#DAA520` → `#FFD700`) over deep violet (`#1a0a3e` / `#2a1a6e`); the compass-rose / astrolabe motif anchors the hero against runic blue-violet brickwork threaded with faint cyan (`#00BFFF`) constellation lines.
- **Open Graph / social:** the full logo (`art/logo.png`) as an absolute-URL `og:image` on a `summary_large_image` card; the favicon and apple-touch icon derive from the astrolabe glyph (`site/assets/icon.png`).
- **Store screenshots (art direction):** 1920×1080, gold-and-violet lighting; subjects that read the brand at a glance — a full shelf wall ringing the table, the five-stat enchanting screen, a library in use, and a signature enchantment firing.

Content lives elsewhere: page copy in `site/pages/*.json`, theme tokens in `site/site.json`, and store listing copy in `site/listing-curseforge.md` / `site/listing-modrinth.md` (authored per the `mc-listing` skill).

---

## 7. Concord Context

Meridian is the **enchanting** overhaul in Concord, a four-mod suite of independent system overhauls. Its **violet + gold** signature and **compass-rose** motif read distinct from its siblings:

| Mod | Domain | Color Signature | Icon Motif |
|-----|--------|----------------|------------|
| **Meridian** | Enchanting | Violet / Gold | Compass rose |
| **Mercantile** | Villagers & Trade | Green / Emerald | Market stall / scales |
| **Tribulation** | Difficulty & Scaling | Crimson / Red | Hourglass with hearts |
| **Prosperity** | Loot & Containers | Gold / Diamond Cyan | Trophy chalice |

The suite-wide rules the four share — base website theme, neutral text palette, typography stack, logo formula, and the standard optional integrations — are the standard, not restated here: see concord's [`design/DESIGN-SYSTEM.md`](../../concord/design/DESIGN-SYSTEM.md) and the collection [`VISION.md`](../../concord/VISION.md).
