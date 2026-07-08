package com.rfizzle.meridian.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

import java.util.UUID;

/**
 * Mock-player factories for gametests.
 *
 * <p>{@link #serverPlayerInLevel(GameTestHelper)} reproduces the exact construction that
 * {@code GameTestHelper.makeMockServerPlayerInLevel()} performs — a fully placed {@link ServerPlayer}
 * with a live (embedded-channel) connection, registered in the server player list and added to the
 * test level — using non-deprecated APIs. Gametests keep the connected, player-list-registered player
 * the vanilla helper gave them. See {@code mc-testing-mock} for when a connected server player is
 * required (Fabric attachments, {@code connection.send} paths such as advancement grants and menu
 * button clicks, proximity and damage attribution) versus the lighter
 * {@code helper.makeMockPlayer(GameType)} stub.
 */
public final class MockPlayers {

    private static final String MOCK_NAME = "test-mock-player";

    private MockPlayers() {
    }

    /**
     * Creates a mock {@link ServerPlayer} placed in the helper's level with a live connection and a
     * player-list registration, matching {@code GameTestHelper.makeMockServerPlayerInLevel()}. The
     * player is forced non-spectator and creative, and spawns near world spawn — callers that need it
     * inside the test structure must teleport it (see {@code mc-testing-mock}).
     */
    public static ServerPlayer serverPlayerInLevel(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), MOCK_NAME);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);

        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        // The embedded channel absorbs any packets sent to the mock player's connection so
        // connection.send(...) paths (advancement grants, menu feedback) work instead of NPEing.
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return player;
    }
}
