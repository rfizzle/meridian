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

    /** Base term of Undertow's crowd-gather radius, in blocks; {@code undertowRadius} adds {@link #UNDERTOW_RADIUS_PER_LEVEL} per level. */
    public static final double UNDERTOW_RADIUS_BASE = 3.0;
    public static final double UNDERTOW_RADIUS_PER_LEVEL = 2.0;
    public static final double UNDERTOW_PULL_BASE = 0.5;
    public static final double UNDERTOW_PULL_PER_LEVEL = 0.3;
    /** Upward kick on Undertow's pull so gathered creatures clear low terrain. */
    public static final double UNDERTOW_LIFT = 0.2;
    /**
     * Close-range damping for Undertow, mirroring Harpoon: the pull speed is capped at
     * {@code distance * factor} so a creature already on the impact point is nudged, never
     * flung across it.
     */
    public static final double UNDERTOW_CLOSE_RANGE_FACTOR = 0.25;

    /** How long a Mark-struck creature glows through walls, in server ticks (6 seconds). */
    public static final int MARK_GLOW_TICKS = 120;

    /** Base-damage multiplier applied to each Volley extra arrow (the primary shot is untouched). */
    public static final float VOLLEY_DAMAGE_MULTIPLIER = 0.5f;
    /** Yaw offset between adjacent arrows in Volley's fan, in degrees. */
    public static final float VOLLEY_SPREAD_DEGREES = 10.0f;

    /**
     * Curse of Wavering: extra firing inaccuracy added per level, on top of the weapon's own
     * inaccuracy. A vanilla bow fires at inaccuracy 1.0, so at max level II this triples the cone
     * the projectile scatters within — a clear handicap that still leaves the weapon aimable at
     * close range, rather than a dead weapon.
     */
    public static final float WAVERING_INACCURACY_PER_LEVEL = 2.0f;

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

    /**
     * Radius, in blocks, within which Undertow gathers creatures toward the thrown trident's
     * impact point. Scales with level; zero without the enchant.
     */
    public static double undertowRadius(int level) {
        if (level <= 0) return 0.0;
        return UNDERTOW_RADIUS_BASE + UNDERTOW_RADIUS_PER_LEVEL * level;
    }

    /**
     * Speed of Undertow's pull impulse toward the impact point, for a creature {@code distance}
     * blocks away. Scales with level and is damped at close range (mirroring
     * {@link #harpoonPullSpeed}) so a creature on top of the point isn't flung across it.
     */
    public static double undertowPullSpeed(int level, double distance) {
        if (level <= 0 || distance <= 0.0) return 0.0;
        double strength = UNDERTOW_PULL_BASE + UNDERTOW_PULL_PER_LEVEL * level;
        return Math.min(strength, distance * UNDERTOW_CLOSE_RANGE_FACTOR);
    }

    /**
     * Total arrows in a Volley fan at {@code level}: three at level I, five at level II —
     * an odd count so the fan stays symmetric about the aim line. Zero without the enchant.
     */
    public static int volleyArrowCount(int level) {
        if (level <= 0) return 0;
        return 1 + 2 * level;
    }

    /**
     * Extra arrows Volley adds on top of the {@code primaryCount} vanilla already fired
     * (one for a bow). Never negative, so a larger vanilla volley is left alone rather than
     * having arrows removed.
     */
    public static int volleyExtraCount(int level, int primaryCount) {
        return Math.max(0, volleyArrowCount(level) - primaryCount);
    }

    /**
     * Yaw offset for Volley's {@code extraIndex}-th extra arrow (0-based), fanning them
     * symmetrically out from the aim line: +step, -step, +2·step, -2·step, and so on.
     */
    public static float volleyArrowYawOffset(int extraIndex) {
        int magnitude = extraIndex / 2 + 1;
        float sign = (extraIndex % 2 == 0) ? 1.0f : -1.0f;
        return sign * magnitude * VOLLEY_SPREAD_DEGREES;
    }

    /**
     * Firing inaccuracy for a shot from a weapon carrying Curse of Wavering: the weapon's own
     * {@code baseInaccuracy} plus a per-level penalty. Level 0 returns the base unchanged, so a
     * clean weapon is never made less accurate.
     */
    public static float waveringInaccuracy(int level, float baseInaccuracy) {
        if (level <= 0) return baseInaccuracy;
        return baseInaccuracy + WAVERING_INACCURACY_PER_LEVEL * level;
    }
}
