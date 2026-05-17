package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.enchanting.recipe.StatRequirements;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-text formatters shared by the EMI / REI / JEI adapters. Kept render-free so the line
 * contents can be asserted in unit tests without a client classpath.
 *
 * <p>Two surfaces:
 * <ul>
 *   <li>{@link #requirementLines(StatRequirements, StatRequirements, int)} — the stat/XP
 *       summary shown beside an {@code enchanting}/{@code keep_nbt_enchanting} recipe in a
 *       recipe-viewer panel.</li>
 *   <li>{@link #shelfStatLines(EnchantingStats)} — the stat contribution shown on a shelf's
 *       info card.</li>
 * </ul>
 *
 * <p>{@link StatRequirements#NO_MAX}'s {@code -1} sentinel per axis suppresses the "up to X"
 * half of the requirement line — a recipe with {@code eterna: 40} and no upper bound reads
 * {@code "Eterna: ≥ 40"}, not {@code "Eterna: 40 – -1"}. Matches the Zenith convention.
 */
public final class RecipeInfoFormatter {

    private RecipeInfoFormatter() {
    }

    public static List<String> requirementLines(StatRequirements reqs, StatRequirements maxReqs, int xpCost) {
        List<String> lines = new ArrayList<>();
        addAxisLine(lines, "Eterna", reqs.eterna(), maxReqs.eterna());
        addAxisLine(lines, "Quanta", reqs.quanta(), maxReqs.quanta());
        addAxisLine(lines, "Arcana", reqs.arcana(), maxReqs.arcana());
        lines.add("XP cost: " + xpCost + " levels");
        return lines;
    }

    private static void addAxisLine(List<String> out, String label, float min, float max) {
        if (min <= 0F && max < 0F) {
            return;
        }
        if (max < 0F) {
            out.add(label + ": ≥ " + formatFloat(min));
        } else if (min <= 0F) {
            out.add(label + ": ≤ " + formatFloat(max));
        } else if (Math.abs(min - max) < 1e-4F) {
            out.add(label + ": " + formatFloat(min));
        } else {
            out.add(label + ": " + formatFloat(min) + " – " + formatFloat(max));
        }
    }

    public static List<String> shelfStatLines(EnchantingStats stats) {
        List<String> lines = new ArrayList<>();
        if (stats.eterna() != 0F) {
            if (stats.eterna() > 0) {
                lines.add("Eterna: +" + formatFloat(stats.eterna()) + " (Max " + formatFloat(stats.maxEterna()) + ")");
            } else {
                lines.add("Eterna: " + formatFloat(stats.eterna()));
            }
        }
        if (stats.quanta() != 0F) {
            lines.add("Quanta: " + (stats.quanta() > 0 ? "+" : "") + formatFloat(stats.quanta()) + "%");
        }
        if (stats.arcana() != 0F) {
            lines.add("Arcana: " + (stats.arcana() > 0 ? "+" : "") + formatFloat(stats.arcana()) + "%");
        }
        if (stats.rectification() != 0F) {
            lines.add("Rectification: " + (stats.rectification() > 0 ? "+" : "") + formatFloat(stats.rectification()) + "%");
        }
        if (stats.clues() != 0) {
            lines.add("Clues: " + (stats.clues() > 0 ? "+" : "") + stats.clues());
        }
        if (lines.isEmpty()) {
            lines.add("No stat contribution");
        }
        return lines;
    }

    /**
     * Round-number floats drop the trailing {@code .0} so {@code 15F} reads {@code "15"}; the
     * fractional Zenith values ({@code 22.5F}) keep one decimal.
     */
    static String formatFloat(float value) {
        if (Math.abs(value - Math.round(value)) < 1e-4F) {
            return Integer.toString(Math.round(value));
        }
        return Float.toString(Math.round(value * 10F) / 10F);
    }
}
