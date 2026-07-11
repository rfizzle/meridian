package com.rfizzle.meridian.enchanting;

/**
 * Pure balance math for the Meticulous brush enchantment. Two levers, both scaling with level:
 * how many brush strokes a suspicious block takes to excavate (speed), and how much luck is fed
 * into its archaeology loot roll (quality bias toward rarer entries, mirroring Fortuity).
 */
public final class BrushEnchantMath {

    /** Vanilla requires this many successful brushes ({@code REQUIRED_BRUSHES_TO_BREAK}) to excavate. */
    public static final int BRUSH_COMPLETION_BASE = 10;

    /** Each Meticulous level shaves this many strokes off the completion count. */
    public static final int BRUSH_COUNT_REDUCTION_PER_LEVEL = 3;

    /** Never let a block excavate in fewer than this many strokes, however high the level. */
    public static final int BRUSH_MIN_COMPLETION = 2;

    /** Extra loot-roll luck per level, matching Fortuity's per-level luck. */
    public static final float METICULOUS_LUCK_PER_LEVEL = 1.0f;

    private BrushEnchantMath() {}

    /**
     * Strokes needed to excavate a suspicious block at the given Meticulous level. Level {@code 0}
     * (or below) is vanilla ({@link #BRUSH_COMPLETION_BASE}); higher levels are faster, floored at
     * {@link #BRUSH_MIN_COMPLETION}.
     */
    public static int brushCompletionCount(int level) {
        if (level <= 0) return BRUSH_COMPLETION_BASE;
        return Math.max(BRUSH_MIN_COMPLETION, BRUSH_COMPLETION_BASE - BRUSH_COUNT_REDUCTION_PER_LEVEL * level);
    }

    /** Extra luck fed into a suspicious block's archaeology loot roll by Meticulous. */
    public static float meticulousLuckBonus(int level) {
        if (level <= 0) return 0.0f;
        return METICULOUS_LUCK_PER_LEVEL * level;
    }
}
