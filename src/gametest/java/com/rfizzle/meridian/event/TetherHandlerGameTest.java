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

    // Tether: items stashed on a player are returned to a respawned player's inventory on restore,
    // and a second restore is a no-op (the stashed entry is consumed).
    @GameTest(template = "meridian:empty_3x3")
    public void restoreReturnsSavedItemsOnce(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        TetherHandler.saveTetheredItems(player,
                List.of(new ItemStack(Items.DIAMOND, 5)));

        TetherHandler.restoreTetheredItems(player, player);
        int diamonds = player.getInventory().countItem(Items.DIAMOND);
        if (diamonds != 5) {
            helper.fail("Tether should return 5 diamonds to the player, found " + diamonds);
            return;
        }

        // The stashed entry is consumed; restoring again must not duplicate it.
        TetherHandler.restoreTetheredItems(player, player);
        int afterSecond = player.getInventory().countItem(Items.DIAMOND);
        if (afterSecond != 5) {
            helper.fail("Second restore must be a no-op, found " + afterSecond + " diamonds");
            return;
        }

        helper.succeed();
    }

    // Tether: items stashed on the dying player are handed to the freshly-respawned player entity.
    // This mirrors the respawn hook (COPY_FROM), which restores from the old player into the new one
    // — the path that also covers a disconnect on the death screen, where the stash rides the player
    // entity to disk and back before the eventual respawn.
    @GameTest(template = "meridian:empty_3x3")
    public void restoreMovesItemsToRespawnedPlayer(GameTestHelper helper) {
        Player oldPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        Player newPlayer = helper.makeMockPlayer(GameType.SURVIVAL);

        TetherHandler.saveTetheredItems(oldPlayer,
                List.of(new ItemStack(Items.DIAMOND, 3)));

        TetherHandler.restoreTetheredItems(oldPlayer, newPlayer);

        int onNew = newPlayer.getInventory().countItem(Items.DIAMOND);
        if (onNew != 3) {
            helper.fail("Respawned player should receive 3 diamonds, found " + onNew);
            return;
        }

        // The stash is consumed off the old player; a repeat restore hands over nothing.
        TetherHandler.restoreTetheredItems(oldPlayer, newPlayer);
        int afterSecond = newPlayer.getInventory().countItem(Items.DIAMOND);
        if (afterSecond != 3) {
            helper.fail("Second restore must be a no-op, found " + afterSecond + " diamonds");
            return;
        }

        helper.succeed();
    }
}
