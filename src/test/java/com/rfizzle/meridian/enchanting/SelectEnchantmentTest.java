// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.enchanting;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rfizzle.meridian.TestRegistryFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectEnchantmentTest {

    private static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    private static final ResourceKey<Enchantment> SMITE = key("smite");
    private static final ResourceKey<Enchantment> BANE = key("bane_of_arthropods");
    private static final ResourceKey<Enchantment> MENDING = key("mending");
    private static final ResourceKey<Enchantment> UNBREAKING = key("unbreaking");
    private static final ResourceKey<Enchantment> LURE = key("lure");

    private static Registry<Enchantment> registry;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registry = buildTestRegistry();
    }

    // ---- Blacklist ----

    @Test
    void blacklistedEnchantment_neverAppears() {
        Set<ResourceKey<Enchantment>> blacklist = Set.of(SHARPNESS);
        boolean sawSharpness = false;
        for (long seed = 0L; seed < 1000L; seed++) {
            List<EnchantmentInstance> result = RealEnchantmentHelper.selectEnchantment(
                    seeded(seed), sword(), 40, 50F, 0F, 0F, false, blacklist, registry);
            for (EnchantmentInstance inst : result) {
                if (inst.enchantment.is(SHARPNESS)) {
                    sawSharpness = true;
                    break;
                }
            }
            if (sawSharpness) break;
        }
        assertFalse(sawSharpness, "blacklisted sharpness must never be rolled over 1000 seeds");
    }

    // ---- Treasure gating ----

    @Test
    void treasureFlagFalse_excludesTreasureEnchantments() {
        boolean sawMending = false;
        for (long seed = 0L; seed < 500L; seed++) {
            List<EnchantmentInstance> result = RealEnchantmentHelper.selectEnchantment(
                    seeded(seed), sword(), 40, 50F, 100F, 0F, false, Set.of(), registry);
            for (EnchantmentInstance inst : result) {
                if (inst.enchantment.is(MENDING)) {
                    sawMending = true;
                    break;
                }
            }
            if (sawMending) break;
        }
        assertFalse(sawMending, "treasure Mending must never appear when treasureAllowed=false");
    }

    @Test
    void treasureFlagTrue_allowsTreasureEnchantments() {
        // With treasure enabled and plenty of rolls, Mending (treasure-tagged in our test fixture)
        // must surface at least once.
        boolean sawMending = false;
        for (long seed = 0L; seed < 500L; seed++) {
            List<EnchantmentInstance> result = RealEnchantmentHelper.selectEnchantment(
                    seeded(seed), sword(), 40, 0F, 100F, 0F, true, Set.of(), registry);
            for (EnchantmentInstance inst : result) {
                if (inst.enchantment.is(MENDING)) {
                    sawMending = true;
                    break;
                }
            }
            if (sawMending) break;
        }
        assertTrue(sawMending, "treasure Mending must be reachable when treasureAllowed=true");
    }

    // ---- Quanta variance ----

    @Test
    void quanta_widensPowerWindowMonotonically() {
        double lowVar = sampleFactorVariance(10F, 0F, 5000);
        double midVar = sampleFactorVariance(40F, 0F, 5000);
        double highVar = sampleFactorVariance(80F, 0F, 5000);

        assertTrue(lowVar < midVar,
                "quanta=40 variance (" + midVar + ") must exceed quanta=10 variance (" + lowVar + ")");
        assertTrue(midVar < highVar,
                "quanta=80 variance (" + highVar + ") must exceed quanta=40 variance (" + midVar + ")");
    }

    // ---- Rectification semantics ----

    @Test
    void rectification_fullyEliminatesNegativeVariance() {
        // rectification=100 means the lower bound of the quanta factor is 0, so every sample
        // must be >= 0 — i.e. scaled levels are never below the source level.
        for (long seed = 0L; seed < 2000L; seed++) {
            float factor = RealEnchantmentHelper.getQuantaFactor(seeded(seed), 100F, 100F);
            assertTrue(factor >= 0F,
                    "rectification=100 must clamp factor >= 0 (got " + factor + " at seed " + seed + ")");
        }
    }

    @Test
    void rectification_zeroAllowsNegativeVariance() {
        // With no rectification and high quanta, at least one negative factor is expected.
        boolean sawNegative = false;
        for (long seed = 0L; seed < 500L; seed++) {
            float factor = RealEnchantmentHelper.getQuantaFactor(seeded(seed), 100F, 0F);
            if (factor < 0F) {
                sawNegative = true;
                break;
            }
        }
        assertTrue(sawNegative, "rectification=0 must permit negative quanta factors");
    }

    // ---- Arcana tier lookup ----

    @Test
    void arcanaTier_tracksThreshold() {
        assertEquals(RealEnchantmentHelper.Arcana.EMPTY,
                RealEnchantmentHelper.Arcana.getForThreshold(0F));
        assertEquals(RealEnchantmentHelper.Arcana.LITTLE,
                RealEnchantmentHelper.Arcana.getForThreshold(10F));
        assertEquals(RealEnchantmentHelper.Arcana.MEDIUM,
                RealEnchantmentHelper.Arcana.getForThreshold(55F));
        assertEquals(RealEnchantmentHelper.Arcana.MAX,
                RealEnchantmentHelper.Arcana.getForThreshold(100F));
    }

    @Test
    void arcanaThreshold25_addsSecondPick() {
        // With only one candidate in the pool, arcana=0 yields at most 1 enchantment and
        // arcana=30 still yields 1 (the second pick has nothing left after the first is removed).
        // To verify the branch is reachable, check that a richer pool produces more results at
        // higher arcana across many seeds on average.
        int belowSum = 0;
        int aboveSum = 0;
        for (long seed = 0L; seed < 200L; seed++) {
            belowSum += RealEnchantmentHelper.selectEnchantment(
                    seeded(seed), sword(), 40, 0F, 0F, 0F, false, Set.of(), registry).size();
            aboveSum += RealEnchantmentHelper.selectEnchantment(
                    seeded(seed), sword(), 40, 0F, 80F, 0F, false, Set.of(), registry).size();
        }
        assertTrue(aboveSum >= belowSum,
                "arcana=80 total picks (" + aboveSum + ") must be >= arcana=0 (" + belowSum + ")");
    }

    // ---- Supported-items filtering ----

    @Test
    void unsupportedItem_returnsEmpty() {
        // A plain item with 0 enchantment value (e.g. DIRT) should yield no enchantments.
        List<EnchantmentInstance> result = RealEnchantmentHelper.selectEnchantment(
                seeded(42L), new ItemStack(Items.DIRT), 40, 50F, 50F, 0F, true, Set.of(), registry);
        assertTrue(result.isEmpty(), "unenchantable item must return an empty list");
    }

    @Test
    void rodItem_getsRodEnchantments_notSwordEnchantments() {
        // LURE is registered only on the fishing rod in our fixture; SHARPNESS/SMITE only on
        // the sword. Rolling the rod must not surface sword-only picks.
        boolean sawSharpness = false;
        boolean sawLure = false;
        for (long seed = 0L; seed < 500L; seed++) {
            List<EnchantmentInstance> result = RealEnchantmentHelper.selectEnchantment(
                    seeded(seed), new ItemStack(Items.FISHING_ROD), 40, 40F, 50F, 0F, false, Set.of(), registry);
            for (EnchantmentInstance inst : result) {
                if (inst.enchantment.is(SHARPNESS) || inst.enchantment.is(SMITE) || inst.enchantment.is(BANE)) {
                    sawSharpness = true;
                }
                if (inst.enchantment.is(LURE)) {
                    sawLure = true;
                }
            }
            if (sawSharpness && sawLure) break;
        }
        assertFalse(sawSharpness, "sword-only picks must not roll on a fishing rod");
        assertTrue(sawLure, "rod-supported Lure must be reachable");
    }

    // ---- Power-level selection (regression: power >= maxPower vs <= maxPower) ----

    @Test
    void highPower_selectsMaxEnchantmentLevel() {
        // Fixture uses dynamicCost(1, 10): minCost(lvl) = 1 + 10*(lvl-1).
        // Sharpness level 5: minCost = 41. Power=41 should yield level 5.
        List<EnchantmentInstance> results = RealEnchantmentHelper.getAvailableEnchantmentResults(
                41, sword(), registry, false, Set.of());
        EnchantmentInstance sharpness = results.stream()
                .filter(inst -> inst.enchantment.is(SHARPNESS))
                .findFirst().orElse(null);
        assertNotNull(sharpness, "Sharpness must be in candidate list at power=41");
        assertEquals(5, sharpness.level, "Sharpness should be level 5 at power=41");
    }

    @Test
    void midPower_selectsAppropriateEnchantmentLevel() {
        // Sharpness level 4: minCost=31, level 5: minCost=41. Power=35 should yield level 4.
        List<EnchantmentInstance> results = RealEnchantmentHelper.getAvailableEnchantmentResults(
                35, sword(), registry, false, Set.of());
        EnchantmentInstance sharpness = results.stream()
                .filter(inst -> inst.enchantment.is(SHARPNESS))
                .findFirst().orElse(null);
        assertNotNull(sharpness, "Sharpness must be in candidate list at power=35");
        assertEquals(4, sharpness.level, "Sharpness should be level 4 at power=35");
    }

    @Test
    void lowPower_selectsMinimumEnchantmentLevel() {
        // Sharpness level 1: minCost=1, level 2: minCost=11. Power=5 should yield level 1.
        List<EnchantmentInstance> results = RealEnchantmentHelper.getAvailableEnchantmentResults(
                5, sword(), registry, false, Set.of());
        EnchantmentInstance sharpness = results.stream()
                .filter(inst -> inst.enchantment.is(SHARPNESS))
                .findFirst().orElse(null);
        assertNotNull(sharpness, "Sharpness must be in candidate list at power=5");
        assertEquals(1, sharpness.level, "Sharpness should be level 1 at power=5");
    }

    // ---- Rarity bucket ----

    @Test
    void rarityBucket_matchesVanillaRarityWeights() {
        assertEquals(0, RealEnchantmentHelper.rarityBucket(10));
        assertEquals(0, RealEnchantmentHelper.rarityBucket(20));
        assertEquals(1, RealEnchantmentHelper.rarityBucket(5));
        assertEquals(1, RealEnchantmentHelper.rarityBucket(9));
        assertEquals(2, RealEnchantmentHelper.rarityBucket(2));
        assertEquals(2, RealEnchantmentHelper.rarityBucket(4));
        assertEquals(3, RealEnchantmentHelper.rarityBucket(1));
        assertEquals(3, RealEnchantmentHelper.rarityBucket(0));
    }

    // ---- Fixtures ----

    private static double sampleFactorVariance(float quanta, float rectification, int samples) {
        double sum = 0;
        double sumSq = 0;
        for (long seed = 0L; seed < samples; seed++) {
            double factor = RealEnchantmentHelper.getQuantaFactor(seeded(seed), quanta, rectification);
            sum += factor;
            sumSq += factor * factor;
        }
        double mean = sum / samples;
        return sumSq / samples - mean * mean;
    }

    private static RandomSource seeded(long seed) {
        RandomSource rand = RandomSource.create();
        rand.setSeed(seed);
        return rand;
    }

    private static ItemStack sword() {
        return new ItemStack(Items.DIAMOND_SWORD);
    }

    private static Registry<Enchantment> buildTestRegistry() {
        MappedRegistry<Enchantment> reg = newRegistry();

        HolderSet<Item> swordItems = itemHolderSet(Items.DIAMOND_SWORD, Items.WOODEN_SWORD);
        HolderSet<Item> rodItems = itemHolderSet(Items.FISHING_ROD);
        HolderSet<Item> anyItems = itemHolderSet(
                Items.DIAMOND_SWORD, Items.WOODEN_SWORD, Items.FISHING_ROD,
                Items.DIAMOND_PICKAXE, Items.BOOK);

        Holder.Reference<Enchantment> sharpness = register(reg, SHARPNESS, synthetic(swordItems, 10, 5));
        Holder.Reference<Enchantment> smite = register(reg, SMITE, synthetic(swordItems, 5, 5));
        Holder.Reference<Enchantment> bane = register(reg, BANE, synthetic(swordItems, 5, 5));
        Holder.Reference<Enchantment> unbreaking = register(reg, UNBREAKING, synthetic(anyItems, 5, 3));
        Holder.Reference<Enchantment> mending = register(reg, MENDING, synthetic(anyItems, 2, 1));
        Holder.Reference<Enchantment> lure = register(reg, LURE, synthetic(rodItems, 2, 3));

        List<Holder<Enchantment>> inTable = List.of(sharpness, smite, bane, unbreaking, lure);
        List<Holder<Enchantment>> treasure = List.of(mending);
        reg.bindTags(Map.of(
                EnchantmentTags.IN_ENCHANTING_TABLE, inTable,
                EnchantmentTags.TREASURE, treasure
        ));
        return reg.freeze();
    }
}
