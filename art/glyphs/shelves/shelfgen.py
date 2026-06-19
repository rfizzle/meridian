#!/usr/bin/env python3
"""Parametric author for Meridian shelf .glyph sources.

Composes each shelf SIDE (32x32) from shared parts -- a material frame, recessed
twin shelves of books, and a themed accent -- plus a shared seamless top per
family. The frame/interior/book/top routines draw with fixed role characters
(P base, h/m bevels, k/K/S interior, b/B ledges, t/T..n/N book spines, W page);
each family supplies a palette mapping those chars to its hexes, so one set of
routines renders every theme. Accents add their own chars to the palette.

Writes the .glyph grids into this directory; render them to PNG with glyph.py.
Re-run after editing to regenerate the sources (do not hand-edit the generated
.glyph files).

Families: sea (prismarine), nether (nether brick, incl. animated blazing).
"""
import math
import os
OUT = os.path.dirname(os.path.abspath(__file__))

W = Hh = 32
def grid(): return [['k' for _ in range(W)] for _ in range(Hh)]
def rect(g, x0, y0, x1, y1, ch):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < W and 0 <= y < Hh: g[y][x] = ch

# ---------------------------------------------------------------- palettes ----
SEA = {
 '.': 'transparent',
 'm': '#22403a', 'F': '#335c54', 'P': '#44726a', 'H': '#5e8f84', 'h': '#84b6a9',
 'K': '#0d1615', 'k': '#16221f', 'S': '#1c2b27', 'q': '#1f4a4d', 'Q': '#2c6f72',
 'b': '#436a61', 'B': '#2c4a44',
 't': '#2f8a80', 'T': '#1c5a52', 'a': '#3aa6b8', 'A': '#226e7a',
 'u': '#34589e', 'U': '#20396b', 'g': '#3f8a55', 'G': '#275e39', 'n': '#bba473', 'N': '#8a7550',
 'W': '#dccda8',
 'c': '#4eeaed', 'C': '#c4fcfb', 'e': '#2bb3c0', 'o': '#1d6f86',
 'w': '#eafdfd',
 # heart-of-the-sea orb: dark navy body, pale cyan veins, blue glow
 'd': '#0f1d3d', 'D': '#1b2f5e', 'V': '#2a4a86', 'i': '#8fd3e0', 'J': '#1c3a66',
}

NETHER = {
 '.': 'transparent',
 # nether-brick frame (dark maroon, lit top/left)
 'm': '#150c0d', 'F': '#1d1113', 'P': '#2c1a1c', 'H': '#5c393b', 'h': '#4d3032',
 # warm-dark interior + ledges
 'K': '#0c0708', 'k': '#150d0e', 'S': '#1b1012', 'b': '#46291f', 'B': '#291712',
 # books: crimson / ember / charcoal / warped teal / gold
 't': '#9a363c', 'T': '#5a2228', 'a': '#cf6a26', 'A': '#8a3a18',
 'u': '#3c333a', 'U': '#241d22', 'g': '#2f8f7e', 'G': '#1f5e54', 'n': '#c39a44', 'N': '#7e5f22',
 'W': '#c9bda6',
 # accents: nether wart / glowstone / blaze fire (+ warm glow halo)
 'M': '#6e1b26', 'R': '#a8313c', 'I': '#cf5560', 'q': '#250a0e',
 'z': '#ffe7a2', 'y': '#ffc94e', 'x': '#c2801f', 'j': '#3a1c06',
 'f': '#ffd84a', 'l': '#ff9e2c', 'r': '#e85a16', 'v': '#7c2408',
}

# -------------------------------------------------------------- shared parts ----
def frame(g):
    rect(g, 0, 0, 31, 31, 'P')
    rect(g, 0, 0, 31, 0, 'h'); rect(g, 0, 0, 0, 31, 'h')          # outer lit edge
    rect(g, 31, 0, 31, 31, 'm'); rect(g, 0, 31, 31, 31, 'm')      # outer dark edge
    rect(g, 2, 2, 29, 2, 'H'); rect(g, 2, 2, 2, 29, 'H')          # inner lit bevel
    rect(g, 29, 2, 29, 29, 'F'); rect(g, 2, 29, 29, 29, 'F')      # inner shade bevel
    for x in (8, 16, 24): rect(g, x, 0, x, 2, 'm')                # brick seams
    for x in (4, 12, 20, 28): rect(g, x, 29, x, 31, 'm')
    for y in (8, 16, 24): rect(g, 0, y, 2, y, 'm')
    for y in (5, 13, 21, 29): rect(g, 29, y, 31, y, 'm')

def interior(g):
    rect(g, 3, 3, 28, 28, 'k')
    rect(g, 3, 3, 28, 4, 'K'); rect(g, 3, 3, 4, 28, 'K')          # depth shadow top/left
    rect(g, 28, 3, 28, 28, 'S'); rect(g, 3, 28, 28, 28, 'S')
    for y in (15, 27):                                            # ledges
        rect(g, 3, y, 28, y, 'b'); rect(g, 3, y + 1, 28, y + 1, 'B')

def books(g, y_base, y_top, seed, xlo=4, xhi=27):
    pal = [('T', 't'), ('A', 'a'), ('U', 'u'), ('G', 'g'), ('N', 'n')]
    widths = [3, 2, 4, 2, 3, 2, 4, 3]; leans = [0, 0, 1, 0, -1, 0, 0, 1]
    hmax = y_base - y_top
    heights = [hmax, hmax - 3, hmax - 1, hmax - 5, hmax - 2, hmax - 4, hmax]
    x = xlo; i = seed
    while x <= xhi - 1:
        w = widths[i % len(widths)]
        if x + w - 1 > xhi: break
        ht = max(4, heights[i % len(heights)]); lean = leans[i % len(leans)]
        sd, sl = pal[i % len(pal)]; top = y_base - ht
        for yy in range(top, y_base + 1):
            sh = lean if yy < top + 2 else 0
            for xx in range(x, x + w):
                xc = min(xhi, max(xlo, xx + sh))
                g[yy][xc] = sl if xx == x else sd
        cap = min(xhi, max(xlo, x + lean))
        rect(g, cap, top, min(xhi, cap + w - 1), top, 'W')        # page top
        x += w + 1; i += 1

def _halo(g, cx, cy, pts, tok):
    for dx, dy, _ in pts:
        for hx, hy in ((dx - 1, dy), (dx + 1, dy), (dx, dy - 1), (dx, dy + 1)):
            X, Y = cx + hx, cy + hy
            if 0 <= X < W and 0 <= Y < Hh and g[Y][X] in ('k', 'S', 'K'): g[Y][X] = tok

# --------------------------------------------------------------- sea accents ----
def _shard(cx, base, tall):
    if tall:
        return [(0, -4, 'w'), (0, -3, 'C'), (-1, -2, 'c'), (0, -2, 'c'), (1, -2, 'e'),
                (-1, -1, 'c'), (0, -1, 'e'), (1, -1, 'e'), (-1, 0, 'o'), (0, 0, 'e'), (1, 0, 'o')]
    return [(0, -2, 'C'), (0, -1, 'c'), (1, -1, 'e'), (0, 0, 'o'), (1, 0, 'o')]

def crystal(g, cx, cy, big=False):
    pts = [(dx, dy, ch) for (dx, dy, ch) in _shard(cx, cy, True)]
    pts += [(dx - 3, dy + 1, ch) for (dx, dy, ch) in _shard(0, 0, False)]
    pts += [(dx + 2, dy + 1, ch) for (dx, dy, ch) in _shard(0, 0, False)]
    if big:
        pts += [(dx + 4, dy, ch) for (dx, dy, ch) in _shard(0, 0, True)]
        pts += [(dx - 4, dy - 1, ch) for (dx, dy, ch) in _shard(0, 0, False)]
    _halo(g, cx, cy, pts, 'q')
    for dx, dy, ch in pts:
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

def heart(g, cx, cy):
    # Heart of the Sea: round dark-navy orb, pale cyan marbled veins, top-left glint
    shade = {                       (-1, -3): 'V', (0, -3): 'D', (1, -3): 'V',
                (-2, -2): 'V', (-1, -2): 'C', (0, -2): 'D', (1, -2): 'D', (2, -2): 'V',
                (-2, -1): 'D', (-1, -1): 'i', (0, -1): 'i', (1, -1): 'D', (2, -1): 'V',
    (-3, 0): 'V', (-2, 0): 'D', (-1, 0): 'd', (0, 0): 'i', (1, 0): 'i', (2, 0): 'D', (3, 0): 'V',
                (-2, 1): 'D', (-1, 1): 'd', (0, 1): 'd', (1, 1): 'i', (2, 1): 'D',
                (-2, 2): 'V', (-1, 2): 'D', (0, 2): 'D', (1, 2): 'D', (2, 2): 'V',
                            (-1, 3): 'V', (0, 3): 'D', (1, 3): 'V'}
    pts = [(dx, dy, ch) for (dx, dy), ch in shade.items()]
    _halo(g, cx, cy, pts, 'J')
    for (dx, dy), ch in shade.items(): g[cy + dy][cx + dx] = ch

# ------------------------------------------------------------ nether accents ----
def wart(g, cx, cy):
    # crimson nether-wart clump sitting on the ledge (base row cy)
    shade = {(-1, 0): 'M', (0, 0): 'M', (1, 0): 'M',
             (-2, -1): 'R', (-1, -1): 'M', (0, -1): 'R', (1, -1): 'M', (2, -1): 'R',
             (-2, -2): 'I', (-1, -2): 'R', (1, -2): 'R', (2, -2): 'I',
             (0, -2): 'R', (-1, -3): 'I', (0, -3): 'R', (1, -3): 'I'}
    pts = [(dx, dy, ch) for (dx, dy), ch in shade.items()]
    _halo(g, cx, cy, pts, 'q')
    for (dx, dy), ch in shade.items(): g[cy + dy][cx + dx] = ch

def glowstone(g, cx, cy, big=False):
    # cluster of golden glowing blobs
    blobs = [(0, 0), (-2, 0), (2, -1), (-1, -2), (1, 1)]
    if big: blobs += [(3, 1), (-3, -1), (0, -3)]
    cells = {}
    for bx, by in blobs:
        cells[(bx, by)] = 'z'
        for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
            cells.setdefault((bx + dx, by + dy), 'y')
        cells.setdefault((bx + 1, by + 1), 'x'); cells.setdefault((bx - 1, by + 1), 'x')
    pts = [(dx, dy, ch) for (dx, dy), ch in cells.items()]
    _halo(g, cx, cy, pts, 'j')
    for (dx, dy), ch in cells.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

def blaze_fire(g, fr, xlo=4, xhi=27, base=26):
    # animated flames filling the bottom shelf; flicker varies per frame
    for x in range(xlo, xhi + 1):
        phase = x * 1.25 + fr * 1.15
        h = 5 + int(round(2.4 * (1 + math.sin(phase)))) + (x % 2)
        top = base - h
        for y in range(max(3, top), base + 1):
            frac = (base - y) / max(1, h)
            ch = 'f' if frac > 0.8 else 'l' if frac > 0.55 else 'r' if frac > 0.3 else 'v'
            g[y][x] = ch
        if top - 1 >= 3 and g[top - 1][x] == 'k': g[top - 1][x] = 'j'

# -------------------------------------------------------------------- emit ----
def _legend(palette): return [f"  {k} {v}" for k, v in palette.items()]

def emit(name, build, palette=SEA, comment=None):
    g = grid(); frame(g); interior(g); build(g)
    comment = comment or f"# {name} side (sea family) — prismarine frame, sea books, themed accent."
    out = [comment, "size: 32", "", "legend:"] + _legend(palette) + ["", "grid:"]
    out += ["  " + "".join(r) for r in g]
    open(f"{OUT}/{name}.glyph", "w").write("\n".join(out) + "\n")

def emit_anim(name, builders, palette, comment, frametime=3, interpolate=True):
    out = [comment, "size: 32", f"frametime: {frametime}",
           f"interpolate: {'true' if interpolate else 'false'}", "", "legend:"] + _legend(palette) + [""]
    for b in builders:
        g = grid(); frame(g); interior(g); b(g)
        out += ["frame:"] + ["  " + "".join(r) for r in g]
    open(f"{OUT}/{name}.glyph", "w").write("\n".join(out) + "\n")

def top(name, palette, comment, speckle):
    g = grid(); rect(g, 0, 0, 31, 31, 'P')
    for y in range(32):
        for x in range(32):
            g[y][x] = ['H', 'P', 'F', 'P'][((x // 8) + (y // 8)) % 4]
    for x in range(0, 32, 8): rect(g, x, 0, x, 31, 'm')
    for y in range(0, 32, 8): rect(g, 0, y, 31, y, 'm')
    for y in range(4, 32, 8): rect(g, 0, y, 31, y, 'F')
    for x, y, ch in speckle: g[y][x] = ch
    out = [comment, "size: 32", "", "legend:"] + _legend(palette) + ["", "grid:"]
    out += ["  " + "".join(r) for r in g]
    open(f"{OUT}/{name}.glyph", "w").write("\n".join(out) + "\n")

# ------------------------------------------------------------------ sea family ----
def sea(g):       books(g, 26, 18, 2, xhi=19); books(g, 14, 5, 0); crystal(g, 23, 25)
def crystalsh(g): books(g, 14, 7, 3, xhi=13); crystal(g, 20, 13); crystal(g, 22, 26, big=True); crystal(g, 8, 26)
def heartsh(g):   books(g, 14, 6, 1); books(g, 26, 18, 4, xlo=4, xhi=16); heart(g, 22, 23)
emit("seashelf", sea); emit("crystal_seashelf", crystalsh); emit("heart_seashelf", heartsh)
top("prismarine_shelf_top", SEA,
    "# shared prismarine shelf top/bottom (sea family), seamless.",
    [(5, 3, 'c'), (21, 19, 'c'), (27, 11, 'C'), (11, 27, 'c')])

# --------------------------------------------------------------- nether family ----
NC = "# {} side (nether family) — nether-brick frame, nether books, {} accent."
def hell(g):     books(g, 26, 18, 1, xhi=27); books(g, 14, 5, 3, xhi=19); wart(g, 23, 26)
def glowing(g):  books(g, 14, 6, 2, xhi=18); books(g, 26, 18, 0, xhi=18); glowstone(g, 22, 23, big=True)
emit("hellshelf", hell, NETHER, NC.format("hellshelf", "nether-wart"))
emit("glowing_hellshelf", glowing, NETHER, NC.format("glowing_hellshelf", "glowstone"))

def blazing_frame(fr):
    def build(g):
        books(g, 14, 5, 3, xhi=27)   # static top shelf
        blaze_fire(g, fr)            # animated bottom shelf
    return build
emit_anim("blazing_hellshelf", [blazing_frame(i) for i in range(7)], NETHER,
          "# blazing_hellshelf side (nether family) — top books static, bottom shelf ablaze (7-frame loop).")

top("nether_brick_shelf_top", NETHER,
    "# shared nether-brick shelf top/bottom (nether family), seamless.",
    [(6, 5, 'r'), (20, 22, 'v'), (26, 12, 'r')])

print("emitted sea + nether families")
