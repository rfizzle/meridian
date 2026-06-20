#!/usr/bin/env python3
"""Compositor for Meridian's GUI sheets.

GUI textures are atlases: a main panel at (0,0) plus discrete widget sprites
packed into the margins, each blitted from a fixed UV by the screen code. This
script builds each full sheet programmatically — drawing the panel + slots and
stamping widgets at the exact UV coordinates the Java screens read — then renders
it to PNG via the shared glyph renderer. No model/screen changes: positions match
the existing blit calls (see EnchantmentScreen / EnchantmentLibraryScreen).

Run: python3 art/gui/guigen.py [OUT_DIR]   (default: the gui asset dir)
"""
import os
import subprocess
import tempfile

ROOT = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(ROOT, "..", ".."))
GLYPH = os.path.join(REPO, ".ai/skills/mc-textures/scripts/glyph.py")
DEFAULT_OUT = os.path.join(REPO, "src/main/resources/assets/meridian/textures/gui")

# Meridian GUI palette (arcane stone panel + purple/gold accents; functional
# stat colours kept: eterna green, quanta red, arcana purple).
PAL = {
 '.': 'transparent',
 'K': '#15131f', 'P': '#332e47', 'p': '#3f3956', 'H': '#544c72', 'h': '#6a608f', 'S': '#26223a',
 'k': '#120f1c', 'r': '#1b1828', 'm': '#564f74',                       # slot recess + rim
 'g': '#ffd700', 'o': '#c8a020', 'y': '#9d5fd3', 'u': '#7b2fbe', 'q': '#3a1460',  # gold + purple accents
 'W': '#e8e0cf', 'w': '#bcae90',                                       # parchment
 'b': '#caa46a', 'B': '#8a6a3a', 'n': '#6a4a24',                       # enchant-button parchment
 'G': '#3db53d', 'd': '#1f6e1f', 'R': '#fc5454', 'N': '#8a2020', 'A': '#a800a8', 'Q': '#5a005a',
 'e': '#3a3550', 'E': '#4a4660',                                       # disabled grey-purple
}
W = H = 256

def grid(w=W, h=H): return [['.' for _ in range(w)] for _ in range(h)]
def rect(g, x0, y0, x1, y1, ch):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < len(g[0]) and 0 <= y < len(g): g[y][x] = ch

def panel(g, x0, y0, x1, y1):
    rect(g, x0, y0, x1, y1, 'P')
    rect(g, x0, y0, x1, y0, 'h'); rect(g, x0, y0, x0, y1, 'h')        # lit top/left bevel
    rect(g, x1, y0, x1, y1, 'K'); rect(g, x0, y1, x1, y1, 'K')        # dark bottom/right
    rect(g, x0 + 1, y1 - 1, x1, y1 - 1, 'S')
    rect(g, x0, y0, x1, y0, 'h')
    # thin gold inset frame
    rect(g, x0 + 2, y0 + 2, x1 - 2, y0 + 2, 'o'); rect(g, x0 + 2, y1 - 2, x1 - 2, y1 - 2, 'o')
    rect(g, x0 + 2, y0 + 2, x0 + 2, y1 - 2, 'o'); rect(g, x1 - 2, y0 + 2, x1 - 2, y1 - 2, 'o')

def slot(g, x, y, w=16, h=16):
    rect(g, x, y, x + w - 1, y + h - 1, 'r')
    rect(g, x, y, x + w - 1, y, 'k'); rect(g, x, y, x, y + h - 1, 'k')   # inset shadow
    rect(g, x + w - 1, y, x + w - 1, y + h - 1, 'm'); rect(g, x, y + h - 1, x + w - 1, y + h - 1, 'm')

def slotgrid(g, x, y, cols, rows, pitch=18):
    for r in range(rows):
        for c in range(cols):
            slot(g, x + c * pitch, y + r * pitch)

def hbar(g, x, y, w, hi, mid, lo):
    rect(g, x, y, x + w - 1, y, hi)
    rect(g, x, y + 1, x + w - 1, y + 3, mid)
    rect(g, x, y + 4, x + w - 1, y + 4, lo)

def button(g, x, y, w, h, base, hi, lo):
    rect(g, x, y, x + w - 1, y + h - 1, base)
    rect(g, x, y, x + w - 1, y, hi); rect(g, x, y, x, y + h - 1, hi)
    rect(g, x + w - 1, y, x + w - 1, y + h - 1, lo); rect(g, x, y + h - 1, x + w - 1, y + h - 1, lo)

def numicon(g, x, y, ring, fill, digit_ch):
    # small 16x16 numbered enchant pip: a ringed disc
    for dx in range(16):
        for dy in range(16):
            d = (dx - 7.5) ** 2 + (dy - 7.5) ** 2
            if d <= 49: g[y + dy][x + dx] = fill
            elif d <= 64: g[y + dy][x + dx] = ring
    g[y + 7][x + 7] = digit_ch; g[y + 8][x + 7] = digit_ch          # tick of a numeral

def build_enchanting_table():
    g = grid()
    panel(g, 0, 0, 175, 196)
    rect(g, 4, 13, 4, 16, 'g'); rect(g, 4, 13, 7, 13, 'g')          # title corner flourish
    slot(g, 14, 46); slot(g, 34, 46)                               # item + lapis slots (items at 15,47/35,47)
    rect(g, 58, 13, 169, 72, 'S'); rect(g, 58, 13, 169, 13, 'K')   # recessed enchant-button bay (buttons blit here)
    rect(g, 58, 74, 170, 100, 'S'); rect(g, 58, 74, 170, 74, 'K')  # recessed stat-bar bay
    for i in range(3): rect(g, 59, 75 + 10 * i, 168, 79 + 10 * i, 'r')  # bar tracks (blit fills over)
    rect(g, 6, 108, 169, 108, 'K'); rect(g, 6, 109, 169, 109, 'h')  # divider above inventory
    slotgrid(g, 7, 114, 9, 3)                                       # inventory 3x9 (items y=115/133/151)
    slotgrid(g, 7, 172, 9, 1)                                       # hotbar (items y=173)
    # --- widget atlas (exact blit UVs) ---
    hbar(g, 0, 197, 110, 'G', 'd', 'K')                            # eterna bar (green)
    hbar(g, 0, 202, 110, 'R', 'N', 'K')                            # quanta bar (red)
    hbar(g, 0, 207, 110, 'y', 'A', 'K')                           # arcana bar (purple)
    for s in range(3): numicon(g, 16 * s, 223, 'd', 'G', 'K')      # enabled pips
    for s in range(3): numicon(g, 16 * s, 239, 'S', 'e', 'K')      # disabled pips
    button(g, 148, 199, 108, 19, 'b', 'W', 'B')                    # enabled enchant row
    button(g, 148, 218, 108, 19, 'e', 'E', 'S')                    # disabled enchant row
    button(g, 148, 237, 108, 19, 'g', 'W', 'o')                    # hover enchant row
    # recipe-browser icon (224,0,16,16): a tiny enchanting glyph
    rect(g, 224, 0, 239, 15, 'P'); panel(g, 224, 0, 239, 15)
    rect(g, 229, 3, 234, 12, 'u'); g[5][231] = 'g'; g[8][231] = 'g'
    return g

def ring(g, cx, cy, outer, inner):
    for dx in range(-4, 5):
        for dy in range(-4, 5):
            d = dx * dx + dy * dy
            if 9 <= d <= 16: g[cy + dy][cx + dx] = outer
            elif d < 9: g[cy + dy][cx + dx] = inner

def build_library():
    g = grid(307, 256)
    panel(g, 0, 0, 175, 229)
    rect(g, 4, 13, 4, 16, 'g'); rect(g, 4, 13, 7, 13, 'g')          # title flourish
    rect(g, 11, 27, 136, 132, 'k'); rect(g, 11, 27, 136, 28, 'K')   # list recess
    rect(g, 11, 27, 12, 132, 'r')                                   # scrollbar track
    for x, y in ((142, 18), (142, 77), (142, 106)):                # io slots, ringed
        ring(g, x + 7, y + 7, 'o', 'q'); slot(g, x, y)
    rect(g, 6, 141, 169, 141, 'K'); rect(g, 6, 142, 169, 142, 'h')  # divider
    slotgrid(g, 7, 147, 9, 3)                                       # inventory (items y=148/166/184)
    slotgrid(g, 7, 205, 9, 1)                                       # hotbar (items y=206)
    # --- widget atlas ---
    def entry(x0, y0, base, hi, lo):                               # 113x20 list row + progress track
        button(g, x0, y0, 113, 20, base, hi, lo)
        rect(g, x0 + 3, y0 + 14, x0 + 90, y0 + 16, 'r')
    entry(194, 0, 'p', 'h', 'K')                                   # normal entry
    entry(194, 20, 'u', 'y', 'q')                                  # hover entry
    rect(g, 197, 42, 281, 44, 'G'); rect(g, 197, 42, 281, 42, 'd') # progress fill (green)
    button(g, 303, 40, 4, 12, 'g', 'W', 'o')                       # scrollbar thumb active
    button(g, 303, 52, 4, 12, 'e', 'H', 'S')                       # scrollbar thumb inactive
    return g

def render(name, g, out):
    # glyph.py renders square grids only; pad to square then crop back to w x h.
    h, w = len(g), len(g[0])
    s = max(h, w)
    rows = [(r + ['.'] * (s - len(r))) for r in g] + [['.'] * s for _ in range(s - h)]
    spec = ["# generated by art/gui/guigen.py", f"size: {s}", "", "legend:"]
    spec += [f"  {k} {v}" for k, v in PAL.items()]
    spec += ["", "grid:"] + ["  " + "".join(r) for r in rows]
    with tempfile.NamedTemporaryFile("w", suffix=".glyph", delete=False) as tf:
        tf.write("\n".join(spec) + "\n"); tmp = tf.name
    dst = os.path.join(out, f"{name}.png")
    square = os.path.join(out, f"{name}.sq.png")
    subprocess.run(["python3", GLYPH, tmp, "-o", square], check=True, stdout=subprocess.DEVNULL)
    if (w, h) != (s, s):
        subprocess.run(["convert", square, "-crop", f"{w}x{h}+0+0", "+repage", dst], check=True)
        os.remove(square)
    else:
        os.replace(square, dst)
    for p in (square, square[:-4] + "@16x.png", dst[:-4] + "@16x.png"):
        if os.path.exists(p): os.remove(p)
    os.remove(tmp)

if __name__ == "__main__":
    import sys
    out = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUT
    os.makedirs(out, exist_ok=True)
    render("enchanting_table", build_enchanting_table(), out)
    render("library", build_library(), out)
    print(f"rendered enchanting_table + library into {out}")
