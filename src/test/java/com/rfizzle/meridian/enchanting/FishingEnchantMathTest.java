package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingEnchantMathTest {

    private static final double EPS = 1e-9;

    @Test
    void twinHookChance_zeroWithoutLevels() {
        assertEquals(0.0, FishingEnchantMath.twinHookChance(0), EPS);
        assertEquals(0.0, FishingEnchantMath.twinHookChance(-1), EPS);
    }

    @Test
    void twinHookChance_scalesPerLevel() {
        assertEquals(FishingEnchantMath.TWIN_HOOK_CHANCE_PER_LEVEL,
                FishingEnchantMath.twinHookChance(1), EPS);
        assertEquals(2 * FishingEnchantMath.TWIN_HOOK_CHANCE_PER_LEVEL,
                FishingEnchantMath.twinHookChance(2), EPS);
    }

    @Test
    void twinHookChance_clampsAtOne() {
        // Absurd level can't exceed a certainty.
        assertEquals(1.0, FishingEnchantMath.twinHookChance(100), EPS);
    }

    @Test
    void twinHookChance_isMonotonic() {
        double prev = -1.0;
        for (int level = 0; level <= 10; level++) {
            double c = FishingEnchantMath.twinHookChance(level);
            assertTrue(c >= prev, "chance not monotonic at level " + level);
            assertTrue(c >= 0.0 && c <= 1.0, "chance out of range at level " + level);
            prev = c;
        }
    }

    @Test
    void shouldDuplicate_firesBelowThreshold() {
        // A roll strictly below the chance duplicates; at or above it does not.
        double chance = FishingEnchantMath.twinHookChance(2);
        assertTrue(FishingEnchantMath.shouldDuplicate(2, chance - 1e-6));
        assertFalse(FishingEnchantMath.shouldDuplicate(2, chance));
        assertFalse(FishingEnchantMath.shouldDuplicate(2, chance + 1e-6));
    }

    @Test
    void shouldDuplicate_neverFiresWithoutLevels() {
        // Even a roll of exactly 0 must not duplicate when the chance is 0.
        assertFalse(FishingEnchantMath.shouldDuplicate(0, 0.0));
    }
}
