// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TwinHookGameTest implements FabricGameTest {

    // The Twin Hook duplication seam spawns exactly one extra item entity matching the catch,
    // and grants no experience — the enchantment doubles yield, not fishing XP.
    @GameTest(template = "meridian:empty_5x5x5")
    public void spawnDuplicateAddsItemOnly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        BlockPos center = new BlockPos(2, 2, 2);
        BlockPos abs = helper.absolutePos(center);
        AABB region = new AABB(abs).inflate(3.0);

        ItemEntity caught = new ItemEntity(level, abs.getX() + 0.5, abs.getY() + 0.5, abs.getZ() + 0.5,
                new ItemStack(Items.COD));
        caught.setDeltaMovement(0.0, 0.0, 0.0);
        level.addFreshEntity(caught);

        int itemsBefore = level.getEntitiesOfClass(ItemEntity.class, region).size();
        int orbsBefore = level.getEntitiesOfClass(ExperienceOrb.class, region).size();

        TwinHookHandler.spawnDuplicate(level, caught);

        List<ItemEntity> itemsAfter = level.getEntitiesOfClass(ItemEntity.class, region);
        int orbsAfter = level.getEntitiesOfClass(ExperienceOrb.class, region).size();

        if (itemsAfter.size() != itemsBefore + 1) {
            helper.fail("Twin Hook must spawn exactly one extra item, had " + itemsBefore
                    + " now " + itemsAfter.size());
            return;
        }
        if (orbsAfter != orbsBefore) {
            helper.fail("Twin Hook duplicate must not grant experience, orbs went " + orbsBefore
                    + " -> " + orbsAfter);
            return;
        }
        boolean allCod = itemsAfter.stream().allMatch(e -> e.getItem().is(Items.COD));
        if (!allCod) {
            helper.fail("Twin Hook duplicate must copy the caught stack");
            return;
        }

        helper.succeed();
    }
}
