package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Grind / Adamant / Reclaim mining enchantments. Kept free
 * of Minecraft and Fabric imports so plain JUnit tests can exercise the formulas;
 * {@code PlayerMixin}, {@code ItemStackMixin}, and {@code BlockDropsMixin} are the
 * only runtime callers. The per-effect tuning constants live here (not in the mixins)
 * for the same reason.
 */
public final class MiningEnchantMath {

    /**
     * Blocks softer than this gain nothing from Grind. Stone (1.5) and everything a
     * bare hand shreds stay vanilla; deepslate, ores (3.0+), and obsidian qualify.
     */
    public static final float GRIND_MIN_HARDNESS = 2.5f;

    /** Break-speed bonus contributed per enchantment level per point of block hardness. */
    public static final float GRIND_SPEED_PER_HARDNESS_PER_LEVEL = 0.5f;

    /**
     * Ceiling on Grind's additive bonus, so obsidian-class hardness (50) cannot
     * out-scale Efficiency V's +26 into instant-mining territory.
     */
    public static final float GRIND_MAX_BONUS = 24.0f;

    private MiningEnchantMath() {}

    /**
     * Grind's additive break-speed bonus for a block of the given hardness. Zero below
     * {@link #GRIND_MIN_HARDNESS} ("does nothing on soft blocks"), then scales linearly
     * with hardness and level, capped at {@link #GRIND_MAX_BONUS}. Stacks additively on
     * top of Efficiency's mining-efficiency attribute, which the cap keeps bounded.
     */
    public static float grindBonus(int level, float hardness) {
        if (level <= 0 || hardness < GRIND_MIN_HARDNESS) return 0.0f;
        return Math.min(GRIND_MAX_BONUS, GRIND_SPEED_PER_HARDNESS_PER_LEVEL * level * hardness);
    }

    /**
     * Adamant's effective mining-tier index: the tool's own ladder position raised by
     * one rung per level, clamped to the ladder's top ({@code maxIndex}, netherite).
     * Levels never lower a tier and a level-0 call is the identity.
     */
    public static int adamantEffectiveTierIndex(int baseIndex, int level, int maxIndex) {
        if (level <= 0) return baseIndex;
        return Math.min(maxIndex, baseIndex + level);
    }
}
