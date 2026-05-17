package com.rfizzle.meridian.enchanting;

/**
 * Produces the single-line stat summary that {@code MeridianEnchantmentScreen} draws below the
 * three enchant preview slots (see DESIGN.md §"Table Menu Implementation"). Kept render-free so
 * the output can be asserted in unit tests without a GL context or font binding.
 *
 * <p>Float values are floored to integers for display — matches Zenith's HUD conventions and
 * keeps the line short enough to fit under the slot column.
 */
public final class StatLineFormatter {

    private StatLineFormatter() {
    }

    /**
     * Format a stat bundle as {@code "E: 50  Q: 12  A: 5  R: 10  C: 2"}. Null input collapses to
     * an all-zero row.
     */
    public static String format(StatCollection stats) {
        StatCollection s = stats != null ? stats : StatCollection.EMPTY;
        return String.format(
                "E: %d  Q: %d  A: %d  R: %d  C: %d",
                (int) s.eterna(),
                (int) s.quanta(),
                (int) s.arcana(),
                (int) s.rectification(),
                s.clues());
    }
}
