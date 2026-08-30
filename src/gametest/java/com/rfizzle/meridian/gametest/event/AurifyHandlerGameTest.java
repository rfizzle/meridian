// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest.event;

import com.rfizzle.meridian.event.AurifyHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class AurifyHandlerGameTest implements FabricGameTest {

    private boolean converts(GameTestHelper helper, BlockState from, net.minecraft.world.level.block.Block expected) {
        BlockState result = AurifyHandler.getGoldConversion(from);
        if (result == null) {
            helper.fail("Aurify should convert " + from + " but got null");
            return false;
        }
        if (!result.is(expected)) {
            helper.fail("Aurify should convert " + from + " to " + expected + ", got " + result);
            return false;
        }
        return true;
    }

    // Aurify's deterministic block-to-gold table: stone-likes -> gold ore, deepslate -> deepslate
    // gold ore, netherrack -> nether gold ore, soft blocks -> a solid gold block, and anything
    // off-table converts to nothing.
    @GameTest(template = "meridian:empty_3x3")
    public void goldConversionTableMapsEachFamily(GameTestHelper helper) {
        if (!converts(helper, Blocks.STONE.defaultBlockState(), Blocks.GOLD_ORE)) return;
        if (!converts(helper, Blocks.COBBLESTONE.defaultBlockState(), Blocks.GOLD_ORE)) return;
        if (!converts(helper, Blocks.GRANITE.defaultBlockState(), Blocks.GOLD_ORE)) return;
        if (!converts(helper, Blocks.DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_GOLD_ORE)) return;
        if (!converts(helper, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_GOLD_ORE)) return;
        if (!converts(helper, Blocks.NETHERRACK.defaultBlockState(), Blocks.NETHER_GOLD_ORE)) return;
        if (!converts(helper, Blocks.DIRT.defaultBlockState(), Blocks.GOLD_BLOCK)) return;
        if (!converts(helper, Blocks.SAND.defaultBlockState(), Blocks.GOLD_BLOCK)) return;

        // Off-table blocks must not convert.
        if (AurifyHandler.getGoldConversion(Blocks.OBSIDIAN.defaultBlockState()) != null) {
            helper.fail("Aurify must not convert obsidian");
            return;
        }
        if (AurifyHandler.getGoldConversion(Blocks.GOLD_BLOCK.defaultBlockState()) != null) {
            helper.fail("Aurify must not re-convert an existing gold block");
            return;
        }

        helper.succeed();
    }
}
