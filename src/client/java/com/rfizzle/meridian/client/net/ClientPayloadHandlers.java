package com.rfizzle.meridian.client.net;

import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import com.rfizzle.meridian.net.CluesPayload;
import com.rfizzle.meridian.net.EnchantingStatSyncPayload;
import com.rfizzle.meridian.net.EnchantmentInfoPayload;
import com.rfizzle.meridian.net.StatsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/**
 * S2C receivers for the enchanting payloads. The stats payload is forwarded to the open
 * {@link MeridianEnchantmentMenu} so {@code MeridianEnchantmentScreen} can read live stat values from
 * the menu instance. Clues are forwarded to the menu's per-slot clue cache for tooltip rendering.
 * The enchantment info payload updates the client-side {@link EnchantmentInfoRegistry}.
 * The enchanting stat sync payload updates the client-side {@link EnchantingStatRegistry}.
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
                (payload, context) -> context.client().execute(() ->
                        EnchantmentInfoRegistry.applyFromPayload(payload.info())));

        ClientPlayNetworking.registerGlobalReceiver(EnchantingStatSyncPayload.TYPE,
                (payload, context) -> context.client().execute(() -> {
                    List<Map.Entry<ResourceLocation, EnchantingStats>> tagEntries =
                            payload.tags().stream()
                                    .map(t -> Map.entry(t.tagId(), t.stats()))
                                    .toList();
                    EnchantingStatRegistry.getInstance().applySync(payload.blocks(), tagEntries);
                }));
    }
}
