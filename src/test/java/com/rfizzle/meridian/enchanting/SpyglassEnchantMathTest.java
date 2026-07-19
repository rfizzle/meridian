// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link SpyglassEnchantMath}'s Tracker's Lens curve — the pure core behind the handler's
 * thin shell.
 */
class SpyglassEnchantMathTest {

    @Test
    void trackersLensGlowTicks_scalesPerLevel() {
        assertEquals(120, SpyglassEnchantMath.trackersLensGlowTicks(1), "level I glows for 6 seconds");
        assertEquals(240, SpyglassEnchantMath.trackersLensGlowTicks(2), "level II glows for 12 seconds");
        assertEquals(400, SpyglassEnchantMath.trackersLensGlowTicks(3), "level III glows for 20 seconds");
        assertEquals(600, SpyglassEnchantMath.trackersLensGlowTicks(4), "level IV glows for 30 seconds");
    }

    @Test
    void trackersLensGlowTicks_isZeroWithoutTheEnchant() {
        assertEquals(0, SpyglassEnchantMath.trackersLensGlowTicks(0), "no enchant means no glow");
        assertEquals(0, SpyglassEnchantMath.trackersLensGlowTicks(-3), "a negative level means no glow");
    }

    @Test
    void trackersLensGlowTicks_clampsAboveMaxLevel() {
        int max = SpyglassEnchantMath.trackersLensGlowTicks(SpyglassEnchantMath.MAX_TRACKERS_LENS_LEVEL);
        assertEquals(max, SpyglassEnchantMath.trackersLensGlowTicks(5),
                "a level above the roster max clamps to the top of the curve rather than throwing");
        assertEquals(max, SpyglassEnchantMath.trackersLensGlowTicks(Integer.MAX_VALUE),
                "an absurd level from a command or third-party tome still clamps");
    }

    @Test
    void trackersLensGlowTicks_increasesStrictlyWithLevel() {
        for (int level = 2; level <= SpyglassEnchantMath.MAX_TRACKERS_LENS_LEVEL; level++) {
            assertTrue(SpyglassEnchantMath.trackersLensGlowTicks(level)
                            > SpyglassEnchantMath.trackersLensGlowTicks(level - 1),
                    "every level must buy a longer glow than the one below it, at level " + level);
        }
    }

    @Test
    void trackersLensLevelOne_matchesMarksGlow() {
        assertEquals(RangedEnchantMath.MARK_GLOW_TICKS, SpyglassEnchantMath.trackersLensGlowTicks(1),
                "level I is deliberately Mark's glow — 'Mark without the arrow' must read literally");
    }

    @Test
    void sightingAndRange_areSane() {
        assertEquals(30, SpyglassEnchantMath.TRACKERS_LENS_SIGHTING_TICKS,
                "the sighting is a second and a half at every level");
        assertEquals(RangedEnchantMath.SEEKER_LOCK_RANGE, SpyglassEnchantMath.TRACKERS_LENS_RANGE,
                "the lens reaches as far as Seeker locks — the shared ceiling is entity tracking, "
                        + "not the enchantment");
    }
}
