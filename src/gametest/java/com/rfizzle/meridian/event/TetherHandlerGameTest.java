// Tier: 3 (Fabric Gametest)
package com.rfizzle.meridian.event;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;

public class TetherHandlerGameTest implements FabricGameTest {

    // Tether: items saved for a player are returned to that player's inventory on restore, and a
    // second restore is a no-op (the saved entry is consumed).
    @GameTest(template = "meridian:empty_3x3")
    public void restoreReturnsSavedItemsOnce(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        TetherHandler.saveTetheredItems(player.getUUID(),
                List.of(new ItemStack(Items.DIAMOND, 5)));

        TetherHandler.restoreTetheredItems(player);
        int diamonds = player.getInventory().countItem(Items.DIAMOND);
        if (diamonds != 5) {
            helper.fail("Tether should return 5 diamonds to the player, found " + diamonds);
            return;
        }

        // The saved entry is consumed; restoring again must not duplicate it.
        TetherHandler.restoreTetheredItems(player);
        int afterSecond = player.getInventory().countItem(Items.DIAMOND);
        if (afterSecond != 5) {
            helper.fail("Second restore must be a no-op, found " + afterSecond + " diamonds");
            return;
        }

        helper.succeed();
    }
}
