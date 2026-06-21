#!/usr/bin/env python3
"""Compose the Meridian astrolabe/compass hero logo as a 128px .glyph grid.

Geometry is computed mathematically (true circles, a symmetric 8-point rose) and
emitted as an ASCII-grid .glyph; glyph.py rasterizes it deterministically.
"""
import math, random

N = 128
CX = CY = (N - 1) / 2.0
random.seed(7)  # deterministic star field

# ---- palette (night-sky neutrals + Meridian brand purple/gold) -------------
COL = {
    'ink':        '#0a0a0a',
    # blue rim glow (alpha falloff)
    'glow1':      '#4a78ffcc',
    'glow2':      '#3a63ee80',
    'glow3':      '#2c4ad840',
    # stone bezel
    'st_sh':      '#15182a',
    'st_dark':    '#23263a',
    'st_mid':     '#363b56',
    'st_lit':     '#535981',
    'st_spec':    '#7b82ad',
    # dial
    'dial_deep':  '#100d28',
    'dial':       '#181440',
    'dial_edge':  '#0d0a20',
    'ring_line':  '#3a2f6e',
    'ring_line2': '#2a2350',
    # moon
    'moon_lit':   '#cdd4f0',
    'moon_mid':   '#9aa0ce',
    'moon_dark':  '#5b6094',
    # compass rose
    'r_lav_lit':  '#d3d8fb',
    'r_lav':      '#a3aaef',
    'r_blue':     '#5d74e0',
    'r_blue_dk':  '#39479f',
    'r_pur':      '#7b2fbe',
    'r_pur_dk':   '#491d76',
    # stars
    'star':       '#eef0ff',
    'star_bri':   '#ffffff',
    'star_dim':   '#97a3df',
    # center gold
    'gold':       '#ffd700',
    'gold_dk':    '#daa520',
    'gold_glo':   '#fff3c0',
    # thorny vines wrapping the bezel
    'vine_dk':    '#161d22',
    'vine':       '#27332e',
    'vine_lit':   '#3c4d3f',
}

# grid of color-keys; None = transparent
G = [[None] * N for _ in range(N)]

def put(x, y, key):
    xi, yi = int(round(x)), int(round(y))
    if 0 <= xi < N and 0 <= yi < N:
        G[yi][xi] = key

def dist(x, y):
    return math.hypot(x - CX, y - CY)

def ang(x, y):
    return math.atan2(y - CY, x - CX)

# ---- 1. glow halo ----------------------------------------------------------
R_OUT = 55.0   # stone ring outer (glow extends +6 -> 61, fits inside 63.5 to edge)
R_IN  = 44.0   # stone ring inner
for y in range(N):
    for x in range(N):
        d = dist(x, y)
        if R_OUT < d <= R_OUT + 2:
            G[y][x] = 'glow1'
        elif R_OUT + 2 < d <= R_OUT + 4:
            G[y][x] = 'glow2'
        elif R_OUT + 4 < d <= R_OUT + 6.5:
            G[y][x] = 'glow3'

# ---- 2. stone bezel annulus ------------------------------------------------
for y in range(N):
    for x in range(N):
        d = dist(x, y)
        if R_IN <= d <= R_OUT:
            a = ang(x, y)
            # light from upper-left: brighter where normal faces UL
            shade = math.cos(a - math.radians(225))
            # bumpy outer profile via angular noise
            bump = 0.6 * math.sin(a * 9) + 0.4 * math.sin(a * 17 + 1.3)
            t = (d - R_IN) / (R_OUT - R_IN)  # 0 inner .. 1 outer
            base = shade + bump * 0.35
            if d >= R_OUT - 1.2 or d <= R_IN + 1.0:
                G[y][x] = 'ink'
            elif base > 0.85:
                G[y][x] = 'st_spec'
            elif base > 0.25:
                G[y][x] = 'st_lit'
            elif base > -0.35:
                G[y][x] = 'st_mid'
            elif base > -0.8:
                G[y][x] = 'st_dark'
            else:
                G[y][x] = 'st_sh'

# ---- 3. dial disk ----------------------------------------------------------
for y in range(N):
    for x in range(N):
        d = dist(x, y)
        if d < R_IN:
            if d > R_IN - 1.5:
                G[y][x] = 'dial_edge'
            elif d > R_IN * 0.62:
                G[y][x] = 'dial'
            else:
                G[y][x] = 'dial_deep'

# ---- 3.5 thorny vines wrapping the bezel -----------------------------------
def vine(freq, phase, amp, rmid):
    steps = 2400
    prev = None
    for i in range(steps):
        a = 2 * math.pi * i / steps
        r = rmid + amp * math.sin(freq * a + phase)
        x = CX + r * math.cos(a); y = CY + r * math.sin(a)
        xi, yi = int(round(x)), int(round(y))
        if not (0 <= xi < N and 0 <= yi < N):
            continue
        # only lay vine over the stone bezel / just outside it
        d = dist(xi, yi)
        if not (R_IN - 1 <= d <= R_OUT + 1):
            continue
        nrm = math.sin(freq * a + phase)
        G[yi][xi] = 'vine_lit' if nrm > 0.5 else 'vine'
        # 2nd px toward centre for thickness
        x2 = CX + (r - 1) * math.cos(a); y2 = CY + (r - 1) * math.sin(a)
        put(x2, y2, 'vine_dk')
        # thorn ticks at the wave crests
        if prev is not None and nrm > 0.93 and i % 11 == 0:
            tx = CX + (r + 2) * math.cos(a); ty = CY + (r + 2) * math.sin(a)
            put(tx, ty, 'vine_dk')
        prev = nrm

vine(7,  0.0, 3.5, 49.0)
vine(7,  math.pi, 3.0, 50.5)

# ---- 4. concentric ring lines ----------------------------------------------
for rr, ck in ((R_IN - 3, 'ring_line'), (35, 'ring_line2'), (25, 'ring_line2'), (15, 'ring_line')):
    steps = int(2 * math.pi * rr * 2)
    for i in range(steps):
        a = 2 * math.pi * i / steps
        x = CX + rr * math.cos(a)
        y = CY + rr * math.sin(a)
        if G[int(round(y))][int(round(x))] in ('dial', 'dial_deep'):
            put(x, y, ck)

# ---- 5. star field ---------------------------------------------------------
def sparkle(x, y, ck, arm=2):
    put(x, y, 'star_bri')
    for i in range(1, arm + 1):
        put(x + i, y, ck); put(x - i, y, ck)
        put(x, y + i, ck); put(x, y - i, ck)

stars = 0
attempts = 0
big = [(0.55, 0.30), (0.30, 0.62), (0.70, 0.66), (0.50, 0.80), (0.66, 0.42)]
while stars < 46 and attempts < 4000:
    attempts += 1
    x = random.uniform(CX - R_IN, CX + R_IN)
    y = random.uniform(CY - R_IN, CY + R_IN)
    if dist(x, y) > R_IN - 4 or dist(x, y) < 7:
        continue
    r = random.random()
    if r > 0.9:
        sparkle(int(x), int(y), 'star_dim', 2)
    elif r > 0.7:
        put(x, y, 'star'); put(x + 1, y, 'star_dim')
    else:
        put(x, y, 'star' if r > 0.4 else 'star_dim')
    stars += 1

# ---- 6. compass rose (8 points) --------------------------------------------
def tri_fill(p0, p1, p2, ck):
    xs = [p0[0], p1[0], p2[0]]; ys = [p0[1], p1[1], p2[1]]
    x0, x1 = int(math.floor(min(xs))), int(math.ceil(max(xs)))
    y0, y1 = int(math.floor(min(ys))), int(math.ceil(max(ys)))
    def sign(a, b, c):
        return (a[0] - c[0]) * (b[1] - c[1]) - (b[0] - c[0]) * (a[1] - c[1])
    for yy in range(y0, y1 + 1):
        for xx in range(x0, x1 + 1):
            p = (xx + 0.0, yy + 0.0)
            d1 = sign(p, p0, p1); d2 = sign(p, p1, p2); d3 = sign(p, p2, p0)
            neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
            pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
            if not (neg and pos):
                if 0 <= xx < N and 0 <= yy < N:
                    G[yy][xx] = ck

def point(theta, length, halfw, light, dark):
    """A kite point split into a lit (left) and dark (right) facet."""
    tip = (CX + length * math.cos(theta), CY + length * math.sin(theta))
    # base corners perpendicular to spine, near center
    pl = (CX + halfw * math.cos(theta - math.pi / 2),
          CY + halfw * math.sin(theta - math.pi / 2))
    pr = (CX + halfw * math.cos(theta + math.pi / 2),
          CY + halfw * math.sin(theta + math.pi / 2))
    c = (CX, CY)
    tri_fill(c, tip, pl, light)   # left facet (lit)
    tri_fill(c, tip, pr, dark)    # right facet (shadow)

# diagonal (minor) points first, then cardinal on top
for k in range(4):
    th = math.radians(45 + 90 * k)
    point(th, 27, 6.5, 'r_pur', 'r_pur_dk')
# cardinal (major) points
for k in range(4):
    th = math.radians(90 * k)  # E, S, W, N
    point(th, 40, 7.5, 'r_lav', 'r_blue')
# rose spine outline accents
for k in range(4):
    th = math.radians(90 * k)
    for t in range(0, 40):
        x = CX + t * math.cos(th); y = CY + t * math.sin(th)
        put(x, y, 'r_lav_lit')

# ---- 7. center gold sun ----------------------------------------------------
for y in range(N):
    for x in range(N):
        d = dist(x, y)
        if d <= 10:
            if d > 9:
                G[y][x] = 'ink'
            elif d > 7.5:
                G[y][x] = 'r_blue_dk'
            else:
                G[y][x] = 'dial_deep'
# crisp gold 4+4-point starburst
point(0,            7, 2.4, 'gold', 'gold_dk')
point(math.pi/2,    7, 2.4, 'gold', 'gold_dk')
point(math.pi,      7, 2.4, 'gold', 'gold_dk')
point(3*math.pi/2,  7, 2.4, 'gold', 'gold_dk')
for k in range(4):
    point(math.radians(45 + 90 * k), 3.6, 1.6, 'gold_dk', 'gold_dk')
for t in range(0, 8):
    put(CX + t, CY, 'gold' if t < 5 else 'gold_dk')
    put(CX - t, CY, 'gold' if t < 5 else 'gold_dk')
    put(CX, CY - t, 'gold' if t < 5 else 'gold_dk')
    put(CX, CY + t, 'gold' if t < 5 else 'gold_dk')
put(CX, CY, 'gold_glo'); put(CX - 1, CY - 1, 'gold_glo'); put(CX, CY - 1, 'gold_glo')

# ---- 8. crescent moon (top, overlapping ring) ------------------------------
# Big circle minus a circle offset down-right -> a bold crescent, horns up,
# tilted like the reference. Drawn last so it sits cleanly on top of the ring.
mx, my = CX - 1, 14.0
R1, R2 = 12.0, 10.7
odx, ody = 3.0, 4.8   # subtract centre offset (down-right)
for y in range(N):
    for x in range(N):
        d1 = math.hypot(x - mx, y - my)
        d2 = math.hypot(x - (mx + odx), y - (my + ody))
        if d1 <= R1 and d2 > R2:
            if d1 > R1 - 1.5 or d2 < R2 + 1.4:
                G[y][x] = 'ink'                       # rim outline both edges
            elif d1 < R1 - 2.6 and (x - mx) < 1:
                G[y][x] = 'moon_lit'                  # lit outer/upper-left
            elif d2 < R2 + 3.2:
                G[y][x] = 'moon_dark'                 # shadow near inner curve
            else:
                G[y][x] = 'moon_mid'

# ---- emit .glyph -----------------------------------------------------------
# assign legend chars
pool = "@$%&*+=oOxX0123456789abcdefghijklmnpqrstuvwzABCDEFGHIJKLMNPQRSTUVWZ?!~^"
used = []
for row in G:
    for c in row:
        if c and c not in used:
            used.append(c)
assert len(used) <= len(pool), f"too many colors: {len(used)}"
key2ch = {k: pool[i] for i, k in enumerate(used)}

lines = ["# Meridian astrolabe hero logo — generated by gen_compass.py", f"size: {N}", "", "legend:", "  . transparent"]
for k in used:
    lines.append(f"  {key2ch[k]} {COL[k]}")
lines.append("")
lines.append("frame:")
for row in G:
    lines.append("  " + "".join(key2ch[c] if c else "." for c in row))

OUT = "art/glyphs/astrolabe_logo.glyph"
with open(OUT, "w") as f:
    f.write("\n".join(lines) + "\n")
print(f"wrote {OUT}  ({len(used)} colors)")
