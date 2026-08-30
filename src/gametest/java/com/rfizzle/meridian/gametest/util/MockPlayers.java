package com.rfizzle.meridian.gametest.util;

import com.mojang.authlib.GameProfile;
import com.rfizzle.meridian.Meridian;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;

import java.util.List;
import java.util.UUID;

/**
 * Mock-player factories for gametests — the canonical shape from {@code mc-testing-mock}.
 *
 * <p>{@link #serverPlayerInLevel(GameTestHelper)} reproduces the exact construction that
 * {@code GameTestHelper.makeMockServerPlayerInLevel()} performs — a fully placed {@link ServerPlayer}
 * with a live (embedded-channel) connection, registered in the server player list and added to the
 * test level — using non-deprecated APIs. {@link #connectedServerPlayerInLevel(GameTestHelper)}
 * hands back the channel too, for outbound-packet assertions, and
 * {@link #spectatorServerPlayerInLevel(GameTestHelper)} is the same replica reporting as a
 * spectator through {@code isSpectator()} only.
 *
 * <p>Every connected replica is torn down with {@link #retire(ServerPlayer)} — a bare
 * {@code discard()} removes the entity but leaves the player in {@code PlayerList}, so a suite
 * that only discards accumulates ghost online players across the shared test server.
 */
public final class MockPlayers {

    /**
     * Profile name for this mod's mocks. Namespaced, because {@link #retireLeaked} matches on it
     * and several mods can share a gametest level — an unnamespaced "test-mock-player" lets one
     * mod's sweep retire another mod's live player.
     */
    private static final String MOCK_NAME = Meridian.MOD_ID + "-test-mock-player";

    /** A connected player plus the embedded channel its outbound packets land in. */
    public record Connected(ServerPlayer player, EmbeddedChannel channel) {
    }

    private MockPlayers() {
    }

    /**
     * The connected replica: forced non-spectator and creative, spawned near world spawn — callers
     * that need it inside the test structure must teleport it (see {@code mc-testing-mock}).
     */
    public static ServerPlayer serverPlayerInLevel(GameTestHelper helper) {
        return connectedServerPlayerInLevel(helper).player();
    }

    /** Same replica, with the packet-absorbing channel exposed for outbound assertions. */
    public static Connected connectedServerPlayerInLevel(GameTestHelper helper) {
        return connectedInLevel(helper, false);
    }

    /**
     * A connected replica that reports as a spectator through {@code isSpectator()} only — it is
     * still placed with the server's default game type and still reports creative.
     */
    public static Connected spectatorServerPlayerInLevel(GameTestHelper helper) {
        return connectedInLevel(helper, true);
    }

    /** Fully retires a connected mock: awake, out of the player list, entity discarded. */
    public static void retire(ServerPlayer player) {
        if (player.isRemoved()) {
            return; // PlayerList#remove is not idempotent — a second call rewrites the player .dat
        }
        if (player.isSleeping()) {
            player.stopSleepInBed(true, true);
        }
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.getPlayerList().remove(player);
        }
        player.discard();
    }

    /**
     * Retires any mock this mod leaked into the helper's level. Level-wide, so the calling test
     * needs a {@code batch} of its own — same-batch tests run concurrently in one level.
     */
    public static void retireLeaked(GameTestHelper helper) {
        for (ServerPlayer player : List.copyOf(helper.getLevel().players())) {
            if (MOCK_NAME.equals(player.getGameProfile().getName())) {
                retire(player);
            }
        }
    }

    private static Connected connectedInLevel(GameTestHelper helper, boolean spectator) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), MOCK_NAME);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);

        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        ServerPlayer player = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return spectator;
            }

            @Override
            public boolean isCreative() {
                return true;
            }
        };

        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        // The embedded channel absorbs any packets sent to the mock player's connection so
        // connection.send(...) paths (advancement grants, menu feedback) work instead of NPEing.
        EmbeddedChannel channel = new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        return new Connected(player, channel);
    }
}
