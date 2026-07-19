package com.rfizzle.meridian.enchanting;

/**
 * Pure decision math for the spyglass enchantments. No {@code net.minecraft.*} types, so the
 * curves are plain JUnit territory and survive a mappings rename untouched — the thin shell that
 * wires them to the game is {@code com.rfizzle.meridian.event.TrackersLensHandler}.
 */
public final class SpyglassEnchantMath {

    /**
     * Server ticks a creature must be held steady in the spyglass before Tracker's Lens marks it
     * (1.5 seconds). Constant at every level: the level buys a longer glow, not a faster sighting,
     * so a maxed lens still asks for the same deliberate pause on the target.
     */
    public static final int TRACKERS_LENS_SIGHTING_TICKS = 30;

    /**
     * How far Tracker's Lens can acquire a target, in blocks — well past Seeker's lock range,
     * matching the spyglass's own long-sight fantasy. The sight line is clipped against blocks
     * first, so range never lets it mark through terrain.
     */
    public static final double TRACKERS_LENS_RANGE = 64.0;

    /** Highest Tracker's Lens level; levels above it are treated as this one. */
    public static final int MAX_TRACKERS_LENS_LEVEL = 4;

    /**
     * Glow duration per level, in server ticks — 6s / 12s / 20s / 30s. The steps widen with level
     * rather than scaling linearly: level I sits at Mark's 120-tick glow so the entry level reads
     * as "Mark without the arrow", and each level above it buys a disproportionately longer tail,
     * which is what makes a maxed lens worth the anvil cost.
     */
    private static final int[] TRACKERS_LENS_GLOW_TICKS = {120, 240, 400, 600};

    private SpyglassEnchantMath() {}

    /**
     * How long, in server ticks, a creature marked by Tracker's Lens glows through walls at
     * {@code level}. Zero without the enchant; a level above {@link #MAX_TRACKERS_LENS_LEVEL}
     * clamps to the top of the curve rather than running off the table, so an over-levelled stack
     * from a command or a third-party tome degrades to max instead of throwing.
     */
    public static int trackersLensGlowTicks(int level) {
        if (level <= 0) return 0;
        return TRACKERS_LENS_GLOW_TICKS[Math.min(level, MAX_TRACKERS_LENS_LEVEL) - 1];
    }
}
