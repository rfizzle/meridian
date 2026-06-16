package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

public class XpTomeGameTest implements FabricGameTest {

    @GameTest(template = "meridian:empty_3x3")
    public void testXpDeposit(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 5;
        ItemStack stack = new ItemStack(MeridianRegistry.XP_TOME_T1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        // Deposit 1 level
        MeridianRegistry.XP_TOME_T1.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(stack.getOrDefault(MeridianRegistry.STORED_XP, 0) == 1, "Expected 1 stored XP");
        helper.assertTrue(player.experienceLevel == 4, "Expected 4 player levels");
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void testXpWithdraw(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 0;
        ItemStack stack = new ItemStack(MeridianRegistry.XP_TOME_T1);
        stack.set(MeridianRegistry.STORED_XP, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        // Shift-right-click (withdraw)
        player.setShiftKeyDown(true);
        MeridianRegistry.XP_TOME_T1.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(stack.getOrDefault(MeridianRegistry.STORED_XP, 0) == 0, "Expected 0 stored XP");
        helper.assertTrue(player.experienceLevel == 1, "Expected 1 player level");
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void testTomeStacking(GameTestHelper helper) {
        ItemStack empty = new ItemStack(MeridianRegistry.XP_TOME_T1);
        ItemStack filled = new ItemStack(MeridianRegistry.XP_TOME_T1);
        filled.set(MeridianRegistry.STORED_XP, 1);

        int emptyMax = empty.getMaxStackSize();
        int filledMax = filled.getMaxStackSize();

        if (emptyMax != 16) {
             helper.fail("Empty tome should stack to 16, but got " + emptyMax);
        }
        if (filledMax != 1) {
             helper.fail("Filled tome should stack to 1, but got " + filledMax);
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void testFullTomeDeposit(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 5;
        ItemStack stack = new ItemStack(MeridianRegistry.XP_TOME_T1);
        stack.set(MeridianRegistry.STORED_XP, 10); // Max for T1
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        // Deposit into full tome
        MeridianRegistry.XP_TOME_T1.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);

        helper.assertTrue(stack.getOrDefault(MeridianRegistry.STORED_XP, 0) == 10, "Expected still 10 stored XP");
        helper.assertTrue(player.experienceLevel == 5, "Expected still 5 player levels");
        helper.succeed();
    }
}
