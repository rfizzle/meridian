package com.rfizzle.meridian.library;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T-4.3.3 — parameterized coverage for the two static point-math helpers on
 * {@link EnchantmentLibraryBlockEntity}. Kept out of the BE-surface test so the deposit/extract
 * suite doesn't need to bootstrap vanilla registries just to assert bit-shift arithmetic.
 *
 * <p>The {@code maxLevelAffordable} cases walk DESIGN's shift-click formula
 * {@code 1 + log₂(points + points(curLvl))}, including the edge where a starved pool at a high
 * current level still "affords" the level already held and the Ender-tier saturation that would
 * overflow {@code int} without the long-widen guard.
 */
class PointMathTest {

    @ParameterizedTest(name = "points({0}) = {1}")
    @CsvSource({
            "0, 0",
            "-1, 0",
            "-100, 0",
            "1, 1",
            "2, 2",
            "3, 4",
            "5, 16",
            "10, 512",
            "16, 32768",
            "31, 1073741824"
    })
    void points_matchesDesignFormula(int level, int expected) {
        assertEquals(expected, EnchantmentLibraryBlockEntity.points(level));
    }

    @ParameterizedTest(name = "maxLevelAffordable(pool={0}, curLvl={1}) = {2}")
    @CsvSource({
            // Empty pool at level 0 → cannot afford any extraction.
            "0, 0, 0",
            // 1 point + points(0)=0 → log2(1)=0 → level 1 (cheapest fresh pull).
            "1, 0, 1",
            // Exactly the cost of level 2 (2 points) → result 2.
            "2, 0, 2",
            // 15 points falls short of level 5 (needs 16) → level 4.
            "15, 0, 4",
            // 16 points affords level 5 cleanly.
            "16, 0, 5",
            // 0 new points but holding a level-4 book: budget = points(4) = 8 → level 4 (no upgrade).
            "0, 4, 4",
            // DESIGN IV→V upgrade path: pool 8 + points(4)=8 → budget 16 → level 5.
            "8, 4, 5",
            // One point short of IV→V → stay at level 4.
            "7, 4, 4",
            // Ender saturation: points(31) = 2^30; budget stays within long → level 31.
            "0, 31, 31",
            // Long-widen guard: 2^30 + points(31) = 2^31 would wrap negative as an int.
            "1073741824, 31, 32"
    })
    void maxLevelAffordable_matchesDesignFormula(int pool, int curLvl, int expected) {
        assertEquals(expected, EnchantmentLibraryBlockEntity.maxLevelAffordable(pool, curLvl));
    }

    /**
     * DESIGN's shift-click consumer clamps {@code maxLevelAffordable} against
     * {@code maxLevels[e]}; the raw helper intentionally does not. Guarding against a future
     * refactor that tries to bake the clamp into the helper.
     */
    @ParameterizedTest(name = "helper is unclamped (pool={0}, curLvl={1})")
    @CsvSource({
            "1073741824, 31, 32",
            "2147483647, 0, 31"
    })
    void maxLevelAffordable_doesNotClampAgainstMaxLevel(int pool, int curLvl, int expected) {
        assertEquals(expected, EnchantmentLibraryBlockEntity.maxLevelAffordable(pool, curLvl),
                "helper stays raw — clamping is the menu's responsibility");
    }
}
