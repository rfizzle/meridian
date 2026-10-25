// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest.event;

import com.rfizzle.meridian.enchanting.MiningEnchantMath;
import com.rfizzle.meridian.event.DowseHandler;
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
    // The scan reaches DOWSE_SEARCH_RADIUS blocks in every direction, far past a small template, so
    // this test owns a template that spans the whole search cube (structure placement clears it to
    // air) and a batch of its own: otherwise ore placed by a neighbouring test lands in range.
    @GameTest(template = "meridian:empty_25x25x25", batch = "meridianDowseEmpty")
    public void emptyWhenNoOreInRange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int r = MiningEnchantMath.DOWSE_SEARCH_RADIUS;
        if (2 * r + 1 > 25) {
            helper.fail("empty_25x25x25 no longer spans the Dowse search cube (radius " + r + "); enlarge it");
            return;
        }

        List<BlockPos> vein = DowseHandler.findNearestVein(level, helper.absolutePos(new BlockPos(r, r, r)));

        if (!vein.isEmpty()) {
            helper.fail("Dowse must find nothing when no ore is nearby, got " + vein);
            return;
        }
        helper.succeed();
    }
}
