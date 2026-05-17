package com.rfizzle.meridian.enchanting.recipe;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.StatCollection;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tier: 2
class EnchantingRecipeRegistryTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void findMatch_returnsHigherEternaThresholdFirstWhenBothMatch() {
        EnchantingRecipe cheap = enchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(10F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND));
        EnchantingRecipe pricey = enchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(40F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.NETHERITE_INGOT));
        RecipeManager rm = managerWith(
                holder("cheap", cheap),
                holder("pricey", pricey));

        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> hit = EnchantingRecipeRegistry
                .findMatch(rm, new ItemStack(Items.DIAMOND_SWORD), stats(40F, 0F, 0F));
        assertTrue(hit.isPresent());
        assertSame(pricey, hit.get().value());
    }

    @Test
    void findMatch_fallsBackToCheaperRecipeWhenStatsDontHitPricey() {
        EnchantingRecipe cheap = enchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(10F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND));
        EnchantingRecipe pricey = enchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(40F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.NETHERITE_INGOT));
        RecipeManager rm = managerWith(
                holder("cheap", cheap),
                holder("pricey", pricey));

        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> hit = EnchantingRecipeRegistry
                .findMatch(rm, new ItemStack(Items.DIAMOND_SWORD), stats(20F, 0F, 0F));
        assertTrue(hit.isPresent());
        assertSame(cheap, hit.get().value());
    }

    @Test
    void findMatch_returnsEmptyWhenNoRecipeMatches() {
        EnchantingRecipe r = enchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(40F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.DIAMOND));
        RecipeManager rm = managerWith(holder("only", r));
        assertFalse(EnchantingRecipeRegistry
                .findMatch(rm, new ItemStack(Items.IRON_SWORD), stats(50F, 50F, 50F))
                .isPresent());
        assertFalse(EnchantingRecipeRegistry
                .findMatch(rm, new ItemStack(Items.DIAMOND_SWORD), stats(10F, 0F, 0F))
                .isPresent());
    }

    @Test
    void findMatch_includesKeepNbtRecipeAcrossBothTypes() {
        KeepNbtEnchantingRecipe keep = new KeepNbtEnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(50F, 45F, 100F),
                new StatRequirements(50F, 50F, 100F),
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                0);
        RecipeManager rm = managerWith(holder("ender", keep));
        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> hit = EnchantingRecipeRegistry
                .findMatch(rm, new ItemStack(Items.BOOK), stats(50F, 50F, 100F));
        assertTrue(hit.isPresent());
        assertInstanceOf(KeepNbtEnchantingRecipe.class, hit.get().value());
    }

    @Test
    void findMatch_respectsMaxRequirementsUpperBound() {
        EnchantingRecipe capped = enchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(0F, 0F, 0F),
                new StatRequirements(-1F, 25F, -1F),
                new ItemStack(Items.DIAMOND));
        RecipeManager rm = managerWith(holder("capped", capped));
        assertTrue(EnchantingRecipeRegistry
                .findMatch(rm, new ItemStack(Items.DIAMOND_SWORD), stats(0F, 25F, 0F))
                .isPresent());
        assertFalse(EnchantingRecipeRegistry
                .findMatch(rm, new ItemStack(Items.DIAMOND_SWORD), stats(0F, 25.1F, 0F))
                .isPresent());
    }

    private static StatCollection stats(float eterna, float quanta, float arcana) {
        return new StatCollection(eterna, quanta, arcana, 0F, 0, eterna, Set.of(), false);
    }

    private static EnchantingRecipe enchantingRecipe(Ingredient input, StatRequirements req,
                                                     StatRequirements max, ItemStack result) {
        return new EnchantingRecipe(input, req, max, result, OptionalInt.empty(), 0);
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
