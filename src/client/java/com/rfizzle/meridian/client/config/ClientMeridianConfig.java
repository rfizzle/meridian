package com.rfizzle.meridian.client.config;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Client-side holder for the server-authoritative gameplay config pushed by
 * {@link com.rfizzle.meridian.network.ConfigSyncPayload} (#149).
 *
 * <p>Gameplay-affecting client code (tooltips, UI math) must read {@link #effective()} rather than
 * {@link Meridian#getConfig()} directly, so it honors the server's values with the local config as a
 * fallback only when no server value is present (true singleplayer, or before the join payload
 * arrives). Client-only {@code display} preferences are excluded from the synced view and continue
 * to be read from the local config directly.
 *
 * <p>The synced copy is cleared on disconnect so stale server rules never bleed into the next world.
 * The field is {@code volatile}: the receiver writes it on the client thread while renderers read it.
 */
public final class ClientMeridianConfig {

    @Nullable
    private static volatile MeridianConfig serverConfig;

    private ClientMeridianConfig() {
    }

    /** Stores the config decoded from a {@link com.rfizzle.meridian.network.ConfigSyncPayload}. */
    public static void setServerConfig(@Nullable MeridianConfig config) {
        serverConfig = config;
    }

    /** The raw server-synced config, or {@code null} when none has arrived (standalone/singleplayer). */
    @Nullable
    public static MeridianConfig getServerConfig() {
        return serverConfig;
    }

    /** Clears the synced copy; call on disconnect so the next world falls back to the local config. */
    public static void clear() {
        serverConfig = null;
    }

    /**
     * The config a gameplay-affecting client reader should use: the server-synced copy when present,
     * otherwise the local file. Never {@code null} — the local config is always loaded by the time a
     * client screen or tooltip renders.
     */
    public static MeridianConfig effective() {
        MeridianConfig synced = serverConfig;
        return synced != null ? synced : Meridian.getConfig();
    }
}
