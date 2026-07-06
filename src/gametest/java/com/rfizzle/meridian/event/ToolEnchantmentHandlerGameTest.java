// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

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
}
