package com.rfizzle.meridian.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.List;

/**
 * Facade for querying accessory/trinket slots. Delegates to {@link TrinketsCompat} when
 * the Trinkets mod is loaded, otherwise returns empty results. Safe to call unconditionally
 * — the Trinkets-dependent class is never loaded when the mod is absent.
 */
public final class AccessorySlotHelper {

    private static final boolean TRINKETS_LOADED =
            FabricLoader.getInstance().isModLoaded("trinkets");

    private AccessorySlotHelper() {}

    /**
     * Returns all non-empty item stacks currently in accessory/trinket slots.
     */
    public static List<ItemStack> getAccessoryItems(LivingEntity entity) {
        if (TRINKETS_LOADED) {
            return TrinketsCompat.getEquippedTrinkets(entity);
        }
        return List.of();
    }

    /**
     * Returns the highest level of {@code enchantment} found on any accessory item, or 0 if
     * none is found. Useful for enchantment effect handlers that should apply from trinket slots.
     */
    public static int getAccessoryEnchantmentLevel(Holder<Enchantment> enchantment, LivingEntity entity) {
        int max = 0;
        for (ItemStack stack : getAccessoryItems(entity)) {
            int level = EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
            if (level > max) {
                max = level;
            }
        }
        return max;
    }
}
