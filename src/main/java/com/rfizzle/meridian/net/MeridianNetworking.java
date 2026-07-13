package com.rfizzle.meridian.net;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.event.LoftHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

/**
 * Registers every S2C payload the enchanting mod sends, and wires the server-side lifecycle
 * hooks that keep client registries in sync. Called from the main
 * {@link com.rfizzle.meridian.Meridian#onInitialize} during mod load so the types are
 * resolvable before any play-phase traffic starts.
 */
public final class MeridianNetworking {

    private MeridianNetworking() {
    }

    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(StatsPayload.TYPE, StatsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CluesPayload.TYPE, CluesPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnchantmentInfoPayload.TYPE, EnchantmentInfoPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(EnchantingStatSyncPayload.TYPE, EnchantingStatSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ConfigSyncPayload.TYPE, ConfigSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DowseGlowPayload.TYPE, DowseGlowPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(LoftJumpPayload.TYPE, LoftJumpPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(LoftJumpPayload.TYPE, (payload, context) ->
                context.player().server.execute(() -> LoftHandler.tryAirJump(context.player())));
    }

    /**
     * Registers the server-side lifecycle hooks that keep {@link EnchantingStatRegistry}
     * in sync on all connected clients. Must be called during
     * {@link com.rfizzle.meridian.Meridian#onInitialize} after
     * {@link #registerPayloads()}.
     *
     * <ul>
     *   <li>{@code END_DATA_PACK_RELOAD} — re-syncs after {@code /reload}.</li>
     *   <li>{@code JOIN} — sends the current snapshot and the server config to each joining player.</li>
     * </ul>
     */
    public static void registerLifecycleHandlers() {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (!success) return;
            EnchantingStatSyncPayload payload = buildSyncPayload();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(player, payload);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayNetworking.send(handler.player, buildSyncPayload());
            sendConfig(handler.player);
        });
    }

    /**
     * Sends the server-authoritative gameplay config to a single player (#149). The {@code canSend}
     * guard skips a client (e.g. vanilla) that has not registered the receiver.
     */
    public static void sendConfig(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, ConfigSyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, buildConfigPayload());
        }
    }

    /**
     * Re-broadcasts the current server config to every connected player. Called from
     * {@link com.rfizzle.meridian.Meridian#reloadConfig(MinecraftServer)} so a
     * {@code /meridian reload} reaches connected clients without a reconnect.
     */
    public static void syncConfigToAll(MinecraftServer server) {
        ConfigSyncPayload payload = buildConfigPayload();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, ConfigSyncPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }

    private static ConfigSyncPayload buildConfigPayload() {
        return new ConfigSyncPayload(Meridian.getConfig().toSyncJson());
    }

    private static EnchantingStatSyncPayload buildSyncPayload() {
        EnchantingStatRegistry reg = EnchantingStatRegistry.getInstance();
        Map<ResourceLocation, EnchantingStats> blocks = reg.blockEntries();
        List<EnchantingStatSyncPayload.TagEntry> tags = reg.tagEntriesForSync().stream()
                .map(e -> new EnchantingStatSyncPayload.TagEntry(e.getKey(), e.getValue()))
                .toList();
        return new EnchantingStatSyncPayload(blocks, tags);
    }
}
