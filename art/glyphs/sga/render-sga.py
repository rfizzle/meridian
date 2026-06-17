#!/usr/bin/env python3
"""Render the 26 SGA shape glyphs into all 4 elemental tints (104 PNGs).

Each art/glyphs/sga/<letter>.glyph is a standalone fire-tinted rune using the
tone tokens H (bright) / M (mid) / D (dim). This driver swaps those three
colors for each element's ramp and renders <out>/sga_<letter>_<element>.png via
the shared glyph renderer.

Usage:
  python3 art/glyphs/sga/render-sga.py [OUT_DIR]
OUT_DIR defaults to the shipped particle directory.
"""
import os, re, subprocess, sys, tempfile

ROOT = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(ROOT, "..", "..", ".."))
GLYPH = os.path.join(REPO, ".ai/skills/mc-textures/scripts/glyph.py")
DEFAULT_OUT = os.path.join(REPO, "src/main/resources/assets/meridian/textures/particle")

# bright -> mid -> dim per element
PALETTES = {
    "fire":  {"H": "#ffe000", "M": "#ffa300", "D": "#cd5600"},
    "water": {"H": "#27ccdb", "M": "#1e95b0", "D": "#1d6d8d"},
    "sculk": {"H": "#29dfeb", "M": "#20c2d0", "D": "#17a5b4"},
    "end":   {"H": "#cfb2dd", "M": "#bf90d7", "D": "#9c52c3"},
}

def recolor(spec, pal):
    def sub(line):
        m = re.match(r"(\s+)([HMD])(\s+)#[0-9a-fA-F]{6}(.*)", line)
        return f"{m.group(1)}{m.group(2)}{m.group(3)}{pal[m.group(2)]}{m.group(4)}" if m else line
    return "\n".join(sub(l) for l in spec.splitlines()) + "\n"

def main():
    out = os.path.abspath(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_OUT
    os.makedirs(out, exist_ok=True)
    n = 0
    for letter in "abcdefghijklmnopqrstuvwxyz":
        spec = open(os.path.join(ROOT, f"{letter}.glyph")).read()
        for el, pal in PALETTES.items():
            with tempfile.NamedTemporaryFile("w", suffix=".glyph", delete=False) as tf:
                tf.write(recolor(spec, pal))
                tmp = tf.name
            dst = os.path.join(out, f"sga_{letter}_{el}.png")
            subprocess.run(["python3", GLYPH, tmp, "-o", dst],
                           check=True, stdout=subprocess.DEVNULL)
            # glyph.py drops a @16x preview beside the target; not wanted for particles
            preview = dst[:-4] + "@16x.png"
            if os.path.exists(preview):
                os.remove(preview)
            os.remove(tmp)
            n += 1
    print(f"rendered {n} SGA particle textures into {out}")

if __name__ == "__main__":
    main()
