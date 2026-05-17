package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipeRegistry;
import com.rfizzle.meridian.enchanting.recipe.KeepNbtEnchantingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link TableCraftingDisplay} entries from a {@link RecipeManager}. Walks both shipped
 * recipe types ({@code meridian:enchanting} and {@code meridian:keep_nbt_enchanting})
 * and tags each entry with {@code keepNbt} so downstream viewers can render the
 * components-preserving variant differently.
 *
 * <p>Stays Minecraft-only on purpose — no recipe-viewer types in the signature — so the same
 * extractor feeds the EMI plugin (T-7.1.2), the REI plugin (S-7.2), and the JEI plugin (S-7.3).
 */
public final class TableCraftingDisplayExtractor {

    private TableCraftingDisplayExtractor() {
    }

    public static List<TableCraftingDisplay> extract(RecipeManager recipes) {
        List<TableCraftingDisplay> out = new ArrayList<>();
        for (RecipeHolder<EnchantingRecipe> holder : recipes.getAllRecipesFor(EnchantingRecipeRegistry.ENCHANTING_TYPE)) {
            out.add(toDisplay(holder, false));
        }
        for (RecipeHolder<KeepNbtEnchantingRecipe> holder : recipes.getAllRecipesFor(EnchantingRecipeRegistry.KEEP_NBT_TYPE)) {
            out.add(toDisplay(holder, true));
        }
        return out;
    }

    private static TableCraftingDisplay toDisplay(RecipeHolder<? extends EnchantingRecipe> holder, boolean keepNbt) {
        EnchantingRecipe recipe = holder.value();
        return new TableCraftingDisplay(
                holder.id(),
                recipe.getInput(),
                recipe.getResult().copy(),
                recipe.getRequirements(),
                recipe.getMaxRequirements(),
                recipe.getDisplayLevel(),
                recipe.getEffectiveXpCost(),
                keepNbt);
    }
}
