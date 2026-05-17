package com.rfizzle.meridian.compat.jei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import mezz.jei.api.recipe.RecipeType;

/**
 * Holds the JEI {@link RecipeType} id for the single "Infusions" category. Wraps the shared
 * {@link TableCraftingDisplay} record so the category code never has to reach at the underlying
 * {@code meridian:enchanting} / {@code keep_nbt_enchanting} recipe types directly.
 */
public final class JeiEnchantingRecipeTypes {

    public static final RecipeType<TableCraftingDisplay> INFUSIONS = RecipeType.create(
            Meridian.MOD_ID,
            "infusions",
            TableCraftingDisplay.class);

    private JeiEnchantingRecipeTypes() {
    }
}
