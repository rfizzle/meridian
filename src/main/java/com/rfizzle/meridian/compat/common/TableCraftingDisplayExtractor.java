package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.config.MeridianConfig;
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
 * extractor feeds the EMI plugin, the REI plugin, and the JEI plugin.
 */
public final class TableCraftingDisplayExtractor {

    private TableCraftingDisplayExtractor() {
    }

    /**
     * Entries whose {@link com.rfizzle.meridian.enchanting.recipe.RecipeModule} is enabled in
     * {@code config} (#163) — the list a viewer should show. Client callers pass the synced
     * config ({@code ClientMeridianConfig.effective()}) so the server's toggles govern what the
     * viewers list.
     */
    public static List<TableCraftingDisplay> extract(RecipeManager recipes, MeridianConfig config) {
        List<TableCraftingDisplay> out = extractAll(recipes);
        out.removeIf(display -> !display.module().isEnabled(config));
        return out;
    }

    /**
     * Every entry regardless of module toggles. Only for callers that apply the module gate
     * themselves — the JEI plugin registers all entries once and hides/unhides by module at
     * runtime, since JEI can't re-register on a live config change.
     */
    public static List<TableCraftingDisplay> extractAll(RecipeManager recipes) {
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
                keepNbt,
                recipe.getModule());
    }
}
