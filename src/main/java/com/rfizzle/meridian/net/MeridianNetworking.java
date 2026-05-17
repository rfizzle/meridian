package com.rfizzle.meridian.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Registers every S2C payload the enchanting mod sends. Called from the main
 * {@link com.rfizzle.meridian.Meridian#onInitialize} during mod load so the
 * types are resolvable before any play-phase traffic starts.
 */
public final class MeridianNetworking {

    private MeridianNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(StatsPayload.TYPE, StatsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CluesPayload.TYPE, CluesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnchantmentInfoPayload.TYPE, EnchantmentInfoPayload.CODEC);
    }
}
