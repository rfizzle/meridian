package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.enchanting.recipe.StatRequirements;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-7.1.3 — pins the text content rendered beside each EMI recipe and shelf info panel.
 * Covers both Acceptance bullets (input/output/stat/XP line set, and shelf stat line set) at the
 * data layer so the visual render in the EMI screen can be checked in a smoke test without
 * second-guessing the values.
 *
 * <p>Pure JUnit — no Minecraft bootstrap, no EMI classes. Keeps the formatter as the single source
 * of truth for what any recipe-viewer integration (EMI/REI/JEI) displays.
 */
class RecipeInfoFormatterTest {

    @Test
    void requirementLines_unboundedMax_rendersGreaterEquals() {
        StatRequirements reqs = new StatRequirements(40F, 15F, 60F);
        List<String> lines = RecipeInfoFormatter.requirementLines(reqs, StatRequirements.NO_MAX, 3);

        assertEquals(List.of(
                "Eterna: ≥ 40",
                "Quanta: ≥ 15",
                "Arcana: ≥ 60",
                "XP cost: 3 levels"
        ), lines, "Unbounded max must render as ≥ floor — a -1 sentinel must never leak as text");
    }

    @Test
    void requirementLines_boundedWindow_rendersRange() {
        StatRequirements reqs = new StatRequirements(22.5F, 25F, 35F);
        StatRequirements max = new StatRequirements(-1F, 50F, -1F);
        List<String> lines = RecipeInfoFormatter.requirementLines(reqs, max, 5);

        assertEquals(List.of(
                "Eterna: ≥ 22.5",
                "Quanta: 25 – 50",
                "Arcana: ≥ 35",
                "XP cost: 5 levels"
        ), lines, "Bounded axis must render as 'min – max', fractional floats must keep one decimal");
    }

    @Test
    void requirementLines_zeroedAxes_suppressed() {
        StatRequirements reqs = new StatRequirements(50F, 0F, 100F);
        StatRequirements max = new StatRequirements(50F, -1F, 100F);
        List<String> lines = RecipeInfoFormatter.requirementLines(reqs, max, 7);

        assertTrue(lines.contains("Eterna: 50"));
        assertTrue(lines.contains("Arcana: 100"));
        assertTrue(lines.contains("XP cost: 7 levels"));
        assertTrue(lines.stream().noneMatch(l -> l.startsWith("Quanta")),
                "A zero-floor axis with no cap must not produce a Quanta line — the recipe viewer"
                        + " panel should only call attention to axes the recipe actually gates on.");
    }

    @Test
    void requirementLines_pinnedAxis_rendersSingleValue() {
        StatRequirements reqs = new StatRequirements(50F, 45F, 100F);
        StatRequirements max = new StatRequirements(50F, 50F, 100F);
        List<String> lines = RecipeInfoFormatter.requirementLines(reqs, max, 7);

        assertTrue(lines.contains("Eterna: 50"),
                "Eterna floor == Eterna cap (ender_library) must collapse to a single value for readability");
        assertTrue(lines.contains("Arcana: 100"));
    }

    @Test
    void shelfStatLines_hellshelf_listsNonZeroAxesOnly() {
        EnchantingStats hellshelf = new EnchantingStats(22.5F, 1.5F, 3F, 0F, 0F, 0);
        List<String> lines = RecipeInfoFormatter.shelfStatLines(hellshelf);

        assertEquals(List.of(
                "Eterna: +1.5 (Max 22.5)",
                "Quanta: +3%"
        ), lines, "Zero axes must be skipped; positive eterna combines with max in Zenith format");
    }

    @Test
    void shelfStatLines_utilityShelf_showsNegativeAndIntegerStats() {
        EnchantingStats stoneshelf = new EnchantingStats(0F, -1.5F, 0F, -7.5F, 0F, 0);
        List<String> lines = RecipeInfoFormatter.shelfStatLines(stoneshelf);

        assertEquals(List.of(
                "Eterna: -1.5",
                "Arcana: -7.5%"
        ), lines, "Negative contributions must flow through verbatim; max-eterna 0 must stay hidden");
    }

    @Test
    void shelfStatLines_sightshelf_renders_clues_as_integer() {
        EnchantingStats sightshelf = new EnchantingStats(0F, 0F, 0F, 0F, 0F, 1);

        assertEquals(List.of("Clues: +1"), RecipeInfoFormatter.shelfStatLines(sightshelf),
                "Clues is the only integer-typed stat — must render without a decimal point");
    }

    @Test
    void shelfStatLines_zeroStats_rendersSentinel() {
        assertEquals(List.of("No stat contribution"),
                RecipeInfoFormatter.shelfStatLines(EnchantingStats.ZERO),
                "An empty info panel would look broken — surface the zero case explicitly");
    }
}
