package com.rfizzle.meridian.enchanting;

import java.util.Optional;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;

/**
 * Kiln: transforms a mined block's drop into its furnace-smelted form. A thin shell over the
 * server's {@code RecipeManager} — a drop with no {@link RecipeType#SMELTING} recipe is returned
 * unchanged, so blocks that can't be smelted drop normally.
 */
public final class KilnSmelting {

    private KilnSmelting() {}

    /**
     * Returns the smelted form of {@code drop}, preserving the input's stack size (each input item
     * yields the recipe's result count). Returns the original stack when it is empty or has no
     * smelting recipe.
     */
    public static ItemStack smelt(ServerLevel level, ItemStack drop) {
        if (drop.isEmpty()) return drop;

        Optional<RecipeHolder<SmeltingRecipe>> recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(drop), level);
        if (recipe.isEmpty()) return drop;

        ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
        if (result.isEmpty()) return drop;

        return result.copyWithCount(result.getCount() * drop.getCount());
    }
}
