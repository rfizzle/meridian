// Tier: 1 (pure JUnit)
package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnduranceHealMathTest {

    private static final float EPSILON = 1.0e-6f;

    @Test
    void healPerPulse_isZeroAtOrBelowLevelZero() {
        assertEquals(0.0f, EnduranceHealMath.healPerPulse(0), EPSILON);
        assertEquals(0.0f, EnduranceHealMath.healPerPulse(-1), EPSILON);
    }

    @Test
    void healPerPulse_scalesLinearlyWithLevel() {
        assertEquals(EnduranceHealMath.HEAL_PER_LEVEL, EnduranceHealMath.healPerPulse(1), EPSILON);
        assertEquals(2 * EnduranceHealMath.HEAL_PER_LEVEL, EnduranceHealMath.healPerPulse(2), EPSILON);
        assertEquals(3 * EnduranceHealMath.HEAL_PER_LEVEL, EnduranceHealMath.healPerPulse(3), EPSILON);
    }

    @Test
    void healPerPulse_strictlyIncreasesWithLevel() {
        assertTrue(EnduranceHealMath.healPerPulse(3) > EnduranceHealMath.healPerPulse(2),
                "level III must out-heal level II");
        assertTrue(EnduranceHealMath.healPerPulse(2) > EnduranceHealMath.healPerPulse(1),
                "level II must out-heal level I");
    }

    @Test
    void pulseInterval_isASlowOutOfCombatDrip() {
        // A multi-second cadence keeps Endurance irrelevant mid-fight while still healing over time.
        assertTrue(EnduranceHealMath.PULSE_INTERVAL_TICKS >= 20,
                "heal pulses must be at least a second apart");
    }
}
