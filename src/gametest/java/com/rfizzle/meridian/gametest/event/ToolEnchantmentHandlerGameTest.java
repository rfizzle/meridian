// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest.event;

import com.rfizzle.meridian.event.ToolEnchantmentHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class ToolEnchantmentHandlerGameTest implements FabricGameTest {

    // Timberfell's flood-fill uses 26-neighbour connectivity: a diagonally-offset branch
    // is part of the same tree, but a log separated by an air gap is a different tree and
    // must be left standing. The starting block (already broken by vanilla) is excluded.
    @GameTest(template = "meridian:empty_5x5x5")
    public void timberfellCollectsConnectedTreeOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos start = new BlockPos(2, 1, 2);
        BlockPos trunkUp = new BlockPos(2, 2, 2);   // face neighbour of start
        BlockPos branch = new BlockPos(3, 3, 3);    // diagonal (26-)neighbour of trunkUp
        BlockPos branchTop = new BlockPos(3, 4, 3); // face neighbour of branch
        BlockPos separate = new BlockPos(0, 1, 0);  // isolated log, air all around

        for (BlockPos p : List.of(start, trunkUp, branch, branchTop, separate)) {
            level.setBlockAndUpdate(helper.absolutePos(p), Blocks.OAK_LOG.defaultBlockState());
        }

        List<BlockPos> logs = ToolEnchantmentHandler.findConnectedLogs(level, helper.absolutePos(start));

        if (logs.contains(helper.absolutePos(start))) {
            helper.fail("Timberfell must exclude the already-broken start log");
            return;
        }
        if (!logs.contains(helper.absolutePos(trunkUp))
                || !logs.contains(helper.absolutePos(branch))
                || !logs.contains(helper.absolutePos(branchTop))) {
            helper.fail("Timberfell must fell the whole connected tree including diagonal branches, got " + logs);
            return;
        }
        if (logs.contains(helper.absolutePos(separate))) {
            helper.fail("Timberfell must not cross an air gap into a separate tree");
            return;
        }
        if (logs.size() != 3) {
            helper.fail("Expected exactly the 3 connected logs (start excluded), got " + logs.size());
            return;
        }

        helper.succeed();
    }

    // Trailblaze paths the surrounding 3x3 using vanilla's shovel eligibility: pathable blocks
    // with air above become DIRT_PATH, the center is left to vanilla, and non-pathable or
    // covered neighbours stay untouched.
    @GameTest(template = "meridian:empty_5x5x5")
    public void trailblazePavesEligibleRing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos center = new BlockPos(2, 1, 2);
        BlockPos grassNeighbor = new BlockPos(1, 1, 2);   // pathable, air above -> DIRT_PATH
        BlockPos dirtNeighbor = new BlockPos(3, 1, 2);    // pathable, air above -> DIRT_PATH
        BlockPos stoneNeighbor = new BlockPos(2, 1, 1);   // not pathable -> untouched
        BlockPos coveredNeighbor = new BlockPos(2, 1, 3); // pathable but roofed -> untouched

        level.setBlockAndUpdate(helper.absolutePos(center), Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(grassNeighbor), Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(dirtNeighbor), Blocks.DIRT.defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(stoneNeighbor), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(coveredNeighbor), Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(helper.absolutePos(coveredNeighbor.above()), Blocks.STONE.defaultBlockState());

        ToolEnchantmentHandler.applyTrailblaze(level, helper.absolutePos(center), 1);

        if (!level.getBlockState(helper.absolutePos(grassNeighbor)).is(Blocks.DIRT_PATH)
                || !level.getBlockState(helper.absolutePos(dirtNeighbor)).is(Blocks.DIRT_PATH)) {
            helper.fail("Trailblaze must path eligible neighbours with air above");
            return;
        }
        if (level.getBlockState(helper.absolutePos(center)).is(Blocks.DIRT_PATH)) {
            helper.fail("Trailblaze must leave the center to vanilla, not path it");
            return;
        }
        if (!level.getBlockState(helper.absolutePos(stoneNeighbor)).is(Blocks.STONE)) {
            helper.fail("Trailblaze must not path a non-pathable neighbour");
            return;
        }
        if (!level.getBlockState(helper.absolutePos(coveredNeighbor)).is(Blocks.GRASS_BLOCK)) {
            helper.fail("Trailblaze must not path a neighbour with a block above it");
            return;
        }

        helper.succeed();
    }
}
