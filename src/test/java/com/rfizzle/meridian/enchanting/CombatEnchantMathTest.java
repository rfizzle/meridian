package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    // --- Reap: finisher bonus damage (the Ambush mirror) ---

    @Test
    void reapBonusDamage_maxAgainstNearDeath_growsAsHealthFalls() {
        float atDeath = CombatEnchantMath.reapBonusDamage(4, 0.0f);
        assertEquals(CombatEnchantMath.REAP_DAMAGE_PER_LEVEL * 4, atDeath, EPS);
        assertEquals(atDeath * 0.5f, CombatEnchantMath.reapBonusDamage(4, 0.5f), EPS);
        assertEquals(0.0f, CombatEnchantMath.reapBonusDamage(4, 1.0f), EPS);
    }

    @Test
    void reapBonusDamage_mirrorsAmbushAboutFullHealth() {
        // Reap at fraction f equals Ambush at (1 - f): they are exact opposites over the range.
        for (float f = 0.0f; f <= 1.0f; f += 0.25f) {
            assertEquals(CombatEnchantMath.ambushBonusDamage(3, 1.0f - f),
                    CombatEnchantMath.reapBonusDamage(3, f), EPS);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void reapBonusDamage_scalesWithLevel(int level) {
        assertEquals(CombatEnchantMath.REAP_DAMAGE_PER_LEVEL * level,
                CombatEnchantMath.reapBonusDamage(level, 0.0f), EPS);
    }

    @Test
    void reapBonusDamage_zeroLevel_isZero() {
        assertEquals(0.0f, CombatEnchantMath.reapBonusDamage(0, 0.0f), EPS);
    }

    @Test
    void reapBonusDamage_clampsOutOfRangeFraction() {
        assertEquals(0.0f, CombatEnchantMath.reapBonusDamage(4, 1.5f), EPS);
        assertEquals(CombatEnchantMath.REAP_DAMAGE_PER_LEVEL * 4,
                CombatEnchantMath.reapBonusDamage(4, -0.5f), EPS);
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

    // --- Torrent ---

    @Test
    void torrentBonusDamage_zeroWithoutEnchant_monotonicWithLevel() {
        assertEquals(0.0f, CombatEnchantMath.torrentBonusDamage(0), EPS);
        float previous = 0.0f;
        for (int level = 1; level <= 3; level++) {
            float bonus = CombatEnchantMath.torrentBonusDamage(level);
            assertEquals(CombatEnchantMath.TORRENT_BASE_DAMAGE
                    + CombatEnchantMath.TORRENT_DAMAGE_PER_LEVEL * level, bonus, EPS);
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

    // --- Crescendo: ramp stacks and cap ---

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void crescendoMaxStacks_isLevelPlusOne(int level) {
        assertEquals(level + 1, CombatEnchantMath.crescendoMaxStacks(level));
    }

    @Test
    void crescendoMaxStacks_zeroWithoutEnchant() {
        assertEquals(0, CombatEnchantMath.crescendoMaxStacks(0));
    }

    @Test
    void crescendoBonusDamage_openingHitCarriesNoBonus_thenRampsPerStack() {
        assertEquals(0.0f, CombatEnchantMath.crescendoBonusDamage(3, 0), EPS);
        assertEquals(CombatEnchantMath.CRESCENDO_DAMAGE_PER_STACK,
                CombatEnchantMath.crescendoBonusDamage(3, 1), EPS);
        assertEquals(CombatEnchantMath.CRESCENDO_DAMAGE_PER_STACK * 2,
                CombatEnchantMath.crescendoBonusDamage(3, 2), EPS);
    }

    @Test
    void crescendoBonusDamage_capsAtMaxStacksPerLevel() {
        for (int level = 1; level <= 3; level++) {
            float atCap = CombatEnchantMath.crescendoBonusDamage(level,
                    CombatEnchantMath.crescendoMaxStacks(level));
            assertEquals(atCap, CombatEnchantMath.crescendoBonusDamage(level, 1000), EPS);
            assertEquals(CombatEnchantMath.CRESCENDO_DAMAGE_PER_STACK * (level + 1), atCap, EPS);
        }
    }

    @Test
    void crescendoBonusDamage_zeroWithoutEnchant() {
        assertEquals(0.0f, CombatEnchantMath.crescendoBonusDamage(0, 5), EPS);
    }

    @Test
    void crescendoStreakExpired_insideAndOutsideTimeout() {
        long start = 1000L;
        assertFalse(CombatEnchantMath.crescendoStreakExpired(start, start));
        assertFalse(CombatEnchantMath.crescendoStreakExpired(start,
                start + CombatEnchantMath.CRESCENDO_TIMEOUT_TICKS));
        assertTrue(CombatEnchantMath.crescendoStreakExpired(start,
                start + CombatEnchantMath.CRESCENDO_TIMEOUT_TICKS + 1));
    }

    // --- Riposte: post-block window and flat bonus ---

    @Test
    void riposteWindowOpen_insideAndOutsideWindow() {
        long block = 500L;
        assertTrue(CombatEnchantMath.riposteWindowOpen(block, block));
        assertTrue(CombatEnchantMath.riposteWindowOpen(block,
                block + CombatEnchantMath.RIPOSTE_WINDOW_TICKS));
        assertFalse(CombatEnchantMath.riposteWindowOpen(block,
                block + CombatEnchantMath.RIPOSTE_WINDOW_TICKS + 1));
        // A block tick from the future (stale entry across a time skip) never validates.
        assertFalse(CombatEnchantMath.riposteWindowOpen(block, block - 1));
    }

    @Test
    void riposteBonusDamage_zeroWithoutEnchant_linearWithLevel() {
        assertEquals(0.0f, CombatEnchantMath.riposteBonusDamage(0), EPS);
        for (int level = 1; level <= 3; level++) {
            assertEquals(CombatEnchantMath.RIPOSTE_DAMAGE_PER_LEVEL * level,
                    CombatEnchantMath.riposteBonusDamage(level), EPS);
        }
    }

    // --- Joust: mount-speed scaling ---

    @Test
    void joustBonusDamage_zeroWhenStationaryOrBelowThreshold() {
        assertEquals(0.0f, CombatEnchantMath.joustBonusDamage(3, 0.0), EPS);
        assertEquals(0.0f, CombatEnchantMath.joustBonusDamage(3,
                CombatEnchantMath.JOUST_MIN_SPEED - 0.001), EPS);
    }

    @Test
    void joustBonusDamage_scalesLinearlyWithSpeedAndLevel() {
        double speed = 0.3;
        for (int level = 1; level <= 3; level++) {
            float expected = (float) (CombatEnchantMath.JOUST_DAMAGE_PER_SPEED_PER_LEVEL * level * speed);
            assertEquals(expected, CombatEnchantMath.joustBonusDamage(level, speed), EPS);
        }
    }

    @Test
    void joustBonusDamage_capsPerLevel() {
        for (int level = 1; level <= 3; level++) {
            assertEquals(CombatEnchantMath.JOUST_DAMAGE_CAP_PER_LEVEL * level,
                    CombatEnchantMath.joustBonusDamage(level, 100.0), EPS);
        }
    }

    @Test
    void joustBonusDamage_zeroWithoutEnchant() {
        assertEquals(0.0f, CombatEnchantMath.joustBonusDamage(0, 0.5), EPS);
    }

    // ---- Curse of Blunting ----

    @Test
    void bluntingGlanceChance_scalesPerLevelAndClamps() {
        for (int level = 1; level <= 3; level++) {
            assertEquals(Math.min(1.0f, CombatEnchantMath.BLUNTING_GLANCE_CHANCE_PER_LEVEL * level),
                    CombatEnchantMath.bluntingGlanceChance(level), EPS);
        }
        assertEquals(0.0f, CombatEnchantMath.bluntingGlanceChance(0), EPS);
        assertEquals(1.0f, CombatEnchantMath.bluntingGlanceChance(1000), EPS);
    }

    @Test
    void bluntingDamageMultiplier_reducesWhenRollLandsInsideChance() {
        // Roll below the chance glances; roll above leaves the strike full.
        assertEquals(CombatEnchantMath.BLUNTING_GLANCE_MULTIPLIER,
                CombatEnchantMath.bluntingDamageMultiplier(3, 0.0f), EPS);
        assertEquals(1.0f, CombatEnchantMath.bluntingDamageMultiplier(3, 0.99f), EPS);
    }

    @Test
    void bluntingDamageMultiplier_neverReducesWithoutEnchant() {
        assertEquals(1.0f, CombatEnchantMath.bluntingDamageMultiplier(0, 0.0f), EPS);
    }
}
