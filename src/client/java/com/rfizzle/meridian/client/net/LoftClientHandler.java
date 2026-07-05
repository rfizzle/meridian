package com.rfizzle.meridian.client.net;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.net.LoftJumpPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Client side of Loft's mid-air jump: edge-detects a fresh jump-key press while airborne
 * and asks the server to jump. The checks here only exist to avoid needless packets — the
 * server re-validates everything in {@code LoftHandler#tryAirJump}, including the
 * one-jump-per-airtime budget, which the client doesn't track.
 */
public final class LoftClientHandler {

    private static boolean jumpKeyWasDown;

    private LoftClientHandler() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            LocalPlayer player = client.player;
            if (player == null) {
                jumpKeyWasDown = false;
                return;
            }

            boolean down = client.options.keyJump.isDown();
            boolean pressed = down && !jumpKeyWasDown;
            jumpKeyWasDown = down;
            if (!pressed) return;

            if (player.onGround() || player.isFallFlying() || player.isPassenger()
                    || player.isInWaterOrBubble() || player.isInLava()
                    || player.isSpectator() || player.getAbilities().flying) {
                return;
            }
            if (EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.LOFT, EquipmentSlot.FEET) <= 0) {
                return;
            }

            ClientPlayNetworking.send(new LoftJumpPayload());
        });
    }
}
