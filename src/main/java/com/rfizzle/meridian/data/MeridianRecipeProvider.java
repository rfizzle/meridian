package com.rfizzle.meridian.data;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.shelf.MeridianShelves;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Ports the vanilla-shape shelf recipes from Zenith 1:1 — same patterns, same keys, same results,
 * only the result namespace rewritten to {@code meridian}. Sources are under
 * {@code /home/rfizzle/Projects/Zenith/src/main/resources/data/zenith/recipes/} (see DESIGN.md
 * "Shelf Blocks").
 *
 * <p>Potion-based ingredients use Fabric's {@code DefaultCustomIngredients.components} in place of
 * Zenith's legacy {@code fabric:nbt} ingredient type — 1.21.1 stores potion identity as a
 * {@link PotionContents} data component, so the equivalent match is a component-patch predicate
 * against {@code minecraft:potion}. The Zenith regeneration/water/night-vision recipes route
 * through {@link #potionIngredient(Holder)}.
 *
 * <p>Every shelf that can be built from registered items gets its Zenith-matched shaped recipe
 * here, including the sculk-tier shelves ({@code echoing_sculkshelf},
 * {@code soul_touched_sculkshelf}) which use {@code warden_tendril}.
 *
 * <p>The Zenith {@code zenith:deepslate} item tag (used by {@code dormant_deepshelf}) ships
 * verbatim as {@code data/meridian/tags/item/deepslate.json}; see {@link #DEEPSLATE_TAG}.
 * The library and the three enchanting-table crafts
 * ({@code infused_hellshelf}, {@code infused_seashelf}, {@code deepshelf}) are hand-shipped —
 * custom-recipe-type JSONs land in Epic 4 (T-4.6.4), and the vanilla-shape {@code library} recipe
 * is hand-shipped in T-4.4.1.
 */
public class MeridianRecipeProvider extends FabricRecipeProvider {

    /**
     * Item tag covering every deepslate variant that can craft a {@code dormant_deepshelf} —
     * the same 8-entry list Zenith ships under {@code zenith:deepslate}. Backed by
     * {@code data/meridian/tags/item/deepslate.json}.
     */
    public static final TagKey<Item> DEEPSLATE_TAG =
            TagKey.create(Registries.ITEM, Meridian.id("deepslate"));

    public MeridianRecipeProvider(FabricDataOutput output,
                                CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void buildRecipes(RecipeOutput exporter) {
        // === Wood tier ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.BEESHELF)
                .pattern("CBC")
                .pattern("HSH")
                .pattern("CBC")
                .define('C', Items.HONEYCOMB)
                .define('B', Items.BEEHIVE)
                .define('H', Items.HONEY_BLOCK)
                .define('S', ConventionalItemTags.BOOKSHELVES)
                .unlockedBy("has_bookshelf", has(Items.BOOKSHELF))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.MELONSHELF)
                .pattern("MMM")
                .pattern("GSG")
                .pattern("MMM")
                .define('M', Items.MELON)
                .define('G', Items.GLISTERING_MELON_SLICE)
                .define('S', ConventionalItemTags.BOOKSHELVES)
                .unlockedBy("has_bookshelf", has(Items.BOOKSHELF))
                .save(exporter);

        // === Stone tier — baseline ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.STONESHELF)
                .pattern("EEE")
                .pattern("BBB")
                .pattern("EEE")
                .define('E', Items.POLISHED_ANDESITE)
                .define('B', Items.BOOK)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(exporter);

        // === Nether (hellshelf family — infused_hellshelf comes from enchanting-table craft) ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.HELLSHELF)
                .pattern("NNN")
                .pattern("BSP")
                .pattern("NNN")
                .define('B', Items.BLAZE_ROD)
                .define('N', Items.NETHER_BRICKS)
                .define('P', potionIngredient(Potions.REGENERATION))
                .define('S', ConventionalItemTags.BOOKSHELVES)
                .unlockedBy("has_blaze_rod", has(Items.BLAZE_ROD))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.BLAZING_HELLSHELF)
                .pattern(" F ")
                .pattern("FSF")
                .pattern("BBB")
                .define('F', Items.FIRE_CHARGE)
                .define('S', MeridianShelves.INFUSED_HELLSHELF)
                .define('B', Items.BLAZE_POWDER)
                .unlockedBy("has_infused_hellshelf", has(MeridianShelves.INFUSED_HELLSHELF))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.GLOWING_HELLSHELF)
                .pattern(" G ")
                .pattern(" S ")
                .pattern("G G")
                .define('G', Items.GLOWSTONE)
                .define('S', MeridianShelves.INFUSED_HELLSHELF)
                .unlockedBy("has_infused_hellshelf", has(MeridianShelves.INFUSED_HELLSHELF))
                .save(exporter);

        // === Ocean (seashelf family — infused_seashelf comes from enchanting-table craft) ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.SEASHELF)
                .pattern("NNN")
                .pattern("BSP")
                .pattern("NNN")
                .define('B', Items.PUFFERFISH)
                .define('N', Items.PRISMARINE_BRICKS)
                .define('P', potionIngredient(Potions.WATER))
                .define('S', ConventionalItemTags.BOOKSHELVES)
                .unlockedBy("has_pufferfish", has(Items.PUFFERFISH))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.HEART_SEASHELF)
                .pattern(" H ")
                .pattern("PSP")
                .pattern("PPP")
                .define('H', Items.HEART_OF_THE_SEA)
                .define('S', MeridianShelves.INFUSED_SEASHELF)
                .define('P', Items.PRISMARINE_SHARD)
                .unlockedBy("has_infused_seashelf", has(MeridianShelves.INFUSED_SEASHELF))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.CRYSTAL_SEASHELF)
                .pattern(" P ")
                .pattern(" S ")
                .pattern("P P")
                .define('P', Items.PRISMARINE_CRYSTALS)
                .define('S', MeridianShelves.INFUSED_SEASHELF)
                .unlockedBy("has_infused_seashelf", has(MeridianShelves.INFUSED_SEASHELF))
                .save(exporter);

        // === End family ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.ENDSHELF)
                .pattern("EEE")
                .pattern("IBP")
                .pattern("EEE")
                .define('E', Items.END_STONE_BRICKS)
                .define('I', MeridianRegistry.INFUSED_BREATH)
                .define('B', ConventionalItemTags.BOOKSHELVES)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_infused_breath", has(MeridianRegistry.INFUSED_BREATH))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.PEARL_ENDSHELF)
                .pattern("R R")
                .pattern("PSP")
                .pattern("R R")
                .define('R', Items.END_ROD)
                .define('S', MeridianShelves.ENDSHELF)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_endshelf", has(MeridianShelves.ENDSHELF))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.DRACONIC_ENDSHELF)
                .pattern(" H ")
                .pattern("PSP")
                .pattern("PPP")
                .define('H', Items.DRAGON_HEAD)
                .define('S', MeridianShelves.ENDSHELF)
                .define('P', Items.ENDER_PEARL)
                .unlockedBy("has_endshelf", has(MeridianShelves.ENDSHELF))
                .save(exporter);

        // === Deep family — deepshelf itself comes from enchanting-table craft (T-4.6.4) ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.DORMANT_DEEPSHELF)
                .pattern("EEE")
                .pattern("BBB")
                .pattern("EEE")
                .define('E', DEEPSLATE_TAG)
                .define('B', Items.BOOK)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.ECHOING_DEEPSHELF)
                .pattern(" E ")
                .pattern(" B ")
                .pattern("CCC")
                .define('E', Items.ECHO_SHARD)
                .define('B', MeridianShelves.DEEPSHELF)
                .define('C', ItemTags.CANDLES)
                .unlockedBy("has_deepshelf", has(MeridianShelves.DEEPSHELF))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.SOUL_TOUCHED_DEEPSHELF)
                .pattern(" E ")
                .pattern(" B ")
                .pattern("CCC")
                .define('E', Items.SOUL_LANTERN)
                .define('B', MeridianShelves.DEEPSHELF)
                .define('C', Items.SCULK)
                .unlockedBy("has_deepshelf", has(MeridianShelves.DEEPSHELF))
                .save(exporter);

        // === Sculk tier ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.ECHOING_SCULKSHELF)
                .pattern(" T ")
                .pattern("SBS")
                .pattern("SCS")
                .define('T', MeridianRegistry.WARDEN_TENDRIL)
                .define('S', Items.SCULK)
                .define('B', MeridianShelves.ECHOING_DEEPSHELF)
                .define('C', Items.SCULK_CATALYST)
                .unlockedBy("has_echoing_deepshelf", has(MeridianShelves.ECHOING_DEEPSHELF))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.SOUL_TOUCHED_SCULKSHELF)
                .pattern(" T ")
                .pattern("SBS")
                .pattern("SCS")
                .define('T', MeridianRegistry.WARDEN_TENDRIL)
                .define('S', Items.SCULK)
                .define('B', MeridianShelves.SOUL_TOUCHED_DEEPSHELF)
                .define('C', Items.SCULK_CATALYST)
                .unlockedBy("has_soul_touched_deepshelf", has(MeridianShelves.SOUL_TOUCHED_DEEPSHELF))
                .save(exporter);

        // === Utility — clue shelves (sightshelf tier) ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.SIGHTSHELF)
                .pattern("GHG")
                .pattern("PES")
                .pattern("GHG")
                .define('G', Items.GOLD_BLOCK)
                .define('H', MeridianShelves.INFUSED_HELLSHELF)
                .define('S', Items.SPYGLASS)
                .define('P', potionIngredient(Potions.NIGHT_VISION))
                .define('E', Items.ENDER_EYE)
                .unlockedBy("has_infused_hellshelf", has(MeridianShelves.INFUSED_HELLSHELF))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.SIGHTSHELF_T2)
                .pattern("GHG")
                .pattern("PEP")
                .pattern("GHG")
                .define('G', Items.EMERALD_BLOCK)
                .define('H', ConventionalItemTags.NETHERITE_INGOTS)
                .define('P', potionIngredient(Potions.LONG_NIGHT_VISION))
                .define('E', MeridianShelves.SIGHTSHELF)
                .unlockedBy("has_sightshelf", has(MeridianShelves.SIGHTSHELF))
                .save(exporter);

        // === Utility — rectifier tier ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.RECTIFIER)
                .pattern("AHA")
                .pattern("HSH")
                .pattern("AHA")
                .define('A', Items.AMETHYST_BLOCK)
                .define('H', Items.HONEYCOMB_BLOCK)
                .define('S', MeridianShelves.INFUSED_SEASHELF)
                .unlockedBy("has_infused_seashelf", has(MeridianShelves.INFUSED_SEASHELF))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.RECTIFIER_T2)
                .pattern("AHA")
                .pattern("HSH")
                .pattern("AHA")
                .define('A', Items.AMETHYST_BLOCK)
                .define('H', Items.GILDED_BLACKSTONE)
                .define('S', MeridianShelves.RECTIFIER)
                .unlockedBy("has_rectifier", has(MeridianShelves.RECTIFIER))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianShelves.RECTIFIER_T3)
                .pattern("AHA")
                .pattern("HSH")
                .pattern("AHA")
                .define('A', Items.AMETHYST_BLOCK)
                .define('H', Items.PURPUR_BLOCK)
                .define('S', MeridianShelves.RECTIFIER_T2)
                .unlockedBy("has_rectifier_t2", has(MeridianShelves.RECTIFIER_T2))
                .save(exporter);

        // === Utility — filtering shelf ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianRegistry.FILTERING_SHELF)
                .pattern("PSP")
                .pattern("LLL")
                .pattern("PSP")
                .define('P', Items.PRISMARINE_BRICKS)
                .define('S', MeridianShelves.INFUSED_SEASHELF)
                .define('L', Items.PRISMARINE_BRICK_SLAB)
                .unlockedBy("has_infused_seashelf", has(MeridianShelves.INFUSED_SEASHELF))
                .save(exporter);

        // === Utility — treasure shelf ===
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, MeridianRegistry.TREASURE_SHELF)
                .pattern("GSG")
                .pattern("DED")
                .pattern("GSG")
                .define('G', Items.GOLD_BLOCK)
                .define('S', MeridianShelves.DEEPSHELF)
                .define('D', ConventionalItemTags.DIAMOND_GEMS)
                .define('E', Items.EMERALD_BLOCK)
                .unlockedBy("has_deepshelf", has(MeridianShelves.DEEPSHELF))
                .save(exporter);

        // === Prismatic Web ===
        // Recipe ships hand-written at src/main/resources/data/meridian/recipe/prismatic_web.json
        // per T-4.1.3 (ported verbatim from Zenith). Not emitted here so datagen output stays scoped
        // to shelf crafts — the prismatic_web crafting is a single static file and re-emitting it
        // through ShapedRecipeBuilder would just add churn.
    }

    /**
     * Builds an {@link Ingredient} that matches a potion bottle whose
     * {@link net.minecraft.world.item.alchemy.PotionContents} identifies the requested potion.
     * Replaces Zenith 1.20.1's {@code fabric:nbt} strict-match ingredient, which predates 1.21's
     * component model.
     *
     * <p>Protected so unit tests can substitute a mixin-free stub — Fabric's custom-ingredient
     * machinery un-finals {@link Ingredient} through {@code IngredientMixin}, so classloading
     * {@code DefaultCustomIngredients.components} outside a running mod environment (e.g. plain
     * JUnit without Fabric's loader) throws {@link IncompatibleClassChangeError}. At datagen time
     * the mixin is applied and this method returns a real component-matching ingredient.
     */
    protected Ingredient potionIngredient(Holder<Potion> potion) {
        ItemStack stack = PotionContents.createItemStack(Items.POTION, potion);
        return DefaultCustomIngredients.components(stack);
    }
}
