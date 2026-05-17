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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.rfizzle.meridian.TestRegistryFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildClueListTest {

    private static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    private static final ResourceKey<Enchantment> SMITE = key("smite");
    private static final ResourceKey<Enchantment> BANE = key("bane_of_arthropods");
    private static final ResourceKey<Enchantment> MENDING = key("mending");
    private static final ResourceKey<Enchantment> UNBREAKING = key("unbreaking");

    private static Registry<Enchantment> registry;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registry = buildTestRegistry();
    }

    // ---- Primary invariants ----

    @Test
    void buildClueList_firstClueMatchesPrimary_over100Seeds() {
        // Primary picking on a freshly-built pool of 3 entries. For every seed the first
        // clue (index 0) is exactly the primary, which is the property the menu relies on
        // so the tooltip headline matches the applied enchant.
        List<EnchantmentInstance> pool = pool3();
        for (long seed = 0L; seed < 100L; seed++) {
            RealEnchantmentHelper.ClueBuild result =
                    RealEnchantmentHelper.buildClueList(seeded(seed), pool, 3);
            assertNotNull(result.primary(), "primary must exist for a non-empty pool");
            assertFalse(result.clues().isEmpty(), "clues must be non-empty for cluesCount > 0");
            assertEquals(result.primary(), result.clues().get(0),
                    "first clue must equal the primary for seed " + seed);
        }
    }

    @Test
    void buildClueList_primaryIsDrawnFromPool() {
        List<EnchantmentInstance> pool = pool3();
        for (long seed = 0L; seed < 100L; seed++) {
            RealEnchantmentHelper.ClueBuild result =
                    RealEnchantmentHelper.buildClueList(seeded(seed), pool, 1);
            assertTrue(containsByKeyAndLevel(pool, result.primary()),
                    "primary must originate from the input pool for seed " + seed);
        }
    }

    @Test
    void buildClueList_firstClueMatchesSelectEnchantment_over100Seeds() {
        // End-to-end: select enchantments for a slot, then build clues from that selection.
        // The first clue is always one of the selected enchants — so the tooltip can never
        // lie about what the player will receive.
        for (long seed = 0L; seed < 100L; seed++) {
            List<EnchantmentInstance> selection = RealEnchantmentHelper.selectEnchantment(
                    seeded(seed), new ItemStack(Items.DIAMOND_SWORD),
                    40, 30F, 50F, 0F, false, Set.of(), registry);
            if (selection.isEmpty()) {
                continue;
            }
            RealEnchantmentHelper.ClueBuild result =
                    RealEnchantmentHelper.buildClueList(seeded(seed), selection, 3);
            assertTrue(containsByKeyAndLevel(selection, result.clues().get(0)),
                    "first clue must be one of the selected enchants for seed " + seed);
        }
    }

    // ---- Remaining-clue fill ----

    @Test
    void buildClueList_fillsUpToCluesCount_fromRemainingPool() {
        List<EnchantmentInstance> pool = pool3();
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(1234L), pool, 3);
        assertEquals(3, result.clues().size(), "clues must be filled to cluesCount when pool allows");
        // Every clue should be from the pool, and all three should be distinct.
        for (EnchantmentInstance clue : result.clues()) {
            assertTrue(containsByKeyAndLevel(pool, clue));
        }
        assertEquals(3, result.clues().stream()
                .map(i -> i.enchantment.unwrapKey().orElseThrow())
                .distinct()
                .count(),
                "clues must be distinct entries from the pool");
    }

    @Test
    void buildClueList_stopsAtCluesCount_whenPoolLarger() {
        List<EnchantmentInstance> pool = new ArrayList<>();
        for (ResourceKey<Enchantment> key : List.of(SHARPNESS, SMITE, BANE, MENDING, UNBREAKING)) {
            pool.add(new EnchantmentInstance(registry.getHolderOrThrow(key), 1));
        }
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(7L), pool, 2);
        assertEquals(2, result.clues().size(), "clue list must stop at cluesCount when pool has more");
        assertFalse(result.exhaustedList(),
                "exhaustedList must be false when pool still has entries left");
    }

    // ---- Exhaustion flag ----

    @Test
    void buildClueList_cluesExceedPool_reportsExhausted() {
        List<EnchantmentInstance> pool = List.of(
                new EnchantmentInstance(registry.getHolderOrThrow(SHARPNESS), 1),
                new EnchantmentInstance(registry.getHolderOrThrow(SMITE), 1));
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(0L), pool, 5);
        assertEquals(2, result.clues().size(),
                "clue list is capped by pool size when cluesCount exceeds pool");
        assertTrue(result.exhaustedList(),
                "exhaustedList must be true when the pool was fully drained");
    }

    @Test
    void buildClueList_cluesExactlyFillPool_reportsExhausted() {
        List<EnchantmentInstance> pool = pool3();
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(0L), pool, 3);
        assertEquals(3, result.clues().size());
        assertTrue(result.exhaustedList(),
                "pool exactly matching cluesCount drains the pool → exhausted=true");
    }

    // ---- Edge cases ----

    @Test
    void buildClueList_emptyPool_returnsNullPrimaryAndExhausted() {
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(42L), List.of(), 3);
        assertNull(result.primary());
        assertTrue(result.clues().isEmpty());
        assertTrue(result.exhaustedList());
    }

    @Test
    void buildClueList_nullPool_returnsNullPrimaryAndExhausted() {
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(42L), null, 3);
        assertNull(result.primary());
        assertTrue(result.clues().isEmpty());
        assertTrue(result.exhaustedList());
    }

    @Test
    void buildClueList_cluesCountZero_picksPrimaryButReturnsEmptyList() {
        // Match Zenith: primary is always picked when the pool is non-empty (so the slot has
        // something to display), but the clue list stays empty when cluesCount is 0.
        List<EnchantmentInstance> pool = pool3();
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(0L), pool, 0);
        assertNotNull(result.primary(), "primary is picked even when cluesCount is 0");
        assertTrue(result.clues().isEmpty(), "clue list is empty when cluesCount is 0");
    }

    @Test
    void buildClueList_cluesCountNegative_treatedAsZero() {
        List<EnchantmentInstance> pool = pool3();
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(0L), pool, -5);
        assertNotNull(result.primary());
        assertTrue(result.clues().isEmpty());
    }

    @Test
    void buildClueList_singleEntryPool_primaryIsTheOnlyEntry() {
        EnchantmentInstance only = new EnchantmentInstance(registry.getHolderOrThrow(SHARPNESS), 3);
        RealEnchantmentHelper.ClueBuild result =
                RealEnchantmentHelper.buildClueList(seeded(0L), List.of(only), 3);
        assertEquals(only, result.primary());
        assertEquals(1, result.clues().size());
        assertEquals(only, result.clues().get(0));
        assertTrue(result.exhaustedList());
    }

    @Test
    void buildClueList_doesNotMutateInput() {
        List<EnchantmentInstance> pool = new ArrayList<>(pool3());
        List<EnchantmentInstance> snapshot = List.copyOf(pool);
        RealEnchantmentHelper.buildClueList(seeded(1L), pool, 3);
        assertEquals(snapshot, pool, "buildClueList must not mutate the caller's pool list");
    }

    @Test
    void buildClueList_sameSeedSamePool_isDeterministic() {
        List<EnchantmentInstance> pool = pool3();
        RealEnchantmentHelper.ClueBuild a =
                RealEnchantmentHelper.buildClueList(seeded(99L), pool, 3);
        RealEnchantmentHelper.ClueBuild b =
                RealEnchantmentHelper.buildClueList(seeded(99L), pool, 3);
        assertEquals(a.primary(), b.primary());
        assertEquals(a.clues(), b.clues());
        assertEquals(a.exhaustedList(), b.exhaustedList());
    }

    // ---- Fixtures ----

    private static List<EnchantmentInstance> pool3() {
        return List.of(
                new EnchantmentInstance(registry.getHolderOrThrow(SHARPNESS), 3),
                new EnchantmentInstance(registry.getHolderOrThrow(SMITE), 2),
                new EnchantmentInstance(registry.getHolderOrThrow(BANE), 1));
    }

    private static boolean containsByKeyAndLevel(List<EnchantmentInstance> pool, EnchantmentInstance target) {
        if (target == null) return false;
        ResourceKey<Enchantment> targetKey = target.enchantment.unwrapKey().orElseThrow();
        for (EnchantmentInstance inst : pool) {
            ResourceKey<Enchantment> key = inst.enchantment.unwrapKey().orElseThrow();
            if (key.equals(targetKey) && inst.level == target.level) {
                return true;
            }
        }
        return false;
    }

    private static RandomSource seeded(long seed) {
        RandomSource rand = RandomSource.create();
        rand.setSeed(seed);
        return rand;
    }

    private static Registry<Enchantment> buildTestRegistry() {
        MappedRegistry<Enchantment> reg = newRegistry();

        HolderSet<Item> swordItems = itemHolderSet(Items.DIAMOND_SWORD, Items.WOODEN_SWORD);
        HolderSet<Item> anyItems = itemHolderSet(
                Items.DIAMOND_SWORD, Items.WOODEN_SWORD, Items.DIAMOND_PICKAXE, Items.BOOK);

        Holder.Reference<Enchantment> sharpness = register(reg, SHARPNESS, synthetic(swordItems, 10, 5));
        Holder.Reference<Enchantment> smite = register(reg, SMITE, synthetic(swordItems, 5, 5));
        Holder.Reference<Enchantment> bane = register(reg, BANE, synthetic(swordItems, 5, 5));
        Holder.Reference<Enchantment> unbreaking = register(reg, UNBREAKING, synthetic(anyItems, 5, 3));
        Holder.Reference<Enchantment> mending = register(reg, MENDING, synthetic(anyItems, 2, 1));

        List<Holder<Enchantment>> inTable = List.of(sharpness, smite, bane, unbreaking, mending);
        List<Holder<Enchantment>> treasure = List.of(mending);
        reg.bindTags(Map.of(
                EnchantmentTags.IN_ENCHANTING_TABLE, inTable,
                EnchantmentTags.TREASURE, treasure
        ));
        return reg.freeze();
    }
}
