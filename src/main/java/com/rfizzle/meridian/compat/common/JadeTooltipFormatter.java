package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.enchanting.StatCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-text formatters for the Jade probe tooltip (T-7.4.1). Keeps the tooltip content as a
 * plain {@code List<String>} so the strings can be pinned in unit tests without a Jade runtime.
 *
 * <p>Two surfaces:
 * <ul>
 *   <li>{@link #enchantingTableLines(StatCollection)} — the five-axis stat summary shown when
 *       the probe is aimed at an enchanting table. Always emits one line per Apotheosis/Zenith
 *       axis (Eterna, Quanta, Arcana, Rectification, Clues) regardless of the stat value, so
 *       the tooltip has a stable shape the player can learn — Jade's own line renderer handles
 *       the visual compaction.</li>
 *   <li>{@link #libraryLine(int)} — the one-line summary shown when aimed at either
 *       library tier. Per-enchant points stay inside the library UI (DESIGN: detailed
 *       pool breakdown only in the menu, never bleeding into world tooltips).</li>
 * </ul>
 *
 * <p>Numeric formatting follows the same rules as {@link RecipeInfoFormatter#formatFloat}:
 * round-number floats drop {@code .0}, fractional values keep one decimal.
 */
public final class JadeTooltipFormatter {

    private JadeTooltipFormatter() {
    }

    /**
     * Five-line stat readout for an enchanting table, in the canonical Apotheosis ordering.
     * Eterna is rendered as {@code "Eterna: <value> / <maxEterna>"} whenever {@code maxEterna}
     * is non-zero so the player can see how much headroom remains on the primary axis; a zero
     * ceiling degrades to a plain {@code "Eterna: 0"}. Rectification is rendered as a percentage
     * (Apotheosis convention — the stat feeds a 0..1 probability filter). The Clues line always
     * renders as an integer.
     */
    public static List<String> enchantingTableLines(StatCollection stats) {
        List<String> lines = new ArrayList<>(5);
        if (stats.maxEterna() > 0F) {
            lines.add("Eterna: " + formatFloat(stats.eterna()) + " / " + formatFloat(stats.maxEterna()));
        } else {
            lines.add("Eterna: " + formatFloat(stats.eterna()));
        }
        lines.add("Quanta: " + formatFloat(stats.quanta()));
        lines.add("Arcana: " + formatFloat(stats.arcana()));
        lines.add("Rectification: " + formatPercent(stats.rectification()));
        lines.add("Clues: " + stats.clues());
        return lines;
    }

    /**
     * Per-block stat contribution lines shown when the probe is aimed at a shelf or other
     * enchanting stat provider. Only emits lines for non-zero stats so the tooltip stays compact.
     * Uses signed format (+/-) to make it clear these are contributions, not totals.
     */
    public static List<String> blockStatsLines(EnchantingStats stats) {
        List<String> lines = new ArrayList<>(6);
        if (stats.eterna() != 0F || stats.maxEterna() > 0F) {
            String eternaLine = "Eterna: " + signedFloat(stats.eterna());
            if (stats.maxEterna() > 0F) {
                eternaLine += " / " + formatFloat(stats.maxEterna());
            }
            lines.add(eternaLine);
        }
        if (stats.quanta() != 0F) {
            lines.add("Quanta: " + signedFloat(stats.quanta()));
        }
        if (stats.arcana() != 0F) {
            lines.add("Arcana: " + signedFloat(stats.arcana()));
        }
        if (stats.rectification() != 0F) {
            lines.add("Rectification: " + signedFloat(stats.rectification()));
        }
        if (stats.clues() != 0) {
            lines.add("Clues: " + (stats.clues() > 0 ? "+" : "") + stats.clues());
        }
        return lines;
    }

    static String signedFloat(float value) {
        String formatted = formatFloat(value);
        if (value > 0F) {
            return "+" + formatted;
        }
        return formatted;
    }

    /**
     * Singular/plural aware library summary. {@code storedCount} is the number of
     * enchantments with at least one stored point. The tier name is omitted because
     * Jade already displays the block name as the tooltip header.
     */
    public static String libraryLine(int storedCount) {
        int clamped = Math.max(0, storedCount);
        String noun = clamped == 1 ? "enchantment" : "enchantments";
        return clamped + " " + noun + " stored";
    }

    static String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 1e-4F) {
            return Integer.toString(Math.round(value));
        }
        return Float.toString(Math.round(value * 10F) / 10F);
    }

    static String formatPercent(float value) {
        if (Math.abs(value - Math.round(value)) < 1e-4F) {
            return Math.round(value) + "%";
        }
        return (Math.round(value * 10F) / 10F) + "%";
    }
}
