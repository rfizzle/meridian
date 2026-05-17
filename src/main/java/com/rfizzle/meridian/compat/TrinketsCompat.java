package com.rfizzle.meridian.compat;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Trinkets API integration — only class-loaded when {@code trinkets} is present.
 * All Trinkets-dependent types are confined to this class so the rest of the mod
 * never triggers a {@link NoClassDefFoundError} when Trinkets is absent.
 */
public final class TrinketsCompat {

    private TrinketsCompat() {}

    /**
     * Returns all non-empty {@link ItemStack}s currently equipped in trinket/accessory slots.
     */
    public static List<ItemStack> getEquippedTrinkets(LivingEntity entity) {
        List<ItemStack> result = new ArrayList<>();
        TrinketsApi.getTrinketComponent(entity).ifPresent(component -> {
            for (Tuple<SlotReference, ItemStack> pair : component.getAllEquipped()) {
                ItemStack stack = pair.getB();
                if (!stack.isEmpty()) {
                    result.add(stack);
                }
            }
        });
        return result;
    }
}
