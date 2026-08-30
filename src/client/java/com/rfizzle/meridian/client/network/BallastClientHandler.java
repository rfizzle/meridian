package com.rfizzle.meridian.client.network;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.network.BallastAscendPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Client side of Ballast's rise: reports whether the local player is holding jump to swim up with
 * the enchant, but only when that intent changes (edge-triggered), so a held key is a single packet
 * rather than one per tick. Sinking on crouch needs nothing from here — the server reads that
 * itself; only the held jump key is input the server can't observe. The server re-checks the enchant
 * and water gate in {@code ArmorTickHandler#handleBallast}, so a lie here can't fly the player.
 */
public final class BallastClientHandler {

    private static boolean lastReportedRising;

    private BallastClientHandler() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            boolean rising = player != null
                    && client.options.keyJump.isDown()
                    && player.isInWater()
                    && EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.BALLAST,
                            EquipmentSlot.LEGS, EquipmentSlot.FEET) > 0;

            if (rising == lastReportedRising) return;
            lastReportedRising = rising;
            if (player == null) return;
            ClientPlayNetworking.send(new BallastAscendPayload(rising));
        });
    }
}
