package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.MeridianRegistry;
import net.minecraft.world.item.ItemStack;

/**
 * Shared classification helper for routing a {@link TableCraftingDisplay} into either the "Tomes"
 * tab or the "Shelves" tab of a recipe viewer. Every adapter (EMI / REI / JEI) needs the same
 * answer, so the decision lives in {@code main} sourceSet alongside the other viewer-neutral
 * compat plumbing rather than being re-implemented in each client-side plugin class.
 *
 * <p>The set of tome results is narrow by design: the three tier items {@code scrap_tome},
 * {@code improved_scrap_tome}, and {@code extraction_tome}. Any other recipe — even one that
 * consumes a tome — sorts into the Shelves tab because its <em>output</em> is the artifact the
 * player is looking up.
 */
public final class TomeRecipeClassifier {

    private TomeRecipeClassifier() {
    }

    public static boolean isTomeResult(ItemStack result) {
        return result.is(MeridianRegistry.SCRAP_TOME)
                || result.is(MeridianRegistry.IMPROVED_SCRAP_TOME)
                || result.is(MeridianRegistry.EXTRACTION_TOME);
    }

    public static boolean isTomeRecipe(TableCraftingDisplay display) {
        return isTomeResult(display.result());
    }
}
