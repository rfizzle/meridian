// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.anvil;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.rfizzle.meridian.TestRegistryFixture.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrismaticWebCurseLogicTest {

    private static final ResourceKey<Enchantment> SHARPNESS = key("sharpness");
    private static final ResourceKey<Enchantment> VANISHING = key("vanishing_curse");
    private static final ResourceKey<Enchantment> BINDING = key("binding_curse");

    private static Holder.Reference<Enchantment> sharpnessHolder;
    private static Holder.Reference<Enchantment> vanishingHolder;
    private static Holder.Reference<Enchantment> bindingHolder;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        buildRegistry();
    }

    @Test
    void hasAnyCurse_withCurse_returnsTrue() {
        ItemEnchantments enchants = buildEnchantments(Map.of(vanishingHolder, 1, sharpnessHolder, 3));
        assertTrue(PrismaticWebHandler.hasAnyCurse(enchants));
    }

    @Test
    void hasAnyCurse_withoutCurse_returnsFalse() {
        ItemEnchantments enchants = buildEnchantments(Map.of(sharpnessHolder, 5));
        assertFalse(PrismaticWebHandler.hasAnyCurse(enchants));
    }

    @Test
    void hasAnyCurse_empty_returnsFalse() {
        assertFalse(PrismaticWebHandler.hasAnyCurse(ItemEnchantments.EMPTY));
    }

    @Test
    void stripCurses_removesCursePreservesNonCurse() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.ENCHANTMENTS,
                buildEnchantments(Map.of(vanishingHolder, 1, sharpnessHolder, 3)));

        ItemStack result = PrismaticWebHandler.stripCurses(sword);

        ItemEnchantments resultEnchants = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        assertEquals(0, resultEnchants.getLevel(vanishingHolder),
                "vanishing curse must be removed");
        assertEquals(3, resultEnchants.getLevel(sharpnessHolder),
                "sharpness must be preserved at original level");
    }

    @Test
    void stripCurses_removesAllCurses() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.ENCHANTMENTS,
                buildEnchantments(Map.of(vanishingHolder, 1, bindingHolder, 1, sharpnessHolder, 5)));

        ItemStack result = PrismaticWebHandler.stripCurses(sword);

        ItemEnchantments resultEnchants = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        assertEquals(0, resultEnchants.getLevel(vanishingHolder));
        assertEquals(0, resultEnchants.getLevel(bindingHolder));
        assertEquals(5, resultEnchants.getLevel(sharpnessHolder));
    }

    @Test
    void stripCurses_doesNotMutateInput() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemEnchantments original = buildEnchantments(Map.of(vanishingHolder, 1, sharpnessHolder, 3));
        sword.set(DataComponents.ENCHANTMENTS, original);

        PrismaticWebHandler.stripCurses(sword);

        ItemEnchantments afterStrip = sword.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        assertEquals(1, afterStrip.getLevel(vanishingHolder),
                "original stack must not be mutated");
    }

    @Test
    void handler_nullConfig_declines() {
        PrismaticWebHandler handler = new PrismaticWebHandler(() -> null);
        assertTrue(handler.handle(new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY, null).isEmpty());
    }

    @Test
    void handler_configDisabled_declines() {
        PrismaticWebHandler handler = new PrismaticWebHandler(() -> {
            var config = new com.rfizzle.meridian.config.MeridianConfig();
            config.anvil.prismaticWebRemovesCurses = false;
            return config;
        });
        assertTrue(handler.handle(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.STRING), null).isEmpty());
    }

    @Test
    void handler_emptySlots_declines() {
        PrismaticWebHandler handler = new PrismaticWebHandler(
                com.rfizzle.meridian.config.MeridianConfig::new);
        assertTrue(handler.handle(ItemStack.EMPTY, new ItemStack(Items.STRING), null).isEmpty());
        assertTrue(handler.handle(new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY, null).isEmpty());
    }

    // --- fixtures ---

    private static ItemEnchantments buildEnchantments(Map<Holder<Enchantment>, Integer> entries) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        entries.forEach(mutable::set);
        return mutable.toImmutable();
    }

    private static void buildRegistry() {
        MappedRegistry<Enchantment> reg = newRegistry();
        HolderSet<Item> swordItems = itemHolderSet(Items.DIAMOND_SWORD);

        sharpnessHolder = register(reg, SHARPNESS, synthetic(swordItems, 10, 5));
        vanishingHolder = register(reg, VANISHING, synthetic(swordItems, 1, 1));
        bindingHolder = register(reg, BINDING, synthetic(swordItems, 1, 1));

        reg.bindTags(Map.of(
                EnchantmentTags.CURSE, List.of(vanishingHolder, bindingHolder)
        ));
        reg.freeze();
    }
}
