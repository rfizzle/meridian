package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Umbral helmet enchantment. Kept free of Minecraft and Fabric
 * imports so plain JUnit tests can exercise the formula; {@code UmbralVisibilityMixin} is the
 * only runtime caller. The tuning constants live here for the same reason.
 *
 * <p>Umbral reduces the range at which a hostile mob can newly acquire a sneaking, in-darkness
 * wearer by scaling down the entity's visibility factor — the same {@code d} multiplier vanilla
 * Invisibility uses in {@code TargetingConditions#test} ({@code effectiveRange = range * d}).
 * A factor of 0.5, for instance, halves the acquisition radius.
 */
public final class UmbralStealthMath {

    private UmbralStealthMath() {}

    /**
     * Highest {@code getMaxLocalRawBrightness} value that still counts as "darkness" for Umbral.
     * 7 sits at the torch-lit edge of dark — daylight (15) and well-lit rooms never qualify.
     */
    public static final int MAX_LIGHT_LEVEL = 7;

    /** Fraction of visibility removed per Umbral level (I → 0.25, II → 0.50, III → 0.75). */
    public static final double REDUCTION_PER_LEVEL = 0.25;

    /** Floor on the multiplier so the wearer is never made completely undetectable. */
    public static final double MIN_MULTIPLIER = 0.05;

    /**
     * The visibility multiplier applied on top of vanilla's own factors. At or below level 0 the
     * multiplier is 1.0 (no effect); each level removes {@link #REDUCTION_PER_LEVEL}, clamped to
     * {@link #MIN_MULTIPLIER} so acquisition range is sharply cut but never zero.
     */
    public static double visibilityMultiplier(int level) {
        if (level <= 0) return 1.0;
        return Math.max(MIN_MULTIPLIER, 1.0 - REDUCTION_PER_LEVEL * level);
    }

    /** Whether Umbral should dim visibility: the enchant is worn, the wearer sneaks, and it is dark. */
    public static boolean isStealthed(int level, boolean sneaking, int lightLevel) {
        return level > 0 && sneaking && lightLevel <= MAX_LIGHT_LEVEL;
    }

    /**
     * The visibility a hostile looker gets, given the base factor and the stealth conditions. When
     * the wearer is stealthed ({@link #isStealthed}) the base is scaled by {@link #visibilityMultiplier};
     * otherwise it is returned unchanged. This is the whole Umbral decision, kept pure so the light
     * threshold and the per-level scaling are unit-tested without a world.
     */
    public static double stealthedVisibility(double baseVisibility, int level, boolean sneaking, int lightLevel) {
        if (!isStealthed(level, sneaking, lightLevel)) return baseVisibility;
        return baseVisibility * visibilityMultiplier(level);
    }
}
