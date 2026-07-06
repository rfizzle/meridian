package com.rfizzle.meridian.library;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tier-1 boundary coverage for {@link EnchantmentLibraryBlockEntity#affordable} — the stateless
 * predicate the library screen uses to decide between the success chime and the deny sound on a
 * row click. Mirrors the instance {@link EnchantmentLibraryBlockEntity#canExtract} gate reduced to
 * primitives, so the two must agree on: cost fully covered by the pool, the per-enchant/tier level
 * cap, and the "target must sit above the current level" rule encoded as a positive cost.
 */
class EnchantmentLibraryAffordabilityTest {

    @ParameterizedTest(name = "affordable(target={0}, maxLvl={1}, cost={2}, points={3}) → true")
    @CsvSource({
            // Cost exactly equals the pooled points — affordable at the boundary.
            "1, 5, 1, 1",
            // Pool comfortably covers the cost.
            "3, 5, 4, 16",
            // Target equals the per-enchant max — the cap is inclusive.
            "5, 5, 16, 32",
    })
    void affordable_whenCovered(int target, int maxLvl, int cost, int points) {
        assertTrue(EnchantmentLibraryBlockEntity.affordable(target, maxLvl, cost, points));
    }

    @ParameterizedTest(name = "affordable(target={0}, maxLvl={1}, cost={2}, points={3}) → false")
    @CsvSource({
            // Cost one point over the pool — cannot afford.
            "3, 5, 5, 4",
            // Target above the per-enchant max — level-gated even with points to spare.
            "6, 5, 32, 4096",
            // Non-positive cost (target at or below current level) is never a valid extraction.
            "0, 5, 0, 100",
            "2, 5, -1, 100",
            // Empty pool cannot cover any positive cost.
            "1, 5, 1, 0",
    })
    void notAffordable_whenGatedOrShort(int target, int maxLvl, int cost, int points) {
        assertFalse(EnchantmentLibraryBlockEntity.affordable(target, maxLvl, cost, points));
    }
}
