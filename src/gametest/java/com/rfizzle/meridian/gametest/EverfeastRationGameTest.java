// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import java.util.Set;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.api.StatCollection;
import com.rfizzle.meridian.enchanting.recipe.EnchantingRecipeRegistry;
import com.rfizzle.meridian.item.EverfeastRationItem;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class EverfeastRationGameTest implements FabricGameTest {

    @GameTest(template = "meridian:empty_3x3")
    public void eatingTicksBitesAndRestoresBaseFoodNutrition(GameTestHelper helper) {
        EverfeastRationItem ration = MeridianRegistry.EVERFEAST_RATIONS.get(Items.COOKED_BEEF);

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(new BlockPos(1, 1, 1));
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0F, 0.0F);
        player.getFoodData().setFoodLevel(0);

        ItemStack stack = new ItemStack(ration);
        EverfeastRationItem.stampBites(stack, 2);

        ItemStack afterOne = ration.finishUsingItem(stack, helper.getLevel(), player);

        FoodProperties baseFood = Items.COOKED_BEEF.components().get(DataComponents.FOOD);
        if (player.getFoodData().getFoodLevel() != baseFood.nutrition()) {
            helper.fail("Eating must restore the base food's nutrition ("
                    + baseFood.nutrition() + "), got " + player.getFoodData().getFoodLevel());
            return;
        }
        if (afterOne.isEmpty() || EverfeastRationItem.remainingBites(afterOne) != 1) {
            helper.fail("First bite of 2 must leave a ration with 1 bite, got " + afterOne);
            return;
        }

        ItemStack afterTwo = ration.finishUsingItem(afterOne, helper.getLevel(), player);
        if (!afterTwo.isEmpty()) {
            helper.fail("Final bite must consume the ration, got " + afterTwo);
            return;
        }
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void stampBites_usesLiveServerConfig(GameTestHelper helper) {
        ItemStack stack = new ItemStack(MeridianRegistry.EVERFEAST_RATIONS.get(Items.BREAD));
        EverfeastRationItem.stampBites(stack);

        int expected = EverfeastRationItem.configuredBites(Meridian.getConfig());
        if (EverfeastRationItem.maxBites(stack) != expected) {
            helper.fail("Freshly stamped ration must carry the configured bite count " + expected
                    + ", got " + EverfeastRationItem.maxBites(stack));
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void infusionRecipes_matchOnlyAtTheGate(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        StatCollection atGate = stats(27F, 15F, 0F);
        StatCollection belowEterna = stats(26F, 15F, 0F);
        StatCollection belowQuanta = stats(27F, 14F, 0F);

        for (var base : MeridianRegistry.EVERFEAST_RATIONS.keySet()) {
            ItemStack input = new ItemStack(base);
            var match = EnchantingRecipeRegistry.findMatch(recipes, input, atGate, Meridian.getConfig());
            if (match.isEmpty()) {
                helper.fail("No everfeast recipe matched " + base + " at the eterna 27 / quanta 15 gate");
                return;
            }
            var result = match.get().value().getResultItem(helper.getLevel().registryAccess());
            if (result.getItem() != MeridianRegistry.EVERFEAST_RATIONS.get(base)) {
                helper.fail("Recipe for " + base + " must produce its everfeast ration, got " + result);
                return;
            }
            if (EnchantingRecipeRegistry.findMatch(recipes, input, belowEterna, Meridian.getConfig()).isPresent()
                    || EnchantingRecipeRegistry.findMatch(recipes, input, belowQuanta, Meridian.getConfig()).isPresent()) {
                helper.fail("Everfeast recipe for " + base + " must not match below the stat gate");
                return;
            }
        }
        helper.succeed();
    }

    private static StatCollection stats(float eterna, float quanta, float arcana) {
        return new StatCollection(eterna, quanta, arcana, 0F, 0, eterna, Set.of(), false);
    }
}
