package com.rfizzle.meridian.compat.rei;

import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.core.Holder;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

/**
 * REI-facing adapter over {@link EnchantmentBrowserRecord}.
 */
public final class ReiEnchantmentBrowserDisplay implements Display {

    private final EnchantmentBrowserRecord record;
    private final CategoryIdentifier<ReiEnchantmentBrowserDisplay> category;

    public ReiEnchantmentBrowserDisplay(EnchantmentBrowserRecord record, CategoryIdentifier<ReiEnchantmentBrowserDisplay> category) {
        this.record = record;
        this.category = category;
    }

    public EnchantmentBrowserRecord record() {
        return record;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        if (record.compatibleItems().isEmpty()) {
            return List.of();
        }
        return List.of(EntryIngredients.ofItems(
                record.compatibleItems().stream().<ItemLike>map(Holder::value).toList()));
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return List.of();
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

}
