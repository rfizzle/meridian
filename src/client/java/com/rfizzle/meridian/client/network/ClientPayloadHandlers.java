package com.rfizzle.meridian.client.network;

import com.rfizzle.meridian.client.config.ClientMeridianConfig;
import com.rfizzle.meridian.client.render.ClientDowseState;
import com.rfizzle.meridian.compat.client.ViewerRefreshTrigger;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import com.rfizzle.meridian.enchanting.MiningEnchantMath;
import com.rfizzle.meridian.network.CluesPayload;
import com.rfizzle.meridian.network.ConfigSyncPayload;
import com.rfizzle.meridian.network.DowseGlowPayload;
import com.rfizzle.meridian.network.EnchantingStatSyncPayload;
import com.rfizzle.meridian.network.EnchantmentInfoPayload;
import com.rfizzle.meridian.network.StatsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * S2C receivers for the enchanting payloads. The stats payload is forwarded to the open
 * {@link MeridianEnchantmentMenu} so {@code MeridianEnchantmentScreen} can read live stat values from
 * the menu instance. Clues are forwarded to the menu's per-slot clue cache for tooltip rendering.
 * The enchantment info payload updates the client-side {@link EnchantmentInfoRegistry} and then
 * triggers a recipe-viewer refresh so the enchantment browser repopulates with server-configured
 * values without requiring a manual viewer reload. The enchanting stat sync payload updates the
 * client-side {@link EnchantingStatRegistry}; shelf info panels read it lazily at render time, so it
 * needs no viewer refresh. The config sync payload stores the server's authoritative gameplay config
 * into {@link ClientMeridianConfig} so gameplay-affecting client readers prefer it over the local file.
 */
public final class ClientPayloadHandlers {

    private ClientPayloadHandlers() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(StatsPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    LocalPlayer player = context.player();
                    if (player != null && player.containerMenu instanceof MeridianEnchantmentMenu menu) {
                        menu.applyClientStats(payload);
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(CluesPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    LocalPlayer player = context.player();
                    if (player != null && player.containerMenu instanceof MeridianEnchantmentMenu menu) {
                        menu.applyClientClues(payload.slot(), payload.clues(), payload.exhaustedList());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(EnchantmentInfoPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    EnchantmentInfoRegistry.applyFromPayload(payload.info());
                    ViewerRefreshTrigger.notifySync();
                }));

        ClientPlayNetworking.registerGlobalReceiver(EnchantingStatSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    List<Map.Entry<ResourceLocation, EnchantingStats>> tagEntries =
                            payload.tags().stream()
                                    .map(t -> Map.entry(t.tagId(), t.stats()))
                                    .toList();
                    EnchantingStatRegistry.getInstance().applySync(payload.blocks(), tagEntries);
                }));

        ClientPlayNetworking.registerGlobalReceiver(DowseGlowPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    ClientLevel level = context.client().level;
                    if (level == null) return;
                    long expiry = level.getGameTime() + MiningEnchantMath.DOWSE_GLOW_TICKS;
                    ClientDowseState.set(payload.positions(), expiry, level.dimension());
                }));

        ClientPlayNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE,
                (payload, context) -> {
                    // Decode off the client thread — GSON parsing is pure and has no client-state
                    // dependency — then publish the immutable result on the client thread.
                    MeridianConfig synced = MeridianConfig.fromJson(payload.configJson());
                    context.client().execute(() -> {
                        // Refresh the recipe viewers only when the sync actually flips a
                        // recipe-module toggle (#163): the EMI path is a full recipe reload, and
                        // every join already gets one via the enchantment-info payload — an
                        // unconditional second reload here would double that cost for nothing.
                        MeridianConfig before = ClientMeridianConfig.effective();
                        ClientMeridianConfig.setServerConfig(synced);
                        boolean moduleGatesChanged =
                                before.tableCrafting.allowDuplication != synced.tableCrafting.allowDuplication
                                        || before.everfeast.enabled != synced.everfeast.enabled;
                        if (moduleGatesChanged) {
                            ViewerRefreshTrigger.notifyConfigSync();
                        }
                    });
                });
    }
}
