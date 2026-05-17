// Tier: 1 (pure — clampDamage uses only int arithmetic, no MC imports)
package com.rfizzle.meridian.anvil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageClampMathTest {

    @Test
    void normalDamage_appliesFullDelta() {
        assertEquals(200, ExtractionTomeHandler.clampDamage(0, 200, 1000));
    }

    @Test
    void damageAtCeiling_clampsToMaxMinusOne() {
        assertEquals(1, ExtractionTomeHandler.clampDamage(1, 1, 2),
                "max=2 → ceiling is 1; curDmg=1 + delta=1 = 2, clamped to 1");
    }

    @Test
    void damageExceedsCeiling_clampsToMaxMinusOne() {
        assertEquals(999, ExtractionTomeHandler.clampDamage(0, 5000, 1000),
                "proposed 5000 exceeds ceiling 999");
    }

    @Test
    void zeroDelta_preservesCurrentDamage() {
        assertEquals(50, ExtractionTomeHandler.clampDamage(50, 0, 1000));
    }

    @Test
    void maxDamageOne_ceilingIsZero() {
        assertEquals(0, ExtractionTomeHandler.clampDamage(0, 100, 1),
                "max=1 → ceiling is max(0, 0) = 0");
    }

    @Test
    void maxDamageZero_ceilingIsZero() {
        assertEquals(0, ExtractionTomeHandler.clampDamage(0, 100, 0),
                "max=0 → ceiling is max(0, -1) = 0");
    }

    @Test
    void twentyPercentOfThousand_applyCorrectly() {
        int maxDamage = 1000;
        int damageDelta = (int) (maxDamage * 0.2F);
        assertEquals(200, ExtractionTomeHandler.clampDamage(0, damageDelta, maxDamage));
    }

    @Test
    void fiftyPercentOfTwo_clampsToOne() {
        int maxDamage = 2;
        int damageDelta = (int) (maxDamage * 0.5F);
        assertEquals(1, ExtractionTomeHandler.clampDamage(1, damageDelta, maxDamage),
                "curDmg=1 + delta=1 = 2, ceiling=1 → clamped to 1");
    }

    @Test
    void preExistingDamage_addedToDelta() {
        assertEquals(350, ExtractionTomeHandler.clampDamage(150, 200, 1000));
    }

    @Test
    void largeDelta_withPreExistingDamage_clampsToMaxMinusOne() {
        assertEquals(999, ExtractionTomeHandler.clampDamage(500, 1000, 1000));
    }
}
