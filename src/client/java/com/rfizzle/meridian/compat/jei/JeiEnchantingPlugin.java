package com.rfizzle.meridian.compat.jei;

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
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JEI integration entry point. Mirrors the EMI/REI plugin's two categories: an "Infusions"
 * crafting category populated from the shared {@link TableCraftingDisplayExtractor}, and an
 * "Enchantments" browser category (per-enchantment max level, exclusive sets, treasure flag,
 * and power windows), plus one info page per shelf block drawn from
 * {@link EnchantingStatRegistry#blockEntries()}.
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

    /**
     * JEI runtime reference; {@code null} when JEI has not yet initialised or has shut down.
     * Callers must copy to a local variable before null-checking to avoid TOCTOU races
     * (see {@link #notifySync()}).
     */
    private static volatile IJeiRuntime jeiRuntime;

    /**
     * Recipes added through the JEI runtime (outside the normal plugin-init lifecycle). Tracked so
     * they can be hidden before re-adding on the next sync notification. Always reassigned to a new
     * immutable list ({@link List#of()} or the result of {@link #extractEnchantments()}), so the
     * {@code volatile} reference swap is the only synchronisation needed.
     */
    private static volatile List<EnchantmentBrowserRecord> runtimeAddedRecipes = List.of();

    /**
     * The exact infusion display instances handed to {@link #registerRecipes} — every entry, with
     * no module gate applied. JEI's hidden-recipe set matches via the display's
     * {@code equals}/{@code hashCode}, and {@link TableCraftingDisplay} is a record over
     * {@link net.minecraft.world.item.ItemStack}/{@code Ingredient} components that only compare by
     * reference — so the module filter in {@link #applyInfusionModuleFilter()} must hide/unhide
     * these same instances; a re-extracted copy would not match. Reassigned wholesale, never
     * mutated after the volatile publish.
     */
    private static volatile List<TableCraftingDisplay> registeredInfusions = List.of();

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        jeiRuntime = runtime;
        // registerRecipes registered every infusion display unfiltered; now that the runtime
        // exists, hide the ones whose module the effective config disables (#163).
        applyInfusionModuleFilter();
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
        runtimeAddedRecipes = List.of();
        registeredInfusions = List.of();
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

        registration.addRecipeCategories(new JeiEnchantmentBrowserCategory(
                JeiEnchantmentBrowserRecipeTypes.ENCHANTMENTS,
                Component.translatable("gui.meridian.enchant_info.title"),
                modIcon));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Register every infusion display, module toggles ignored: JEI cannot re-register on a
        // live config change, so the module gate is applied as hide/unhide against this full set
        // (see applyInfusionModuleFilter). Disabled entries are hidden as soon as the runtime is
        // available, before the player can browse.
        List<TableCraftingDisplay> infusions = extractDisplays();
        registeredInfusions = infusions;
        registration.addRecipes(JeiEnchantingRecipeTypes.INFUSIONS, infusions);
        registration.addRecipes(JeiEnchantmentBrowserRecipeTypes.ENCHANTMENTS, extractEnchantments());
        registerShelfInfoPanels(registration);
        registerAnvilInfoPages(registration);
    }

    /**
     * Registers one JEI info page per anvil interaction (salvage tomes, Prismatic Web, Tempered
     * Core, iron-block repair) so each item's page names the anvil as the place it is used. Sourced
     * from the shared {@link AnvilInfoEntries#snapshot()}.
     */
    private static void registerAnvilInfoPages(IRecipeRegistration registration) {
        for (AnvilInfoEntry entry : AnvilInfoEntries.snapshot()) {
            registration.addItemStackInfo(entry.item(), entry.description().toArray(Component[]::new));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ENCHANTING_TABLE), JeiEnchantingRecipeTypes.INFUSIONS);
        registration.addRecipeCatalyst(new ItemStack(Items.ENCHANTING_TABLE), JeiEnchantmentBrowserRecipeTypes.ENCHANTMENTS);
    }

    /**
     * Registers one JEI info page per shelf block so hovering a shelf in JEI reveals its stat
     * contribution.
     *
     * <p>Two passes keep singleplayer and dedicated-server clients correct:
     * <ol>
     *   <li><b>Meridian-shelf pass</b> — iterates every {@link IEnchantingStatProvider} block in
     *       {@link MeridianRegistry#BLOCKS} and registers a lazy info page whose stat text is
     *       resolved from {@link EnchantingStatRegistry#blockEntries()} at render time.</li>
     *   <li><b>Fallback pass</b> — iterates remaining {@link EnchantingStatRegistry#blockEntries()}
     *       entries (vanilla blocks such as {@code amethyst_cluster}) and registers eager info pages
     *       using already-populated registry data (singleplayer / LAN host only).</li>
     * </ol>
     *
     * <p>Tag-keyed entries are skipped — their stats flow through whatever concrete block the tag
     * targets, and enumerating those would double-surface the same info.
     */
    private static void registerShelfInfoPanels(IRecipeRegistration registration) {
        Set<ResourceLocation> covered = new HashSet<>();

        for (Map.Entry<ResourceLocation, Block> entry : MeridianRegistry.BLOCKS.entrySet()) {
            if (!(entry.getValue() instanceof IEnchantingStatProvider)) {
                continue;
            }
            ResourceLocation blockId = entry.getKey();
            covered.add(blockId);
            registration.addItemStackInfo(new ItemStack(entry.getValue()), lazyShelfComponent(blockId));
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
            Component[] lines = RecipeInfoFormatter.shelfStatLines(entry.getValue()).stream()
                    .map(Component::literal)
                    .toArray(Component[]::new);
            registration.addItemStackInfo(new ItemStack(block), lines);
        }
    }

    private static MutableComponent lazyShelfComponent(ResourceLocation blockId) {
        return LazyShelfStatsContents.component(blockId);
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
        return TableCraftingDisplayExtractor.extractAll(level.getRecipeManager());
    }

    private static List<EnchantmentBrowserRecord> extractEnchantments() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return List.of();
        }
        ClientLevel level = client.level;
        if (level == null) {
            return List.of();
        }
        return EnchantmentBrowserExtractor.extract(level.registryAccess());
    }

    /**
     * Triggered by {@link com.rfizzle.meridian.compat.client.ViewerRefreshTrigger} after
     * {@link com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry#applyFromPayload} completes.
     * Hides any recipes that were added via a previous runtime call (to avoid duplicates), then
     * adds a fresh set extracted from the now-populated registry.
     */
    public static void notifySync() {
        IJeiRuntime runtime = jeiRuntime;
        if (runtime == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return;
        }
        // Hide any recipes we added at runtime in an earlier sync notification.
        List<EnchantmentBrowserRecord> stale = runtimeAddedRecipes;
        if (!stale.isEmpty()) {
            runtime.getRecipeManager().hideRecipes(JeiEnchantmentBrowserRecipeTypes.ENCHANTMENTS, stale);
        }
        // Add the fresh set. extractEnchantments() now returns non-empty because hasSyncBeenReceived() == true.
        List<EnchantmentBrowserRecord> fresh = extractEnchantments();
        if (!fresh.isEmpty()) {
            runtime.getRecipeManager().addRecipes(JeiEnchantmentBrowserRecipeTypes.ENCHANTMENTS, fresh);
        }
        runtimeAddedRecipes = fresh;
    }

    /**
     * Applies the recipe-module config gate (#163) to the infusion category: hides every display
     * whose module the effective config disables and unhides the rest. Runs against the exact
     * instances registered in {@link #registerRecipes} because JEI's hidden set matches by
     * identity. Idempotent, so it is safe to run both from {@link #onRuntimeAvailable} (covers a
     * config sync that landed before JEI finished loading) and from
     * {@link com.rfizzle.meridian.compat.client.ViewerRefreshTrigger} after a config sync (covers
     * a live {@code /meridian reload} or ModMenu save), in either order.
     */
    public static void applyInfusionModuleFilter() {
        IJeiRuntime runtime = jeiRuntime;
        if (runtime == null) {
            return;
        }
        List<TableCraftingDisplay> all = registeredInfusions;
        if (all.isEmpty()) {
            return;
        }
        MeridianConfig config = ClientMeridianConfig.effective();
        List<TableCraftingDisplay> disabled = all.stream()
                .filter(display -> !display.module().isEnabled(config))
                .toList();
        List<TableCraftingDisplay> enabled = all.stream()
                .filter(display -> display.module().isEnabled(config))
                .toList();
        IRecipeManager recipeManager = runtime.getRecipeManager();
        if (!disabled.isEmpty()) {
            recipeManager.hideRecipes(JeiEnchantingRecipeTypes.INFUSIONS, disabled);
        }
        if (!enabled.isEmpty()) {
            recipeManager.unhideRecipes(JeiEnchantingRecipeTypes.INFUSIONS, enabled);
        }
    }
}

