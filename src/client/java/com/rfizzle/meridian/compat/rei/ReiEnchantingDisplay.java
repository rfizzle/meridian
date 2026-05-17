package com.rfizzle.meridian.compat.rei;

import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import java.util.List;
import java.util.Optional;

/**
 * REI-facing adapter over {@link TableCraftingDisplay}. The shared record is built once by
 * {@link com.rfizzle.meridian.compat.common.TableCraftingDisplayExtractor} and wrapped
 * here so the REI plugin never touches the recipe types directly — that same record also feeds
 * the EMI plugin and the (upcoming) JEI plugin.
 *
 * <p>The category identifier is passed in at construction so the display class stays decoupled
 * from the plugin's category setup.
 */
public final class ReiEnchantingDisplay implements Display {

    private final TableCraftingDisplay source;
    private final CategoryIdentifier<ReiEnchantingDisplay> category;
    private final List<EntryIngredient> inputs;
    private final List<EntryIngredient> outputs;

    public ReiEnchantingDisplay(TableCraftingDisplay source, CategoryIdentifier<ReiEnchantingDisplay> category) {
        this.source = source;
        this.category = category;
        this.inputs = List.of(EntryIngredients.ofIngredient(source.input()));
        this.outputs = List.of(EntryIngredients.of(source.result()));
    }

    public TableCraftingDisplay source() {
        return source;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        return inputs;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return outputs;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    @Override
    public Optional<net.minecraft.resources.ResourceLocation> getDisplayLocation() {
        return Optional.of(source.recipeId());
    }
}
