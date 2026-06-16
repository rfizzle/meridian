package com.rfizzle.meridian.compat.jei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import mezz.jei.api.recipe.RecipeType;

/**
 * Registry of JEI {@link RecipeType} constants used to map the shared
 * {@link EnchantmentBrowserRecord} record so the category code never has to reach at the underlying
 * vanilla {@link net.minecraft.world.item.enchantment.Enchantment} instance.
 */
public final class JeiEnchantmentBrowserRecipeTypes {

    public static final RecipeType<EnchantmentBrowserRecord> ENCHANTMENTS = RecipeType.create(
            Meridian.MOD_ID,
            "enchantments",
            EnchantmentBrowserRecord.class);

    private JeiEnchantmentBrowserRecipeTypes() {
    }
}
