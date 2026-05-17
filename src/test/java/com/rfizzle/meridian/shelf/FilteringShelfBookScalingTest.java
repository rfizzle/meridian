package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.enchanting.EnchantingStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class FilteringShelfBookScalingTest {

    private static final EnchantingStats BASE =
            new EnchantingStats(15F, 1F, 0F, 0F, 0F, 0);

    @Test
    void zeroBooks_returnsBaseUnchanged() {
        assertSame(BASE, FilteringShelfBlock.applyBookScaling(BASE, 0));
    }

    @Test
    void negativeBooks_returnsBaseUnchanged() {
        assertSame(BASE, FilteringShelfBlock.applyBookScaling(BASE, -1));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void eterna_scalesPointFivePerBook(int books) {
        EnchantingStats scaled = FilteringShelfBlock.applyBookScaling(BASE, books);
        assertEquals(1F + books * 0.5F, scaled.eterna(), 1e-6);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void arcana_scalesOnePerBook(int books) {
        EnchantingStats scaled = FilteringShelfBlock.applyBookScaling(BASE, books);
        assertEquals(books * 1.0F, scaled.arcana(), 1e-6);
    }

    @Test
    void fullShelf_expectedValues() {
        EnchantingStats scaled = FilteringShelfBlock.applyBookScaling(BASE, 6);
        assertEquals(15F, scaled.maxEterna(), 1e-6);
        assertEquals(4F, scaled.eterna(), 1e-6);
        assertEquals(0F, scaled.quanta(), 1e-6);
        assertEquals(6F, scaled.arcana(), 1e-6);
        assertEquals(0F, scaled.rectification(), 1e-6);
        assertEquals(0, scaled.clues());
    }

    @Test
    void scalingPreservesOtherStats() {
        EnchantingStats rich = new EnchantingStats(30F, 2F, 5F, 3F, 10F, 2);
        EnchantingStats scaled = FilteringShelfBlock.applyBookScaling(rich, 4);
        assertEquals(30F, scaled.maxEterna(), 1e-6);
        assertEquals(4F, scaled.eterna(), 1e-6);
        assertEquals(5F, scaled.quanta(), 1e-6);
        assertEquals(7F, scaled.arcana(), 1e-6);
        assertEquals(10F, scaled.rectification(), 1e-6);
        assertEquals(2, scaled.clues());
    }
}
