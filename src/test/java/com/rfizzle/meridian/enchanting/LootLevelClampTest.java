// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.enchanting;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static com.rfizzle.meridian.TestRegistryFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit coverage for {@link RealEnchantmentHelper#clampToMaxLootLevel}, the pure post-processor
 * that enforces {@code maxLootLevel} on loot-rolled enchantments. Uses a stub cap lookup rather
 * than the live config so the cases stay deterministic and config-independent.
 */
class LootLevelClampTest {

    private static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    private static final ResourceKey<Enchantment> UNBREAKING = key("unbreaking");

    private static Holder<Enchantment> sharpness;
    private static Holder<Enchantment> unbreaking;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        MappedRegistry<Enchantment> reg = newRegistry();
        sharpness = register(reg, SHARPNESS, synthetic(itemHolderSet(Items.DIAMOND_SWORD), 10, 5));
        unbreaking = register(reg, UNBREAKING, synthetic(itemHolderSet(Items.DIAMOND_SWORD), 5, 5));
        reg.freeze();
    }

    @Test
    void capMinusOne_leavesListUnchanged() {
        List<EnchantmentInstance> input = List.of(
                new EnchantmentInstance(sharpness, 3),
                new EnchantmentInstance(unbreaking, 2));
        List<EnchantmentInstance> out = RealEnchantmentHelper.clampToMaxLootLevel(input, ench -> -1);
        // No-op returns the same reference so callers can cheaply detect "nothing changed".
        assertSame(input, out, "cap -1 must pass through the original list unchanged");
    }

    @Test
    void capTwo_clampsHigherLevelDown() {
        List<EnchantmentInstance> input = List.of(new EnchantmentInstance(sharpness, 3));
        List<EnchantmentInstance> out = RealEnchantmentHelper.clampToMaxLootLevel(input, ench -> 2);
        assertEquals(1, out.size(), "the clamped entry must survive");
        assertEquals(2, out.get(0).level, "a level-3 entry must clamp to the cap of 2");
        assertTrue(out.get(0).enchantment.is(SHARPNESS));
    }

    @Test
    void levelAtOrBelowCap_keptUnchanged() {
        List<EnchantmentInstance> input = List.of(new EnchantmentInstance(sharpness, 2));
        List<EnchantmentInstance> out = RealEnchantmentHelper.clampToMaxLootLevel(input, ench -> 2);
        assertSame(input, out, "an entry already at the cap must not be rewritten");
    }

    @Test
    void capZero_dropsEntry() {
        List<EnchantmentInstance> input = List.of(
                new EnchantmentInstance(sharpness, 3),
                new EnchantmentInstance(unbreaking, 1));
        // Sharpness clamps to 0 (dropped); Unbreaking keeps its vanilla pass-through.
        Function<Holder<Enchantment>, Integer> lookup =
                ench -> ench.is(SHARPNESS) ? 0 : -1;
        List<EnchantmentInstance> out = RealEnchantmentHelper.clampToMaxLootLevel(input, lookup);
        assertEquals(1, out.size(), "an entry clamped to 0 must be removed");
        assertTrue(out.get(0).enchantment.is(UNBREAKING), "the uncapped entry must remain");
    }

    @Test
    void emptyInput_producesEmptyOutput() {
        List<EnchantmentInstance> out =
                RealEnchantmentHelper.clampToMaxLootLevel(List.of(), ench -> 1);
        assertTrue(out.isEmpty(), "empty input must yield empty output");
    }

    @Test
    void nullInput_producesEmptyList() {
        List<EnchantmentInstance> out =
                RealEnchantmentHelper.clampToMaxLootLevel(null, ench -> 1);
        assertTrue(out.isEmpty(), "null input must yield an empty list");
    }
}
