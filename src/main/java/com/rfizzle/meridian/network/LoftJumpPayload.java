package com.rfizzle.meridian.network;

import com.rfizzle.meridian.Meridian;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * C2S request to perform Loft's mid-air jump, sent on an airborne jump-key press. Carries
 * no data — the server re-validates everything against its own view of the player
 * ({@code LoftHandler#tryAirJump}) and simply ignores requests that don't qualify.
 */
public record LoftJumpPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LoftJumpPayload> TYPE =
            new CustomPacketPayload.Type<>(Meridian.id("loft_jump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LoftJumpPayload> CODEC =
            StreamCodec.unit(new LoftJumpPayload());

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
