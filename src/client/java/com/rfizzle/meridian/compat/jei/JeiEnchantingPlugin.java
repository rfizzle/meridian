package com.rfizzle.meridian.compat.jei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.RecipeInfoFormatter;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import com.rfizzle.meridian.compat.common.TableCraftingDisplayExtractor;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.Map;

/**
 * JEI integration entry point. Mirrors the EMI/REI plugin's single "Infusions" category,
 * populated from the shared {@link TableCraftingDisplayExtractor}, plus one info page per shelf
 * block drawn from {@link EnchantingStatRegistry#blockEntries()}.
 *
 * <p>Discovered by JEI via the {@code jei_mod_plugin} entrypoint declared in
 * {@code fabric.mod.json}. The {@link JeiPlugin} annotation remains for cross-loader parity but
 * JEI-Fabric only uses the Fabric entrypoint to find the class — the annotation is load-bearing on
 * Forge/NeoForge only. JEI is client-only, so this class lives under {@code src/client/java/} and
 * its JEI imports are never resolved on a dedicated server.
 *
 * <p>This keeps the three viewer plugins functionally equivalent: the same recipes and the same
 * stat info panels regardless of which recipe viewer the player has installed.
 */
@JeiPlugin
public final class JeiEnchantingPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID = Meridian.id("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IDrawable modIcon = new IDrawable() {
            private static final ResourceLocation TEXTURE = Meridian.id("icon.png");
            @Override public int getWidth() { return 16; }
            @Override public int getHeight() { return 16; }
            @Override
            public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
                guiGraphics.blit(TEXTURE, xOffset, yOffset, 0, 0, 16, 16, 16, 16);
            }
        };
        registration.addRecipeCategories(new JeiEnchantingCategory(
                JeiEnchantingRecipeTypes.INFUSIONS,
                Component.translatable("jei.meridian.category.infusions"),
                modIcon));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(JeiEnchantingRecipeTypes.INFUSIONS, extractDisplays());
        registerShelfInfoPanels(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ENCHANTING_TABLE), JeiEnchantingRecipeTypes.INFUSIONS);
    }

    /**
     * Converts every block-keyed {@code enchanting_stats/*.json} entry into a JEI info page so
     * hovering a shelf in JEI reveals its stat contribution. Tag-keyed entries are skipped — their
     * stats flow through whatever concrete block the tag targets, and enumerating those would
     * double-surface the same info. Matches the EMI/REI panels line-for-line via
     * {@link RecipeInfoFormatter#shelfStatLines(EnchantingStats)}.
     *
     * <p>In a dedicated-multiplayer client the stat registry may not have been populated at plugin
     * register time; in that case {@link EnchantingStatRegistry#blockEntries()} returns an empty
     * map and no info pages are emitted — matches EMI's and REI's behavior.
     */
    private static void registerShelfInfoPanels(IRecipeRegistration registration) {
        Map<ResourceLocation, EnchantingStats> entries = EnchantingStatRegistry.getInstance().blockEntries();
        for (Map.Entry<ResourceLocation, EnchantingStats> entry : entries.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.get(entry.getKey());
            if (block == Blocks.AIR) {
                continue;
            }
            Component[] lines = RecipeInfoFormatter.shelfStatLines(entry.getValue()).stream()
                    .map(Component::literal)
                    .toArray(Component[]::new);
            registration.addItemStackInfo(new ItemStack(block), lines);
        }
    }

    /**
     * Resolves the live {@link RecipeManager} off the client world (same handle EMI / REI use).
     * Returns an empty list if the player hasn't joined a world yet — JEI will re-invoke
     * {@link #registerRecipes} on the next reload once a world is loaded.
     */
    private static List<TableCraftingDisplay> extractDisplays() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return List.of();
        }
        ClientLevel level = client.level;
        if (level == null) {
            return List.of();
        }
        return TableCraftingDisplayExtractor.extract(level.getRecipeManager());
    }
}
