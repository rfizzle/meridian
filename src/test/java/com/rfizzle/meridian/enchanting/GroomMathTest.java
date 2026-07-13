package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroomMathTest {

    private static final double EPS = 1.0e-9;

    @Test
    void groomChance_isZeroAtOrBelowLevelZero() {
        assertEquals(0.0, GroomMath.groomChance(0, 0.25, 0.45), EPS);
        assertEquals(0.0, GroomMath.groomChance(-1, 0.25, 0.45), EPS);
    }

    @Test
    void groomChance_usesLevelOneLeverAtLevelOne() {
        assertEquals(0.25, GroomMath.groomChance(1, 0.25, 0.45), EPS);
    }

    @Test
    void groomChance_usesLevelTwoLeverAtLevelTwoAndAbove() {
        assertEquals(0.45, GroomMath.groomChance(2, 0.25, 0.45), EPS);
        // Levels above the max still resolve to the level-two lever, never higher.
        assertEquals(0.45, GroomMath.groomChance(3, 0.25, 0.45), EPS);
    }

    @Test
    void groomChance_clampsLeversToUnitInterval() {
        assertEquals(1.0, GroomMath.groomChance(1, 1.5, 0.45), EPS);
        assertEquals(0.0, GroomMath.groomChance(2, 0.25, -0.5), EPS);
    }

    @Test
    void cooldownElapsed_neverBrushedIsAlwaysReady() {
        assertTrue(GroomMath.cooldownElapsed(GroomMath.NEVER_BRUSHED, 0L, 2400));
        assertTrue(GroomMath.cooldownElapsed(GroomMath.NEVER_BRUSHED, 5_000_000L, 2400));
    }

    @Test
    void cooldownElapsed_zeroOrNegativeCooldownIsAlwaysReady() {
        assertTrue(GroomMath.cooldownElapsed(1000L, 1000L, 0));
        assertTrue(GroomMath.cooldownElapsed(1000L, 1000L, -5));
    }

    @Test
    void cooldownElapsed_respectsTheWindow() {
        // Groomed at tick 1000, cooldown 2400: not ready until tick 3400.
        assertFalse(GroomMath.cooldownElapsed(1000L, 1000L, 2400));
        assertFalse(GroomMath.cooldownElapsed(1000L, 3399L, 2400));
        assertTrue(GroomMath.cooldownElapsed(1000L, 3400L, 2400));
        assertTrue(GroomMath.cooldownElapsed(1000L, 9999L, 2400));
    }
}
