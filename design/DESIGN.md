# Meridian — Design Specification

> Enchanting Overhaul for Minecraft 1.21.1 Fabric

---

## 1. Brand Identity

### Narrative

Meridian transforms Minecraft's enchanting table from a flat, luck-based mechanic into a deep five-axis progression system. The name evokes celestial navigation — charting a course through arcane knowledge. The visual language draws from **astral cartography**, **runic stonework**, and **cosmic energy** — an enchanting table reimagined as a portal to the stars.

### Tagline

*"Chart your enchantments."*

### Motif

The **compass rose** — an eight-pointed rose with a golden central star, the navigator's instrument for charting a course. It is the one object Meridian owns (concord `design/DESIGN-SYSTEM.md` §4): it anchors the full logo, is rendered as the circular astrolabe dial in the icon glyph, and may appear in site headers and flavor art. It never appears in another member's assets, and Meridian uses no sibling's motif (no scales, hourglass, or chalice).

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

Shared neutrals (text and surfaces) follow the standard tokens as-is —
`--color-bone`, `--color-ash`, `--color-smoke`, `--color-ink`,
`--color-card`, `--color-elevated` — see concord
[`design/DESIGN-SYSTEM.md`](../../concord/design/DESIGN-SYSTEM.md) §2.

### Typography

- **Headings:** Pixel/blocky display font in gradient (`#DAA520` → `#FFD700`)
- **Body:** Monospace stack: SF Mono, Cascadia Code, Fira Code, Consolas
- **Website gradient animation:** `gold-pulse` keyframes (4s ease-in-out, brightness 1→1.15)

---

## 2. HUD

Meridian occupies **no HUD slot**, by design — enchanting is table-interaction-based, with no persistent ambient stat that warrants an always-on element (concord [`HUD-STANDARD.md`](../../concord/HUD-STANDARD.md) §1: "opting out is conformant"). Eterna, Quanta, Arcana, Rectification, clues, shelf stats, and library contents surface through the enchanting-table screen, Jade/WTHIT overlays on enchanting blocks, and the EMI/REI/JEI recipe browser. [`SPEC.md`](SPEC.md) owns when and where each of these appears.

---

## 3. Assets

The full asset manifest — every `.glyph` source under `art/`, the final resource/site path it ships as, and what is still `MISSING` a glyph source — lives in [`ASSETS.md`](ASSETS.md). This document owns the *why and the look* of each asset family; the manifest owns the *where*.

- **Textures:** original pixel art through the glyph pipeline (`art/glyphs/`), in the violet/gold palette above — shelves, tomes, specialty materials, the SGA rune particle sprites. Nothing is Zenith/Apotheosis-derived.
- **Audio:** stays vanilla. Meridian's cues are organic foley — the enchanting hum, the anvil clang, the sculk bloom, book pages — which vanilla already renders convincingly; synthesizing them would only make them feel fake. A custom synthesized cue (via the `/sfx` pipeline, concord `design/DESIGN-SYSTEM.md` §9) earns its place only where a sound needs its own identity, and none currently do. [`SPEC.md`](SPEC.md) owns the trigger-to-sound mapping.

---

## 4. Generation Prompts

The non-glyph masters — `art/logo.png` (full logo), `art/icon-128.png` / `art/icon-512.png` — predate this document's prompt-keeping rule and have **no committed prompt**; their look is specified by the Logo Description above and the palette hexes, which is what a regeneration should be prompted from (stone archway, eight-point compass rose, golden central star, blue-violet runic brickwork `#1a0a3e` / `#2a1a6e`, gold `#DAA520` → `#FFD700`, cyan `#00BFFF` constellation lines, crescent moon). Every pixel-art asset is a `.glyph` source under `art/glyphs/` and is regenerated from that file — referenced from [`ASSETS.md`](ASSETS.md), never duplicated here. Recording the prompt the next time a master is regenerated is the open decision below.

---

## 5. Image References

No exploration or reference images are committed (`art/exploration/` does not exist). The brand's references are the vanilla enchanting table's glyph book and the Standard Galactic Alphabet, and real astrolabe and compass-rose engravings — the runic-stone plus celestial-instrument register the Narrative names.

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

---

## Open Decisions

- **Master prompts:** the next regeneration of `art/logo.png` or the icon masters should commit its prompt under §4 so the art stays regenerable without reverse-engineering the description.
