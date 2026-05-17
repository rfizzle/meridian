// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.enchanting;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealEnchantmentHelperTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void getEnchantmentCost_seededRng_isDeterministic() {
        int a0 = RealEnchantmentHelper.getEnchantmentCost(seeded(1234L), 0, 30F, ItemStack.EMPTY);
        int b0 = RealEnchantmentHelper.getEnchantmentCost(seeded(1234L), 0, 30F, ItemStack.EMPTY);
        int a1 = RealEnchantmentHelper.getEnchantmentCost(seeded(9876L), 1, 30F, ItemStack.EMPTY);
        int b1 = RealEnchantmentHelper.getEnchantmentCost(seeded(9876L), 1, 30F, ItemStack.EMPTY);
        int a2 = RealEnchantmentHelper.getEnchantmentCost(seeded(42L), 2, 30F, ItemStack.EMPTY);
        int b2 = RealEnchantmentHelper.getEnchantmentCost(seeded(42L), 2, 30F, ItemStack.EMPTY);

        assertEquals(a0, b0, "slot 0 must be deterministic for a fixed seed");
        assertEquals(a1, b1, "slot 1 must be deterministic for a fixed seed");
        assertEquals(a2, b2, "slot 2 must be deterministic for a fixed seed");
    }

    @Test
    void getEnchantmentCost_slotOrdering_isMonotonic() {
        for (long seed = 0L; seed < 256L; seed++) {
            int slot0 = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 0, 50F, ItemStack.EMPTY);
            int slot1 = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 1, 50F, ItemStack.EMPTY);
            int slot2 = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 2, 50F, ItemStack.EMPTY);

            assertTrue(slot0 <= slot1,
                    "slot 0 must be <= slot 1 at seed " + seed + ": " + slot0 + " > " + slot1);
            assertTrue(slot1 <= slot2,
                    "slot 1 must be <= slot 2 at seed " + seed + ": " + slot1 + " > " + slot2);
        }
    }

    @Test
    void getEnchantmentCost_slot2_respectsEternaAsLevel() {
        for (long seed = 0L; seed < 64L; seed++) {
            int slot2 = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 2, 50F, ItemStack.EMPTY);
            assertTrue(slot2 >= 25 && slot2 <= 50,
                    "eterna=50 slot 2 must be in [25, 50]; got " + slot2);
        }
    }

    @Test
    void getEnchantmentCost_slot2_isDeterministicInEterna() {
        assertEquals(50, RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, 50F, ItemStack.EMPTY));
        assertEquals(30, RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, 30F, ItemStack.EMPTY));
        assertEquals(15, RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, 15F, ItemStack.EMPTY));
    }

    @Test
    void getEnchantmentCost_slot0Bounds_matchLowerRange() {
        int level = 50;
        for (long seed = 0L; seed < 256L; seed++) {
            int cost = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 0, level, ItemStack.EMPTY);
            assertTrue(cost >= 1, "slot 0 must be floored at 1; got " + cost);
            assertTrue(cost <= Math.round(level * 0.4F),
                    "slot 0 must be <= 40% of slot 2; got " + cost);
        }
    }

    @Test
    void getEnchantmentCost_slot1Bounds_matchMidRange() {
        int level = 50;
        for (long seed = 0L; seed < 256L; seed++) {
            int cost = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 1, level, ItemStack.EMPTY);
            assertTrue(cost >= Math.max(1, Math.round(level * 0.6F)),
                    "slot 1 must be >= 60% of slot 2; got " + cost);
            assertTrue(cost <= Math.round(level * 0.8F),
                    "slot 1 must be <= 80% of slot 2; got " + cost);
        }
    }

    @Test
    void getEnchantmentCost_zeroEterna_returnsZero() {
        assertEquals(0, RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, 0F, ItemStack.EMPTY));
    }

    @Test
    void getEnchantmentCost_negativeEterna_clampsToZero() {
        assertEquals(0, RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, -10F, ItemStack.EMPTY));
    }

    @Test
    void getEnchantmentCost_eternaBeyondConfigCap_clampsToCap() {
        // Default config cap is 100 (null config falls back to RealEnchantmentHelper.DEFAULT_MAX_ETERNA).
        assertEquals(RealEnchantmentHelper.DEFAULT_MAX_ETERNA,
                RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, 200F, ItemStack.EMPTY));
    }

    @Test
    void getEnchantmentCost_slot0_staysAboveOneAtLowEterna() {
        // Even when the raw multiplier rounds to 0, slot 0/1 are floored at 1 so the vanilla
        // "slot has a valid cost to charge" invariant is preserved.
        for (long seed = 0L; seed < 64L; seed++) {
            assertTrue(RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 0, 1F, ItemStack.EMPTY) >= 1);
            assertTrue(RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 1, 1F, ItemStack.EMPTY) >= 1);
        }
    }

    private static RandomSource seeded(long seed) {
        RandomSource rand = RandomSource.create();
        rand.setSeed(seed);
        return rand;
    }
}
