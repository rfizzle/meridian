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

**Icon (`art/icon-128.png`):** The compass rose and stone archway isolated — no text. The golden star center radiates against deep blue-violet. The arch has twisted vine/tendril detailing. A purple-blue glow emanates outward.

**In-Game Icon (`assets/meridian/icon.png`):** A pixel-art open spellbook — purple covers with gold trim, glowing white pages, cyan sparkle particles above.

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

---

## 2. Asset Inventory

### Existing Assets

| Asset | Location | Size | Status |
|-------|----------|------|--------|
| Logo master | `art/logo.png` | 2760×1504 | Final |
| Icon master | `art/icon-128.png` | 128×128 | Final |
| Site Logo | `site/assets/logo.png` | 1280×698 | Derived from master |
| Site Icon | `site/assets/icon.png` | 256×256 | Derived from master |
| In-Game Icon | `src/main/resources/assets/meridian/icon.png` | 128×128 | Final — pixel-art spellbook |

### Needed Assets

| Asset | Generator | Priority | Spec |
|-------|-----------|----------|------|
| Recipe browser icon (EMI/REI/JEI tab) | `/glyph` | High | 16×16 or 32×32 pixel art, compass rose or enchanting table motif, violet/gold palette |
| Shelf progression icons (per tier) | `/glyph` | Medium | 16×16 pixel art icons for each shelf tier, used in tooltips/docs |
| Website hero background | Gemini | Medium | Wide banner (1920×600) — runic brickwork with constellation overlay, dark violet |
| Open Graph image | Gemini | Medium | 1200×630, logo centered on dark background with subtle glow |
| CurseForge gallery screenshots | Screenshot | High | 1920×1080, Complementary Shaders, showing enchanting table with shelves |
| Favicon (`.ico` / `.svg`) | Derived | Low | 32×32 / 16×16 from icon |
| Apple Touch Icon | Derived | Low | 180×180 from icon |
| Discord embed banner | Gemini | Low | 1280×640, logo on dark background |

---

## 3. Generation Prompts

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

### HUD

Meridian occupies **no HUD slot**, by design — enchanting is table-interaction-based, with no persistent ambient stat that warrants an always-on element (concord [`HUD-STANDARD.md`](../../concord/HUD-STANDARD.md) §1: "opting out is conformant"). Eterna, Quanta, Arcana, Rectification, clues, shelf stats, and library contents surface through Jade/WTHIT overlays on enchanting blocks and EMI/REI/JEI recipe-browser integration.

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

## 4. Image References

| Image | Reference Source | Notes |
|-------|----------------|-------|
| Compass rose motif | Meridian-Icon.png | Eight-pointed star, stone arch frame |
| Runic background | Meridian-Logo.png background | Enchanting table glyphs on brickwork |
| Color temperature | Meridian-Logo.png | Deep violet → cosmic blue gradient |
| Glow style | Meridian-Icon.png outer glow | Purple-blue radial, not hard-edged |
| Pixel art item style | `assets/meridian/icon.png` | Open book with sparkles — sets the in-game pixel density |
| Shelf textures | `src/main/resources/assets/meridian/textures/block/` | Reference for shelf tier icon colors |
| Constellation overlay | Meridian-Logo.png background | Thin cyan lines connecting star points |
| Stone texture | Meridian-Icon.png arch | Dark gray stone with vine/tendril wrapping |

---

## 5. Website Specification

### Domain & Hosting

- **Domain:** `meridian.rfizzle.com`
- **Hosting:** GitHub Pages from `docs/` directory
- **CNAME:** `docs/CNAME` → `meridian.rfizzle.com`

### Current Pages

| Page | File | Content |
|------|------|---------|
| Home | `index.html` | Hero with logo, feature overview, download links |
| Enchantments | `enchantments.html` | Full enchantment list with categories |
| Shelves | `shelves.html` | Shelf blocks with stats and tier progression |
| Config | `config.html` | Configuration reference |
| Commands | `commands.html` | Command reference |

### Pages to Add

| Page | File | Content |
|------|------|---------|
| Getting Started | `guide.html` | Installation, first enchanting table setup, stat explanation |
| FAQ | `faq.html` | Common questions, compatibility, performance |
| Changelog | `changelog.html` | Version history with dates |
| Stat Calculator | `calculator.html` | Interactive shelf layout → stat calculator (JS) |

### Website Design Tokens (Tailwind)

```javascript
colors: {
    base: '#0a0a0a',
    card: '#1a1a1a',
    elevated: '#222222',
    gold: { DEFAULT: '#DAA520', dark: '#8B6914' },
    amber: { DEFAULT: '#F0C040', bright: '#FFD700' },
    bone: '#e8e0d4',
    ash: '#a89f93',
    smoke: '#6b6359',
}
```

### SEO & Social

- **Title pattern:** `{Page} — Meridian | Enchanting Overhaul for Minecraft`
- **og:image:** Must be absolute URL (`https://meridian.rfizzle.com/logo.png`)
- **twitter:card:** `summary_large_image` (upgrade from `summary`)
- **Favicon:** `<link rel="icon" type="image/png" href="icon.png">`
- **Apple Touch:** `<link rel="apple-touch-icon" href="apple-touch-icon.png">` (need to create)

### Cross-Mod Navigation

Footer section linking to all companion mods:
```
Part of the rfizzle mod suite:
[Meridian] [Mercantile] [Tribulation] [Prosperity]
```

---

## 6. Distribution Listings

### CurseForge / Modrinth

**Description Template:**
1. Logo image (centered)
2. One-paragraph summary
3. Feature list with headers (Stat System, Shelves, Enchantments, Libraries, Tomes, Anvil)
4. Screenshot gallery (3–5 images)
5. Requirements section (Fabric Loader, Fabric API, Cloth Config)
6. Optional dependencies (EMI/REI/JEI, Jade/WTHIT)
7. Links to companion mods

**Screenshot Standards:**
- Resolution: 1920×1080
- Shader: Complementary Shaders (or vanilla for clarity shots)
- HUD: Visible but not cluttered
- Subjects: (1) Enchanting table with full shelf setup, (2) Enchanting GUI with stats, (3) Library block in use, (4) Shelf variety showcase, (5) Custom enchantment in action

**Changelog Format:**
```markdown
## [0.1.0] — 2025-XX-XX
### Added
- Feature description
### Changed
- Change description
### Fixed
- Fix description
```

### README Badges

```markdown
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)
![GitHub](https://img.shields.io/github/v/release/rfizzle/meridian)
```

---

## 7. Companion Mod Context

Meridian is part of a four-mod suite. Each mod overhauls a different Minecraft system:

| Mod | Domain | Color Signature | Icon Motif |
|-----|--------|----------------|------------|
| **Meridian** | Enchanting | Violet / Gold | Compass rose |
| **Mercantile** | Villagers & Trade | Green / Emerald | Market stall / scales |
| **Tribulation** | Difficulty & Scaling | Crimson / Red | Hourglass with hearts |
| **Prosperity** | Loot & Containers | Gold / Diamond Cyan | Trophy chalice |

All four share:
- Minecraft 1.21.1, Java 21, Fabric
- Dark base website theme (`#0a0a0a` / `#1a1a1a` / `#222222`)
- Bone/Ash/Smoke text palette
- Monospace font stack
- Pixel art logo style (Gemini-generated)
- Same website structural pattern (hero → features → config → commands)
- MIT license
- Optional Jade/WTHIT, EMI/REI/JEI, ModMenu, Cloth Config integrations
