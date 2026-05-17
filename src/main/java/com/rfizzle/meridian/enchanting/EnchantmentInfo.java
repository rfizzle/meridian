package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Per-enchantment configuration data. Populated from the JSON config's
 * {@code enchantmentOverrides} section (server-side) or from an
 * {@link com.rfizzle.meridian.net.EnchantmentInfoPayload} (client-side).
 *
 * <p>Mirrors Zenith's {@code EnchantmentInfo} record with the same fields
 * and power function model.
 */
public record EnchantmentInfo(
        Holder<Enchantment> ench,
        int maxLevel,
        int maxLootLevel,
        int levelCap,
        PowerFunction maxPower,
        PowerFunction minPower,
        boolean enabled
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantmentInfo> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EnchantmentInfo decode(RegistryFriendlyByteBuf buf) {
            Holder<Enchantment> ench = ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT).decode(buf);
            int maxLevel = ByteBufCodecs.VAR_INT.decode(buf);
            int maxLootLevel = ByteBufCodecs.VAR_INT.decode(buf);
            int levelCap = ByteBufCodecs.VAR_INT.decode(buf);
            PowerFunction maxPower = PowerFunction.STREAM_CODEC.decode(buf);
            PowerFunction minPower = PowerFunction.STREAM_CODEC.decode(buf);
            boolean enabled = buf.readBoolean();
            return new EnchantmentInfo(ench, maxLevel, maxLootLevel, levelCap, maxPower, minPower, enabled);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnchantmentInfo info) {
            ((StreamCodec<RegistryFriendlyByteBuf, Holder<Enchantment>>) (StreamCodec<?, ?>)
                    ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT)).encode(buf, info.ench());
            ByteBufCodecs.VAR_INT.encode(buf, info.maxLevel());
            ByteBufCodecs.VAR_INT.encode(buf, info.maxLootLevel());
            ByteBufCodecs.VAR_INT.encode(buf, info.levelCap());
            PowerFunction.STREAM_CODEC.encode(buf, info.maxPower());
            PowerFunction.STREAM_CODEC.encode(buf, info.minPower());
            buf.writeBoolean(info.enabled());
        }
    };

    /**
     * Effective max level, respecting the hard cap if set.
     */
    public int getMaxLevel() {
        return levelCap > 0 ? Math.min(levelCap, maxLevel) : maxLevel;
    }

    /**
     * Effective max loot level (for loot tables and villager trades), respecting the hard cap.
     */
    public int getMaxLootLevel() {
        return levelCap > 0 ? Math.min(levelCap, maxLootLevel) : maxLootLevel;
    }

    public int getMinPower(int level) {
        return minPower.getPower(level);
    }

    public int getMaxPower(int level) {
        return maxPower.getPower(level);
    }

    /**
     * Creates an info record using vanilla defaults for an enchantment with no config override.
     */
    public static EnchantmentInfo fallback(Holder<Enchantment> ench) {
        Enchantment e = ench.value();
        return new EnchantmentInfo(
                ench, e.getMaxLevel(), e.getMaxLevel(), -1,
                PowerFunction.DefaultMaxPowerFunction.INSTANCE,
                new PowerFunction.DefaultMinPowerFunction(ench),
                true);
    }

    /**
     * Creates an info record by merging a config override with vanilla defaults.
     */
    public static EnchantmentInfo fromOverride(
            Holder<Enchantment> ench, MeridianConfig.EnchantmentOverride override) {
        Enchantment e = ench.value();
        int maxLevel = override.maxLevel > 0 ? override.maxLevel : e.getMaxLevel();
        int maxLootLevel = override.maxLootLevel > 0 ? override.maxLootLevel : e.getMaxLevel();
        int levelCap = override.levelCap;
        PowerFunction minPower = resolvePowerFunction(override.minPowerFunction,
                new PowerFunction.DefaultMinPowerFunction(ench));
        PowerFunction maxPower = resolvePowerFunction(override.maxPowerFunction,
                PowerFunction.DefaultMaxPowerFunction.INSTANCE);
        return new EnchantmentInfo(ench, maxLevel, maxLootLevel, levelCap, maxPower, minPower, override.enabled);
    }

    private static PowerFunction resolvePowerFunction(
            MeridianConfig.PowerFunctionConfig pfc, PowerFunction fallback) {
        if (pfc == null || "default".equals(pfc.type)) return fallback;
        return switch (pfc.type) {
            case "linear" -> new PowerFunction.LinearPowerFunction(pfc.base, pfc.perLevel);
            case "fixed" -> new PowerFunction.FixedPowerFunction(pfc.value);
            default -> fallback;
        };
    }
}
