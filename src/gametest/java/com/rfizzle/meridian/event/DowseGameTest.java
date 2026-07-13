// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class DowseGameTest implements FabricGameTest {

    // Dowse locates the ore vein nearest the player and floods its connected blocks, leaving a
    // farther, unconnected vein alone.
    @GameTest(template = "meridian:empty_5x5x5")
    public void findsNearestConnectedVein(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos origin = new BlockPos(0, 1, 0);
        BlockPos nearA = new BlockPos(1, 1, 0);   // adjacent to origin
        BlockPos nearB = new BlockPos(2, 1, 0);   // connected to nearA -> same vein
        BlockPos far = new BlockPos(4, 4, 4);      // isolated, well away from the near vein

        level.setBlockAndUpdate(helper.absolutePos(nearA), Blocks.IRON_ORE.defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(nearB), Blocks.IRON_ORE.defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(far), Blocks.DIAMOND_ORE.defaultBlockState());

        List<BlockPos> vein = DowseHandler.findNearestVein(level, helper.absolutePos(origin));

        if (!vein.contains(helper.absolutePos(nearA)) || !vein.contains(helper.absolutePos(nearB))) {
            helper.fail("Dowse must reveal the whole nearest connected vein, got " + vein);
            return;
        }
        if (vein.contains(helper.absolutePos(far))) {
            helper.fail("Dowse must not include a disconnected, farther vein");
            return;
        }
        if (vein.size() != 2) {
            helper.fail("Expected exactly the 2-block near vein, got " + vein.size());
            return;
        }

        helper.succeed();
    }

    // With no ore in range the scan returns nothing (and the handler burns only the cooldown).
    @GameTest(template = "meridian:empty_5x5x5")
    public void emptyWhenNoOreInRange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        List<BlockPos> vein = DowseHandler.findNearestVein(level, helper.absolutePos(new BlockPos(2, 2, 2)));

        if (!vein.isEmpty()) {
            helper.fail("Dowse must find nothing when no ore is nearby, got " + vein);
            return;
        }
        helper.succeed();
    }
}
