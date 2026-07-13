package com.rfizzle.meridian.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Client-side hold for the ore positions a Dowse pulse revealed and the game-time at which the
 * glow expires. Written by the {@link com.rfizzle.meridian.net.DowseGlowPayload} receiver and read
 * by {@link DowseOverlayRenderer} — both on the client render thread, but the fields are
 * {@code volatile} to publish safely per the {@code mc-shared-state} guardrails. Cleared on
 * disconnect so a stale vein can't flash when joining another server. The reveal is also pinned to
 * the dimension it was found in, so a dimension change (which shares the game-time clock but never
 * fires {@code DISCONNECT}) doesn't leave the old dimension's ore glowing in the new one.
 */
public final class ClientDowseState {

    private static volatile List<BlockPos> positions = List.of();
    private static volatile long expiryGameTime = 0L;
    private static volatile ResourceKey<Level> dimension = null;

    private ClientDowseState() {}

    /** Records a fresh reveal in {@code dim}: {@code veinPositions} glow until {@code expiry} (a client game-time). */
    public static void set(List<BlockPos> veinPositions, long expiry, ResourceKey<Level> dim) {
        positions = List.copyOf(veinPositions);
        expiryGameTime = expiry;
        dimension = dim;
    }

    public static void clear() {
        positions = List.of();
        expiryGameTime = 0L;
        dimension = null;
    }

    public static List<BlockPos> positions() {
        return positions;
    }

    /** Ticks of glow left at client game-time {@code now}; zero once expired or never set. */
    public static int ticksRemaining(long now) {
        long remaining = expiryGameTime - now;
        return remaining <= 0 ? 0 : (int) Math.min(remaining, Integer.MAX_VALUE);
    }

    /**
     * True while there is a vein to draw, its glow has not expired at {@code now}, and the viewer is
     * still in the dimension the vein was revealed in.
     */
    public static boolean isActive(long now, ResourceKey<Level> currentDimension) {
        return now < expiryGameTime && !positions.isEmpty() && currentDimension.equals(dimension);
    }
}
