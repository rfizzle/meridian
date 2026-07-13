package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.api.StatCollection;

/**
 * Pure sabotage math for Curse of Dissonance. While the wearer carries the curse, their own
 * enchanting-table session rolls with reduced Eterna and Clues — the item actively works against
 * future enchanting, so Rectification and the cleansing loop matter more. Only these two axes move;
 * Quanta, Arcana, and Rectification are left alone (issue #225 scope).
 *
 * <p>The reduction is applied per level and floored through {@link StatCollection#clamped()} so it
 * can never push a stat negative. Kept Fabric-free so it is unit-testable without a running game.
 */
public final class DissonanceMath {

    /** Eterna removed from the wearer's table per level of Curse of Dissonance. */
    public static final float ETERNA_REDUCTION_PER_LEVEL = 5.0f;
    /** Clues removed from the wearer's table per level of Curse of Dissonance. */
    public static final int CLUES_REDUCTION_PER_LEVEL = 1;

    private DissonanceMath() {}

    /**
     * The stats a wearer's table should roll with when carrying Curse of Dissonance at
     * {@code level}. At {@code level <= 0} the input is returned unchanged; otherwise Eterna and
     * Clues are reduced by the per-level amounts, floored at 0.
     *
     * @param base  the session stats gathered from shelves (already baseline-applied and clamped)
     * @param level the wearer's Curse of Dissonance level (0 = not equipped)
     * @return the reduced stats, or {@code base} unchanged when the curse is absent
     */
    public static StatCollection apply(StatCollection base, int level) {
        if (level <= 0) return base;
        return base.withDissonanceReduction(
                ETERNA_REDUCTION_PER_LEVEL * level,
                CLUES_REDUCTION_PER_LEVEL * level);
    }
}
