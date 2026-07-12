package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Grapnel (fishing rod) and Thermal (elytra) traversal
 * enchantments. Kept free of Minecraft and Fabric imports so plain JUnit tests can
 * exercise the formulas; {@code GrapnelHandler} and {@code ArmorTickHandler} are the
 * only runtime callers. The per-effect tuning constants live here (not in the handlers'
 * balance blocks) for the same reason.
 */
public final class TraversalEnchantMath {

    // ---- Grapnel: hook a block, reel the player toward it ----

    /** Reel range at level 1; each further level adds {@link #GRAPNEL_RANGE_PER_LEVEL}. */
    public static final double GRAPNEL_RANGE_BASE = 8.0;
    public static final double GRAPNEL_RANGE_PER_LEVEL = 6.0;

    public static final double GRAPNEL_PULL_BASE = 0.8;
    public static final double GRAPNEL_PULL_PER_LEVEL = 0.25;
    /**
     * Close-range damping: pull speed is capped at {@code distance * factor} so a hook
     * anchored a block away nudges the player rather than snapping them into the wall.
     */
    public static final double GRAPNEL_CLOSE_RANGE_FACTOR = 0.35;
    /** Upward kick added to the pull so the player arcs up toward a ledge, not into its face. */
    public static final double GRAPNEL_LIFT = 0.3;

    /** Ticks the rod is unusable after a pull — the "short cooldown" that prevents free flight. */
    public static final int GRAPNEL_COOLDOWN_TICKS = 20;
    /** Durability spent per successful pull. */
    public static final int GRAPNEL_DURABILITY_COST = 2;
    /**
     * A hook counts as anchored only once it has settled against terrain. Above this speed
     * (squared, blocks/tick) it is still mid-flight, so reeling in must not grapple — that
     * would let a cast that merely grazes a block face yank the player unexpectedly.
     */
    public static final double GRAPNEL_ANCHOR_MAX_SPEED_SQR = 0.02;

    // ---- Thermal: updraft while gliding over a heat source ----

    public static final double THERMAL_LIFT_BASE = 0.06;
    public static final double THERMAL_LIFT_PER_LEVEL = 0.03;
    // A gentle updraft, not an elevator: the assisted climb tops out at ~0.25/0.35 blocks per
    // tick (5–7 blocks/s). Heat below is still required every tick, so leaving it drops the
    // player — the lift soars, it never becomes self-sustaining elytra flight.
    public static final double THERMAL_MAX_CLIMB_BASE = 0.15;
    public static final double THERMAL_MAX_CLIMB_PER_LEVEL = 0.10;
    public static final int THERMAL_SCAN_DEPTH_BASE = 5;
    public static final int THERMAL_SCAN_DEPTH_PER_LEVEL = 2;

    // ---- Tailwind: firework rockets give a stronger, longer boost while gliding ----

    /** Extra ticks of boost lifetime a Tailwind firework burns per level — the "longer-burning" half. */
    public static final int TAILWIND_LIFETIME_TICKS_PER_LEVEL = 10;
    /**
     * Extra forward velocity (blocks/tick, along the glider's look) Tailwind adds each gliding tick
     * per level — the "stronger" half, layered on top of vanilla's own boost.
     */
    public static final double TAILWIND_PUSH_PER_LEVEL = 0.05;

    /** Curse of Molting: chance an elytra firework boost fizzles out instead of pushing the glider. */
    public static final float MOLTING_FIZZLE_CHANCE = 0.25f;
    /** Curse of Molting: how often (in ticks) a glide sheds an extra burst of elytra durability. */
    public static final int MOLTING_SHED_INTERVAL_TICKS = 20;
    /** Curse of Molting: extra durability shed from the elytra on each burst while gliding. */
    public static final int MOLTING_SHED_DURABILITY = 2;

    private TraversalEnchantMath() {}

    /**
     * Maximum straight-line distance from the player to the anchored hook for a pull to
     * fire. Beyond this the reel-in does nothing, capping how far Grapnel can fling.
     */
    public static double grapnelMaxRange(int level) {
        if (level <= 0) return 0.0;
        return GRAPNEL_RANGE_BASE + GRAPNEL_RANGE_PER_LEVEL * level;
    }

    /**
     * Speed of Grapnel's pull impulse toward the hook, for a hook {@code distance} blocks
     * away. Scales with level, damped at close range so the player never overshoots the
     * anchor.
     */
    public static double grapnelPullSpeed(int level, double distance) {
        if (level <= 0 || distance <= 0.0) return 0.0;
        double strength = GRAPNEL_PULL_BASE + GRAPNEL_PULL_PER_LEVEL * level;
        return Math.min(strength, distance * GRAPNEL_CLOSE_RANGE_FACTOR);
    }

    /** Upward velocity Thermal adds each tick while a heat source sits below the glider. */
    public static double thermalLiftPerTick(int level) {
        if (level <= 0) return 0.0;
        return THERMAL_LIFT_BASE + THERMAL_LIFT_PER_LEVEL * level;
    }

    /**
     * Terminal upward speed the updraft may build to. The lift never pushes vertical
     * velocity past this, so Thermal boosts a climb but can't self-sustain infinite flight.
     */
    public static double thermalMaxClimb(int level) {
        if (level <= 0) return 0.0;
        return THERMAL_MAX_CLIMB_BASE + THERMAL_MAX_CLIMB_PER_LEVEL * level;
    }

    /** How many blocks below the glider Thermal searches for a heat source. */
    public static int thermalScanDepth(int level) {
        if (level <= 0) return 0;
        return THERMAL_SCAN_DEPTH_BASE + THERMAL_SCAN_DEPTH_PER_LEVEL * level;
    }

    /** Extra lifetime (ticks) a boost firework burns when carried by a Tailwind elytra. Zero at level 0. */
    public static int tailwindLifetimeBonus(int level) {
        if (level <= 0) return 0;
        return TAILWIND_LIFETIME_TICKS_PER_LEVEL * level;
    }

    /** Extra forward push (blocks/tick, along look) Tailwind adds each gliding tick. Zero at level 0. */
    public static double tailwindPush(int level) {
        if (level <= 0) return 0.0;
        return TAILWIND_PUSH_PER_LEVEL * level;
    }
}
