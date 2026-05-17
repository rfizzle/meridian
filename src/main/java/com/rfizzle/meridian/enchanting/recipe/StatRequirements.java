package com.rfizzle.meridian.enchanting.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Eterna / Quanta / Arcana thresholds read from an enchantment-table crafting recipe.
 *
 * <p>Used in two directions by {@link EnchantingRecipe}:
 * <ul>
 *   <li>{@code requirements} — minimum stat totals the table must reach before the recipe matches.</li>
 *   <li>{@code maxRequirements} — optional upper bounds. A value of {@code -1} on any axis means
 *       "no upper bound" and matches Zenith's sentinel; see {@link #NO_MAX}.</li>
 * </ul>
 *
 * <p>Stored as {@code float} because the Zenith data (and the stat pipeline) uses fractional
 * values like {@code 22.5}.
 */
public record StatRequirements(float eterna, float quanta, float arcana) {

    /** Sentinel for "no upper bound on any axis" — matches Zenith's `-1` convention. */
    public static final StatRequirements NO_MAX = new StatRequirements(-1F, -1F, -1F);

    public static final Codec<StatRequirements> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("eterna", 0F).forGetter(StatRequirements::eterna),
            Codec.FLOAT.optionalFieldOf("quanta", 0F).forGetter(StatRequirements::quanta),
            Codec.FLOAT.optionalFieldOf("arcana", 0F).forGetter(StatRequirements::arcana)
    ).apply(instance, StatRequirements::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StatRequirements> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StatRequirements::eterna,
            ByteBufCodecs.FLOAT, StatRequirements::quanta,
            ByteBufCodecs.FLOAT, StatRequirements::arcana,
            StatRequirements::new);
}
