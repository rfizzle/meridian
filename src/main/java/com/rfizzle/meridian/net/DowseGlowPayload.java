package com.rfizzle.meridian.net;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.MiningEnchantMath;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/**
 * S2C reveal pulse for Dowse: the ore-vein positions the acting player's client should glow
 * through walls for {@link MiningEnchantMath#DOWSE_GLOW_TICKS}. Sent only to the one player who
 * triggered the pulse, and only when the server actually found a vein. The list is capped at
 * {@link MiningEnchantMath#DOWSE_MAX_VEIN} on both ends so a malformed packet can't flood the client.
 */
public record DowseGlowPayload(List<BlockPos> positions) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DowseGlowPayload> TYPE =
            new CustomPacketPayload.Type<>(Meridian.id("dowse_glow"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DowseGlowPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MiningEnchantMath.DOWSE_MAX_VEIN)),
                    DowseGlowPayload::positions,
                    DowseGlowPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
