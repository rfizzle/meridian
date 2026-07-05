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
}
