package com.rfizzle.meridian.enchanting;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.api.EnchantmentInfo;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.net.EnchantmentInfoPayload;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Stores per-enchantment configuration ({@link EnchantmentInfo}) for every registered enchantment.
 *
 * <p>Server-side: populated by {@link #rebuild} on server start and datapack reload, merging
 * config overrides with vanilla defaults.
 *
 * <p>Client-side: populated by {@link #applyFromPayload} when the server sends
 * {@link EnchantmentInfoPayload}. {@link #hasSyncBeenReceived()} returns {@code true} only after
 * {@link #applyFromPayload} has been called at least once.
 *
 * <p>Both maps and the sync flag are published as a single atomic snapshot via an
 * {@link AtomicReference}, eliminating the consistency window between two separate volatile writes.
 */
public final class EnchantmentInfoRegistry {

    /**
     * Immutable snapshot of the registry contents. Published as a single unit so readers always
     * observe a consistent pair of maps together with the {@code syncReceived} flag.
     */
    record EnchantmentInfoSnapshot(
            Map<ResourceKey<Enchantment>, EnchantmentInfo> byKey,
            Map<Enchantment, EnchantmentInfo> byInstance,
            boolean syncReceived) {

        static final EnchantmentInfoSnapshot EMPTY =
                new EnchantmentInfoSnapshot(Map.of(), Map.of(), false);
    }

    private static final AtomicReference<EnchantmentInfoSnapshot> SNAPSHOT =
            new AtomicReference<>(EnchantmentInfoSnapshot.EMPTY);

    private EnchantmentInfoRegistry() {
    }

    /**
     * Returns {@code true} if the client has received at least one {@link EnchantmentInfoPayload}
     * from the server. Always {@code false} before the first {@link #applyFromPayload} call and
     * after {@link #clear()}.
     */
    public static boolean hasSyncBeenReceived() {
        return SNAPSHOT.get().syncReceived();
    }

    /**
     * Returns the info for the given enchantment, falling back to vanilla defaults if not present.
     */
    public static EnchantmentInfo getInfo(Holder<Enchantment> ench) {
        EnchantmentInfoSnapshot snap = SNAPSHOT.get();
        ResourceKey<Enchantment> key = ench.unwrapKey().orElse(null);
        if (key != null) {
            EnchantmentInfo info = snap.byKey().get(key);
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
        return SNAPSHOT.get().byInstance().get(ench);
    }

    public static Map<ResourceKey<Enchantment>, EnchantmentInfo> getAll() {
        return SNAPSHOT.get().byKey();
    }

    /**
     * Rebuilds the registry from the enchantment registry + config overrides. Called on server
     * start and after datapack reload.
     */
    public static void rebuild(Registry<Enchantment> registry, MeridianConfig config) {
        Map<ResourceKey<Enchantment>, EnchantmentInfo> newByKey = new HashMap<>();
        Map<Enchantment, EnchantmentInfo> newByInstance = new IdentityHashMap<>();
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
            newByKey.put(key, info);
            newByInstance.put(holder.value(), info);
        }
        Map<ResourceKey<Enchantment>, EnchantmentInfo> immutableByKey = Map.copyOf(newByKey);
        Map<Enchantment, EnchantmentInfo> immutableByInstance = Collections.unmodifiableMap(newByInstance);
        int total = immutableByKey.size();
        SNAPSHOT.set(new EnchantmentInfoSnapshot(immutableByKey, immutableByInstance, false));
        Meridian.LOGGER.info(
                "Rebuilt enchantment info registry: {} enchantments, {} overrides",
                total, overrideCount);
    }

    /**
     * Client-side: replaces the local registry with data received from the server and marks
     * {@link #hasSyncBeenReceived()} as {@code true}.
     */
    public static void applyFromPayload(Map<ResourceKey<Enchantment>, EnchantmentInfo> data) {
        Map<Enchantment, EnchantmentInfo> newByInstance = new IdentityHashMap<>();
        for (EnchantmentInfo info : data.values()) {
            newByInstance.put(info.ench().value(), info);
        }
        Map<ResourceKey<Enchantment>, EnchantmentInfo> immutableByKey = Map.copyOf(data);
        Map<Enchantment, EnchantmentInfo> immutableByInstance = Collections.unmodifiableMap(newByInstance);
        SNAPSHOT.set(new EnchantmentInfoSnapshot(immutableByKey, immutableByInstance, true));
    }

    /**
     * Clears the registry and resets {@link #hasSyncBeenReceived()} to {@code false}. Called on
     * client disconnect.
     */
    public static void clear() {
        SNAPSHOT.set(EnchantmentInfoSnapshot.EMPTY);
    }

    /**
     * Builds the payload to sync the full registry to a client. Entries are ordered by
     * enchantment id so the serialized wire form is deterministic across rebuilds.
     */
    public static EnchantmentInfoPayload buildPayload() {
        Map<ResourceKey<Enchantment>, EnchantmentInfo> info = SNAPSHOT.get().byKey();
        Map<ResourceKey<Enchantment>, EnchantmentInfo> ordered = new LinkedHashMap<>();
        info.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().location().toString()))
                .forEach(e -> ordered.put(e.getKey(), e.getValue()));
        return new EnchantmentInfoPayload(ordered);
    }
}
