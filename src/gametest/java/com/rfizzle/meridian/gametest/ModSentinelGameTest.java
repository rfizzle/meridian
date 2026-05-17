// Tier: 3 (Fabric Gametest) — TEST-1.2-T3
package com.rfizzle.meridian.gametest;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.MeridianRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class ModSentinelGameTest implements FabricGameTest {

    @GameTest(template = "meridian:empty_3x3")
    public void modInitializeHasFired(GameTestHelper helper) {
        if (Meridian.getConfig() == null) {
            helper.fail("Config is null — onInitialize has not fired");
            return;
        }
        if (MeridianRegistry.BLOCKS.isEmpty()) {
            helper.fail("BLOCKS map is empty — registry not populated");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "meridian:empty_3x3")
    public void modIdConstantCorrect(GameTestHelper helper) {
        if (!"meridian".equals(Meridian.MOD_ID)) {
            helper.fail("MOD_ID should be 'meridian', got '" + Meridian.MOD_ID + "'");
            return;
        }
        if (Meridian.LOGGER == null) {
            helper.fail("LOGGER should not be null");
            return;
        }
        helper.succeed();
    }
}
