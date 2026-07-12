package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Endurance mount enchantment. Kept free of Minecraft and Fabric
 * imports so plain JUnit tests can exercise the formula; {@code EnduranceMixin} is the only
 * runtime caller. The tuning constants live here for the same reason.
 *
 * <p>Endurance slowly restores a mount's health with no feeding: once every
 * {@link #PULSE_INTERVAL_TICKS} the mount heals {@link #healPerPulse(int)} health, so the rate
 * scales with the enchantment level. At level III a wounded mount recovers noticeably faster
 * than at level I, but never fast enough to matter mid-combat.
 */
public final class EnduranceHealMath {

    private EnduranceHealMath() {}

    /** Ticks between heal pulses. 100 ticks is five seconds — a deliberately slow, out-of-combat drip. */
    public static final int PULSE_INTERVAL_TICKS = 100;

    /** Health restored per pulse per level (level I → 1.0, II → 2.0, III → 3.0). */
    public static final float HEAL_PER_LEVEL = 1.0f;

    /**
     * Health restored on a single pulse for a mount carrying Endurance at {@code level}. At or
     * below level 0 the mount heals nothing; each level adds {@link #HEAL_PER_LEVEL}.
     */
    public static float healPerPulse(int level) {
        if (level <= 0) return 0.0f;
        return level * HEAL_PER_LEVEL;
    }
}
