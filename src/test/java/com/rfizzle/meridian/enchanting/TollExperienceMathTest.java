// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the Curse of Toll XP-tax math — the inverse of Insight/Animus. Without the curse the
 * wearer keeps the full orb value; with it, a flat per-level fraction is skimmed off whatever
 * experience the player collects, floored so it never goes negative.
 */
class TollExperienceMathTest {

    @Test
    void unwornKeepsFullExperience() {
        assertEquals(40, TollExperienceMath.reduce(40, 0), "level 0 must leave the value unchanged");
        assertEquals(40, TollExperienceMath.reduce(40, -1), "a non-positive level must leave it unchanged");
    }

    @Test
    void oneLevelRemovesThePerLevelFraction() {
        // 15% of 100 removed → 85 kept.
        assertEquals(85, TollExperienceMath.reduce(100, 1));
    }

    @Test
    void twoLevelsRemoveTwiceTheFraction() {
        // 30% of 100 removed → 70 kept.
        assertEquals(70, TollExperienceMath.reduce(100, 2));
    }

    @Test
    void reductionIsMonotonicInLevel() {
        int base = 200;
        int keep0 = TollExperienceMath.reduce(base, 0);
        int keep1 = TollExperienceMath.reduce(base, 1);
        int keep2 = TollExperienceMath.reduce(base, 2);
        assertTrue(keep1 < keep0, "level 1 must keep less than unworn");
        assertTrue(keep2 < keep1, "each further level must keep less");
    }

    @Test
    void zeroValueOrbStaysZero() {
        assertEquals(0, TollExperienceMath.reduce(0, 2), "no experience to tax stays zero");
    }

    @Test
    void resultNeverGoesNegative() {
        // Even if the per-level fraction summed past 1.0, the floor holds at 0.
        assertTrue(TollExperienceMath.reduce(3, 100) >= 0, "reduction must never go negative");
    }
}
