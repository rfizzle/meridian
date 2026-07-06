// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.event.DragonLootHandler;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Verifies the Ender Dragon Dormant Core drop routine (#158): {@link DragonLootHandler#dropDormantCore}
 * spawns exactly one {@code dormant_core} as a free item entity at the requested position.
 *
 * <p>This exercises the drop routine directly rather than through a live dragon: the Ender Dragon
 * cannot complete its ten second death animation inside a bounded gametest structure without being
 * culled first, so a real-dragon test is inherently flaky. The mixin that calls this routine at the
 * dragon's terminal death frame is verified against the vanilla {@code tickDeath} source and by manual
 * play; this test guards the payload — one core, correct item — which is the part with logic.
 */
public class DragonDropGameTest implements FabricGameTest {

    @GameTest(template = "meridian:empty_3x3")
    public void dropRoutineSpawnsExactlyOneDormantCore(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        DragonLootHandler.dropDormantCore(helper.getLevel(),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        helper.runAfterDelay(2, () -> {
            long cores = helper.getEntities(EntityType.ITEM).stream()
                    .map(e -> ((ItemEntity) e).getItem())
                    .filter(s -> s.is(MeridianRegistry.DORMANT_CORE))
                    .mapToLong(s -> s.getCount())
                    .sum();
            if (cores != 1) {
                helper.fail("drop routine must spawn exactly one Dormant Core, got " + cores);
            } else {
                helper.succeed();
            }
        });
    }
}
