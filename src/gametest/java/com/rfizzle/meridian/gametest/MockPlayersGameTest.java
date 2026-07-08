package com.rfizzle.meridian.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Guards that {@link MockPlayers#serverPlayerInLevel(GameTestHelper)} stays a faithful replica of the
 * (deprecated) {@code makeMockServerPlayerInLevel} it replaced: a live connection, a player-list
 * registration, level membership, and the creative / non-spectator flags the connected-player call
 * sites rely on. If someone later "simplifies" the helper into a bare {@code new ServerPlayer(...)},
 * this fails instead of silently breaking the advancement and menu-feedback tests.
 */
public class MockPlayersGameTest implements FabricGameTest {

    @GameTest(template = "meridian:empty_3x3")
    public void serverPlayerInLevelIsFaithfulReplica(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.serverPlayerInLevel(helper);

        if (player.connection == null) {
            helper.fail("mock server player must have a live connection (advancement/menu paths send packets)");
            return;
        }
        if (!player.getServer().getPlayerList().getPlayers().contains(player)) {
            helper.fail("mock server player must be registered in the server player list");
            return;
        }
        if (player.level() != helper.getLevel()) {
            helper.fail("mock server player must be added to the test level");
            return;
        }
        if (!player.isCreative()) {
            helper.fail("mock server player must be creative, matching makeMockServerPlayerInLevel");
            return;
        }
        if (player.isSpectator()) {
            helper.fail("mock server player must not be a spectator");
            return;
        }

        player.discard();
        helper.succeed();
    }
}
