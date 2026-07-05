// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangedEnchantMathTest {

    private static final double EPSILON = 1.0e-9;

    // ---- Longshot ----

    @Test
    void longshotMultiplier_isOneAtPointBlank() {
        assertEquals(1.0, RangedEnchantMath.longshotMultiplier(3, 0.0), EPSILON);
    }

    @Test
    void longshotMultiplier_isOneInsideGraceDistance() {
        assertEquals(1.0, RangedEnchantMath.longshotMultiplier(3,
                RangedEnchantMath.LONGSHOT_GRACE_DISTANCE), EPSILON);
        assertEquals(1.0, RangedEnchantMath.longshotMultiplier(3,
                RangedEnchantMath.LONGSHOT_GRACE_DISTANCE - 1.0), EPSILON);
    }

    @Test
    void longshotMultiplier_rampsLinearlyBetweenGraceAndMax() {
        double midpoint = (RangedEnchantMath.LONGSHOT_GRACE_DISTANCE
                + RangedEnchantMath.LONGSHOT_MAX_DISTANCE) / 2.0;
        assertEquals(1.0 + RangedEnchantMath.LONGSHOT_BONUS_PER_LEVEL * 2 * 0.5,
                RangedEnchantMath.longshotMultiplier(2, midpoint), EPSILON);
    }

    @Test
    void longshotMultiplier_capsAtMaxDistance() {
        double atMax = RangedEnchantMath.longshotMultiplier(3, RangedEnchantMath.LONGSHOT_MAX_DISTANCE);
        assertEquals(1.0 + RangedEnchantMath.LONGSHOT_BONUS_PER_LEVEL * 3, atMax, EPSILON);
        assertEquals(atMax, RangedEnchantMath.longshotMultiplier(3, 500.0), EPSILON);
    }

    @Test
    void longshotMultiplier_scalesWithLevel() {
        double distance = RangedEnchantMath.LONGSHOT_MAX_DISTANCE;
        assertTrue(RangedEnchantMath.longshotMultiplier(2, distance)
                > RangedEnchantMath.longshotMultiplier(1, distance));
    }

    @Test
    void longshotMultiplier_isOneAtLevelZero() {
        assertEquals(1.0, RangedEnchantMath.longshotMultiplier(0, 100.0), EPSILON);
    }

    // ---- Seeker ----

    @Test
    void seekerTurnRadians_isZeroAtLevelZero() {
        assertEquals(0.0, RangedEnchantMath.seekerTurnRadians(0), EPSILON);
    }

    @Test
    void seekerTurnRadians_matchesTunedDegrees() {
        assertEquals(Math.toRadians(RangedEnchantMath.SEEKER_TURN_DEGREES_BASE
                        + RangedEnchantMath.SEEKER_TURN_DEGREES_PER_LEVEL),
                RangedEnchantMath.seekerTurnRadians(1), EPSILON);
        assertEquals(Math.toRadians(RangedEnchantMath.SEEKER_TURN_DEGREES_BASE
                        + RangedEnchantMath.SEEKER_TURN_DEGREES_PER_LEVEL * 2),
                RangedEnchantMath.seekerTurnRadians(2), EPSILON);
    }

    @Test
    void seekerTurnRadians_staysWeak() {
        // The issue calls for a "weak curve angle" — a bolt must never whip around.
        assertTrue(RangedEnchantMath.seekerTurnRadians(2) < Math.toRadians(10.0));
    }

    // ---- Harpoon ----

    @Test
    void harpoonPullSpeed_isZeroAtLevelZero() {
        assertEquals(0.0, RangedEnchantMath.harpoonPullSpeed(0, 10.0), EPSILON);
    }

    @Test
    void harpoonPullSpeed_scalesWithLevel() {
        assertEquals(RangedEnchantMath.HARPOON_PULL_BASE + RangedEnchantMath.HARPOON_PULL_PER_LEVEL,
                RangedEnchantMath.harpoonPullSpeed(1, 100.0), EPSILON);
        assertEquals(RangedEnchantMath.HARPOON_PULL_BASE + RangedEnchantMath.HARPOON_PULL_PER_LEVEL * 2,
                RangedEnchantMath.harpoonPullSpeed(2, 100.0), EPSILON);
    }

    @Test
    void harpoonPullSpeed_dampedAtCloseRange() {
        // At 2 blocks the cap is distance * factor, well below full strength —
        // a point-blank victim is nudged, never launched past the thrower.
        assertEquals(2.0 * RangedEnchantMath.HARPOON_CLOSE_RANGE_FACTOR,
                RangedEnchantMath.harpoonPullSpeed(2, 2.0), EPSILON);
    }

    @Test
    void harpoonPullSpeed_isZeroAtZeroDistance() {
        assertEquals(0.0, RangedEnchantMath.harpoonPullSpeed(2, 0.0), EPSILON);
    }
}
