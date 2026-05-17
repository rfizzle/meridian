package com.rfizzle.meridian.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Single preview-slot hint sent alongside {@link CluesPayload}. Ships the enchantment as a
 * {@link ResourceKey} because the 1.21.1 enchantment registry is dynamic — there are no stable
 * integer IDs, and {@code ResourceKey} carries the reload-safety a raw {@code ResourceLocation}
 * wouldn't.
 */
public record EnchantmentClue(ResourceKey<Enchantment> enchantment, int level) {

    public static final StreamCodec<ByteBuf, EnchantmentClue> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(Registries.ENCHANTMENT), EnchantmentClue::enchantment,
            ByteBufCodecs.VAR_INT, EnchantmentClue::level,
            EnchantmentClue::new);
}
