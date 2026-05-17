package com.rfizzle.meridian.enchanting;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Converts an enchantment level to a required enchanting power value. Used by
 * {@link EnchantmentInfo} to determine what power level is needed for each
 * enchantment level at the enchanting table.
 */
public sealed interface PowerFunction {

    StreamCodec<RegistryFriendlyByteBuf, PowerFunction> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PowerFunction decode(RegistryFriendlyByteBuf buf) {
            Type type = Type.values()[buf.readByte()];
            return switch (type) {
                case DEFAULT_MIN -> DefaultMinPowerFunction.INNER_CODEC.decode(buf);
                case DEFAULT_MAX -> DefaultMaxPowerFunction.INSTANCE;
                case LINEAR -> LinearPowerFunction.INNER_CODEC.decode(buf);
                case FIXED -> FixedPowerFunction.INNER_CODEC.decode(buf);
            };
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, PowerFunction value) {
            buf.writeByte(value.getType().ordinal());
            if (value instanceof DefaultMinPowerFunction dmp) {
                DefaultMinPowerFunction.INNER_CODEC.encode(buf, dmp);
            } else if (value instanceof LinearPowerFunction lp) {
                LinearPowerFunction.INNER_CODEC.encode(buf, lp);
            } else if (value instanceof FixedPowerFunction fp) {
                FixedPowerFunction.INNER_CODEC.encode(buf, fp);
            }
        }
    };

    int getPower(int level);

    Type getType();

    enum Type {
        DEFAULT_MIN,
        DEFAULT_MAX,
        LINEAR,
        FIXED
    }

    /**
     * Delegates to the enchantment's {@link Enchantment#getMinCost(int)} for levels at or below
     * vanilla max. For levels above vanilla max, extrapolates using the cost slope raised to the
     * 1.6 power — making higher levels progressively harder to roll at the table.
     */
    record DefaultMinPowerFunction(Holder<Enchantment> enchHolder) implements PowerFunction {

        static final StreamCodec<RegistryFriendlyByteBuf, DefaultMinPowerFunction> INNER_CODEC =
                ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT)
                        .map(DefaultMinPowerFunction::new, DefaultMinPowerFunction::enchHolder);

        @Override
        public int getPower(int level) {
            Enchantment ench = enchHolder.value();
            int vanillaMax = ench.getMaxLevel();
            if (level > vanillaMax && level > 1) {
                int diff = ench.getMinCost(vanillaMax) - ench.getMinCost(vanillaMax - 1);
                if (diff == 0) diff = 15;
                return ench.getMinCost(level) + diff * (int) Math.pow(level - vanillaMax, 1.6);
            }
            return ench.getMinCost(level);
        }

        @Override
        public Type getType() {
            return Type.DEFAULT_MIN;
        }
    }

    /**
     * Always returns 200 (the enchanting table power ceiling). Any enchantment level whose
     * {@link DefaultMinPowerFunction min power} exceeds 200 is unreachable via the table.
     */
    final class DefaultMaxPowerFunction implements PowerFunction {

        public static final DefaultMaxPowerFunction INSTANCE = new DefaultMaxPowerFunction();

        private DefaultMaxPowerFunction() {
        }

        @Override
        public int getPower(int level) {
            return 200;
        }

        @Override
        public Type getType() {
            return Type.DEFAULT_MAX;
        }
    }

    /**
     * Returns {@code base + perLevel * level}. Covers the most common enchantment cost curves.
     */
    record LinearPowerFunction(int base, int perLevel) implements PowerFunction {

        static final StreamCodec<RegistryFriendlyByteBuf, LinearPowerFunction> INNER_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, LinearPowerFunction::base,
                        ByteBufCodecs.VAR_INT, LinearPowerFunction::perLevel,
                        LinearPowerFunction::new);

        @Override
        public int getPower(int level) {
            return base + perLevel * level;
        }

        @Override
        public Type getType() {
            return Type.LINEAR;
        }
    }

    /**
     * Always returns a fixed value regardless of level. Useful for flat max-power ceilings.
     */
    record FixedPowerFunction(int value) implements PowerFunction {

        static final StreamCodec<RegistryFriendlyByteBuf, FixedPowerFunction> INNER_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, FixedPowerFunction::value,
                        FixedPowerFunction::new);

        @Override
        public int getPower(int level) {
            return value;
        }

        @Override
        public Type getType() {
            return Type.FIXED;
        }
    }
}
