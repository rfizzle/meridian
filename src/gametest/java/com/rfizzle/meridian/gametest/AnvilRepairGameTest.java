// Tier: 3 (Fabric Gametest) — TEST-4.2-T3
package com.rfizzle.meridian.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public class AnvilRepairGameTest implements FabricGameTest {

    private static final BlockPos ANVIL_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "meridian:empty_3x3")
    public void damagedAnvilPlusIronBlockYieldsChipped(GameTestHelper helper) {
        AnvilMenu menu = setupAnvil(helper);

        menu.getSlot(0).set(new ItemStack(Items.DAMAGED_ANVIL));
        menu.getSlot(1).set(new ItemStack(Items.IRON_BLOCK));

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("Damaged anvil + iron block should produce output");
            return;
        }
        if (!output.is(Items.CHIPPED_ANVIL)) {
            helper.fail("Expected chipped anvil, got " + output.getItem());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void chippedAnvilPlusIronBlockYieldsNormal(GameTestHelper helper) {
        AnvilMenu menu = setupAnvil(helper);

        menu.getSlot(0).set(new ItemStack(Items.CHIPPED_ANVIL));
        menu.getSlot(1).set(new ItemStack(Items.IRON_BLOCK));

        ItemStack output = menu.getSlot(2).getItem();
        if (output.isEmpty()) {
            helper.fail("Chipped anvil + iron block should produce output");
            return;
        }
        if (!output.is(Items.ANVIL)) {
            helper.fail("Expected pristine anvil, got " + output.getItem());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void normalAnvilPlusIronBlockDeclines(GameTestHelper helper) {
        AnvilMenu menu = setupAnvil(helper);

        menu.getSlot(0).set(new ItemStack(Items.ANVIL));
        menu.getSlot(1).set(new ItemStack(Items.IRON_BLOCK));

        ItemStack output = menu.getSlot(2).getItem();
        if (!output.isEmpty() && output.is(Items.ANVIL)) {
            helper.fail("Pristine anvil should not produce a repair output");
            return;
        }
        helper.succeed();
    }

    private AnvilMenu setupAnvil(GameTestHelper helper) {
        helper.setBlock(ANVIL_POS, Blocks.ANVIL.defaultBlockState());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 30;
        BlockPos absAnvil = helper.absolutePos(ANVIL_POS);
        return new AnvilMenu(
                1, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absAnvil));
    }
}
