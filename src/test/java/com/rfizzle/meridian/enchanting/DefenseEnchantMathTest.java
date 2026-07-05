package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefenseEnchantMathTest {

    // --- Blink cooldown ---

    @Test
    void blinkOffCooldown_whenNeverUsed() {
        assertTrue(DefenseEnchantMath.blinkOffCooldown(DefenseEnchantMath.BLINK_NEVER_USED, 0L));
        assertTrue(DefenseEnchantMath.blinkOffCooldown(DefenseEnchantMath.BLINK_NEVER_USED, 123_456L));
    }

    @Test
    void blinkOnCooldown_immediatelyAfterUse() {
        assertFalse(DefenseEnchantMath.blinkOffCooldown(1000L, 1000L));
        assertFalse(DefenseEnchantMath.blinkOffCooldown(1000L, 1001L));
    }

    @Test
    void blinkOffCooldown_whenTimestampIsInTheFuture() {
        // Restored backup / playerdata carried into a fresh world: a future timestamp
        // must read as available, not lock Blink out until the clock catches up.
        assertTrue(DefenseEnchantMath.blinkOffCooldown(20_000_000L, 1000L));
    }

    @Test
    void blinkCooldown_boundaryIsExactlyOneGameDay() {
        long used = 5000L;
        assertFalse(DefenseEnchantMath.blinkOffCooldown(used, used + DefenseEnchantMath.BLINK_COOLDOWN_TICKS - 1));
        assertTrue(DefenseEnchantMath.blinkOffCooldown(used, used + DefenseEnchantMath.BLINK_COOLDOWN_TICKS));
    }

    @Test
    void blinkCooldown_isOneGameDayPerConfiguredDay() {
        assertEquals((long) DefenseEnchantMath.GAME_DAY_TICKS * DefenseEnchantMath.BLINK_COOLDOWN_GAME_DAYS,
                DefenseEnchantMath.BLINK_COOLDOWN_TICKS);
    }

    // --- Reprieve i-frames ---

    @Test
    void reprieve_levelZeroIsVanillaWindow() {
        assertEquals(DefenseEnchantMath.VANILLA_HURT_INVULNERABILITY_TICKS,
                DefenseEnchantMath.reprieveInvulnerabilityTicks(0));
    }

    @Test
    void reprieve_smallBumpPerLevel() {
        assertEquals(24, DefenseEnchantMath.reprieveInvulnerabilityTicks(1));
        assertEquals(28, DefenseEnchantMath.reprieveInvulnerabilityTicks(2));
    }

    @Test
    void reprieve_maxLevelStaysWellUnderDoubleWindow() {
        assertTrue(DefenseEnchantMath.reprieveInvulnerabilityTicks(2)
                < 2 * DefenseEnchantMath.VANILLA_HURT_INVULNERABILITY_TICKS,
                "i-frames are potent — level 2 must not double the vanilla window");
    }

    // --- Loft safe fall ---

    @Test
    void loftSafeFall_zeroAtLevelZero() {
        assertEquals(0.0f, DefenseEnchantMath.loftSafeFallReduction(0));
    }

    @Test
    void loftSafeFall_scalesLinearlyWithLevel() {
        assertEquals(DefenseEnchantMath.LOFT_SAFE_FALL_PER_LEVEL,
                DefenseEnchantMath.loftSafeFallReduction(1), 1e-6f);
        assertEquals(2 * DefenseEnchantMath.LOFT_SAFE_FALL_PER_LEVEL,
                DefenseEnchantMath.loftSafeFallReduction(2), 1e-6f);
    }

    // --- Decoy threshold crossing ---

    @Test
    void decoyCrossing_trueOnlyWhenHitCarriesAcrossHalf() {
        // 20 max health, threshold at 10: a hit from above half to at/below half crosses.
        assertTrue(DefenseEnchantMath.decoyThresholdCrossed(12.0f, 8.0f, 20.0f));
        assertTrue(DefenseEnchantMath.decoyThresholdCrossed(11.0f, 10.0f, 20.0f),
                "landing exactly on the half line counts as a crossing");
    }

    @Test
    void decoyCrossing_falseWhenStartingAtOrBelowHalf() {
        // Chip damage taken while already low must not re-arm the decoy.
        assertFalse(DefenseEnchantMath.decoyThresholdCrossed(9.0f, 5.0f, 20.0f));
        assertFalse(DefenseEnchantMath.decoyThresholdCrossed(10.0f, 8.0f, 20.0f),
                "pre-hit health already on the line is not a downward crossing");
    }

    @Test
    void decoyCrossing_falseWhenHitStaysAboveHalf() {
        assertFalse(DefenseEnchantMath.decoyThresholdCrossed(20.0f, 15.0f, 20.0f));
    }

    // --- Bastion resistance duration ---

    @Test
    void bastionResistance_zeroAtLevelZero() {
        assertEquals(0, DefenseEnchantMath.bastionResistanceTicks(0));
    }

    @Test
    void bastionResistance_scalesPerLevel() {
        assertEquals(DefenseEnchantMath.BASTION_BASE_RESIST_TICKS
                        + DefenseEnchantMath.BASTION_RESIST_TICKS_PER_LEVEL,
                DefenseEnchantMath.bastionResistanceTicks(1));
        assertEquals(DefenseEnchantMath.BASTION_BASE_RESIST_TICKS
                        + 2 * DefenseEnchantMath.BASTION_RESIST_TICKS_PER_LEVEL,
                DefenseEnchantMath.bastionResistanceTicks(2));
    }

    // --- Everbloom beneficial-duration extension ---

    @Test
    void everbloom_unchangedAtLevelZero() {
        assertEquals(600, DefenseEnchantMath.everbloomExtendedDuration(600, 0));
    }

    @Test
    void everbloom_extendsByFifteenPercentPerLevel() {
        assertEquals(690, DefenseEnchantMath.everbloomExtendedDuration(600, 1)); // +15%
        assertEquals(780, DefenseEnchantMath.everbloomExtendedDuration(600, 2)); // +30%
        assertEquals(870, DefenseEnchantMath.everbloomExtendedDuration(600, 3)); // +45%
    }

    @Test
    void everbloom_maxLevelStaysUnderDoubleDuration() {
        int extended = DefenseEnchantMath.everbloomExtendedDuration(600, 3);
        assertTrue(extended < 2 * 600, "max level must stay well short of doubling duration");
    }

    @Test
    void everbloom_bonusIsCappedAboveMaxLevel() {
        // Guards against a future max-level bump: the cap binds before duration doubles.
        int extended = DefenseEnchantMath.everbloomExtendedDuration(600, 10);
        assertEquals((int) Math.ceil(600 * (1.0f + DefenseEnchantMath.EVERBLOOM_MAX_DURATION_BONUS)), extended);
    }

    @Test
    void everbloom_leavesInfiniteDurationUntouched() {
        assertEquals(-1, DefenseEnchantMath.everbloomExtendedDuration(-1, 3));
    }
}
