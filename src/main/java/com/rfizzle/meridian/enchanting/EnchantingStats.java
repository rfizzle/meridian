package com.rfizzle.meridian.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EnchantingStats(
        float maxEterna,
        float eterna,
        float quanta,
        float arcana,
        float rectification,
        int clues
) {
    public static final EnchantingStats ZERO = new EnchantingStats(0F, 0F, 0F, 0F, 0F, 0);

    public static final MapCodec<EnchantingStats> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst
            .group(
                    Codec.FLOAT.optionalFieldOf("maxEterna", 0F).forGetter(EnchantingStats::maxEterna),
                    Codec.FLOAT.optionalFieldOf("eterna", 0F).forGetter(EnchantingStats::eterna),
                    Codec.FLOAT.optionalFieldOf("quanta", 0F).forGetter(EnchantingStats::quanta),
                    Codec.FLOAT.optionalFieldOf("arcana", 0F).forGetter(EnchantingStats::arcana),
                    Codec.FLOAT.optionalFieldOf("rectification", 0F).forGetter(EnchantingStats::rectification),
                    Codec.INT.optionalFieldOf("clues", 0).forGetter(EnchantingStats::clues))
            .apply(inst, EnchantingStats::new));

    public static final Codec<EnchantingStats> CODEC = MAP_CODEC.codec();
}
