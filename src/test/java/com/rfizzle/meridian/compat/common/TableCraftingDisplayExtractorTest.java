// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.KeepNbtEnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.StatRequirements;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableCraftingDisplayExtractorTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void extract_returnsBothRecipeTypesWithKeepNbtFlagged() {
        EnchantingRecipe shelfUpgrade = new EnchantingRecipe(
                Ingredient.of(Items.BOOKSHELF),
                new StatRequirements(22.5F, 30F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.ENCHANTING_TABLE),
                OptionalInt.of(5),
                3);
        KeepNbtEnchantingRecipe libraryUpgrade = new KeepNbtEnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(50F, 45F, 100F),
                new StatRequirements(50F, 50F, 100F),
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                7);
        RecipeManager rm = managerWith(
                holder("shelf_upgrade", shelfUpgrade),
                holder("library_upgrade", libraryUpgrade));

        List<TableCraftingDisplay> displays = TableCraftingDisplayExtractor.extract(rm);

        assertEquals(2, displays.size(), "extractor must surface both recipe types as displays");
        Map<ResourceLocation, TableCraftingDisplay> byId = displays.stream()
                .collect(Collectors.toMap(TableCraftingDisplay::recipeId, Function.identity()));

        TableCraftingDisplay shelfDisplay = byId.get(Meridian.id("shelf_upgrade"));
        assertNotNull(shelfDisplay);
        assertFalse(shelfDisplay.keepNbt(), "plain enchanting recipes must report keepNbt=false");
        assertSame(shelfUpgrade.getInput(), shelfDisplay.input(),
                "extractor must surface the live ingredient instance — viewers compare by reference");
        assertEquals(Items.ENCHANTING_TABLE, shelfDisplay.result().getItem());
        assertEquals(22.5F, shelfDisplay.requirements().eterna(), 1e-4F);
        assertEquals(30F, shelfDisplay.requirements().quanta(), 1e-4F);
        assertEquals(StatRequirements.NO_MAX, shelfDisplay.maxRequirements());
        assertEquals(OptionalInt.of(5), shelfDisplay.displayLevel());
        assertEquals(3, shelfDisplay.xpCost());

        TableCraftingDisplay libraryDisplay = byId.get(Meridian.id("library_upgrade"));
        assertNotNull(libraryDisplay);
        assertTrue(libraryDisplay.keepNbt(),
                "keep-nbt recipes must report keepNbt=true so viewers can flag the component carry-over");
        assertEquals(50F, libraryDisplay.maxRequirements().eterna(), 1e-4F);
        assertEquals(50F, libraryDisplay.maxRequirements().quanta(), 1e-4F);
        assertEquals(7, libraryDisplay.xpCost());
    }

    @Test
    void extract_emptyManager_returnsEmptyList() {
        RecipeManager rm = new RecipeManager(RegistryAccess.EMPTY);
        rm.replaceRecipes(List.of());
        assertTrue(TableCraftingDisplayExtractor.extract(rm).isEmpty(),
                "no fizzle recipes loaded → extractor must produce nothing, not even placeholder displays");
    }

    @Test
    void extract_resultIsCopied_soDownstreamMutationsDoNotLeak() {
        EnchantingRecipe r = new EnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(0F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.ENCHANTED_BOOK, 4),
                OptionalInt.empty(),
                0);
        RecipeManager rm = managerWith(holder("only", r));

        TableCraftingDisplay display = TableCraftingDisplayExtractor.extract(rm).getFirst();
        display.result().setCount(99);

        // Re-extract: the cached recipe's result must still report the original count, proving the
        // extractor handed back a defensive copy. Viewers routinely mutate display stacks (changing
        // amounts for tooltips), so leaking the recipe's source stack would corrupt cooking flows.
        TableCraftingDisplay refreshed = TableCraftingDisplayExtractor.extract(rm).getFirst();
        assertEquals(4, refreshed.result().getCount());
    }

    private static RecipeHolder<EnchantingRecipe> holder(String path, EnchantingRecipe recipe) {
        return new RecipeHolder<>(Meridian.id(path), recipe);
    }

    private static RecipeManager managerWith(RecipeHolder<?>... holders) {
        RecipeManager manager = new RecipeManager(RegistryAccess.EMPTY);
        manager.replaceRecipes(List.of(holders));
        return manager;
    }

}
