package com.rfizzle.meridian.network;

import com.rfizzle.meridian.Meridian;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * S2C payload carrying the server's authoritative gameplay config as a compact JSON string
 * ({@link com.rfizzle.meridian.config.MeridianConfig#toSyncJson()}). Sent on player join and after
 * {@code /meridian reload} so a connected client's UI/tooltip math honors the server's rules
 * instead of its own local {@code meridian.json}.
 *
 * <p>The client stores this as the server-authoritative copy and reads it first, falling back to
 * its local config only in true singleplayer or when no server value has arrived yet. The client
 * still reads its own local {@code display} preferences — those are intentionally excluded from
 * the synced view (see {@code toSyncJson()}).
 */
public record ConfigSyncPayload(String configJson) implements CustomPacketPayload {

    // Cap the serialized config JSON. writeUtf/readUtf enforce a char limit; the cap sits well above
    // the realistic config size (the default is ~1k chars, leaving headroom for user-grown
    // enchantmentOverrides maps) while bounding a hostile server's payload below writeUtf's 32767
    // hard limit. If a future config addition legitimately exceeds this, the codec throws
    // EncoderException — a deliberate fail-fast signal to bump the cap or switch to per-field encoding.
    public static final int MAX_CONFIG_JSON_CHARS = 16384;

    public static final Type<ConfigSyncPayload> TYPE =
            new Type<>(Meridian.id("config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPayload> CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeUtf(payload.configJson, MAX_CONFIG_JSON_CHARS),
                    buf -> new ConfigSyncPayload(buf.readUtf(MAX_CONFIG_JSON_CHARS)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
