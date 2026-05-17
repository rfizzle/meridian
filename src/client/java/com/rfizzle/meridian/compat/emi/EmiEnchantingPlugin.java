package com.rfizzle.meridian.compat.emi;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.RecipeInfoFormatter;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import com.rfizzle.meridian.compat.common.TableCraftingDisplayExtractor;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Map;

/**
 * EMI integration entry point. Registers the "Infusions" display category and populates it with
 * the two shipped recipe types plus per-shelf stat info panels (T-7.1.3).
 *
 * <p>This class only loads when EMI itself loads it (via the {@code emi} entry point in
 * {@code fabric.mod.json}), so the static EMI references here are safe even when EMI is absent
 * from the runtime — Fabric never resolves the entry point.
 *
 * <p>Recipe extraction lives in {@link TableCraftingDisplayExtractor} so REI/JEI plugins share the
 * same source of truth (S-7.2.2).
 */
public final class EmiEnchantingPlugin implements EmiPlugin {

    private static final EmiTexture MOD_ICON = new EmiTexture(
            Meridian.id("icon.png"), 0, 0, 16, 16, 128, 128, 128, 128);

    public static final EmiRecipeCategory INFUSIONS = new EmiRecipeCategory(
            Meridian.id("infusions"),
            MOD_ICON);

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(INFUSIONS);
        registry.addWorkstation(INFUSIONS, EmiStack.of(Items.ENCHANTING_TABLE));

        for (TableCraftingDisplay display : TableCraftingDisplayExtractor.extract(registry.getRecipeManager())) {
            registry.addRecipe(new EmiEnchantingRecipe(INFUSIONS, display));
        }

        registerShelfInfoPanels(registry);
    }

    /**
     * Converts every block-keyed {@code enchanting_stats/*.json} entry into an {@link EmiInfoRecipe}
     * so hovering a shelf in EMI reveals its stat contribution. Tag-keyed entries are skipped —
     * their stats flow through whatever concrete block the tag targets, and enumerating those would
     * double-surface the same info.
     *
     * <p>In dedicated-multiplayer clients the server data listener has not populated the registry
     * at plugin-register time; {@link EnchantingStatRegistry#blockEntries()} returns an empty map
     * in that case and no info panels are emitted. Singleplayer and LAN hosts populate the map
     * from the integrated server's reload pass.
     */
    private static void registerShelfInfoPanels(EmiRegistry registry) {
        Map<ResourceLocation, EnchantingStats> entries = EnchantingStatRegistry.getInstance().blockEntries();
        for (Map.Entry<ResourceLocation, EnchantingStats> entry : entries.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getKey());
            if (block == Blocks.AIR) {
                continue;
            }
            List<Component> text = RecipeInfoFormatter.shelfStatLines(entry.getValue()).stream()
                    .map(Component::literal)
                    .map(c -> (Component) c)
                    .toList();
            EmiIngredient stack = EmiStack.of(block);
            registry.addRecipe(new EmiInfoRecipe(
                    List.of(stack),
                    text,
                    Meridian.id("/shelf_info/" + entry.getKey().getNamespace() + "/" + entry.getKey().getPath())));
        }
    }
}
