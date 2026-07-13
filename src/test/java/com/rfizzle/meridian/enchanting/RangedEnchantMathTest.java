// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangedEnchantMathTest {

    private static final double EPSILON = 1.0e-9;

    // ---- Curse of Wavering ----

    @Test
    void waveringInaccuracy_addsPerLevelOnTopOfBase() {
        float base = 1.0f;
        assertEquals(base + RangedEnchantMath.WAVERING_INACCURACY_PER_LEVEL,
                RangedEnchantMath.waveringInaccuracy(1, base), EPSILON);
        assertEquals(base + RangedEnchantMath.WAVERING_INACCURACY_PER_LEVEL * 2,
                RangedEnchantMath.waveringInaccuracy(2, base), EPSILON);
    }

    @Test
    void waveringInaccuracy_leavesCleanWeaponUnchanged() {
        assertEquals(1.0f, RangedEnchantMath.waveringInaccuracy(0, 1.0f), EPSILON);
    }

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

    // ---- Undertow ----

    @Test
    void undertowRadius_isZeroAtLevelZero() {
        assertEquals(0.0, RangedEnchantMath.undertowRadius(0), EPSILON);
    }

    @Test
    void undertowRadius_growsWithLevel() {
        assertEquals(RangedEnchantMath.UNDERTOW_RADIUS_BASE + RangedEnchantMath.UNDERTOW_RADIUS_PER_LEVEL,
                RangedEnchantMath.undertowRadius(1), EPSILON);
        assertTrue(RangedEnchantMath.undertowRadius(2) > RangedEnchantMath.undertowRadius(1),
                "radius must grow with level");
    }

    @Test
    void undertowPullSpeed_isZeroAtLevelZero() {
        assertEquals(0.0, RangedEnchantMath.undertowPullSpeed(0, 10.0), EPSILON);
    }

    @Test
    void undertowPullSpeed_scalesWithLevel() {
        assertEquals(RangedEnchantMath.UNDERTOW_PULL_BASE + RangedEnchantMath.UNDERTOW_PULL_PER_LEVEL,
                RangedEnchantMath.undertowPullSpeed(1, 100.0), EPSILON);
        assertEquals(RangedEnchantMath.UNDERTOW_PULL_BASE + RangedEnchantMath.UNDERTOW_PULL_PER_LEVEL * 2,
                RangedEnchantMath.undertowPullSpeed(2, 100.0), EPSILON);
    }

    @Test
    void undertowPullSpeed_dampedAtCloseRange() {
        // On top of the impact point the cap is distance * factor, so a creature there is
        // nudged, not flung across the point.
        assertEquals(2.0 * RangedEnchantMath.UNDERTOW_CLOSE_RANGE_FACTOR,
                RangedEnchantMath.undertowPullSpeed(2, 2.0), EPSILON);
    }

    @Test
    void undertowPullSpeed_isZeroAtZeroDistance() {
        assertEquals(0.0, RangedEnchantMath.undertowPullSpeed(2, 0.0), EPSILON);
    }

    // ---- Volley ----

    @Test
    void volleyArrowCount_isThreeAtOneAndFiveAtTwo() {
        assertEquals(3, RangedEnchantMath.volleyArrowCount(1));
        assertEquals(5, RangedEnchantMath.volleyArrowCount(2));
    }

    @Test
    void volleyArrowCount_isZeroWithoutEnchant() {
        assertEquals(0, RangedEnchantMath.volleyArrowCount(0));
        assertEquals(0, RangedEnchantMath.volleyArrowCount(-1));
    }

    @Test
    void volleyExtraCount_subtractsThePrimaryShots() {
        // A bow fires one primary arrow, so Volley adds two at I and four at II.
        assertEquals(2, RangedEnchantMath.volleyExtraCount(1, 1));
        assertEquals(4, RangedEnchantMath.volleyExtraCount(2, 1));
    }

    @Test
    void volleyExtraCount_neverGoesNegative() {
        // A larger vanilla volley than Volley's own count is left untouched, not trimmed.
        assertEquals(0, RangedEnchantMath.volleyExtraCount(1, 5));
        assertEquals(0, RangedEnchantMath.volleyExtraCount(0, 1));
    }

    @Test
    void volleyArrowYawOffset_fansSymmetricallyOutward() {
        float step = RangedEnchantMath.VOLLEY_SPREAD_DEGREES;
        assertEquals(step, RangedEnchantMath.volleyArrowYawOffset(0), 1.0e-6);
        assertEquals(-step, RangedEnchantMath.volleyArrowYawOffset(1), 1.0e-6);
        assertEquals(2 * step, RangedEnchantMath.volleyArrowYawOffset(2), 1.0e-6);
        assertEquals(-2 * step, RangedEnchantMath.volleyArrowYawOffset(3), 1.0e-6);
    }

    @Test
    void volleyDamageMultiplier_reducesPerArrowDamage() {
        assertTrue(RangedEnchantMath.VOLLEY_DAMAGE_MULTIPLIER > 0.0f
                && RangedEnchantMath.VOLLEY_DAMAGE_MULTIPLIER < 1.0f);
    }

    // ---- Pin ----

    @Test
    void pinRootTicks_scalesPerLevelAboveBase() {
        assertEquals(RangedEnchantMath.PIN_ROOT_TICKS_BASE + RangedEnchantMath.PIN_ROOT_TICKS_PER_LEVEL,
                RangedEnchantMath.pinRootTicks(1));
        assertEquals(RangedEnchantMath.PIN_ROOT_TICKS_BASE + RangedEnchantMath.PIN_ROOT_TICKS_PER_LEVEL * 2,
                RangedEnchantMath.pinRootTicks(2));
    }

    @Test
    void pinRootTicks_isZeroWithoutEnchant() {
        assertEquals(0, RangedEnchantMath.pinRootTicks(0));
        assertEquals(0, RangedEnchantMath.pinRootTicks(-1));
    }

    // ---- Skyfall ----

    @Test
    void skyfallMultiplier_addsBonusPerLevel() {
        assertEquals(1.0 + RangedEnchantMath.SKYFALL_BONUS_PER_LEVEL,
                RangedEnchantMath.skyfallMultiplier(1), EPSILON);
        assertEquals(1.0 + RangedEnchantMath.SKYFALL_BONUS_PER_LEVEL * 2,
                RangedEnchantMath.skyfallMultiplier(2), EPSILON);
        assertEquals(1.0 + RangedEnchantMath.SKYFALL_BONUS_PER_LEVEL * 3,
                RangedEnchantMath.skyfallMultiplier(3), EPSILON);
    }

    @Test
    void skyfallMultiplier_isOneWithoutEnchant() {
        assertEquals(1.0, RangedEnchantMath.skyfallMultiplier(0), EPSILON);
        assertEquals(1.0, RangedEnchantMath.skyfallMultiplier(-1), EPSILON);
    }
}
