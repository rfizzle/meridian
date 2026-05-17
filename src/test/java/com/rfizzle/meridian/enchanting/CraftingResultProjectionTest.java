// Tier: 2 (fabric-loader-junit)
package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.KeepNbtEnchantingRecipe;
import com.rfizzle.meridian.enchanting.recipe.StatRequirements;
import com.rfizzle.meridian.net.CraftingResultEntry;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-5.3.2 — proves {@link MeridianEnchantmentMenu#projectCraftingResult} mirrors the cached
 * {@code currentRecipe} into the {@link CraftingResultEntry} that piggybacks on
 * {@code StatsPayload}. Round-trip semantics: a present recipe must surface as
 * {@code Optional.of(entry)} carrying the recipe's result item, XP cost, and id; an absent
 * recipe must collapse to {@link Optional#empty()} so the client clears its fourth-row preview.
 */
class CraftingResultProjectionTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void projectCraftingResult_emptyOptional_returnsEmpty() {
        Optional<CraftingResultEntry> projected = MeridianEnchantmentMenu.projectCraftingResult(Optional.empty());

        assertEquals(Optional.empty(), projected,
                "no current recipe must collapse to empty so the client clears its preview");
    }

    @Test
    void projectCraftingResult_enchantingRecipe_carriesResultXpCostAndId() {
        EnchantingRecipe recipe = new EnchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(40F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.NETHERITE_INGOT),
                OptionalInt.empty(),
                12);
        ResourceLocation id = Meridian.id("netherite_upgrade");
        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> wrapped =
                Optional.of(new RecipeHolder<>(id, recipe));

        Optional<CraftingResultEntry> projected = MeridianEnchantmentMenu.projectCraftingResult(wrapped);

        assertTrue(projected.isPresent(),
                "present current recipe must surface on the outgoing payload");
        CraftingResultEntry entry = projected.orElseThrow();
        assertTrue(ItemStack.matches(recipe.getResult(), entry.result()),
                "projected result must match the recipe's static result item");
        assertEquals(12, entry.xpCost(), "XP cost must travel verbatim");
        assertSame(id, entry.recipeId(), "recipe id must travel verbatim for JEI/EMI highlight");
    }

    @Test
    void projectCraftingResult_keepNbtRecipe_carriesResultXpCostAndId() {
        // Library → Ender Library upgrade shape — keep-nbt recipes must surface on the payload
        // so the client preview shows the prospective output even before stored books are copied.
        KeepNbtEnchantingRecipe recipe = new KeepNbtEnchantingRecipe(
                Ingredient.of(Items.BOOK),
                new StatRequirements(50F, 45F, 100F),
                new StatRequirements(50F, 50F, 100F),
                new ItemStack(Items.ENCHANTED_BOOK),
                OptionalInt.empty(),
                30);
        ResourceLocation id = Meridian.id("ender_library");
        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> wrapped =
                Optional.of(new RecipeHolder<>(id, recipe));

        Optional<CraftingResultEntry> projected = MeridianEnchantmentMenu.projectCraftingResult(wrapped);

        assertTrue(projected.isPresent(),
                "keep-nbt recipes must project just like base enchanting recipes");
        CraftingResultEntry entry = projected.orElseThrow();
        assertTrue(ItemStack.matches(recipe.getResult(), entry.result()),
                "projected result must match the keep-nbt recipe's static result");
        assertEquals(30, entry.xpCost(), "XP cost must travel verbatim from keep-nbt recipes");
        assertSame(id, entry.recipeId(),
                "recipe id must travel verbatim — JEI/EMI hover relies on it for source highlight");
    }

    @Test
    void projectCraftingResult_resultIsCopied_notSharedReference() {
        // Mutating the projected payload's result must not bleed back into the recipe definition;
        // payloads can be retained by network code while the menu re-runs slotsChanged.
        EnchantingRecipe recipe = new EnchantingRecipe(
                Ingredient.of(Items.DIAMOND_SWORD),
                new StatRequirements(40F, 0F, 0F),
                StatRequirements.NO_MAX,
                new ItemStack(Items.NETHERITE_INGOT, 1),
                OptionalInt.empty(),
                0);
        Optional<RecipeHolder<? extends Recipe<SingleRecipeInput>>> wrapped =
                Optional.of(new RecipeHolder<>(Meridian.id("upgrade"), recipe));

        CraftingResultEntry entry =
                MeridianEnchantmentMenu.projectCraftingResult(wrapped).orElseThrow();
        entry.result().setCount(64);

        assertEquals(1, recipe.getResult().getCount(),
                "projection must defensively copy — payloads must not alias the recipe's result stack");
    }
}
