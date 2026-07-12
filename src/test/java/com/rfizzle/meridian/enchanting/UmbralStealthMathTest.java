// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UmbralStealthMathTest {

    private static final double EPSILON = 1.0e-9;

    @Test
    void multiplier_isOneAtOrBelowLevelZero() {
        assertEquals(1.0, UmbralStealthMath.visibilityMultiplier(0), EPSILON);
        assertEquals(1.0, UmbralStealthMath.visibilityMultiplier(-1), EPSILON);
    }

    @Test
    void multiplier_removesReductionPerLevel() {
        assertEquals(1.0 - UmbralStealthMath.REDUCTION_PER_LEVEL,
                UmbralStealthMath.visibilityMultiplier(1), EPSILON);
        assertEquals(1.0 - 2 * UmbralStealthMath.REDUCTION_PER_LEVEL,
                UmbralStealthMath.visibilityMultiplier(2), EPSILON);
        assertEquals(1.0 - 3 * UmbralStealthMath.REDUCTION_PER_LEVEL,
                UmbralStealthMath.visibilityMultiplier(3), EPSILON);
    }

    @Test
    void multiplier_strictlyDecreasesUntilFloor() {
        double prev = UmbralStealthMath.visibilityMultiplier(0);
        for (int level = 1; level <= 3; level++) {
            double current = UmbralStealthMath.visibilityMultiplier(level);
            assertTrue(current < prev,
                    "level " + level + " should reduce visibility below level " + (level - 1));
            prev = current;
        }
    }

    @Test
    void multiplier_neverDropsBelowFloor() {
        // A very high level (e.g. via anvil stacking past max) is clamped, never zero or negative.
        double high = UmbralStealthMath.visibilityMultiplier(100);
        assertEquals(UmbralStealthMath.MIN_MULTIPLIER, high, EPSILON);
        assertTrue(high > 0.0, "visibility multiplier must stay positive");
    }

    // ---- isStealthed: all three conditions must hold ----

    @Test
    void stealthed_requiresWornSneakingAndDark() {
        int dark = UmbralStealthMath.MAX_LIGHT_LEVEL;
        assertTrue(UmbralStealthMath.isStealthed(1, true, dark));
        assertFalse(UmbralStealthMath.isStealthed(0, true, dark), "no enchant -> not stealthed");
        assertFalse(UmbralStealthMath.isStealthed(1, false, dark), "not sneaking -> not stealthed");
        assertFalse(UmbralStealthMath.isStealthed(1, true, dark + 1), "too bright -> not stealthed");
    }

    @Test
    void stealthed_lightThresholdIsInclusive() {
        assertTrue(UmbralStealthMath.isStealthed(1, true, UmbralStealthMath.MAX_LIGHT_LEVEL));
        assertFalse(UmbralStealthMath.isStealthed(1, true, UmbralStealthMath.MAX_LIGHT_LEVEL + 1));
    }

    // ---- stealthedVisibility: reduces only when stealthed ----

    @Test
    void stealthedVisibility_scalesBaseWhenDarkAndSneaking() {
        double base = 0.8; // vanilla's sneak factor
        int dark = UmbralStealthMath.MAX_LIGHT_LEVEL;
        assertEquals(base * UmbralStealthMath.visibilityMultiplier(3),
                UmbralStealthMath.stealthedVisibility(base, 3, true, dark), EPSILON);
        assertEquals(base * UmbralStealthMath.visibilityMultiplier(1),
                UmbralStealthMath.stealthedVisibility(base, 1, true, dark), EPSILON);
    }

    @Test
    void stealthedVisibility_unchangedWhenGatesFail() {
        double base = 0.8;
        int dark = UmbralStealthMath.MAX_LIGHT_LEVEL;
        assertEquals(base, UmbralStealthMath.stealthedVisibility(base, 0, true, dark), EPSILON);
        assertEquals(base, UmbralStealthMath.stealthedVisibility(base, 3, false, dark), EPSILON);
        assertEquals(base, UmbralStealthMath.stealthedVisibility(base, 3, true, dark + 1), EPSILON);
    }
}
