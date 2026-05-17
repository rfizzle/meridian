// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.anvil;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.rfizzle.meridian.TestRegistryFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TomeHandlerLogicTest {

    private static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    private static final ResourceKey<Enchantment> MENDING = key("mending");
    private static final ResourceKey<Enchantment> UNBREAKING = key("unbreaking");

    private static Holder.Reference<Enchantment> sharpnessHolder;
    private static Holder.Reference<Enchantment> mendingHolder;
    private static Holder.Reference<Enchantment> unbreakingHolder;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        buildRegistry();
    }

    // --- ScrapTomeHandler: sortedKeys ---

    @Test
    void sortedKeys_producesConsistentOrdering() {
        ItemEnchantments enchants = buildEnchantments(
                Map.of(sharpnessHolder, 5, mendingHolder, 1, unbreakingHolder, 3));

        List<Holder<Enchantment>> sorted = ScrapTomeHandler.sortedKeys(enchants);

        assertEquals(3, sorted.size());
        for (int i = 0; i < sorted.size() - 1; i++) {
            String a = sorted.get(i).unwrapKey().map(k -> k.location().toString()).orElse("");
            String b = sorted.get(i + 1).unwrapKey().map(k -> k.location().toString()).orElse("");
            assertTrue(a.compareTo(b) <= 0,
                    "keys must be sorted by resource location: " + a + " vs " + b);
        }
    }

    @Test
    void sortedKeys_singleEntry_returnsSingleton() {
        ItemEnchantments enchants = buildEnchantments(Map.of(sharpnessHolder, 3));
        List<Holder<Enchantment>> sorted = ScrapTomeHandler.sortedKeys(enchants);
        assertEquals(1, sorted.size());
        assertTrue(sorted.get(0).is(SHARPNESS));
    }

    // --- ScrapTomeHandler: seedFor ---

    @Test
    void seedFor_nullPlayer_isDeterministic() {
        ItemEnchantments enchants = buildEnchantments(
                Map.of(sharpnessHolder, 5, mendingHolder, 1));
        List<Holder<Enchantment>> candidates = ScrapTomeHandler.sortedKeys(enchants);

        long seed1 = ScrapTomeHandler.seedFor(null, candidates);
        long seed2 = ScrapTomeHandler.seedFor(null, candidates);
        assertEquals(seed1, seed2, "same candidate list must produce same seed");
    }

    @Test
    void seedFor_differentCandidates_differentSeeds() {
        List<Holder<Enchantment>> single = ScrapTomeHandler.sortedKeys(
                buildEnchantments(Map.of(sharpnessHolder, 5)));
        List<Holder<Enchantment>> multi = ScrapTomeHandler.sortedKeys(
                buildEnchantments(Map.of(sharpnessHolder, 5, mendingHolder, 1)));

        assertNotEquals(ScrapTomeHandler.seedFor(null, single),
                ScrapTomeHandler.seedFor(null, multi),
                "different enchantment sets must produce different seeds");
    }

    // --- ScrapTomeHandler: seeded pick reproducibility ---

    @Test
    void seededPick_isDeterministic() {
        ItemEnchantments enchants = buildEnchantments(
                Map.of(sharpnessHolder, 5, mendingHolder, 1, unbreakingHolder, 3));
        List<Holder<Enchantment>> candidates = ScrapTomeHandler.sortedKeys(enchants);
        long seed = ScrapTomeHandler.seedFor(null, candidates);

        int pick1 = new Random(seed).nextInt(candidates.size());
        int pick2 = new Random(seed).nextInt(candidates.size());
        assertEquals(pick1, pick2, "same seed must always pick the same index");
    }

    @Test
    void seededPick_indexWithinBounds() {
        ItemEnchantments enchants = buildEnchantments(
                Map.of(sharpnessHolder, 5, mendingHolder, 1, unbreakingHolder, 3));
        List<Holder<Enchantment>> candidates = ScrapTomeHandler.sortedKeys(enchants);
        long seed = ScrapTomeHandler.seedFor(null, candidates);

        int pick = new Random(seed).nextInt(candidates.size());
        assertTrue(pick >= 0 && pick < candidates.size(),
                "pick index must be in [0, " + candidates.size() + ")");
        assertNotNull(candidates.get(pick).unwrapKey().orElse(null));
    }

    // --- ExtractionTomeHandler: stripAndDamage ---

    @Test
    void stripAndDamage_removesEnchantments() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.ENCHANTMENTS,
                buildEnchantments(Map.of(sharpnessHolder, 5, unbreakingHolder, 3)));

        ItemStack result = ExtractionTomeHandler.stripAndDamage(sword, 50);

        ItemEnchantments resultEnchants = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        assertTrue(resultEnchants.isEmpty(), "all enchantments must be stripped");
    }

    @Test
    void stripAndDamage_appliesDamage() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        ItemStack result = ExtractionTomeHandler.stripAndDamage(sword, 200);

        assertEquals(200, result.getDamageValue(),
                "damage delta should be applied to fresh item");
    }

    @Test
    void stripAndDamage_clampsDamageToMaxMinusOne() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        int maxDmg = sword.getMaxDamage();

        ItemStack result = ExtractionTomeHandler.stripAndDamage(sword, maxDmg + 100);

        assertEquals(maxDmg - 1, result.getDamageValue(),
                "damage must be clamped to maxDamage - 1");
    }

    @Test
    void stripAndDamage_zeroDelta_noDamage() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        ItemStack result = ExtractionTomeHandler.stripAndDamage(sword, 0);

        assertEquals(0, result.getDamageValue(),
                "zero delta must not apply damage");
    }

    @Test
    void stripAndDamage_nonDamageableItem_noDamage() {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS,
                buildEnchantments(Map.of(sharpnessHolder, 3)));

        ItemStack result = ExtractionTomeHandler.stripAndDamage(book, 200);

        assertFalse(result.isDamageableItem(), "book is not damageable");
        assertTrue(result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
                "stored enchantments must be stripped from book");
    }

    @Test
    void stripAndDamage_preservesItemType() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        ItemStack result = ExtractionTomeHandler.stripAndDamage(sword, 50);

        assertEquals(Items.DIAMOND_SWORD, result.getItem());
        assertEquals(1, result.getCount());
    }

    @Test
    void stripAndDamage_doesNotMutateInput() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.ENCHANTMENTS,
                buildEnchantments(Map.of(sharpnessHolder, 5)));

        ExtractionTomeHandler.stripAndDamage(sword, 200);

        assertEquals(5, sword.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                        .getLevel(sharpnessHolder),
                "original stack must not be mutated");
        assertEquals(0, sword.getDamageValue(), "original damage must be unchanged");
    }

    // --- fixtures ---

    private static ItemEnchantments buildEnchantments(Map<Holder<Enchantment>, Integer> entries) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        entries.forEach(mutable::set);
        return mutable.toImmutable();
    }

    private static void buildRegistry() {
        MappedRegistry<Enchantment> reg = newRegistry();
        HolderSet<Item> anyItems = itemHolderSet(Items.DIAMOND_SWORD, Items.BOOK);

        sharpnessHolder = register(reg, SHARPNESS, synthetic(anyItems, 10, 5));
        mendingHolder = register(reg, MENDING, synthetic(anyItems, 2, 1));
        unbreakingHolder = register(reg, UNBREAKING, synthetic(anyItems, 5, 3));
        reg.freeze();
    }
}
