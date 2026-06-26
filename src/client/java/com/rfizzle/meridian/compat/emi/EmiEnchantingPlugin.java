package com.rfizzle.meridian.compat.emi;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserExtractor;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
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
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Map;

/**
 * EMI integration entry point. Registers the "Infusions" display category and populates it with
 * the two shipped recipe types plus per-shelf stat info panels.
 *
 * <p>This class only loads when EMI itself loads it (via the {@code emi} entry point in
 * {@code fabric.mod.json}), so the static EMI references here are safe even when EMI is absent
 * from the runtime — Fabric never resolves the entry point.
 *
 * <p>Recipe extraction lives in {@link TableCraftingDisplayExtractor} so REI/JEI plugins share the
 * same source of truth.
 */
public final class EmiEnchantingPlugin implements EmiPlugin {

    private static final EmiTexture MOD_ICON = new EmiTexture(
            Meridian.id("icon.png"), 0, 0, 16, 16, 256, 256, 256, 256);

    public static final EmiRecipeCategory INFUSIONS = new EmiRecipeCategory(
            Meridian.id("infusions"),
            MOD_ICON);

    public static final EmiRecipeCategory ENCHANTMENTS = new EmiRecipeCategory(
            Meridian.id("enchantments"),
            MOD_ICON);

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(INFUSIONS);
        registry.addWorkstation(INFUSIONS, EmiStack.of(Items.ENCHANTING_TABLE));

        for (TableCraftingDisplay display : TableCraftingDisplayExtractor.extract(registry.getRecipeManager())) {
            registry.addRecipe(new EmiEnchantingRecipe(INFUSIONS, display));
        }

        registry.addCategory(ENCHANTMENTS);
        registry.addWorkstation(ENCHANTMENTS, EmiStack.of(Items.ENCHANTING_TABLE));

        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null) {
            for (EnchantmentBrowserRecord record : EnchantmentBrowserExtractor.extract(client.level.registryAccess())) {
                registry.addRecipe(new EmiEnchantmentBrowserRecipe(ENCHANTMENTS, record));
            }
        }

        registerShelfInfoPanels(registry);
    }

    /** Latches off the first time the EMI reload symbol can't be resolved (e.g. a future EMI rename). */
    private static volatile boolean reloadUnavailable = false;

    /**
     * Triggers an EMI recipe reload so the enchantment browser repopulates with server-configured
     * values after {@link com.rfizzle.meridian.net.EnchantmentInfoPayload} has been applied: EMI
     * re-invokes {@link #register}, which now calls {@link EnchantmentBrowserExtractor#extract} with
     * the sync guard satisfied and emits the browser cards.
     *
     * <p>EMI's only reload entry point is the internal {@code dev.emi.emi.runtime.EmiReloadManager},
     * absent from the {@code :api} artifact this mod compiles against, so it is reached by reflection.
     * The call latches off on the first failure (a future EMI that moved or renamed the symbol), after
     * which the browser simply refreshes on rejoin or a manual resource reload (F3+T).
     */
    public static void notifySync() {
        if (reloadUnavailable) {
            return;
        }
        try {
            Class<?> reloadManager = Class.forName("dev.emi.emi.runtime.EmiReloadManager");
            MethodHandle reloadRecipes = MethodHandles.publicLookup()
                    .findStatic(reloadManager, "reloadRecipes", MethodType.methodType(void.class));
            reloadRecipes.invokeExact();
        } catch (Throwable t) {
            reloadUnavailable = true;
            Meridian.LOGGER.warn("EMI enchantment-browser reload unavailable; it will refresh on rejoin", t);
        }
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
