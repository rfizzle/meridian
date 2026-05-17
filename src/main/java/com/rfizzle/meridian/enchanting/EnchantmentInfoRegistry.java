package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.net.EnchantmentInfoPayload;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Stores per-enchantment configuration ({@link EnchantmentInfo}) for every registered enchantment.
 *
 * <p>Server-side: populated by {@link #rebuild} on server start and datapack reload, merging
 * config overrides with vanilla defaults.
 *
 * <p>Client-side: populated by {@link #applyFromPayload} when the server sends
 * {@link EnchantmentInfoPayload}.
 */
public final class EnchantmentInfoRegistry {

    private static final Map<ResourceKey<Enchantment>, EnchantmentInfo> INFO = new HashMap<>();
    private static final IdentityHashMap<Enchantment, EnchantmentInfo> INSTANCE_INFO = new IdentityHashMap<>();

    private EnchantmentInfoRegistry() {
    }

    /**
     * Returns the info for the given enchantment, falling back to vanilla defaults if not present.
     */
    public static EnchantmentInfo getInfo(Holder<Enchantment> ench) {
        ResourceKey<Enchantment> key = ench.unwrapKey().orElse(null);
        if (key != null) {
            EnchantmentInfo info = INFO.get(key);
            if (info != null) return info;
        }
        return EnchantmentInfo.fallback(ench);
    }

    /**
     * Returns the info for an {@link Enchantment} instance, or {@code null} if the registry
     * has not been populated yet. Used by the {@code getMaxLevel()} mixin where only the
     * bare instance is available (no {@link Holder}).
     */
    public static EnchantmentInfo getInfoByInstance(Enchantment ench) {
        return INSTANCE_INFO.get(ench);
    }

    public static Map<ResourceKey<Enchantment>, EnchantmentInfo> getAll() {
        return Collections.unmodifiableMap(INFO);
    }

    /**
     * Rebuilds the registry from the enchantment registry + config overrides. Called on server
     * start and after datapack reload.
     */
    public static void rebuild(Registry<Enchantment> registry, MeridianConfig config) {
        INFO.clear();
        INSTANCE_INFO.clear();
        Map<String, MeridianConfig.EnchantmentOverride> overrides =
                config.enchantmentOverrides != null ? config.enchantmentOverrides : Map.of();
        int overrideCount = 0;
        for (Holder.Reference<Enchantment> holder :
                (Iterable<Holder.Reference<Enchantment>>) registry.holders()::iterator) {
            ResourceKey<Enchantment> key = holder.key();
            MeridianConfig.EnchantmentOverride override =
                    overrides.get(key.location().toString());
            EnchantmentInfo info;
            if (override != null) {
                info = EnchantmentInfo.fromOverride(holder, override);
                overrideCount++;
            } else {
                info = EnchantmentInfo.fallback(holder);
            }
            INFO.put(key, info);
            INSTANCE_INFO.put(holder.value(), info);
        }
        Meridian.LOGGER.info(
                "Rebuilt enchantment info registry: {} enchantments, {} overrides",
                INFO.size(), overrideCount);
    }

    /**
     * Client-side: replaces the local registry with data received from the server.
     */
    public static void applyFromPayload(Map<ResourceKey<Enchantment>, EnchantmentInfo> data) {
        INFO.clear();
        INSTANCE_INFO.clear();
        INFO.putAll(data);
        for (EnchantmentInfo info : data.values()) {
            INSTANCE_INFO.put(info.ench().value(), info);
        }
    }

    /**
     * Builds the payload to sync the full registry to a client.
     */
    public static EnchantmentInfoPayload buildPayload() {
        return new EnchantmentInfoPayload(new HashMap<>(INFO));
    }
}
