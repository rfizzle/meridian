package com.rfizzle.meridian.api;

import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.PowerFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Per-enchantment configuration data. Populated from the JSON config's
 * {@code enchantmentOverrides} section (server-side) or from the server's sync payload
 * (client-side). Look entries up via {@link MeridianAPI#getEnchantmentInfo}.
 *
 * <p>Mirrors Zenith's {@code EnchantmentInfo} record with the same fields
 * and power function model.
 *
 * @param ench         the enchantment this info describes
 * @param maxLevel     configured max level rollable at the table (before the hard cap)
 * @param maxLootLevel configured max level for loot tables and trades (before the hard cap)
 * @param levelCap     hard cap applied on top of both maxima; {@code <= 0} means uncapped
 * @param maxPower     power function for the maximum enchanting power per level
 * @param minPower     power function for the minimum enchanting power per level
 * @param enabled      {@code false} when the enchantment is disabled by config
 */
@Stable
public record EnchantmentInfo(
        Holder<Enchantment> ench,
        int maxLevel,
        int maxLootLevel,
        int levelCap,
        PowerFunction maxPower,
        PowerFunction minPower,
        boolean enabled
) {

    /**
     * Wire codec for the server-to-client info sync. Not part of the stable API surface —
     * the sync protocol may change between releases.
     */
    @org.jetbrains.annotations.ApiStatus.Internal
    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantmentInfo> STREAM_CODEC = new StreamCodec<>() {
        private static final StreamCodec<RegistryFriendlyByteBuf, Holder<Enchantment>> ENCH_CODEC =
                ByteBufCodecs.holderRegistry(Registries.ENCHANTMENT);

        @Override
        public EnchantmentInfo decode(RegistryFriendlyByteBuf buf) {
            Holder<Enchantment> ench = ENCH_CODEC.decode(buf);
            int maxLevel = ByteBufCodecs.VAR_INT.decode(buf);
            int maxLootLevel = ByteBufCodecs.VAR_INT.decode(buf);
            int levelCap = ByteBufCodecs.VAR_INT.decode(buf);
            PowerFunction maxPower = PowerFunction.STREAM_CODEC.decode(buf);
            PowerFunction minPower = PowerFunction.STREAM_CODEC.decode(buf);
            boolean enabled = buf.readBoolean();
            return new EnchantmentInfo(ench, maxLevel, maxLootLevel, levelCap, maxPower, minPower, enabled);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnchantmentInfo info) {
            ENCH_CODEC.encode(buf, info.ench());
            ByteBufCodecs.VAR_INT.encode(buf, info.maxLevel());
            ByteBufCodecs.VAR_INT.encode(buf, info.maxLootLevel());
            ByteBufCodecs.VAR_INT.encode(buf, info.levelCap());
            PowerFunction.STREAM_CODEC.encode(buf, info.maxPower());
            PowerFunction.STREAM_CODEC.encode(buf, info.minPower());
            buf.writeBoolean(info.enabled());
        }
    };

    /**
     * Effective max level rollable at the table, respecting the hard cap if set.
     *
     * @return {@code min(levelCap, maxLevel)} when capped, otherwise {@code maxLevel}
     */
    public int getMaxLevel() {
        return levelCap > 0 ? Math.min(levelCap, maxLevel) : maxLevel;
    }

    /**
     * Effective max loot level (for loot tables and villager trades), respecting the hard cap.
     *
     * @return {@code min(levelCap, maxLootLevel)} when capped, otherwise {@code maxLootLevel}
     */
    public int getMaxLootLevel() {
        return levelCap > 0 ? Math.min(levelCap, maxLootLevel) : maxLootLevel;
    }

    /**
     * Minimum enchanting power required to roll the given level.
     *
     * @param level the enchantment level
     * @return the minimum power for {@code level}
     */
    public int getMinPower(int level) {
        return minPower.getPower(level);
    }

    /**
     * Maximum enchanting power at which the given level still rolls.
     *
     * @param level the enchantment level
     * @return the maximum power for {@code level}
     */
    public int getMaxPower(int level) {
        return maxPower.getPower(level);
    }

    /**
     * Creates an info record using vanilla defaults for an enchantment with no config override.
     *
     * @param ench the enchantment holder
     * @return an info record mirroring the enchantment's vanilla values
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
     * Creates an info record by merging a config override with vanilla defaults. Construction
     * hook for Meridian's own config pipeline — not part of the stable API surface.
     *
     * @param ench     the enchantment holder
     * @param override the config override block
     * @return the merged info record
     */
    @org.jetbrains.annotations.ApiStatus.Internal
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
