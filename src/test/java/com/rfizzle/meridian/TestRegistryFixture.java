package com.rfizzle.meridian;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared test helpers for building synthetic enchantment registries.
 *
 * <p>Six test files previously duplicated the same {@code synthetic}, {@code register},
 * {@code itemHolderSet}, and {@code key} helpers. This utility class extracts those into
 * a single location so each test only keeps its own registry-population logic.
 */
public final class TestRegistryFixture {

    private TestRegistryFixture() {}

    /**
     * Creates a synthetic {@link Enchantment} with the given supported items, weight, and max level.
     *
     * <p>Uses {@code Component.literal("test")} as the description, {@code dynamicCost(1, 10)} /
     * {@code dynamicCost(51, 10)} cost functions, anvil cost 1, and {@link EquipmentSlotGroup#ANY}.
     */
    public static Enchantment synthetic(HolderSet<Item> supportedItems, int weight, int maxLevel) {
        Enchantment.EnchantmentDefinition def = Enchantment.definition(
                supportedItems,
                weight,
                maxLevel,
                Enchantment.dynamicCost(1, 10),
                Enchantment.dynamicCost(51, 10),
                1,
                EquipmentSlotGroup.ANY);
        return new Enchantment(
                Component.literal("test"),
                def,
                HolderSet.empty(),
                DataComponentMap.EMPTY);
    }

    /**
     * No-arg overload: weight=1, maxLevel=1, supported items = diamond sword only.
     */
    public static Enchantment synthetic() {
        HolderSet<Item> swordOnly = HolderSet.direct(List.of(
                BuiltInRegistries.ITEM.wrapAsHolder(Items.DIAMOND_SWORD)));
        return synthetic(swordOnly, 1, 1);
    }

    /**
     * Registers an enchantment in the given mutable registry with {@link RegistrationInfo#BUILT_IN}.
     */
    public static Holder.Reference<Enchantment> register(
            MappedRegistry<Enchantment> registry, ResourceKey<Enchantment> key, Enchantment ench) {
        return registry.register(key, ench, RegistrationInfo.BUILT_IN);
    }

    /**
     * Wraps the given items into a {@link HolderSet} via {@link BuiltInRegistries#ITEM}.
     */
    public static HolderSet<Item> itemHolderSet(Item... items) {
        List<Holder<Item>> holders = new ArrayList<>(items.length);
        for (Item item : items) {
            holders.add(BuiltInRegistries.ITEM.wrapAsHolder(item));
        }
        return HolderSet.direct(holders);
    }

    /**
     * Creates an enchantment {@link ResourceKey} under the {@code minecraft} namespace.
     */
    public static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath("minecraft", path));
    }

    /**
     * Creates a fresh, unfrozen {@link MappedRegistry} for enchantments.
     */
    public static MappedRegistry<Enchantment> newRegistry() {
        return new MappedRegistry<>(Registries.ENCHANTMENT, Lifecycle.stable());
    }
}
