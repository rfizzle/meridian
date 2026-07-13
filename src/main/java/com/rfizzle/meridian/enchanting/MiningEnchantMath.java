package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Grind / Adamant / Reclaim / Dowse mining enchantments. Kept free
 * of Minecraft and Fabric imports so plain JUnit tests can exercise the formulas;
 * {@code PlayerMixin}, {@code ItemStackMixin}, {@code BlockDropsMixin}, {@code DowseHandler},
 * and {@code DowseOverlayRenderer} are the runtime callers. The per-effect tuning constants
 * live here (not in the mixins/handlers) for the same reason.
 */
public final class MiningEnchantMath {

    /** Half-extent, in blocks, of the cube Dowse scans around the player for the nearest ore. */
    public static final int DOWSE_SEARCH_RADIUS = 12;
    /** Upper bound on the ore positions Dowse floods and reveals, so a huge vein can't flood the packet. */
    public static final int DOWSE_MAX_VEIN = 64;
    /** How long a revealed vein glows, in ticks (8 seconds). */
    public static final int DOWSE_GLOW_TICKS = 160;
    /** Ticks over which the glow fades out as it expires. */
    public static final int DOWSE_FADE_TICKS = 20;
    /** Cooldown after a Dowse pulse, in ticks (30 seconds) — the "substantial cooldown" the issue asks for. */
    public static final int DOWSE_COOLDOWN_TICKS = 600;
    /** Peak opacity of the reveal glow; the fade curve scales down from here. */
    public static final float DOWSE_GLOW_MAX_ALPHA = 0.45f;

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
     * Glow opacity in {@code [0, DOWSE_GLOW_MAX_ALPHA]} for a vein with {@code ticksRemaining}
     * left before it expires. Full strength until the final {@link #DOWSE_FADE_TICKS}, then a
     * linear fade to zero. Negative/expired remainders clamp to zero.
     */
    public static float glowAlpha(int ticksRemaining) {
        if (ticksRemaining <= 0) return 0.0f;
        float fade = ticksRemaining >= DOWSE_FADE_TICKS ? 1.0f : (float) ticksRemaining / DOWSE_FADE_TICKS;
        return DOWSE_GLOW_MAX_ALPHA * fade;
    }

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
