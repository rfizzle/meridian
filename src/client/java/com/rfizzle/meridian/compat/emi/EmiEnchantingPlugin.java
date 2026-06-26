package com.rfizzle.meridian.compat.emi;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.api.IEnchantingStatProvider;
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
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Registers one {@link EmiInfoRecipe} per shelf block so hovering a shelf in EMI reveals its
     * stat contribution.
     *
     * <p>Two passes keep singleplayer and dedicated-server clients correct:
     * <ol>
     *   <li><b>Meridian-shelf pass</b> — iterates every {@link IEnchantingStatProvider} block in
     *       {@link MeridianRegistry#BLOCKS} and registers a lazy info recipe whose text is
     *       resolved from {@link EnchantingStatRegistry#blockEntries()} at render time. This
     *       ensures panels exist even when the registry has not yet been populated (dedicated
     *       server, before the S2C sync arrives).</li>
     *   <li><b>Fallback pass</b> — iterates any remaining {@link EnchantingStatRegistry#blockEntries()}
     *       entries not already covered (vanilla blocks such as {@code amethyst_cluster}) and
     *       registers eager info recipes using the stat text already in the registry. These are
     *       populated only when the registry is non-empty (singleplayer / LAN host), preserving
     *       the previous singleplayer behaviour for non-Meridian blocks.</li>
     * </ol>
     *
     * <p>Tag-keyed entries are skipped — their stats flow through whatever concrete block the tag
     * targets.
     */
    private static void registerShelfInfoPanels(EmiRegistry registry) {
        Set<ResourceLocation> covered = new HashSet<>();

        for (Map.Entry<ResourceLocation, Block> entry : MeridianRegistry.BLOCKS.entrySet()) {
            if (!(entry.getValue() instanceof IEnchantingStatProvider)) {
                continue;
            }
            ResourceLocation blockId = entry.getKey();
            covered.add(blockId);
            EmiIngredient stack = EmiStack.of(entry.getValue());
            Component lazyText = lazyShelfComponent(blockId);
            registry.addRecipe(new EmiInfoRecipe(
                    List.of(stack),
                    List.of(lazyText),
                    Meridian.id("/shelf_info/" + blockId.getNamespace() + "/" + blockId.getPath())));
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

    /**
     * Returns a lazy {@link MutableComponent} whose text content is resolved from
     * {@link EnchantingStatRegistry#blockEntries()} at render time (each call to
     * {@link ComponentContents#visit}). Stat lines are joined with {@code \n} so EMI's
     * word-wrap renders them as separate visual lines.
     */
    private static MutableComponent lazyShelfComponent(ResourceLocation blockId) {
        return MutableComponent.create(new ComponentContents() {
            @Override
            public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
                return visitor.accept(computeText());
            }

            @Override
            public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
                return visitor.accept(style, computeText());
            }

            private String computeText() {
                EnchantingStats stats = EnchantingStatRegistry.getInstance().blockEntries()
                        .getOrDefault(blockId, EnchantingStats.ZERO);
                return String.join("\n", RecipeInfoFormatter.shelfStatLines(stats));
            }

            @Override
            public String toString() {
                return "lazyShelfStats(" + blockId + ")";
            }
        });
    }
}

