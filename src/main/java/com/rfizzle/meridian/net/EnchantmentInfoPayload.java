package com.rfizzle.meridian.net;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantmentInfo;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;

/**
 * S2C payload that syncs the full per-enchantment configuration from server to client. Sent on
 * player join and after datapack/config reload so the client's
 * {@link com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry} stays in sync.
 */
public record EnchantmentInfoPayload(
        Map<ResourceKey<Enchantment>, EnchantmentInfo> info
) implements CustomPacketPayload {

    public static final Type<EnchantmentInfoPayload> TYPE =
            new Type<>(Meridian.id("enchantment_info"));

    private static final StreamCodec<ByteBuf, ResourceKey<Enchantment>> KEY_CODEC =
            ResourceKey.streamCodec(Registries.ENCHANTMENT);

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantmentInfoPayload> CODEC =
            StreamCodec.of(EnchantmentInfoPayload::encode, EnchantmentInfoPayload::decode);

    private static void encode(RegistryFriendlyByteBuf buf, EnchantmentInfoPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buf, payload.info.size());
        for (var entry : payload.info.entrySet()) {
            KEY_CODEC.encode(buf, entry.getKey());
            EnchantmentInfo.STREAM_CODEC.encode(buf, entry.getValue());
        }
    }

    private static EnchantmentInfoPayload decode(RegistryFriendlyByteBuf buf) {
        int size = ByteBufCodecs.VAR_INT.decode(buf);
        Map<ResourceKey<Enchantment>, EnchantmentInfo> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            ResourceKey<Enchantment> key = KEY_CODEC.decode(buf);
            EnchantmentInfo info = EnchantmentInfo.STREAM_CODEC.decode(buf);
            map.put(key, info);
        }
        return new EnchantmentInfoPayload(map);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
