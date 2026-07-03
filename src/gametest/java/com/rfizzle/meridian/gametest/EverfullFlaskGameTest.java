// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.item.EverfullFlaskItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/**
 * The template's interior is capped by a barrier roof at rel y4, so the player stands low
 * (feet at rel y1, eye ~y2.6) and looks straight down — the bucket ray hits the stone at
 * rel y0 and the water lands in the player's own cell at rel y1, all comfortably inside
 * the structure bounds.
 */
public class EverfullFlaskGameTest implements FabricGameTest {

    private static final BlockPos FLOOR_POS = new BlockPos(1, 0, 1);
    private static final BlockPos WATER_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "meridian:empty_3x3")
    public void placementNeverEmptiesTheFlask(GameTestHelper helper) {
        helper.setBlock(FLOOR_POS, Blocks.STONE.defaultBlockState());
        helper.setBlock(WATER_POS, Blocks.AIR.defaultBlockState());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(WATER_POS);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0.0F, 90.0F);

        ItemStack hand = new ItemStack(MeridianRegistry.EVERFULL_FLASK);
        // Two consecutive uses: an ordinary bucket empties on the first; the flask must
        // survive both with a full-count stack.
        for (int use = 1; use <= 2; use++) {
            player.setItemInHand(InteractionHand.MAIN_HAND, hand);

            InteractionResultHolder<ItemStack> result = MeridianRegistry.EVERFULL_FLASK.use(
                    helper.getLevel(), player, InteractionHand.MAIN_HAND);

            if (!result.getResult().consumesAction()) {
                helper.fail("Use #" + use + " should place water, got " + result.getResult());
                return;
            }
            if (!helper.getBlockState(WATER_POS).is(Blocks.WATER)) {
                helper.fail("Use #" + use + " should leave water at " + WATER_POS
                        + ", found " + helper.getBlockState(WATER_POS));
                return;
            }
            ItemStack returned = result.getObject();
            if (!(returned.getItem() instanceof EverfullFlaskItem) || returned.getCount() != 1) {
                helper.fail("Use #" + use + " must hand back the intact flask, got " + returned);
                return;
            }
            hand = returned;
            helper.setBlock(WATER_POS, Blocks.AIR.defaultBlockState());
        }
        player.discard();
        helper.succeed();
    }
}
