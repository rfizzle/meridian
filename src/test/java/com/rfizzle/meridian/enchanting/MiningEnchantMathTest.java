package com.rfizzle.meridian.enchanting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningEnchantMathTest {

    private static final float EPS = 1e-6f;

    // --- Grind: hardness-speed curve ---

    @Test
    void grindBonus_zeroWithoutLevels() {
        assertEquals(0.0f, MiningEnchantMath.grindBonus(0, 50.0f), EPS);
        assertEquals(0.0f, MiningEnchantMath.grindBonus(-1, 50.0f), EPS);
    }

    @Test
    void grindBonus_softBlocksGainNothing() {
        // Dirt (0.5) and stone (1.5) sit below the hardness gate.
        assertEquals(0.0f, MiningEnchantMath.grindBonus(3, 0.5f), EPS);
        assertEquals(0.0f, MiningEnchantMath.grindBonus(3, 1.5f), EPS);
        assertEquals(0.0f, MiningEnchantMath.grindBonus(3,
                MiningEnchantMath.GRIND_MIN_HARDNESS - 0.01f), EPS);
    }

    @Test
    void grindBonus_startsAtThreshold() {
        float expected = MiningEnchantMath.GRIND_SPEED_PER_HARDNESS_PER_LEVEL
                * MiningEnchantMath.GRIND_MIN_HARDNESS;
        assertEquals(expected,
                MiningEnchantMath.grindBonus(1, MiningEnchantMath.GRIND_MIN_HARDNESS), EPS);
    }

    @Test
    void grindBonus_scalesLinearlyWithHardnessAndLevel() {
        // Deepslate ore hardness 4.5: level doubles the bonus.
        float levelOne = MiningEnchantMath.grindBonus(1, 4.5f);
        assertEquals(0.5f * 4.5f, levelOne, EPS);
        assertEquals(2 * levelOne, MiningEnchantMath.grindBonus(2, 4.5f), EPS);
        // Harder block, bigger bonus at the same level.
        assertEquals(0.5f * 9.0f, MiningEnchantMath.grindBonus(1, 9.0f), EPS);
    }

    @Test
    void grindBonus_cappedOnExtremeHardness() {
        // Obsidian (50) at level III would be 75 uncapped.
        assertEquals(MiningEnchantMath.GRIND_MAX_BONUS,
                MiningEnchantMath.grindBonus(3, 50.0f), EPS);
        // The cap keeps Grind below Efficiency V's +26 attribute bonus.
        assertEquals(24.0f, MiningEnchantMath.GRIND_MAX_BONUS, EPS);
    }

    // --- Adamant: tier ladder ---

    @Test
    void adamantTier_levelZeroIsIdentity() {
        assertEquals(1, MiningEnchantMath.adamantEffectiveTierIndex(1, 0, 4));
        assertEquals(3, MiningEnchantMath.adamantEffectiveTierIndex(3, -2, 4));
    }

    @Test
    void adamantTier_raisesOneRungPerLevel() {
        // Stone (1) + I -> iron (2); stone + II -> diamond (3).
        assertEquals(2, MiningEnchantMath.adamantEffectiveTierIndex(1, 1, 4));
        assertEquals(3, MiningEnchantMath.adamantEffectiveTierIndex(1, 2, 4));
        // Wood (0) + I -> stone (1).
        assertEquals(1, MiningEnchantMath.adamantEffectiveTierIndex(0, 1, 4));
    }

    @Test
    void adamantTier_clampsAtLadderTop() {
        // Diamond (3) + II clamps at netherite (4); netherite stays netherite.
        assertEquals(4, MiningEnchantMath.adamantEffectiveTierIndex(3, 2, 4));
        assertEquals(4, MiningEnchantMath.adamantEffectiveTierIndex(4, 2, 4));
    }
}
