package com.rfizzle.meridian.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side rising-intent tracking for Ballast. Crouching to sink is read straight off the
 * server's view of the player, but a held jump key is client-only input the server never sees for a
 * real player — so the client reports it through {@link com.rfizzle.meridian.network.BallastAscendPayload}
 * and this class remembers which players are currently asking to rise. The lift itself is applied,
 * and re-gated on the enchant and the water check, in {@code ArmorTickHandler#handleBallast}; a
 * spoofed "rising" flag only ever lets a wearer swim up in water, never fly.
 */
public final class BallastHandler {

    /** Players whose client currently reports a held jump for Ballast. */
    private static final Set<UUID> rising = ConcurrentHashMap.newKeySet();

    private BallastHandler() {}

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                rising.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> rising.clear());
    }

    /** Records the client's latest rising intent for the player. */
    public static void setRising(ServerPlayer player, boolean value) {
        if (value) {
            rising.add(player.getUUID());
        } else {
            rising.remove(player.getUUID());
        }
    }

    /** Whether the player is currently asking to rise with Ballast. */
    public static boolean isRising(UUID id) {
        return rising.contains(id);
    }

    // Test support: gametests drive the intent directly and dispose mock players with
    // player.discard(), which never fires the DISCONNECT cleanup — so they must clear the flag
    // themselves or a mock UUID would linger for the life of the test JVM.
    public static void setRisingForTest(UUID id, boolean value) {
        if (value) {
            rising.add(id);
        } else {
            rising.remove(id);
        }
    }

    public static void clearPlayerForTest(UUID id) {
        rising.remove(id);
    }
}
