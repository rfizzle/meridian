package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.enchanting.StatCollection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-7.4.1 — pins the text content the Jade probe shows when aimed at an enchanting table or a
 * library BE. Pure JUnit — no Minecraft bootstrap, no Jade classes on the classpath. Keeps the
 * formatter as the single source of truth for the probe tooltip so the in-game rendering can be
 * inspected in a smoke test without second-guessing the strings.
 */
class JadeTooltipFormatterTest {

    @Test
    void enchantingTableLines_level50Table_rendersFiveStatsWithEternaHeadroom() {
        StatCollection stats = new StatCollection(50F, 5F, 3F, 25F, 2, 50F, Set.of(), false);

        assertEquals(List.of(
                "Eterna: 50 / 50",
                "Quanta: 5",
                "Arcana: 3",
                "Rectification: 25%",
                "Clues: 2"
        ), JadeTooltipFormatter.enchantingTableLines(stats),
                "Full five-axis readout; Eterna must show the running total against its ceiling so"
                        + " the player can see headroom at a glance.");
    }

    @Test
    void enchantingTableLines_partialTable_showsCurrentVsCap() {
        StatCollection stats = new StatCollection(22.5F, -1.5F, 0F, 0F, 0, 45F, Set.of(), false);

        assertEquals(List.of(
                "Eterna: 22.5 / 45",
                "Quanta: -1.5",
                "Arcana: 0",
                "Rectification: 0%",
                "Clues: 0"
        ), JadeTooltipFormatter.enchantingTableLines(stats),
                "Fractional floats keep one decimal, integers drop .0, negative quanta flows through,"
                        + " and zero axes still render so the tooltip has a stable five-line shape.");
    }

    @Test
    void enchantingTableLines_zeroCap_degradesToPlainEterna() {
        StatCollection stats = StatCollection.EMPTY;

        assertEquals(List.of(
                "Eterna: 0",
                "Quanta: 0",
                "Arcana: 0",
                "Rectification: 0%",
                "Clues: 0"
        ), JadeTooltipFormatter.enchantingTableLines(stats),
                "A table with no shelves has maxEterna=0; the Eterna line must degrade to a plain"
                        + " value rather than reading 'Eterna: 0 / 0'.");
    }

    @Test
    void enchantingTableLines_fractionalRectification_rendersOneDecimal() {
        StatCollection stats = new StatCollection(10F, 0F, 0F, 12.5F, 0, 15F, Set.of(), false);

        assertEquals("Rectification: 12.5%",
                JadeTooltipFormatter.enchantingTableLines(stats).get(3),
                "Rectification values that don't round to a whole percent must keep one decimal.");
    }

    @Test
    void libraryLine_pluralNoun_appliesToZeroAndMulti() {
        assertEquals("0 enchantments stored",
                JadeTooltipFormatter.libraryLine(0));
        assertEquals("7 enchantments stored",
                JadeTooltipFormatter.libraryLine(7));
    }

    @Test
    void libraryLine_singularNoun_forOneEnchant() {
        assertEquals("1 enchantment stored",
                JadeTooltipFormatter.libraryLine(1),
                "Singular form must match English — '1 enchantments stored' would look like a bug.");
    }

    @Test
    void libraryLine_negativeStoredClampsToZero() {
        assertEquals("0 enchantments stored",
                JadeTooltipFormatter.libraryLine(-3),
                "Defensive clamp: a miscounted BE must never leak a negative count to the tooltip.");
    }

    @Test
    void blockStatsLines_vanillaBookshelf_showsEternaWithMaxEterna() {
        EnchantingStats stats = new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);

        assertEquals(List.of("Eterna: +1 / 15"),
                JadeTooltipFormatter.blockStatsLines(stats),
                "Vanilla bookshelf: eterna contribution with maxEterna ceiling, zero stats omitted.");
    }

    @Test
    void blockStatsLines_hellshelf_showsEternaAndQuanta() {
        EnchantingStats stats = new EnchantingStats(22.5F, 1.5F, 3F, 0F, 0F, 0);

        assertEquals(List.of(
                "Eterna: +1.5 / 22.5",
                "Quanta: +3"
        ), JadeTooltipFormatter.blockStatsLines(stats));
    }

    @Test
    void blockStatsLines_stoneshelf_showsNegativeValues() {
        EnchantingStats stats = new EnchantingStats(0F, -1.5F, 0F, -7.5F, 0F, 0);

        assertEquals(List.of(
                "Eterna: -1.5",
                "Arcana: -7.5"
        ), JadeTooltipFormatter.blockStatsLines(stats),
                "Negative contributions display without + sign; maxEterna of 0 is suppressed.");
    }

    @Test
    void blockStatsLines_sightshelf_showsCluesOnly() {
        EnchantingStats stats = new EnchantingStats(0F, 0F, 0F, 0F, 0F, 1);

        assertEquals(List.of("Clues: +1"),
                JadeTooltipFormatter.blockStatsLines(stats));
    }

    @Test
    void blockStatsLines_rectifier_showsRectificationOnly() {
        EnchantingStats stats = new EnchantingStats(0F, 0F, 0F, 0F, 10F, 0);

        assertEquals(List.of("Rectification: +10"),
                JadeTooltipFormatter.blockStatsLines(stats));
    }

    @Test
    void blockStatsLines_allZero_returnsEmptyList() {
        assertTrue(JadeTooltipFormatter.blockStatsLines(EnchantingStats.ZERO).isEmpty(),
                "A block with no stat contributions should produce no tooltip lines.");
    }

    @Test
    void blockStatsLines_heartSeashelf_showsNegativeRectification() {
        EnchantingStats stats = new EnchantingStats(30F, 3F, 0F, 10F, -5F, 0);

        assertEquals(List.of(
                "Eterna: +3 / 30",
                "Arcana: +10",
                "Rectification: -5"
        ), JadeTooltipFormatter.blockStatsLines(stats));
    }
}
