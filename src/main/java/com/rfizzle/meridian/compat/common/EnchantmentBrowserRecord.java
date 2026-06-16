package com.rfizzle.meridian.compat.common;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/**
 * Provider-neutral view of an enchantment's metadata, power functions, and exclusivity groups.
 * Used by {@link EnchantmentBrowserExtractor} to seed the "Enchantments" browser category
 * in EMI, JEI, and REI.
 *
 * @param ench                the enchantment holder
 * @param maxLevel            the maximum level reachable (respecting config caps)
 * @param exclusiveSetNames   short names of exclusive tags (e.g. "damage", "mining")
 * @param isConfigOverridden  true if the enchantment's stats differ from vanilla defaults
 * @param isTreasure          true if the enchantment is tagged as treasure
 * @param isEnabled           false if the enchantment is disabled via config
 * @param powerWindows        list of [min, max] power thresholds per level (index = level-1)
 * @param compatibleItems     list of items that can naturally receive this enchantment
 */
public record EnchantmentBrowserRecord(
        Holder<Enchantment> ench,
        int maxLevel,
        List<String> exclusiveSetNames,
        boolean isConfigOverridden,
        boolean isTreasure,
        boolean isEnabled,
        List<int[]> powerWindows,
        List<Holder<Item>> compatibleItems
) {
}
