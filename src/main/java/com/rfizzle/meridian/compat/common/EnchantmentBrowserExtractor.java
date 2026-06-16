package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.api.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.PowerFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link EnchantmentBrowserRecord} entries for all registered enchantments.
 * Resolves exclusivity tags, config overrides, and power thresholds per level.
 */
public final class EnchantmentBrowserExtractor {

    private EnchantmentBrowserExtractor() {
    }

    public static List<EnchantmentBrowserRecord> extract(HolderLookup.Provider registries) {
        HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Map<ResourceKey<Enchantment>, EnchantmentInfo> infoMap = EnchantmentInfoRegistry.getAll();

        List<EnchantmentBrowserRecord> records = new ArrayList<>();

        for (Holder.Reference<Enchantment> holder : lookup.listElements().toList()) {
            EnchantmentInfo info = infoMap.get(holder.key());
            if (info == null) {
                info = EnchantmentInfo.fallback(holder);
            }

            records.add(toRecord(holder, info));
        }

        records.sort(Comparator.comparing(r -> r.ench().getRegisteredName()));
        return records;
    }

    private static EnchantmentBrowserRecord toRecord(Holder.Reference<Enchantment> holder, EnchantmentInfo info) {
        List<String> exclusiveSetNames = holder.tags()
                .filter(tag -> tag.location().getPath().startsWith("exclusive_set/"))
                .map(tag -> tag.location().getPath().substring("exclusive_set/".length()))
                .distinct()
                .sorted()
                .toList();

        boolean isTreasure = holder.is(EnchantmentTags.TREASURE);
        int maxLevel = info.getMaxLevel();

        List<int[]> powerWindows = new ArrayList<>();
        for (int i = 1; i <= maxLevel; i++) {
            powerWindows.add(new int[]{info.getMinPower(i), info.getMaxPower(i)});
        }

        // getSupportedItems() is a tag-backed HolderSet (e.g. enchantable/mining). It can only be
        // dereferenced once datapack tags have bound — true at the recipe-registration timing this
        // extractor runs at, but not during the brief pre-world-join window. Degrade to an empty
        // item list there rather than failing the whole browser.
        List<Holder<Item>> compatibleItems = new ArrayList<>();
        try {
            holder.value().getSupportedItems().forEach(compatibleItems::add);
        } catch (IllegalStateException | UnsupportedOperationException tagsNotBound) {
            compatibleItems.clear();
        }

        return new EnchantmentBrowserRecord(
                holder,
                maxLevel,
                exclusiveSetNames,
                isConfigOverridden(holder, info),
                isTreasure,
                info.enabled(),
                powerWindows,
                compatibleItems
        );
    }

    private static boolean isConfigOverridden(Holder.Reference<Enchantment> holder, EnchantmentInfo info) {
        EnchantmentInfo fallback = EnchantmentInfo.fallback(holder);

        if (info.maxLevel() != fallback.maxLevel()) return true;
        if (info.maxLootLevel() != fallback.maxLootLevel()) return true;
        if (info.levelCap() != fallback.levelCap()) return true;
        if (info.enabled() != fallback.enabled()) return true;

        // Compare power functions by type first
        if (info.minPower().getType() != fallback.minPower().getType()) return true;
        if (info.maxPower().getType() != fallback.maxPower().getType()) return true;

        // For default types, they should match if they have the same config (which fallback has)
        // For non-default types, we already flagged it as overridden by the type check above if it changed from default.
        // If it was already non-default in fallback (which shouldn't happen), we'd need deeper comparison.
        // However, the spec says "presence of a non-default type field flags the override".
        if (info.minPower().getType() != PowerFunction.Type.DEFAULT_MIN) return true;
        if (info.maxPower().getType() != PowerFunction.Type.DEFAULT_MAX) return true;

        return false;
    }
}
