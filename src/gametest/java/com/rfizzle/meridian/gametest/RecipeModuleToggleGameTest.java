// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import java.util.Set;

import com.rfizzle.meridian.api.StatCollection;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipeRegistry;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * #163 acceptance — the recipe-module config toggles gate the real shipped recipes. The registry
 * methods take the config explicitly, so each case drives the matcher with a purpose-built config
 * instead of mutating the live server config.
 */
public class RecipeModuleToggleGameTest implements FabricGameTest {

    /** Stats comfortably above every duplication/everfeast gate (totem needs 50/45/85). */
    private static final StatCollection MAXED = stats(50F, 45F, 100F);

    @GameTest(template = "meridian:empty_3x3")
    public void duplicationDisabled_hidesTotemRecipeOnly(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        ItemStack emeraldBlock = new ItemStack(Items.EMERALD_BLOCK);

        MeridianConfig noDuplication = new MeridianConfig();
        noDuplication.tableCrafting.allowDuplication = false;

        if (EnchantingRecipeRegistry.findMatch(recipes, emeraldBlock, MAXED, noDuplication).isPresent()) {
            helper.fail("Totem duplication recipe must not match while allowDuplication is off");
            return;
        }
        if (EnchantingRecipeRegistry.hasItemMatch(recipes, emeraldBlock, noDuplication)) {
            helper.fail("The craft-slot hint must not fire for an emerald block while allowDuplication is off");
            return;
        }
        // The everfeast module is untouched by the duplication toggle.
        if (EnchantingRecipeRegistry.findMatch(recipes, new ItemStack(Items.BREAD), MAXED, noDuplication).isEmpty()) {
            helper.fail("Everfeast recipes must be unaffected by the duplication toggle");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void everfeastDisabled_hidesRationAndFlaskRecipes(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();

        MeridianConfig noEverfeast = new MeridianConfig();
        noEverfeast.everfeast.enabled = false;

        if (EnchantingRecipeRegistry.findMatch(recipes, new ItemStack(Items.BREAD), MAXED, noEverfeast).isPresent()) {
            helper.fail("Everfeast ration recipe must not match while everfeast.enabled is off");
            return;
        }
        if (EnchantingRecipeRegistry.findMatch(recipes, new ItemStack(Items.WATER_BUCKET), MAXED, noEverfeast).isPresent()) {
            helper.fail("Everfull Flask recipe must not match while everfeast.enabled is off");
            return;
        }
        if (EnchantingRecipeRegistry.hasItemMatch(recipes, new ItemStack(Items.BREAD), noEverfeast)) {
            helper.fail("The craft-slot hint must not fire for bread while everfeast.enabled is off");
            return;
        }
        // The duplication module is untouched by the everfeast toggle.
        if (EnchantingRecipeRegistry.findMatch(recipes, new ItemStack(Items.EMERALD_BLOCK), MAXED, noEverfeast).isEmpty()) {
            helper.fail("Duplication recipes must be unaffected by the everfeast toggle");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void defaultConfig_keepsBothModulesCraftable(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        MeridianConfig defaults = new MeridianConfig();

        if (EnchantingRecipeRegistry.findMatch(recipes, new ItemStack(Items.EMERALD_BLOCK), MAXED, defaults).isEmpty()) {
            helper.fail("Totem duplication recipe must match under an untouched config");
            return;
        }
        if (EnchantingRecipeRegistry.findMatch(recipes, new ItemStack(Items.BREAD), MAXED, defaults).isEmpty()) {
            helper.fail("Everfeast ration recipe must match under an untouched config");
            return;
        }
        helper.succeed();
    }

    private static StatCollection stats(float eterna, float quanta, float arcana) {
        return new StatCollection(eterna, quanta, arcana, 0F, 0, eterna, Set.of(), false);
    }
}
