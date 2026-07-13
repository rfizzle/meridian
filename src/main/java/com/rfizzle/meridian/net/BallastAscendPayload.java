package com.rfizzle.meridian.net;

import com.rfizzle.meridian.Meridian;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S notice of whether the player is currently holding jump to rise with Ballast, sent by the
 * client only when that intent changes (edge-triggered) while they are in water wearing the
 * enchant. The server still re-checks the enchant and the water gate before applying any lift
 * ({@code ArmorTickHandler#handleBallast}); the packet only supplies the one input the server
 * cannot observe for a real player — a held jump key.
 */
public record BallastAscendPayload(boolean rising) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BallastAscendPayload> TYPE =
            new CustomPacketPayload.Type<>(Meridian.id("ballast_ascend"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BallastAscendPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, BallastAscendPayload::rising,
                    BallastAscendPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
