package com.rfizzle.meridian.compat.rei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.RecipeInfoFormatter;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import com.rfizzle.meridian.compat.common.TableCraftingDisplayExtractor;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.plugin.common.displays.DefaultInformationDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;

/**
 * REI integration entry point. Mirrors the EMI plugin's single "Infusions" category and populates
 * it from the shared {@link TableCraftingDisplayExtractor} plus per-shelf stat info panels.
 *
 * <p>Only loads when REI itself loads it (via the {@code rei_client} entry point in
 * {@code fabric.mod.json}), so the REI classes imported here are safe even when REI is absent —
 * the entry point is never resolved.
 *
 * <p>Recipes come from {@link TableCraftingDisplayExtractor}; shelf info panels come from
 * {@link EnchantingStatRegistry#blockEntries()}. Both sources are shared with the EMI plugin so a
 * recipe added to the table only has to be plumbed through one place (S-7.2.2).
 */
public final class ReiEnchantingPlugin implements REIClientPlugin {

    public static final CategoryIdentifier<ReiEnchantingDisplay> INFUSIONS_ID =
            CategoryIdentifier.of(Meridian.MOD_ID, "infusions");

    @Override
    public String getPluginProviderName() {
        return Meridian.MOD_ID;
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new ReiEnchantingCategory(
                INFUSIONS_ID,
                Component.translatable("rei.meridian.category.infusions")));

        registry.addWorkstations(INFUSIONS_ID, EntryIngredients.of(new ItemStack(Items.ENCHANTING_TABLE)));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }
        for (TableCraftingDisplay display : TableCraftingDisplayExtractor.extract(client.level.getRecipeManager())) {
            registry.add(new ReiEnchantingDisplay(display, INFUSIONS_ID));
        }

        registerShelfInfoPanels(registry);
    }

    /**
     * Converts every block-keyed {@code enchanting_stats/*.json} entry into a
     * {@link DefaultInformationDisplay} so hovering a shelf in REI reveals its stat contribution.
     * Tag-keyed entries are skipped — their stats flow through whatever concrete block the tag
     * targets, and enumerating those would double-surface the same info.
     */
    private static void registerShelfInfoPanels(DisplayRegistry registry) {
        Map<ResourceLocation, EnchantingStats> entries = EnchantingStatRegistry.getInstance().blockEntries();
        for (Map.Entry<ResourceLocation, EnchantingStats> entry : entries.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getKey());
            if (block == Blocks.AIR) {
                continue;
            }
            DefaultInformationDisplay info = DefaultInformationDisplay.createFromEntry(
                    EntryStacks.of(block),
                    Component.translatable(block.getDescriptionId()));
            for (String line : RecipeInfoFormatter.shelfStatLines(entry.getValue())) {
                info.line(Component.literal(line));
            }
            registry.add(info);
        }
    }

}
