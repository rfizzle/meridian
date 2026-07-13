package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Groom brush enchantment. Two levers: the per-level chance that
 * brushing an eligible farm animal yields its renewable drop, and the per-animal cooldown that
 * stops a pen from being farmed continuously. All logic here is plain arithmetic with no
 * {@code net.minecraft} types, so the roster wiring (which animal gives what) stays in the shell
 * handler while the numbers stay unit-testable.
 */
public final class GroomMath {

    /** Default chance at Groom I that a groom attempt yields a drop. */
    public static final double DEFAULT_CHANCE_LEVEL_1 = 0.25;

    /** Default chance at Groom II. */
    public static final double DEFAULT_CHANCE_LEVEL_2 = 0.45;

    /** Default per-animal cooldown between groom attempts, in ticks (two minutes). */
    public static final int DEFAULT_COOLDOWN_TICKS = 2400;

    /**
     * Sentinel game time meaning "this animal has never been groomed". Stored in the cooldown
     * attachment's initializer; checked explicitly in {@link #cooldownElapsed(long, long, int)}
     * before any subtraction so the sentinel can never underflow into a spurious cooldown.
     */
    public static final long NEVER_BRUSHED = Long.MIN_VALUE;

    private GroomMath() {}

    /**
     * Chance that a groom attempt at {@code level} yields a drop, given the two configured levers.
     * Level {@code 0} (or below) never yields; level {@code 1} uses {@code chanceLevel1}; level
     * {@code 2}+ uses {@code chanceLevel2}. The returned chance is clamped to {@code [0, 1]}.
     */
    public static double groomChance(int level, double chanceLevel1, double chanceLevel2) {
        if (level <= 0) return 0.0;
        return clampUnit(level == 1 ? chanceLevel1 : chanceLevel2);
    }

    /**
     * Whether an animal last groomed at {@code lastBrushed} is off cooldown at game time
     * {@code now}. A never-groomed animal ({@link #NEVER_BRUSHED}) is always ready. A
     * non-positive {@code cooldownTicks} means "no cooldown".
     */
    public static boolean cooldownElapsed(long lastBrushed, long now, int cooldownTicks) {
        if (lastBrushed == NEVER_BRUSHED) return true;
        if (cooldownTicks <= 0) return true;
        return now - lastBrushed >= cooldownTicks;
    }

    private static double clampUnit(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }
}
