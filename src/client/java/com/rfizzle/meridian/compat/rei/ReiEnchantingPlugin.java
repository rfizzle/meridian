package com.rfizzle.meridian.compat.rei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.api.IEnchantingStatProvider;
import com.rfizzle.meridian.client.config.ClientMeridianConfig;
import com.rfizzle.meridian.compat.client.LazyShelfStatsContents;
import com.rfizzle.meridian.compat.common.AnvilInfoEntries;
import com.rfizzle.meridian.compat.common.AnvilInfoEntries.AnvilInfoEntry;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserExtractor;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
 *
 * <p>Unlike EMI, REI has no safe programmatic reload — only fragile staged-pipeline internals — so
 * the enchantment browser is not force-refreshed after a late sync. It relies on
 * {@link com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry} being populated before
 * {@link #registerDisplays} builds the list, which holds on first join (the common case); a live
 * {@code /reload} is picked up on rejoin or a manual resource reload (F3+T).
 */
public final class ReiEnchantingPlugin implements REIClientPlugin {

    public static final CategoryIdentifier<ReiEnchantingDisplay> INFUSIONS_ID =
            CategoryIdentifier.of(Meridian.MOD_ID, "infusions");

    public static final CategoryIdentifier<ReiEnchantmentBrowserDisplay> ENCHANTMENTS_ID =
            CategoryIdentifier.of(Meridian.MOD_ID, "enchantments");

    @Override
    public String getPluginProviderName() {
        return Meridian.MOD_ID;
    }

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new ReiEnchantingCategory(
                INFUSIONS_ID,
                Component.translatable("rei.meridian.category.infusions")));

        registry.add(new ReiEnchantmentBrowserCategory(
                ENCHANTMENTS_ID,
                Component.translatable("gui.meridian.enchant_info.title")));

        registry.addWorkstations(INFUSIONS_ID, EntryIngredients.of(new ItemStack(Items.ENCHANTING_TABLE)));
        registry.addWorkstations(ENCHANTMENTS_ID, EntryIngredients.of(new ItemStack(Items.ENCHANTING_TABLE)));
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }
        // Module-gated (#163). REI has no safe programmatic reload (see class javadoc), so a config
        // toggle flipped mid-session shows here only after rejoin or a manual resource reload
        // (F3+T) — same limitation as the enchantment browser below.
        for (TableCraftingDisplay display : TableCraftingDisplayExtractor.extract(
                client.level.getRecipeManager(), ClientMeridianConfig.effective())) {
            registry.add(new ReiEnchantingDisplay(display, INFUSIONS_ID));
        }

        for (EnchantmentBrowserRecord record : EnchantmentBrowserExtractor.extract(client.level.registryAccess())) {
            registry.add(new ReiEnchantmentBrowserDisplay(record, ENCHANTMENTS_ID));
        }

        registerShelfInfoPanels(registry);
        registerAnvilInfoPages(registry);
    }

    /**
     * Registers one {@link DefaultInformationDisplay} per anvil interaction (salvage tomes,
     * Prismatic Web, Tempered Core, iron-block repair) so each item's info page in REI names the
     * anvil as the place it is used. Sourced from the shared {@link AnvilInfoEntries#snapshot()}.
     */
    private static void registerAnvilInfoPages(DisplayRegistry registry) {
        for (AnvilInfoEntry entry : AnvilInfoEntries.snapshot()) {
            DefaultInformationDisplay info = DefaultInformationDisplay.createFromEntry(
                    EntryStacks.of(entry.item()),
                    entry.item().getHoverName());
            for (Component line : entry.description()) {
                info.line(line);
            }
            registry.add(info);
        }
    }

    /**
     * Registers one {@link DefaultInformationDisplay} per shelf block so hovering a shelf in REI
     * reveals its stat contribution.
     *
     * <p>Two passes keep singleplayer and dedicated-server clients correct:
     * <ol>
     *   <li><b>Meridian-shelf pass</b> — iterates every {@link IEnchantingStatProvider} block in
     *       {@link MeridianRegistry#BLOCKS} and registers a lazy display whose stat text is
     *       resolved from {@link EnchantingStatRegistry#blockEntries()} at render time.</li>
     *   <li><b>Fallback pass</b> — iterates remaining {@link EnchantingStatRegistry#blockEntries()}
     *       entries (vanilla blocks such as {@code amethyst_cluster}) and registers eager displays
     *       using already-populated registry data (singleplayer / LAN host only).</li>
     * </ol>
     */
    private static void registerShelfInfoPanels(DisplayRegistry registry) {
        Set<ResourceLocation> covered = new HashSet<>();

        for (Map.Entry<ResourceLocation, Block> entry : MeridianRegistry.BLOCKS.entrySet()) {
            if (!(entry.getValue() instanceof IEnchantingStatProvider)) {
                continue;
            }
            ResourceLocation blockId = entry.getKey();
            covered.add(blockId);
            DefaultInformationDisplay info = DefaultInformationDisplay.createFromEntry(
                    EntryStacks.of(entry.getValue()),
                    Component.translatable(entry.getValue().getDescriptionId()));
            info.line(lazyShelfComponent(blockId));
            registry.add(info);
        }

        for (Map.Entry<ResourceLocation, EnchantingStats> entry :
                EnchantingStatRegistry.getInstance().blockEntries().entrySet()) {
            if (covered.contains(entry.getKey())) {
                continue;
            }
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

    private static MutableComponent lazyShelfComponent(ResourceLocation blockId) {
        return LazyShelfStatsContents.component(blockId);
    }
}

