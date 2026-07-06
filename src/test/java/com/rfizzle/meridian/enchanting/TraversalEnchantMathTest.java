// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraversalEnchantMathTest {

    private static final double EPSILON = 1.0e-9;

    // ---- Grapnel: range ----

    @Test
    void grapnelMaxRange_isZeroAtLevelZero() {
        assertEquals(0.0, TraversalEnchantMath.grapnelMaxRange(0), EPSILON);
    }

    @Test
    void grapnelMaxRange_growsPerLevel() {
        assertEquals(TraversalEnchantMath.GRAPNEL_RANGE_BASE + TraversalEnchantMath.GRAPNEL_RANGE_PER_LEVEL,
                TraversalEnchantMath.grapnelMaxRange(1), EPSILON);
        assertTrue(TraversalEnchantMath.grapnelMaxRange(2) > TraversalEnchantMath.grapnelMaxRange(1));
    }

    // ---- Grapnel: pull speed ----

    @Test
    void grapnelPullSpeed_isZeroAtLevelZero() {
        assertEquals(0.0, TraversalEnchantMath.grapnelPullSpeed(0, 10.0), EPSILON);
    }

    @Test
    void grapnelPullSpeed_isZeroAtZeroDistance() {
        assertEquals(0.0, TraversalEnchantMath.grapnelPullSpeed(2, 0.0), EPSILON);
    }

    @Test
    void grapnelPullSpeed_scalesWithLevelAtLongRange() {
        // Far enough that the close-range cap doesn't bite, so full strength shows through.
        assertEquals(TraversalEnchantMath.GRAPNEL_PULL_BASE + TraversalEnchantMath.GRAPNEL_PULL_PER_LEVEL,
                TraversalEnchantMath.grapnelPullSpeed(1, 100.0), EPSILON);
        assertEquals(TraversalEnchantMath.GRAPNEL_PULL_BASE + TraversalEnchantMath.GRAPNEL_PULL_PER_LEVEL * 2,
                TraversalEnchantMath.grapnelPullSpeed(2, 100.0), EPSILON);
    }

    @Test
    void grapnelPullSpeed_dampedAtCloseRange() {
        // One block away, the cap is distance * factor — a gentle nudge into the wall, not a slam.
        assertEquals(1.0 * TraversalEnchantMath.GRAPNEL_CLOSE_RANGE_FACTOR,
                TraversalEnchantMath.grapnelPullSpeed(2, 1.0), EPSILON);
    }

    // ---- Thermal: lift ----

    @Test
    void thermalLiftPerTick_isZeroAtLevelZero() {
        assertEquals(0.0, TraversalEnchantMath.thermalLiftPerTick(0), EPSILON);
    }

    @Test
    void thermalLiftPerTick_scalesWithLevel() {
        assertTrue(TraversalEnchantMath.thermalLiftPerTick(2) > TraversalEnchantMath.thermalLiftPerTick(1));
    }

    @Test
    void thermalMaxClimb_isZeroAtLevelZero() {
        assertEquals(0.0, TraversalEnchantMath.thermalMaxClimb(0), EPSILON);
    }

    @Test
    void thermalMaxClimb_scalesWithLevel() {
        assertTrue(TraversalEnchantMath.thermalMaxClimb(2) > TraversalEnchantMath.thermalMaxClimb(1));
    }

    @Test
    void thermalLift_neverExceedsTerminalClimb() {
        // A single tick's lift must be a fraction of the terminal cap, or the "boost, not
        // sustain" guarantee collapses into an instant launch.
        for (int level = 1; level <= 2; level++) {
            assertTrue(TraversalEnchantMath.thermalLiftPerTick(level) < TraversalEnchantMath.thermalMaxClimb(level),
                    "lift per tick must stay below the terminal climb at level " + level);
        }
    }

    // ---- Thermal: scan depth ----

    @Test
    void thermalScanDepth_isZeroAtLevelZero() {
        assertEquals(0, TraversalEnchantMath.thermalScanDepth(0));
    }

    @Test
    void thermalScanDepth_growsPerLevel() {
        assertEquals(TraversalEnchantMath.THERMAL_SCAN_DEPTH_BASE + TraversalEnchantMath.THERMAL_SCAN_DEPTH_PER_LEVEL,
                TraversalEnchantMath.thermalScanDepth(1));
        assertTrue(TraversalEnchantMath.thermalScanDepth(2) > TraversalEnchantMath.thermalScanDepth(1));
    }
}
