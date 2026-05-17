package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipeRegistry;
import com.rfizzle.meridian.enchanting.recipe.KeepNbtEnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.StatRequirements;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tier: 2
class TableCraftingLookupTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void lookupCraftingResult_inputAtMatchingStats_locatesRecipe() {
        KeepNbtEnchantingRecipe enderUpgrade = new KeepNbtEnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(50F, 45F, 100F),
                new StatRequirements(50F, 50F, 100F),
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                0);
        RecipeManager rm = managerWith(holder("ender_library", enderUpgrade));

        StatCollection shelves = stats(50F, 48F, 100F);
        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> hit =
                MeridianEnchantmentMenu.lookupCraftingResult(rm, new ItemStack(Items.BOOK), shelves);

        assertTrue(hit.isPresent(),
                "library input at Eterna 50 / Quanta 48 / Arcana 100 must resolve the ender upgrade");
        assertSame(enderUpgrade, hit.get().value(),
                "resolver must return the keep-nbt recipe, not another tier that happens to match");
        assertInstanceOf(KeepNbtEnchantingRecipe.class, hit.get().value(),
                "keep-nbt variant is required so clicking the crafting row preserves stored books");
    }

    @Test
    void lookupCraftingResult_statsBelowRequirements_returnsEmpty() {
        EnchantingRecipe tierTwoUpgrade = new EnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(40F, 15F, 60F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                0);
        RecipeManager rm = managerWith(holder("upgrade", tierTwoUpgrade));

        StatCollection underpowered = stats(30F, 10F, 40F);
        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> hit =
                MeridianEnchantmentMenu.lookupCraftingResult(rm, new ItemStack(Items.BOOK), underpowered);

        assertEquals(Optional.empty(), hit,
                "stats below the recipe's minima must leave the menu's currentRecipe empty");
    }

    @Test
    void lookupCraftingResult_emptyInput_shortCircuits() {
        EnchantingRecipe always = new EnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(0F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                0);
        RecipeManager rm = managerWith(holder("always", always));

        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> hit =
                MeridianEnchantmentMenu.lookupCraftingResult(rm, ItemStack.EMPTY, stats(50F, 50F, 100F));

        assertEquals(Optional.empty(), hit,
                "empty input must short-circuit before the recipe manager scan — no point matching against nothing");
    }

    @Test
    void lookupCraftingResult_ingredientMismatch_returnsEmpty() {
        EnchantingRecipe bookRecipe = new EnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(0F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                0);
        RecipeManager rm = managerWith(holder("books_only", bookRecipe));

        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> hit =
                MeridianEnchantmentMenu.lookupCraftingResult(rm, new ItemStack(Items.DIAMOND_SWORD), stats(50F, 50F, 100F));

        assertEquals(Optional.empty(), hit,
                "input that fails the ingredient predicate must not match regardless of shelf totals");
    }

    private static StatCollection stats(float eterna, float quanta, float arcana) {
        return new StatCollection(eterna, quanta, arcana, 0F, 0, eterna, Set.of(), false);
    }

    private static RecipeHolder<? extends Recipe<SingleRecipeInput>> holder(String path, EnchantingRecipe recipe) {
        return new RecipeHolder<>(Meridian.id(path), recipe);
    }

    private static RecipeManager managerWith(RecipeHolder<?>... holders) {
        RecipeManager manager = new RecipeManager(RegistryAccess.EMPTY);
        manager.replaceRecipes(List.of(holders));
        return manager;
    }
}
