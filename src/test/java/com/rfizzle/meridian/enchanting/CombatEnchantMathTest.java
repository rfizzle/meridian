package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tier: 1 (pure JUnit)
class CombatEnchantMathTest {

    private static final float EPS = 1e-6f;

    // --- Ambush: pre-hit health fraction ---

    @Test
    void ambushHealthFraction_fullHealthTarget_isOne() {
        assertEquals(1.0f, CombatEnchantMath.ambushHealthFraction(20.0f, 20.0f), EPS);
    }

    @Test
    void ambushHealthFraction_woundedTarget_isProportional() {
        assertEquals(0.5f, CombatEnchantMath.ambushHealthFraction(10.0f, 20.0f), EPS);
        assertEquals(0.25f, CombatEnchantMath.ambushHealthFraction(5.0f, 20.0f), EPS);
    }

    @Test
    void ambushHealthFraction_absorptionOverflow_clampsToOne() {
        // Absorption hearts can push effective pre-hit health above max
        assertEquals(1.0f, CombatEnchantMath.ambushHealthFraction(26.0f, 20.0f), EPS);
    }

    @Test
    void ambushHealthFraction_degenerateInputs_areSafe() {
        assertEquals(0.0f, CombatEnchantMath.ambushHealthFraction(10.0f, 0.0f), EPS);
        assertEquals(0.0f, CombatEnchantMath.ambushHealthFraction(-1.0f, 20.0f), EPS);
    }

    // --- Ambush: bonus damage scaling ---

    @Test
    void ambushBonusDamage_maxAgainstFullHealth_fadesLinearly() {
        float full = CombatEnchantMath.ambushBonusDamage(4, 1.0f);
        assertEquals(CombatEnchantMath.AMBUSH_DAMAGE_PER_LEVEL * 4, full, EPS);
        assertEquals(full * 0.5f, CombatEnchantMath.ambushBonusDamage(4, 0.5f), EPS);
        assertEquals(0.0f, CombatEnchantMath.ambushBonusDamage(4, 0.0f), EPS);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void ambushBonusDamage_scalesWithLevel(int level) {
        assertEquals(CombatEnchantMath.AMBUSH_DAMAGE_PER_LEVEL * level,
                CombatEnchantMath.ambushBonusDamage(level, 1.0f), EPS);
    }

    @Test
    void ambushBonusDamage_zeroLevel_isZero() {
        assertEquals(0.0f, CombatEnchantMath.ambushBonusDamage(0, 1.0f), EPS);
    }

    // --- Pinpoint ---

    @Test
    void pinpointBonusDamage_zeroWithoutEnchant_monotonicWithLevel() {
        assertEquals(0.0f, CombatEnchantMath.pinpointBonusDamage(0), EPS);
        float previous = 0.0f;
        for (int level = 1; level <= 4; level++) {
            float bonus = CombatEnchantMath.pinpointBonusDamage(level);
            assertEquals(CombatEnchantMath.PINPOINT_BASE_DAMAGE
                    + CombatEnchantMath.PINPOINT_DAMAGE_PER_LEVEL * level, bonus, EPS);
            assertTrue(bonus > previous, "bonus must grow with level");
            previous = bonus;
        }
    }

    // --- Sunder / Trophy chances ---

    @Test
    void sunderChance_zeroWithoutEnchant_scalesAndStaysAProbability() {
        assertEquals(0.0f, CombatEnchantMath.sunderChance(0), EPS);
        for (int level = 1; level <= 3; level++) {
            float chance = CombatEnchantMath.sunderChance(level);
            assertEquals(CombatEnchantMath.SUNDER_CHANCE_PER_LEVEL * level, chance, EPS);
            assertTrue(chance > 0.0f && chance <= 1.0f);
        }
    }

    @Test
    void trophyChance_zeroWithoutEnchant_scalesAndStaysAProbability() {
        assertEquals(0.0f, CombatEnchantMath.trophyChance(0), EPS);
        for (int level = 1; level <= 2; level++) {
            float chance = CombatEnchantMath.trophyChance(level);
            assertEquals(CombatEnchantMath.TROPHY_CHANCE_PER_LEVEL * level, chance, EPS);
            assertTrue(chance > 0.0f && chance <= 1.0f);
        }
    }

    @Test
    void chances_clampAtOne_forAbsurdLevels() {
        assertEquals(1.0f, CombatEnchantMath.sunderChance(1000), EPS);
        assertEquals(1.0f, CombatEnchantMath.trophyChance(1000), EPS);
    }

    // --- Fortuity ---

    @Test
    void fortuityLuckBonus_zeroWithoutEnchant_linearWithLevel() {
        assertEquals(0.0f, CombatEnchantMath.fortuityLuckBonus(0), EPS);
        for (int level = 1; level <= 3; level++) {
            assertEquals(CombatEnchantMath.FORTUITY_LUCK_PER_LEVEL * level,
                    CombatEnchantMath.fortuityLuckBonus(level), EPS);
        }
    }
}
