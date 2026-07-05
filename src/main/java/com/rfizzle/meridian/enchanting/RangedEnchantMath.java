package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Longshot / Seeker / Harpoon ranged and thrown enchantments.
 * Kept free of Minecraft and Fabric imports so plain JUnit tests can exercise the
 * formulas; {@code ProjectileEnchantmentHandler} is the only runtime caller. The
 * per-effect tuning constants live here (not in the handler's balance block) for the
 * same reason.
 */
public final class RangedEnchantMath {

    /** Straight-line distance from launch before Longshot starts granting any bonus. */
    public static final double LONGSHOT_GRACE_DISTANCE = 12.0;
    /** Distance at which Longshot's bonus stops growing — the hard cap the issue requires. */
    public static final double LONGSHOT_MAX_DISTANCE = 48.0;
    /** Damage multiplier bonus per enchantment level at {@link #LONGSHOT_MAX_DISTANCE}. */
    public static final double LONGSHOT_BONUS_PER_LEVEL = 0.25;

    /** How far ahead of the shooter Seeker looks for a crosshair target at fire time. */
    public static final double SEEKER_LOCK_RANGE = 64.0;
    public static final double SEEKER_TURN_DEGREES_BASE = 0.5;
    public static final double SEEKER_TURN_DEGREES_PER_LEVEL = 1.5;

    public static final double HARPOON_PULL_BASE = 0.65;
    public static final double HARPOON_PULL_PER_LEVEL = 0.35;
    /** Upward kick added to the pull so victims clear low terrain instead of snagging. */
    public static final double HARPOON_LIFT = 0.25;
    /**
     * Close-range damping: the pull speed is capped at {@code distance * factor} so a
     * point-blank victim is nudged, never yanked past the thrower.
     */
    public static final double HARPOON_CLOSE_RANGE_FACTOR = 0.25;

    private RangedEnchantMath() {}

    /**
     * Longshot's damage multiplier for an arrow that has flown {@code distance} blocks
     * (straight-line from launch). 1.0 inside the grace distance — bonus only, never a
     * point-blank penalty — ramping linearly to the per-level cap at
     * {@link #LONGSHOT_MAX_DISTANCE} and holding there.
     */
    public static double longshotMultiplier(int level, double distance) {
        if (level <= 0 || distance <= LONGSHOT_GRACE_DISTANCE) return 1.0;
        double progress = Math.min(1.0,
                (distance - LONGSHOT_GRACE_DISTANCE) / (LONGSHOT_MAX_DISTANCE - LONGSHOT_GRACE_DISTANCE));
        return 1.0 + LONGSHOT_BONUS_PER_LEVEL * level * progress;
    }

    /** Seeker's per-tick steering limit, in radians — the "weak curve angle" tuning knob. */
    public static double seekerTurnRadians(int level) {
        if (level <= 0) return 0.0;
        return Math.toRadians(SEEKER_TURN_DEGREES_BASE + SEEKER_TURN_DEGREES_PER_LEVEL * level);
    }

    /**
     * Speed of Harpoon's pull impulse toward the thrower, for a victim {@code distance}
     * blocks away. Scales with level, damped at close range so the victim never
     * overshoots the thrower.
     */
    public static double harpoonPullSpeed(int level, double distance) {
        if (level <= 0 || distance <= 0.0) return 0.0;
        double strength = HARPOON_PULL_BASE + HARPOON_PULL_PER_LEVEL * level;
        return Math.min(strength, distance * HARPOON_CLOSE_RANGE_FACTOR);
    }
}
