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

DEEP = {
 '.': 'transparent',
 # deepslate frame (cool grey, lit top/left)
 'm': '#15161b', 'F': '#212329', 'P': '#3a3e46', 'H': '#565b66', 'h': '#4e535d',
 # cold-dark interior + ledges
 'K': '#0c0d10', 'k': '#16181d', 'S': '#1c1e24', 'b': '#3a3a42', 'B': '#222329',
 # books: teal / steel-blue / slate / amethyst / pale stone
 't': '#2f8f8a', 'T': '#1c5a58', 'a': '#3f74a6', 'A': '#27496e',
 'u': '#444b57', 'U': '#2a2e36', 'g': '#6a47a0', 'G': '#3f2a63', 'n': '#9aa0aa', 'N': '#6a6f78',
 'W': '#cfd3da',
 # amethyst shards (reuses crystal() chars, recoloured purple)
 'c': '#b58cf0', 'C': '#d9c2ff', 'e': '#8a5fce', 'o': '#5a3596', 'w': '#ece0ff', 'q': '#34205a',
 # echo shard: dark teal body, bright cyan core, teal glow
 'i': '#46e9ed', 'D': '#1f5a64', 'd': '#123238', 'V': '#bdf6f8', 'J': '#0e3b42',
 # soul fire: blue flame, white-hot core, blue glow halo
 'f': '#d4f7ff', 'l': '#62d8f6', 'r': '#2f97d6', 'v': '#1b4f86', 'j': '#0d2c4e',
}

END = {
 '.': 'transparent',
 # end-stone-brick frame (pale yellow-tan, lit top/left)
 'm': '#908b62', 'F': '#a8a374', 'P': '#d4cf94', 'H': '#e0dba8', 'h': '#e6e1b4',
 # dark neutral interior + pale ledges
 'K': '#0e0e12', 'k': '#1a1a20', 'S': '#222230', 'b': '#b6b07e', 'B': '#827d56',
 # books: ender-teal / slate / purpur / sage / bone + chorus / amber / ender-blue
 't': '#36a596', 'T': '#1f6b62', 'a': '#5a6678', 'A': '#3a4250',
 'u': '#8a6aa6', 'U': '#5a3f73', 'g': '#9aa67e', 'G': '#6f7a5e', 'n': '#cfc4a0', 'N': '#9a8f72',
 'x': '#c06a96', 'X': '#7a3358', 'y': '#d8b056', 'Y': '#9a7a32', 'z': '#42588a', 'Z': '#26314e',
 'W': '#efe9c6',
 # ender pearl: teal orb, pale glint, teal glow halo
 'c': '#3fd6c0', 'C': '#9af2e4', 'e': '#28a08e', 'o': '#176b5e', 'w': '#d6fff7', 'q': '#0e3b34',
 # dragon egg: black-purple body, magenta speckles, purple glow halo
 'd': '#1a0f24', 'D': '#2c1a3e', 'V': '#43285e', 'i': '#e03ca0', 'I': '#ff8fd0', 'J': '#2a0e3e',
}

SCULK = {
 '.': 'transparent',
 # sculk frame (black-blue, lit edges dark teal)
 'm': '#060c0f', 'F': '#0a1418', 'P': '#0e1a1f', 'H': '#1d4450', 'h': '#16323a',
 # near-black interior + ledges
 'K': '#05090c', 'k': '#0a1015', 'S': '#0f1a20', 'b': '#16323a', 'B': '#0c1c22',
 # books: teal / sculk-cyan / slate / deep-blue / bone
 't': '#27a0a0', 'T': '#155a5a', 'a': '#2cc0d4', 'A': '#1a6e7e',
 'u': '#33424f', 'U': '#1a2630', 'g': '#356a9e', 'G': '#1c3a5e', 'n': '#b8b8a2', 'N': '#7a7a6a',
 'W': '#c0ccc8',
 # sculk veins
 'C': '#29dfeb',
 # echo shard: bright cyan core, teal body, teal glow
 'i': '#33e6ec', 'D': '#1a5a64', 'd': '#0e2e34', 'V': '#bdf6f8', 'J': '#0a2e34',
 # soul fire: blue flame, white-hot core, blue glow halo
 'f': '#d4f7ff', 'l': '#5fd6f5', 'r': '#2f97d6', 'v': '#1b4f86', 'j': '#0d2c4e',
}

BEE = {
 '.': 'transparent',
 'm': '#7a5410', 'F': '#b07e18', 'P': '#e0a72a', 'H': '#f0c040', 'h': '#f3c84e',
 'K': '#190f04', 'k': '#2a1d08', 'S': '#33240c', 'b': '#caa030', 'B': '#8a6a18',
 't': '#e0a030', 'T': '#a86a14', 'a': '#f0c84a', 'A': '#c88a1a',
 'u': '#8a6418', 'U': '#5a3e10', 'g': '#e8d088', 'G': '#c8a050', 'n': '#d6c498', 'N': '#9a8358',
 'W': '#f5ecc0',
 'y': '#ffd23a', 'x': '#2a1c06', 'w': '#fff4d2', 'o': '#8a5a10',  # bee
}

MELON = {
 '.': 'transparent',
 'm': '#274516', 'F': '#356020', 'P': '#4a7a2a', 'H': '#7ab84a', 'h': '#6fa83e',
 'K': '#0c1407', 'k': '#16220e', 'S': '#1d2c12', 'b': '#4a7a2a', 'B': '#2c4d18',
 't': '#d8584a', 'T': '#a83228', 'a': '#e87058', 'A': '#c0402e',
 'u': '#5a8a30', 'U': '#356020', 'g': '#e89886', 'G': '#c86a5a', 'n': '#cfcf9a', 'N': '#8a8a5a',
 'W': '#f0e8c0',
 'r': '#e8604c', 'q': '#a02e24', 'l': '#6fa83e', 'o': '#3a6b1e', 'x': '#241404', 'w': '#ffd0c0',  # slice
}

STONE = {
 '.': 'transparent',
 'm': '#54565b', 'F': '#6a6d72', 'P': '#8a8d92', 'H': '#9da0a6', 'h': '#a8abb0',
 'K': '#101113', 'k': '#1c1d20', 'S': '#252629', 'b': '#7a7d82', 'B': '#4e5054',
 't': '#5a7088', 'T': '#3a4a5e', 'a': '#a85a4e', 'A': '#7a3a32',
 'u': '#5e6e52', 'U': '#3f4a3a', 'g': '#9a7e54', 'G': '#6a563a', 'n': '#b6b0a0', 'N': '#7a766a',
 'W': '#d8d4ca',
 'C': '#c4c8ce', 'd': '#34363a', 'D': '#4a4d52',  # carved rune
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

def books(g, y_base, y_top, seed, xlo=4, xhi=27, pal=None):
    if pal is None: pal = [('T', 't'), ('A', 'a'), ('U', 'u'), ('G', 'g'), ('N', 'n')]
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

def mote(g, cx, cy, core, br, arm, halo):
    # radiant 4-point infusion star: bright core, ringed, long thin arms, glow
    cells = {(0, 0): core, (0, -1): br, (0, 1): br, (-1, 0): br, (1, 0): br,
             (0, -2): arm, (0, 2): arm, (-2, 0): arm, (2, 0): arm,
             (0, -3): arm, (-3, 0): arm, (3, 0): arm}
    pts = [(dx, dy, ch) for (dx, dy), ch in cells.items()]
    _halo(g, cx, cy, pts, halo)
    for (dx, dy), ch in cells.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

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

# -------------------------------------------------------------- deep accents ----
def echo(g, cx, cy, small=False, level=1):
    # dark teal shard with a glowing cyan core; level 0/1/2 = dim/normal/peak pulse
    if small:
        shade = {(0, -1): 'i' if level else 'D', (-1, 0): 'D', (0, 0): 'i', (1, 0): 'D', (0, 1): 'd'}
    else:
        shade = {(0, -3): 'V', (-1, -2): 'D', (0, -2): 'i', (1, -2): 'D',
                 (-2, -1): 'd', (-1, -1): 'i', (0, -1): 'i', (1, -1): 'i', (2, -1): 'D',
                 (-2, 0): 'd', (-1, 0): 'D', (0, 0): 'i', (1, 0): 'D', (2, 0): 'd',
                 (-1, 1): 'd', (0, 1): 'D', (1, 1): 'd', (0, 2): 'd'}
        if level == 2:                       # peak: arms brighten, spark above
            for k in ((-1, -2), (1, -2), (-1, 0), (1, 0)): shade[k] = 'i'
            shade[(0, -4)] = 'V'
        elif level == 0:                     # dim: arms recede to mid/dark
            for k in ((-1, -2), (1, -2), (-1, -1), (1, -1)): shade[k] = 'D'
            shade[(0, -3)] = 'd'
    pts = [(dx, dy, ch) for (dx, dy), ch in shade.items()]
    _halo(g, cx, cy, pts, 'J')
    for (dx, dy), ch in shade.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

DULL = {'p': '#9a90a6', 's': '#6f6678', 'Z': '#4a4453'}  # dormant (unlit) amethyst
def dull_amethyst(g, cx, cy):
    # same shard cluster as crystal(), desaturated grey-purple, no glow halo
    remap = {'w': 'p', 'C': 'p', 'c': 's', 'e': 's', 'o': 'Z'}
    pts = [(dx, dy, remap[ch]) for (dx, dy, ch) in _shard(0, 0, True)]
    pts += [(dx - 3, dy + 1, remap[ch]) for (dx, dy, ch) in _shard(0, 0, False)]
    pts += [(dx + 2, dy + 1, remap[ch]) for (dx, dy, ch) in _shard(0, 0, False)]
    for dx, dy, ch in pts:
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

def soul_flame(g, cx, cy, fr=0):
    # blue soul-fire tongue with a white-hot core; upper tongue sways per frame
    sway = [0, 1, 0, -1][fr % 4]
    base = {(-2, 0): 'v', (-1, 0): 'r', (0, 0): 'r', (1, 0): 'r', (2, 0): 'v',
            (-2, -1): 'v', (-1, -1): 'r', (0, -1): 'l', (1, -1): 'r', (2, -1): 'v'}
    upper = {(-1, -2): 'l', (0, -2): 'f', (1, -2): 'l',
             (-1, -3): 'r', (0, -3): 'l', (1, -3): 'r',
             (0, -4): 'l', (-1, -4): 'v', (0, -5): 'l'}
    cells = dict(base)
    for (dx, dy), ch in upper.items(): cells[(dx + sway, dy)] = ch
    pts = [(dx, dy, ch) for (dx, dy), ch in cells.items()]
    _halo(g, cx, cy, pts, 'j')
    for (dx, dy), ch in cells.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

# --------------------------------------------------------------- end accents ----
def ender_pearl(g, cx, cy, small=False):
    # rounded teal pearl with top-left glint and a darker swirl
    if small:
        shade = {(0, -1): 'C', (-1, -1): 'c', (1, -1): 'e',
                 (-1, 0): 'c', (0, 0): 'c', (1, 0): 'o', (0, 1): 'o'}
    else:
        shade = {(0, -2): 'C', (-1, -2): 'c', (1, -2): 'c',
                 (-2, -1): 'c', (-1, -1): 'w', (0, -1): 'C', (1, -1): 'c', (2, -1): 'e',
                 (-2, 0): 'e', (-1, 0): 'c', (0, 0): 'c', (1, 0): 'e', (2, 0): 'o',
                 (-2, 1): 'e', (-1, 1): 'e', (0, 1): 'o', (1, 1): 'o', (2, 1): 'o',
                 (-1, 2): 'o', (0, 2): 'o', (1, 2): 'o'}
    pts = [(dx, dy, ch) for (dx, dy), ch in shade.items()]
    _halo(g, cx, cy, pts, 'q')
    for (dx, dy), ch in shade.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

def dragon_egg(g, cx, cy, level=1):
    # black-purple ovoid with magenta speckles; level 0/1/2 = dim/normal/peak glow
    spk = {0: 'D', 1: 'i', 2: 'I'}[level]
    shade = {(0, -5): 'V',
             (-1, -4): 'D', (0, -4): 'V', (1, -4): 'D',
             (-2, -3): 'd', (-1, -3): 'D', (0, -3): 'D', (1, -3): 'V', (2, -3): 'd',
             (-2, -2): 'd', (-1, -2): spk, (0, -2): 'D', (1, -2): 'D', (2, -2): 'd',
             (-2, -1): 'd', (-1, -1): 'D', (0, -1): spk, (1, -1): 'D', (2, -1): 'd',
             (-2, 0): 'd', (-1, 0): 'D', (0, 0): 'D', (1, 0): spk, (2, 0): 'd',
             (-1, 1): 'd', (0, 1): 'D', (1, 1): 'd'}
    pts = [(dx, dy, ch) for (dx, dy), ch in shade.items()]
    if level > 0: _halo(g, cx, cy, pts, 'J')
    for (dx, dy), ch in shade.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

def dragon_skull(g, cx, cy):
    # front-facing dragon skull silhouette with glowing magenta eyes + horns
    shade = {(-3, -3): 'D', (3, -3): 'D', (-2, -3): 'D', (2, -3): 'D',
             (-2, -2): 'V', (-1, -2): 'D', (0, -2): 'D', (1, -2): 'D', (2, -2): 'V',
             (-2, -1): 'D', (-1, -1): 'i', (0, -1): 'd', (1, -1): 'i', (2, -1): 'D',
             (-1, 0): 'V', (0, 0): 'D', (1, 0): 'V',
             (-1, 1): 'D', (0, 1): 'D', (1, 1): 'D',
             (0, 2): 'd'}
    pts = [(dx, dy, ch) for (dx, dy), ch in shade.items()]
    _halo(g, cx, cy, pts, 'J')
    for (dx, dy), ch in shade.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

def bee(g, cx, cy):
    # small striped bee facing right, pale wings, dark head
    shade = {(-1, -2): 'w', (0, -2): 'w', (1, -2): 'w',
             (-2, -1): 'y', (-1, -1): 'y', (0, -1): 'y', (1, -1): 'y', (2, -1): 'x',
             (-2, 0): 'x', (-1, 0): 'y', (0, 0): 'x', (1, 0): 'y', (2, 0): 'x',
             (-2, 1): 'y', (-1, 1): 'x', (0, 1): 'y', (1, 1): 'o'}
    for (dx, dy), ch in shade.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

def melon_slice(g, cx, cy):
    # triangular melon wedge: red flesh, green rind base, dark seeds
    shade = {(0, -3): 'r',
             (-1, -2): 'r', (0, -2): 'w', (1, -2): 'r',
             (-2, -1): 'r', (-1, -1): 'r', (0, -1): 'x', (1, -1): 'r', (2, -1): 'r',
             (-3, 0): 'r', (-2, 0): 'r', (-1, 0): 'x', (0, 0): 'r', (1, 0): 'r', (2, 0): 'q', (3, 0): 'r',
             (-3, 1): 'o', (-2, 1): 'l', (-1, 1): 'o', (0, 1): 'l', (1, 1): 'o', (2, 1): 'l', (3, 1): 'o'}
    for (dx, dy), ch in shade.items():
        X, Y = cx + dx, cy + dy
        if 0 <= X < W and 0 <= Y < Hh: g[Y][X] = ch

def rune(g, cx, cy):
    # small carved stone plaque with an engraved glyph
    for dy in range(-2, 3):
        for dx in range(-2, 3):
            g[cy + dy][cx + dx] = 'd'
    for dx, dy in ((0, -1), (0, 0), (0, 1), (-1, -1), (1, 1)):
        g[cy + dy][cx + dx] = 'C'
    for dx, dy in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        g[cy + dy][cx + dx] = 'D'

def sculk_veins(g, level=1):
    # glowing cyan freckles scattered on the sculk frame; dim at level 0
    ch = 'C' if level else 'D'
    for x, y in ((5, 1), (1, 9), (10, 2), (30, 6), (31, 17), (29, 25),
                 (6, 31), (18, 30), (25, 31), (2, 22)):
        g[y][x] = ch

def sculk_top(g, level=1):
    # dark sculk top with scattered glowing veins; seamless
    for y in range(32):
        for x in range(32):
            g[y][x] = ['P', 'k', 'F', 'k'][((x // 4) + (y // 5)) % 4]
    vein = 'C' if level else 'D'
    for x, y in ((3, 4), (11, 9), (19, 2), (27, 14), (6, 20), (22, 25), (14, 29), (30, 22)):
        g[y][x] = vein
        if 0 <= y + 1 < 32: g[y + 1][x] = 'D'

# -------------------------------------------------------------------- emit ----
def _legend(palette): return [f"  {k} {v}" for k, v in palette.items()]

# Every shelf spec renders 1:1 into the block texture tree under its own name, so
# the `ships:` target follows from the name alone. `kind:` follows the face's
# role: a shelf side tiles against copies of itself (block), a top or bottom cap
# never repeats (cap), and the UV atlases are flat fields by design (ui).
SHIPS_ROOT = "src/main/resources/assets/meridian/textures/block"

def _header(name, kind): return [f"kind: {kind}", f"ships: {SHIPS_ROOT}/{name}.png"]

def emit(name, build, palette=SEA, comment=None, kind="block"):
    g = grid(); frame(g); interior(g); build(g)
    comment = comment or f"# {name} side (sea family) — prismarine frame, sea books, themed accent."
    out = [comment, "size: 32"] + _header(name, kind) + ["", "legend:"] + _legend(palette) + ["", "grid:"]
    out += ["  " + "".join(r) for r in g]
    open(f"{OUT}/{name}.glyph", "w").write("\n".join(out) + "\n")

def emit_anim(name, builders, palette, comment, frametime=3, interpolate=True, kind="block"):
    out = [comment, "size: 32", f"frametime: {frametime}",
           f"interpolate: {'true' if interpolate else 'false'}"] + _header(name, kind) \
        + ["", "legend:"] + _legend(palette) + [""]
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
    out = [comment, "size: 32"] + _header(name, "cap") + ["", "legend:"] + _legend(palette) + ["", "grid:"]
    out += ["  " + "".join(r) for r in g]
    open(f"{OUT}/{name}.glyph", "w").write("\n".join(out) + "\n")

# ------------------------------------------------------------------ sea family ----
def sea(g):       books(g, 26, 18, 2, xhi=19); books(g, 14, 5, 0); crystal(g, 23, 25)
def crystalsh(g): books(g, 14, 7, 3, xhi=13); crystal(g, 20, 13); crystal(g, 22, 26, big=True); crystal(g, 8, 26)
def heartsh(g):   books(g, 14, 6, 1); books(g, 26, 18, 4, xlo=4, xhi=16); heart(g, 22, 23)
def infusedsea(g): books(g, 14, 5, 0, xhi=18); books(g, 26, 18, 2, xhi=19); crystal(g, 23, 25); mote(g, 22, 9, 'w', 'C', 'c', 'q')
emit("seashelf", sea); emit("crystal_seashelf", crystalsh); emit("heart_seashelf", heartsh)
emit("infused_seashelf", infusedsea, SEA,
     "# infused_seashelf side (sea family) — base seashelf empowered with a cyan infusion mote.")
top("prismarine_shelf_top", SEA,
    "# shared prismarine shelf top/bottom (sea family), seamless.",
    [(5, 3, 'c'), (21, 19, 'c'), (27, 11, 'C'), (11, 27, 'c')])

# --------------------------------------------------------------- nether family ----
NC = "# {} side (nether family) — nether-brick frame, nether books, {} accent."
def hell(g):     books(g, 26, 18, 1, xhi=27); books(g, 14, 5, 3, xhi=19); wart(g, 23, 26)
def glowing(g):  books(g, 14, 6, 2, xhi=18); books(g, 26, 18, 0, xhi=18); glowstone(g, 22, 23, big=True)
def infusedhell(g): books(g, 26, 18, 1, xhi=27); books(g, 14, 5, 3, xhi=18); wart(g, 23, 26); mote(g, 23, 9, 'z', 'f', 'y', 'j')
emit("hellshelf", hell, NETHER, NC.format("hellshelf", "nether-wart"))
emit("glowing_hellshelf", glowing, NETHER, NC.format("glowing_hellshelf", "glowstone"))
emit("infused_hellshelf", infusedhell, NETHER,
     "# infused_hellshelf side (nether family) — base hellshelf empowered with a gold infusion mote.")

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

# ------------------------------------------------------------ deepslate family ----
DC = "# {} side (deepslate family) — deepslate frame, cool books, {} accent."
def deep(g):       books(g, 26, 18, 1, xhi=27); books(g, 14, 5, 4); crystal(g, 23, 25)
def dormant(g):    books(g, 26, 18, 1, xhi=27); books(g, 14, 5, 4); dull_amethyst(g, 23, 25)
emit("deepshelf", deep, DEEP, DC.format("deepshelf", "amethyst-cluster"))
emit("dormant_deepshelf", dormant, {**DEEP, **DULL}, DC.format("dormant_deepshelf", "dormant (unlit) amethyst"))

# echoing: shards pulse (slow 4-frame breath); soul-touched: flame sways (livelier)
def echoing_frame(level):
    def build(g):
        books(g, 14, 6, 2, xhi=18); books(g, 26, 18, 0, xhi=18)
        echo(g, 22, 24, level=level); echo(g, 24, 12, small=True, level=level)
    return build
emit_anim("echoing_deepshelf", [echoing_frame(l) for l in (1, 2, 1, 0)], DEEP,
          DC.format("echoing_deepshelf", "echo-shard (4-frame pulse)"), frametime=6)

def soul_frame(fr):
    def build(g):
        books(g, 14, 6, 3); books(g, 26, 18, 1, xhi=17); soul_flame(g, 22, 26, fr)
    return build
emit_anim("soul_touched_deepshelf", [soul_frame(i) for i in range(4)], DEEP,
          DC.format("soul_touched_deepshelf", "soul-fire (4-frame flicker)"), frametime=3)
top("deepslate_shelf_top", DEEP,
    "# shared deepslate-tile shelf top/bottom (deepslate family), seamless.",
    [(7, 6, 'm'), (8, 6, 'm'), (22, 21, 'm'), (19, 11, 'c')])

# ------------------------------------------------------------------ end family ----
EC = "# {} side (end family) — end-stone frame, varied books, {} accent."
EB = [('T', 't'), ('A', 'a'), ('U', 'u'), ('G', 'g'), ('N', 'n'), ('X', 'x'), ('Y', 'y'), ('Z', 'z')]
def endsh(g):      books(g, 14, 5, 0, xhi=20, pal=EB); books(g, 26, 18, 2, xhi=27, pal=EB); ender_pearl(g, 24, 9)
def pearlsh(g):    # pearl-rich: a cluster of large + small pearls
    books(g, 14, 7, 3, xhi=20, pal=EB); books(g, 26, 19, 5, xhi=14, pal=EB)
    ender_pearl(g, 24, 10); ender_pearl(g, 22, 25); ender_pearl(g, 26, 24, small=True); ender_pearl(g, 19, 26, small=True)
emit("endshelf", endsh, END, EC.format("endshelf", "ender-pearl"))
emit("pearl_endshelf", pearlsh, END, EC.format("pearl_endshelf", "ender-pearl cluster"))

# draconic: static dragon skull (top shelf) + pulsing dragon egg (bottom shelf)
def draconic_frame(level):
    def build(g):
        dragon_skull(g, 8, 11)
        books(g, 14, 6, 2, xlo=13, xhi=27, pal=EB)
        books(g, 26, 18, 4, xhi=15, pal=EB)
        dragon_egg(g, 22, 27, level)
    return build
emit_anim("draconic_endshelf", [draconic_frame(l) for l in (1, 2, 1, 0)], END,
          EC.format("draconic_endshelf", "dragon-skull + pulsing dragon-egg"), frametime=5)
top("end_stone_shelf_top", END,
    "# shared end-stone shelf top/bottom (end family), seamless.",
    [(6, 5, 'm'), (21, 20, 'm'), (12, 26, 'u')])

# ---------------------------------------------------------------- sculk family ----
def emit_grid(name, drawfn, palette, comment, kind="cap"):
    g = grid(); drawfn(g)
    out = [comment, "size: 32"] + _header(name, kind) + ["", "legend:"] + _legend(palette) + ["", "grid:"]
    out += ["  " + "".join(r) for r in g]
    open(f"{OUT}/{name}.glyph", "w").write("\n".join(out) + "\n")

SC = "# {} side (sculk family) — sculk frame + glowing veins, cyan books, {} accent."
def echoing_sculk(level):
    def build(g):
        sculk_veins(g, level)
        books(g, 14, 6, 1, xhi=18); books(g, 26, 18, 4, xhi=18)
        echo(g, 22, 24, level=level); echo(g, 24, 12, small=True, level=level)
    return build
emit_anim("echoing_sculkshelf", [echoing_sculk(l) for l in (1, 2, 1, 0)], SCULK,
          SC.format("echoing_sculkshelf", "echo-shard (pulsing) + pulsing veins"), frametime=6)
def soul_sculk(fr):
    def build(g):
        sculk_veins(g, 1)
        books(g, 14, 6, 3); books(g, 26, 18, 1, xhi=17)
        soul_flame(g, 22, 26, fr)
    return build
emit_anim("soul_touched_sculkshelf", [soul_sculk(i) for i in range(4)], SCULK,
          SC.format("soul_touched_sculkshelf", "soul-fire (flicker)"), frametime=3)
emit_grid("sculkshelf_top", sculk_top, SCULK,
          "# shared sculk shelf top/bottom — dark with glowing veins, seamless.")

# --------------------------------------------------------------- singles ----
def beesh(g):   books(g, 14, 5, 0, xhi=20); books(g, 26, 18, 2, xhi=27); bee(g, 23, 9)
def melonsh(g): books(g, 14, 6, 3, xhi=20); books(g, 26, 18, 1, xhi=19); melon_slice(g, 23, 24)
def stonesh(g): books(g, 14, 5, 2, xhi=20); books(g, 26, 18, 4, xhi=27); rune(g, 23, 10)
emit("beeshelf", beesh, BEE, "# beeshelf side (single) — honeycomb frame, honey books, bee accent.")
emit("melonshelf", melonsh, MELON, "# melonshelf side (single) — melon-rind frame, flesh books, melon-slice accent.")
emit("stoneshelf", stonesh, STONE, "# stoneshelf side (single) — andesite frame, muted books, carved-rune accent.")
top("honeycomb_shelf_top", BEE, "# beeshelf top/bottom — honeycomb, seamless.",
    [(6, 5, 'h'), (20, 22, 'h'), (26, 12, 'h')])
top("melon_shelf_top", MELON, "# melonshelf top/bottom — melon rind, seamless.",
    [(7, 6, 'x'), (21, 20, 'x'), (13, 27, 'x')])
top("stone_shelf_top", STONE, "# stoneshelf top/bottom — polished andesite, seamless.",
    [(6, 5, 'F'), (20, 22, 'm'), (26, 12, 'F')])

# =================================================================== devices ====
# Device shelves: the shelf frame + recessed shelves (matching their originals and
# the rest of the set), filled with thematic items instead of plain books.
# Recipe-themed: rectifier = sea/purpur-tier frame + orange potion + amethyst crystal;
# sight = nether frame + corner gems (gold/emerald) + green eye + purple potion;
# treasure = gold frame + diamond/emerald gems + a gold pile.

def potion(g, cx, cy):
    # small round-bottomed bottle, blue glass + coloured liquid + cork (base row cy)
    cells = {(0, -6): 'D', (0, -5): 'd', (0, -4): 'l',
             (-1, -3): 'l', (0, -3): 'l', (1, -3): 'l',
             (-2, -2): 'l', (-1, -2): 'f', (0, -2): 'r', (1, -2): 'r', (2, -2): 'v',
             (-2, -1): 'l', (-1, -1): 'r', (0, -1): 'r', (1, -1): 'r', (2, -1): 'v',
             (-2, 0): 'v', (-1, 0): 'r', (0, 0): 'r', (1, 0): 'r', (2, 0): 'v',
             (-1, 1): 'v', (0, 1): 'r', (1, 1): 'v'}
    for (dx, dy), ch in cells.items():
        if 0 <= cx + dx < W and 0 <= cy + dy < Hh: g[cy + dy][cx + dx] = ch

def eye(g, cx, cy):
    # green ender-eye orb with dark pupil and a glint
    cells = {(-1, -2): 'I', (0, -2): 'I', (1, -2): 'I',
             (-2, -1): 'I', (-1, -1): 'i', (0, -1): 'i', (1, -1): 'i', (2, -1): 'I',
             (-2, 0): 'I', (-1, 0): 'i', (0, 0): 'd', (1, 0): 'i', (2, 0): 'I',
             (-2, 1): 'I', (-1, 1): 'i', (0, 1): 'i', (1, 1): 'i', (2, 1): 'I',
             (-1, 2): 'I', (0, 2): 'I', (1, 2): 'I'}
    cells[(-1, -1)] = 'y'
    for (dx, dy), ch in cells.items(): g[cy + dy][cx + dx] = ch

def _gem(g, cx, cy, hi, mid, dk):
    for (dx, dy), ch in {(0, -1): hi, (-1, 0): mid, (0, 0): hi, (1, 0): dk, (0, 1): dk}.items():
        if 0 <= cx + dx < W and 0 <= cy + dy < Hh: g[cy + dy][cx + dx] = ch

def corner_gems(g):
    for x, y in ((2, 2), (29, 2), (2, 29), (29, 29)): _gem(g, x, y, 'C', 'c', 'q')

# items shared across rectifier tiers (amethyst crystal + orange potion + cool books)
RECT_ITEMS = {
 '.': 'transparent', 'k': '#16221f', 'K': '#0d1615', 'S': '#1c2b27',
 't': '#2f8a80', 'T': '#1c5a52', 'a': '#3aa6b8', 'A': '#226e7a', 'u': '#34589e', 'U': '#20396b',
 'g': '#3f8a55', 'G': '#275e39', 'n': '#bba473', 'N': '#8a7550', 'W': '#dccda8',
 'c': '#b58cf0', 'C': '#d9c2ff', 'e': '#8a5fce', 'o': '#5a3596', 'w': '#ece0ff', 'q': '#34205a',
 'l': '#acd0e8', 'v': '#4a6e8a', 'r': '#e8731c', 'f': '#ffb347', 'd': '#8a5a2a', 'D': '#b07a3a',
}
RECT_FRAME = {
 'rectifier':    {'P': '#44726a', 'h': '#84b6a9', 'm': '#22403a', 'H': '#5e8f84', 'F': '#335c54', 'b': '#436a61', 'B': '#2c4a44'},
 'rectifier_t2': {'P': '#2c2630', 'h': '#5a4a2e', 'm': '#100d14', 'H': '#3e3844', 'F': '#1a1720', 'b': '#3a3020', 'B': '#221a10'},
 'rectifier_t3': {'P': '#7a5e8e', 'h': '#a484b8', 'm': '#3a2848', 'H': '#8e6ea2', 'F': '#5a4070', 'b': '#6a5080', 'B': '#43305a'},
}
def rectifier(g, st=0, sb=3, cbig=False):
    books(g, 14, 5, st, xlo=12, xhi=27); potion(g, 7, 14)
    books(g, 26, 18, sb, xhi=18); crystal(g, 23, 25, big=cbig)

SIGHT_FRAME = {
 'sight':         {'P': '#2c1a1c', 'h': '#4d3032', 'm': '#150c0d', 'H': '#5c393b', 'F': '#1d1112', 'b': '#46291f', 'B': '#291712',
                   'C': '#ffe080', 'c': '#ffd23a', 'q': '#c8901a'},
 'sightshelf_t2': {'P': '#1c1e22', 'h': '#2c2f34', 'm': '#08090b', 'H': '#282b30', 'F': '#121316', 'b': '#33363b', 'B': '#202327',
                   'C': '#9ff0b0', 'c': '#4fe07a', 'q': '#1f8a44'},
}
SIGHT_ITEMS = {
 '.': 'transparent', 'k': '#0e0c10', 'K': '#070608', 'S': '#16141a',
 't': '#9a363c', 'T': '#5a2228', 'a': '#cf6a26', 'A': '#8a3a18', 'u': '#3c333a', 'U': '#241d22',
 'g': '#3a6f7e', 'G': '#1f4450', 'n': '#bba473', 'N': '#8a7550', 'W': '#cabfa8',
 'i': '#3a9e64', 'I': '#1f5e38', 'y': '#9fe6b0', 'd': '#0a0c0a',           # eye
 'l': '#acd0e8', 'v': '#4a6e8a', 'r': '#9a3fd0', 'f': '#c06af0', 'D': '#8a5a2a', 'p': '#b07a3a',  # purple potion
}
def sight_dev(g, st=2):
    corner_gems(g)
    books(g, 14, 6, st, xhi=27)
    eye(g, 10, 22); potion(g, 23, 26)

TREASURE = {
 '.': 'transparent',
 'P': '#d9a72a', 'h': '#f0c84a', 'm': '#7a5410', 'H': '#e8be3a', 'F': '#a67c1a', 'b': '#caa030', 'B': '#8a6a18',
 'k': '#241a08', 'K': '#160f04', 'S': '#33240c',
 't': '#a86a14', 'T': '#7a4c0e', 'a': '#c89030', 'A': '#9a6c1a', 'u': '#8a6418', 'U': '#5a3e10',
 'g': '#3fc06a', 'G': '#1f7a40', 'n': '#d6c498', 'N': '#9a8358', 'W': '#f5ecc0',
 'y': '#ffd23a', 'o': '#c8901a', 'D': '#fff0a0',
 'c': '#9fe8f0', 'C': '#d2f8fc', 'e': '#4fb0c8',                           # diamond
 'l': '#4fd07a', 'v': '#1f8a44', 'V': '#145e30',                          # emerald
}
def _ingot(g, x, y):
    g[y][x] = 'D'; g[y][x + 1] = 'y'; g[y][x + 2] = 'y'
    g[y + 1][x] = 'o'; g[y + 1][x + 1] = 'o'; g[y + 1][x + 2] = 'm'
def _dia(g, cx, cy):
    for (dx, dy), ch in {(0, -1): 'C', (-1, 0): 'c', (0, 0): 'c', (1, 0): 'e', (0, 1): 'e'}.items():
        g[cy + dy][cx + dx] = ch
def _eme(g, cx, cy):
    for (dx, dy), ch in {(0, -1): 'l', (-1, 0): 'l', (0, 0): 'V', (1, 0): 'l', (0, 1): 'V'}.items():
        g[cy + dy][cx + dx] = ch
def treasure_dev(g):
    # overflowing hoard — gold ingots, gems, and a heaped pile fill both shelves
    for x in (5, 9, 18, 22): _ingot(g, x, 13)                       # top-shelf ingot row
    _dia(g, 14, 11); _eme(g, 25, 11)                                # gems among ingots
    for x in range(5, 28): g[28][x] = 'o'; g[27][x] = 'y' if x % 2 else 'o'   # heaped pile
    for x in range(6, 27): g[26][x] = 'y' if x % 3 else 'o'
    for x in range(8, 25, 4): g[25][x] = 'y'
    _eme(g, 10, 24); _dia(g, 20, 23)                                # gems on the pile
    g[24][16] = 'C'; g[25][16] = 'c'

def rect_pal(t): return {**RECT_ITEMS, **RECT_FRAME[t]}
def sight_pal(t): return {**SIGHT_ITEMS, **SIGHT_FRAME[t]}

# side concepts for direction review
emit("rectifier", lambda g: rectifier(g, 0, 3, False), rect_pal('rectifier'), "# rectifier t1 concept.")
emit("rectifier_t2", lambda g: rectifier(g, 4, 1, False), rect_pal('rectifier_t2'), "# rectifier t2 concept.")
emit("rectifier_t3", lambda g: rectifier(g, 2, 5, True), rect_pal('rectifier_t3'), "# rectifier t3 concept.")
emit("sight_side", lambda g: sight_dev(g, 2), sight_pal('sight'), "# sight t1 concept.")
emit("sightshelf_t2", lambda g: sight_dev(g, 5), sight_pal('sightshelf_t2'), "# sight t2 concept.")
emit("treasure_shelf_side", treasure_dev, TREASURE, "# treasure concept.")

# device tops (rectifier t1 reuses prismarine_shelf_top; others custom)
TOP_RECT_T2 = {'.': 'transparent', 'P': '#2c2630', 'H': '#3a3442', 'F': '#1a1720', 'm': '#100d14', 'y': '#caa24a'}
TOP_RECT_T3 = {'.': 'transparent', 'P': '#7a5e8e', 'H': '#9276a8', 'F': '#5a4070', 'm': '#3a2848', 'c': '#c8b0e0'}
TOP_SIGHT_T1 = {'.': 'transparent', 'P': '#2c1a1c', 'H': '#3c2628', 'F': '#1d1112', 'm': '#150c0d', 'c': '#ffd23a'}
TOP_SIGHT_T2 = {'.': 'transparent', 'P': '#1c1e22', 'H': '#282b30', 'F': '#121316', 'm': '#08090b', 'c': '#4fe07a'}
TOP_TREASURE = {'.': 'transparent', 'P': '#d9a72a', 'H': '#e8be3a', 'F': '#a67c1a', 'm': '#7a5410', 'c': '#8a8d92', 'y': '#fff0a0'}
top("rectifier_t2_top", TOP_RECT_T2, "# rectifier t2 top — gilded blackstone, seamless.", [(6, 5, 'y'), (20, 22, 'y'), (26, 12, 'y')])
top("rectifier_t3_top", TOP_RECT_T3, "# rectifier t3 top — purpur, seamless.", [(7, 6, 'c'), (21, 20, 'c'), (13, 27, 'c')])
top("sight_top", TOP_SIGHT_T1, "# sight t1 top — nether brick + gold corner gems, seamless.", [(4, 4, 'c'), (27, 4, 'c'), (4, 27, 'c'), (27, 27, 'c')])
top("sightshelf_t2_top", TOP_SIGHT_T2, "# sight t2 top — netherite + emerald corner gems, seamless.", [(4, 4, 'c'), (27, 4, 'c'), (4, 27, 'c'), (27, 27, 'c')])
top("treasure_shelf_top", TOP_TREASURE, "# treasure top — gold with stone flecks, seamless.", [(6, 5, 'c'), (20, 22, 'c'), (13, 27, 'c'), (26, 12, 'y')])

print("emitted families + devices (sides + tops)")

# =============================================================== library set ====
# Decorative enchantment-library blocks (custom multi-face models). 32px faces on
# the existing UVs; books.png stays a UV atlas (book covers in baked regions).
LIBRARY = {
 '.': 'transparent',
 # dark wood frame/carcass
 'P': '#4a3018', 'h': '#6a4828', 'm': '#241608', 'H': '#5c3c20', 'F': '#33210e',
 'k': '#1c1208', 'K': '#0f0904',
 # purple cloth drape + gold trim (Meridian identity)
 'p': '#7b2fbe', 'q': '#5a2090', 'Q': '#3a1460', 'l': '#9d5fd3', 'y': '#ffd700', 'o': '#c8a020',
 # book spines (purple / gold / teal / blue / crimson) + pages
 't': '#3aa6b8', 'T': '#226e7a', 'a': '#8a5fce', 'A': '#5a3596', 'u': '#3f74a6', 'U': '#27496e',
 'g': '#caa030', 'G': '#8a6a18', 'n': '#b8485a', 'N': '#7a2838', 'W': '#e8e0cf', 'w': '#bcae90',
}
def _planks(g, base, hi, dark):
    for y in range(32):
        for x in range(32):
            g[y][x] = base
            if y % 8 == 0: g[y][x] = dark
            if x % 16 == (0 if (y // 8) % 2 == 0 else 8): g[y][x] = dark
        if y % 8 == 1:
            for x in range(32): g[y][x] = hi
def lib_bottom(g): _planks(g, 'P', 'h', 'm')
def lib_top(g):
    rect(g, 0, 0, 31, 31, 'q')                                   # purple cloth
    rect(g, 0, 0, 31, 1, 'l'); rect(g, 0, 0, 1, 31, 'l')
    rect(g, 0, 30, 31, 31, 'Q'); rect(g, 30, 0, 31, 31, 'Q')
    rect(g, 3, 3, 28, 28, 'p')
    for x, y in ((6, 6), (25, 6), (6, 25), (25, 25)): g[y][x] = 'y'  # gold studs
def lib_side(g):
    _planks(g, 'P', 'h', 'm')                                    # wood carcass
    rect(g, 9, 0, 22, 31, 'q')                                   # central drape
    rect(g, 9, 0, 9, 31, 'l'); rect(g, 22, 0, 22, 31, 'Q')
    rect(g, 11, 0, 20, 31, 'p')
    rect(g, 9, 0, 22, 1, 'y'); rect(g, 9, 2, 22, 2, 'o')         # gold top rail
    # gold arcane sigil (diamond eye) mid-drape
    sig = {(0, -3): 'y', (-2, -1): 'y', (-1, -1): 'o', (0, -1): 'y', (1, -1): 'o', (2, -1): 'y',
           (-1, 0): 'o', (0, 0): 'l', (1, 0): 'o', (-2, 1): 'y', (0, 1): 'y', (2, 1): 'y', (0, 3): 'y'}
    for (dx, dy), ch in sig.items(): g[16 + dy][15 + dx] = ch
    rect(g, 11, 26, 20, 31, 'Q')                                 # drape fold shadow
    for x in range(12, 20, 2): g[31][x] = 'q'; g[30][x] = 'l'    # fringe
def _bookrow(g, y, seed):
    pal = [('A', 'a'), ('G', 'g'), ('T', 't'), ('U', 'u'), ('N', 'n')]
    x = 3; i = seed
    while x <= 28:
        w = (2, 3, 2, 4, 3)[i % 5]
        if x + w - 1 > 28: break
        sd, sl = pal[i % 5]
        for yy in range(y, y + 5):
            for xx in range(x, x + w): g[yy][xx] = sl if xx == x else sd
        g[y][min(28, x + w - 1)] = 'W'
        x += w + 1; i += 1
def lib_side2(g):
    rect(g, 0, 0, 31, 31, 'F')                                   # back of shelf
    rect(g, 0, 0, 31, 2, 'P'); rect(g, 0, 29, 31, 31, 'P')       # wood frame
    rect(g, 0, 0, 2, 31, 'P'); rect(g, 29, 0, 31, 31, 'P')
    for y in (3, 13, 23): rect(g, 3, y + 7, 28, y + 7, 'H')      # shelf ledges
    _bookrow(g, 4, 0); _bookrow(g, 14, 3); _bookrow(g, 24, 1)
def lib_books(g):
    # UV atlas (book covers in the model's baked regions, ×2 for 32px)
    rect(g, 0, 0, 31, 31, '.')
    def cover(x0, y0, x1, y1, base, trim):
        rect(g, x0, y0, x1, y1, base)
        rect(g, x0, y0, x1, y0 + 1, trim); rect(g, x0, y1 - 1, x1, y1, trim)
    cover(0, 0, 11, 13, 'a', 'y')          # purple book + gold trim (bottom-left stack)
    g[6][5] = 'y'; g[7][5] = 'y'; g[6][6] = 'o'  # clasp
    cover(0, 20, 9, 31, 'N', 'W')          # crimson book
    cover(12, 22, 23, 31, 'A', 'o')        # purple book
    cover(0, 0, 11, 11, 'G', 'W')          # (up-region) green book
    rect(g, 12, 0, 23, 9, 'T'); rect(g, 12, 0, 23, 1, 'W')  # teal book
    cover(24, 0, 31, 13, 'u', 'g')         # blue book + gold
emit_grid("library/side", lib_side, LIBRARY, "# library front — purple drape + gold sigil.", kind="block")
emit_grid("library/side2", lib_side2, LIBRARY, "# library bookshelf face.", kind="block")
emit_grid("library/top", lib_top, LIBRARY, "# library top — purple cloth.")
emit_grid("library/bottom", lib_bottom, LIBRARY, "# library bottom — planks.")
emit_grid("library/books", lib_books, LIBRARY, "# library books atlas (UV-mapped to book stacks).", kind="ui")
print("emitted library set")

# ender_library — same geometry, end-tier palette (teal cloth, end-stone wood)
ENDER_LIBRARY = {
 '.': 'transparent',
 'P': '#9a9670', 'h': '#bcb890', 'm': '#6a6650', 'H': '#aaa67e', 'F': '#7e7a5e',
 'k': '#16201d', 'K': '#0c1410',
 'p': '#2f8f8a', 'q': '#1c5a56', 'Q': '#103a36', 'l': '#4fc0b8', 'y': '#e6d68a', 'o': '#b0a050',
 't': '#3aa6b8', 'T': '#226e7a', 'a': '#8a5fce', 'A': '#5a3596', 'u': '#4fb0a0', 'U': '#2a7068',
 'g': '#caa030', 'G': '#8a6a18', 'n': '#b8485a', 'N': '#7a2838', 'W': '#e4ecd8', 'w': '#b6c0a8',
}
emit_grid("ender_library/side", lib_side, ENDER_LIBRARY, "# ender_library front — teal drape + pale sigil.", kind="block")
emit_grid("ender_library/side2", lib_side2, ENDER_LIBRARY, "# ender_library bookshelf face.", kind="block")
emit_grid("ender_library/top", lib_top, ENDER_LIBRARY, "# ender_library top — teal cloth.")
emit_grid("ender_library/bottom", lib_bottom, ENDER_LIBRARY, "# ender_library bottom — end-stone.")
emit_grid("ender_library/books", lib_books, ENDER_LIBRARY, "# ender_library books atlas.", kind="ui")
print("emitted ender_library set")

# =========================================================== filtering_shelf ====
# Prismarine item shelf. empty/occupied are coherent front faces (vanilla
# chiseled-bookshelf slot UVs keep each slot in place); side/top are full faces.
FILTERING = {
 '.': 'transparent',
 'P': '#44726a', 'h': '#84b6a9', 'm': '#22403a', 'H': '#5e8f84', 'F': '#335c54',
 'k': '#12201d', 'K': '#0a1614', 'b': '#436a61', 'B': '#2c4a44', 'S': '#1c2b27',
 'c': '#c7e8df', 'C': '#eafdfd', 'd': '#4eeaed', 'e': '#2bb3c0', 'o': '#1d6f86',
 't': '#2f8a80', 'T': '#1c5a52', 'a': '#3aa6b8', 'A': '#226e7a', 'u': '#8a5fce', 'U': '#5a3596',
 'g': '#caa030', 'G': '#8a6a18', 'n': '#b8485a', 'N': '#7a2838', 'W': '#e8e0cf',
}
def _filt_frame(g):
    rect(g, 0, 0, 31, 31, 'P')
    rect(g, 0, 0, 31, 0, 'h'); rect(g, 0, 0, 0, 31, 'h')
    rect(g, 31, 0, 31, 31, 'm'); rect(g, 0, 31, 31, 31, 'm')
    for x in (8, 16, 24): rect(g, x, 0, x, 1, 'm')
    for x in (4, 12, 20, 28): rect(g, x, 30, x, 31, 'm')
def filt_side(g):
    _filt_frame(g)
    for x in range(2, 30):                                        # prismarine wave band
        y = 15 + (1 if (x // 2) % 2 == 0 else -1)
        g[y][x] = 'c'; g[y + 1][x] = 'e'
def filt_top(g):
    _filt_frame(g)
    dia = {(0, -2): 'C', (-1, -1): 'd', (0, -1): 'C', (1, -1): 'd', (-2, 0): 'd', (-1, 0): 'd',
           (0, 0): 'C', (1, 0): 'd', (2, 0): 'e', (-1, 1): 'd', (0, 1): 'e', (1, 1): 'e', (0, 2): 'o'}
    for (dx, dy), ch in dia.items(): g[15 + dy][15 + dx] = ch
def _filt_shelves(g):
    _filt_frame(g)
    rect(g, 2, 2, 29, 14, 'k'); rect(g, 2, 2, 29, 3, 'K')         # top recess
    rect(g, 2, 16, 29, 28, 'k'); rect(g, 2, 16, 29, 17, 'K')      # bottom recess
    rect(g, 2, 14, 29, 15, 'b'); rect(g, 2, 28, 29, 29, 'b')      # ledges
    rect(g, 2, 15, 29, 15, 'B'); rect(g, 2, 29, 29, 29, 'B')
def filt_empty(g): _filt_shelves(g)
def filt_occupied(g):
    _filt_shelves(g)
    items = [('A', 'u'), ('N', 'n'), ('G', 'g'), ('T', 't'), ('U', 'u'), ('A', 'a')]
    slots = [(4, 13), (13, 13), (22, 13), (4, 27), (13, 27), (22, 27)]  # (x, base_y)
    for i, (x, by) in enumerate(slots):
        sd, sl = items[i]; w = (5, 4, 6)[i % 3]; ht = (8, 6, 9, 7)[i % 4]
        for yy in range(by - ht, by):
            for xx in range(x, x + w): g[yy][xx] = sl if xx == x else sd
        g[by - ht][min(x + w - 1, 28)] = 'W'                      # page top
emit_grid("filtering_shelf/side", filt_side, FILTERING, "# filtering_shelf side — prismarine wave.", kind="block")
emit_grid("filtering_shelf/top", filt_top, FILTERING, "# filtering_shelf top — prismarine + diamond.")
emit_grid("filtering_shelf/empty", filt_empty, FILTERING, "# filtering_shelf empty slots (atlas).", kind="ui")
emit_grid("filtering_shelf/occupied", filt_occupied, FILTERING, "# filtering_shelf occupied slots (atlas).", kind="ui")
print("emitted filtering_shelf set")
