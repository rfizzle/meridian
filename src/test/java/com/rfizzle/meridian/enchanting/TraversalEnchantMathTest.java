// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.UUID;

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

    // ---- Tailwind: firework boost lifetime + push ----

    @Test
    void tailwindLifetimeBonus_isZeroAtLevelZero() {
        assertEquals(0, TraversalEnchantMath.tailwindLifetimeBonus(0));
    }

    @Test
    void tailwindLifetimeBonus_scalesPerLevel() {
        assertEquals(10, TraversalEnchantMath.tailwindLifetimeBonus(1));
        assertEquals(20, TraversalEnchantMath.tailwindLifetimeBonus(2));
        assertEquals(30, TraversalEnchantMath.tailwindLifetimeBonus(3));
    }

    @Test
    void tailwindPush_isZeroAtLevelZero() {
        assertEquals(0.0, TraversalEnchantMath.tailwindPush(0), EPSILON);
    }

    @Test
    void tailwindPush_scalesPerLevel() {
        assertEquals(0.05, TraversalEnchantMath.tailwindPush(1), EPSILON);
        assertEquals(0.15, TraversalEnchantMath.tailwindPush(3), EPSILON);
        assertTrue(TraversalEnchantMath.tailwindPush(3) > TraversalEnchantMath.tailwindPush(1));
    }

    // ---- Curse of Molting: the fizzle verdict derived from the boost rocket's UUID ----

    @Test
    void moltingFizzles_isDeterministicForTheSameUuid() {
        // The property client prediction rests on: the same rocket always yields the same verdict,
        // so the client and the server reach it independently without ever disagreeing.
        UUID id = UUID.fromString("6f3a1c9e-2b7d-4e58-9a01-c4d5e6f70819");
        boolean first = TraversalEnchantMath.moltingFizzles(
                id.getMostSignificantBits(), id.getLeastSignificantBits());
        for (int i = 0; i < 100; i++) {
            assertEquals(first, TraversalEnchantMath.moltingFizzles(
                    id.getMostSignificantBits(), id.getLeastSignificantBits()));
        }
    }

    @Test
    void moltingFizzles_dependsOnBothUuidHalves() {
        // Neither half may be ignored, or whole classes of UUIDs would share one verdict.
        boolean lowHalfMatters = false;
        boolean highHalfMatters = false;
        for (long i = 1; i <= 200 && !(lowHalfMatters && highHalfMatters); i++) {
            if (TraversalEnchantMath.moltingFizzles(0L, i) != TraversalEnchantMath.moltingFizzles(0L, 0L)) {
                lowHalfMatters = true;
            }
            if (TraversalEnchantMath.moltingFizzles(i, 0L) != TraversalEnchantMath.moltingFizzles(0L, 0L)) {
                highHalfMatters = true;
            }
        }
        assertTrue(lowHalfMatters, "the least significant bits must affect the verdict");
        assertTrue(highHalfMatters, "the most significant bits must affect the verdict");
    }

    @Test
    void moltingFizzles_matchesFizzleChanceOverManyUuids() {
        // Deriving rather than drawing must not skew the rate players actually see.
        Random uuidSource = new Random(20231231L);
        int trials = 100_000;
        int fizzled = 0;
        for (int i = 0; i < trials; i++) {
            if (TraversalEnchantMath.moltingFizzles(uuidSource.nextLong(), uuidSource.nextLong())) {
                fizzled++;
            }
        }
        double rate = (double) fizzled / trials;
        // ±1% around 25%; the sampling error at 100k trials is ~0.14%, so this is not flaky.
        assertTrue(Math.abs(rate - TraversalEnchantMath.MOLTING_FIZZLE_CHANCE) < 0.01,
                "expected ~" + TraversalEnchantMath.MOLTING_FIZZLE_CHANCE + " fizzle rate, got " + rate);
    }

    // ---- Thrift: chance a firework boost leaves the rocket unspent ----

    @Test
    void thriftRefundChance_isZeroAtLevelZero() {
        assertEquals(0.0f, TraversalEnchantMath.thriftRefundChance(0), EPSILON);
    }

    @Test
    void thriftRefundChance_scalesPerLevel() {
        assertEquals(0.25f, TraversalEnchantMath.thriftRefundChance(1), EPSILON);
        assertEquals(0.50f, TraversalEnchantMath.thriftRefundChance(2), EPSILON);
    }

    @Test
    void thriftRefundChance_isClampedToOne() {
        // A level beyond the enchantment's max must never yield a >100% chance.
        assertEquals(1.0f, TraversalEnchantMath.thriftRefundChance(10), EPSILON);
    }

    // ---- Falconstrike: kinetic glide-strike ----

    @Test
    void falconstrikeKineticDamage_isZeroAtLevelZero() {
        assertEquals(0.0f, TraversalEnchantMath.falconstrikeKineticDamage(0, 2.0), EPSILON);
    }

    @Test
    void falconstrikeKineticDamage_isZeroBelowDriftThreshold() {
        double slow = TraversalEnchantMath.FALCONSTRIKE_MIN_SPEED - 0.01;
        assertEquals(0.0f, TraversalEnchantMath.falconstrikeKineticDamage(2, slow), EPSILON);
    }

    @Test
    void falconstrikeKineticDamage_scalesWithSpeedAndLevel() {
        // At the drift threshold (0.5), level 1: 3.0 * 1 * 0.5 = 1.5, below the 6.0 cap.
        assertEquals(1.5f, TraversalEnchantMath.falconstrikeKineticDamage(1,
                TraversalEnchantMath.FALCONSTRIKE_MIN_SPEED), 1.0e-6);
        assertTrue(TraversalEnchantMath.falconstrikeKineticDamage(1, 1.0)
                > TraversalEnchantMath.falconstrikeKineticDamage(1, 0.6));
        assertTrue(TraversalEnchantMath.falconstrikeKineticDamage(2, 1.0)
                > TraversalEnchantMath.falconstrikeKineticDamage(1, 1.0));
    }

    @Test
    void falconstrikeKineticDamage_isCappedPerLevel() {
        // A steep power-dive can't scale unbounded — a huge speed clamps to CAP * level.
        assertEquals(TraversalEnchantMath.FALCONSTRIKE_DAMAGE_CAP_PER_LEVEL * 1,
                TraversalEnchantMath.falconstrikeKineticDamage(1, 100.0), 1.0e-6);
        assertEquals(TraversalEnchantMath.FALCONSTRIKE_DAMAGE_CAP_PER_LEVEL * 2,
                TraversalEnchantMath.falconstrikeKineticDamage(2, 100.0), 1.0e-6);
    }

    @Test
    void falconstrikeMomentumRetention_preservesMostButNotAllSpeed() {
        assertTrue(TraversalEnchantMath.FALCONSTRIKE_MOMENTUM_RETENTION > 0.0);
        assertTrue(TraversalEnchantMath.FALCONSTRIKE_MOMENTUM_RETENTION < 1.0);
    }

    // ---- Ballast: vertical speed ----

    @Test
    void ballastVerticalSpeed_isZeroAtLevelZero() {
        assertEquals(0.0, TraversalEnchantMath.ballastVerticalSpeed(0), EPSILON);
    }

    @Test
    void ballastVerticalSpeed_growsPerLevel() {
        assertEquals(TraversalEnchantMath.BALLAST_SPEED_BASE + TraversalEnchantMath.BALLAST_SPEED_PER_LEVEL,
                TraversalEnchantMath.ballastVerticalSpeed(1), EPSILON);
        assertEquals(TraversalEnchantMath.BALLAST_SPEED_BASE + 2 * TraversalEnchantMath.BALLAST_SPEED_PER_LEVEL,
                TraversalEnchantMath.ballastVerticalSpeed(2), EPSILON);
        assertTrue(TraversalEnchantMath.ballastVerticalSpeed(2) > TraversalEnchantMath.ballastVerticalSpeed(1));
    }

    @Test
    void ballastVerticalSpeed_outpacesVanillaWaterAscent() {
        // The whole point of Ballast is a brisk sink/rise — even level 1 clears a tenth of a block a tick.
        assertTrue(TraversalEnchantMath.ballastVerticalSpeed(1) > 0.1);
    }
}
