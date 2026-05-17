// Tier: 1 (pseudo — uses RandomSource, Mth as POJOs; no Bootstrap)
package com.rfizzle.meridian.enchanting;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnchantmentCostMathTest {

    @Test
    void seededRng_isDeterministic() {
        for (long seed = 0L; seed < 1000L; seed++) {
            for (int slot = 0; slot < 3; slot++) {
                int a = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), slot, 30F, null);
                int b = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), slot, 30F, null);
                assertEquals(a, b,
                        "slot " + slot + " must be deterministic at seed " + seed);
            }
        }
    }

    @Test
    void slotOrdering_isMonotonic() {
        for (long seed = 0L; seed < 1000L; seed++) {
            int slot0 = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 0, 50F, null);
            int slot1 = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 1, 50F, null);
            int slot2 = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 2, 50F, null);

            assertTrue(slot0 <= slot1,
                    "slot 0 <= slot 1 at seed " + seed + ": " + slot0 + " > " + slot1);
            assertTrue(slot1 <= slot2,
                    "slot 1 <= slot 2 at seed " + seed + ": " + slot1 + " > " + slot2);
        }
    }

    @Test
    void slot2_returnsRoundOfEterna() {
        assertEquals(50, RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, 50F, null));
        assertEquals(30, RealEnchantmentHelper.getEnchantmentCost(seeded(42L), 2, 30F, null));
        assertEquals(15, RealEnchantmentHelper.getEnchantmentCost(seeded(99L), 2, 15F, null));
        assertEquals(0, RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, 0F, null));
    }

    @Test
    void slot0_boundsMatchLowerRange() {
        int level = 50;
        for (long seed = 0L; seed < 1000L; seed++) {
            int cost = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 0, level, null);
            assertTrue(cost >= 1,
                    "slot 0 must be floored at 1; got " + cost + " at seed " + seed);
            assertTrue(cost <= Math.round(level * 0.4F),
                    "slot 0 must be <= 40% of level; got " + cost + " at seed " + seed);
        }
    }

    @Test
    void slot1_boundsMatchMidRange() {
        int level = 50;
        for (long seed = 0L; seed < 1000L; seed++) {
            int cost = RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 1, level, null);
            assertTrue(cost >= Math.max(1, Math.round(level * 0.6F)),
                    "slot 1 must be >= 60% of level; got " + cost + " at seed " + seed);
            assertTrue(cost <= Math.round(level * 0.8F),
                    "slot 1 must be <= 80% of level; got " + cost + " at seed " + seed);
        }
    }

    @Test
    void negativeEterna_clampsToZero() {
        assertEquals(0, RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, -10F, null));
    }

    @Test
    void eternaBeyondDefaultCap_clampsToCap() {
        assertEquals(RealEnchantmentHelper.DEFAULT_MAX_ETERNA,
                RealEnchantmentHelper.getEnchantmentCost(seeded(0L), 2, 200F, null));
    }

    @Test
    void lowEterna_slots0And1_floorAtOne() {
        for (long seed = 0L; seed < 64L; seed++) {
            assertTrue(
                    RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 0, 1F, null) >= 1,
                    "slot 0 at eterna=1 must be >= 1");
            assertTrue(
                    RealEnchantmentHelper.getEnchantmentCost(seeded(seed), 1, 1F, null) >= 1,
                    "slot 1 at eterna=1 must be >= 1");
        }
    }

    private static RandomSource seeded(long seed) {
        RandomSource rand = RandomSource.create();
        rand.setSeed(seed);
        return rand;
    }
}
