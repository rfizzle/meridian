package com.rfizzle.meridian.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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

    public static final MapCodec<EnchantingStats> MAP_CODEC = RecordCodecBuilder.<EnchantingStats>mapCodec(inst -> inst
            .group(
                    Codec.FLOAT.optionalFieldOf("maxEterna", 0F).forGetter(EnchantingStats::maxEterna),
                    Codec.FLOAT.optionalFieldOf("eterna", 0F).forGetter(EnchantingStats::eterna),
                    Codec.FLOAT.optionalFieldOf("quanta", 0F).forGetter(EnchantingStats::quanta),
                    Codec.FLOAT.optionalFieldOf("arcana", 0F).forGetter(EnchantingStats::arcana),
                    Codec.FLOAT.optionalFieldOf("rectification", 0F).forGetter(EnchantingStats::rectification),
                    Codec.INT.optionalFieldOf("clues", 0).forGetter(EnchantingStats::clues))
            .apply(inst, EnchantingStats::new))
            .flatXmap(EnchantingStats::validate, EnchantingStats::validate);

    private static DataResult<EnchantingStats> validate(EnchantingStats stats) {
        if (stats.maxEterna() < -50F) return DataResult.error(() -> "maxEterna must be >= -50");
        if (stats.eterna() < -50F) return DataResult.error(() -> "eterna must be >= -50");
        if (stats.quanta() < -50F) return DataResult.error(() -> "quanta must be >= -50");
        if (stats.arcana() < -50F) return DataResult.error(() -> "arcana must be >= -50");
        if (stats.rectification() < -50F) return DataResult.error(() -> "rectification must be >= -50");
        if (stats.clues() < -50) return DataResult.error(() -> "clues must be >= -50");
        return DataResult.success(stats);
    }

    public static final Codec<EnchantingStats> CODEC = MAP_CODEC.codec();
}
