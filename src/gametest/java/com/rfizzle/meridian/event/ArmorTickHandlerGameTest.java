// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class ArmorTickHandlerGameTest implements FabricGameTest {

    // Cinderwalk reverts obsidian -> lava only in the dimension that created it.
    // Regression guard for cross-dimension corruption (#7): a block tracked under one
    // dimension must never be reverted at the same coordinates in another dimension.
    @GameTest(template = "meridian:empty_3x3")
    public void cinderwalkRevertsOnlyInTrackedDimension(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        // Anchor tracking one tick past the revert threshold relative to the canonical clock
        // (server.overworld().getGameTime(), which revertCinderwalkBlocks compares against), so
        // the entries are always due no matter how many ticks elapsed before the test ran.
        long pastTick = server.overworld().getGameTime() - (ArmorTickHandler.CINDERWALK_REVERT_TICKS + 1L);

        // Positive case: tracked in THIS dimension past the threshold, so the obsidian reverts.
        ArmorTickHandler.cinderwalkResetForTest();
        level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
        ArmorTickHandler.cinderwalkTrackForTest(level.dimension(), pos, pastTick);
        ArmorTickHandler.revertCinderwalkBlocks(server);
        if (!level.getBlockState(pos).is(Blocks.LAVA)) {
            helper.fail("Cinderwalk block tracked in this dimension should revert to lava, got "
                    + level.getBlockState(pos));
            return;
        }

        // Isolation case: same coordinates, but tracked under a DIFFERENT dimension key.
        // The revert targets the other dimension, so this dimension's block is untouched.
        ResourceKey<Level> otherDimension =
                level.dimension().equals(Level.OVERWORLD) ? Level.NETHER : Level.OVERWORLD;
        ArmorTickHandler.cinderwalkResetForTest();
        level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
        ArmorTickHandler.cinderwalkTrackForTest(otherDimension, pos, pastTick);
        ArmorTickHandler.revertCinderwalkBlocks(server);
        if (!level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
            helper.fail("Cinderwalk block tracked in another dimension must not revert here, got "
                    + level.getBlockState(pos));
            return;
        }

        ArmorTickHandler.cinderwalkResetForTest();
        helper.succeed();
    }
}
