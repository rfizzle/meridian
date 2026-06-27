package com.rfizzle.meridian.api;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import com.rfizzle.meridian.library.EnchantmentLibraryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Stable, read-only entry point to Meridian's enchanting data, per the
 * <a href="https://github.com/rfizzle/concord/blob/master/API-STANDARD.md">Concord API
 * Standard</a>. Nothing here mutates Meridian's state.
 *
 * <p>Consume as a soft dependency: compile against Meridian with {@code modCompileOnly} and
 * guard every call site with
 * {@code FabricLoader.getInstance().isModLoaded("meridian")}.
 */
@Stable
public final class MeridianAPI {

    private MeridianAPI() {
    }

    /**
     * Scans the 15 vanilla shelf offsets around {@code tablePos} and returns the aggregated
     * {@link StatCollection} — the same scan Meridian's enchanting table menu performs.
     * Positions whose line-of-sight midpoint is not an
     * {@code enchantment_power_transmitter} contribute nothing, matching vanilla rules.
     *
     * <p>Server authority: call this server-side for gameplay decisions; the result reflects
     * the current world state at call time.
     *
     * @param level    the level containing the table
     * @param tablePos the enchantment table position to scan around
     * @return the aggregated stats; {@link StatCollection#EMPTY}-like zeros when no shelves
     *         are in range (never {@code null})
     */
    public static StatCollection gatherStats(Level level, BlockPos tablePos) {
        return EnchantingStatRegistry.gatherStats(level, tablePos);
    }

    /**
     * Per-enchantment configuration for {@code ench}: effective max level, max loot level,
     * level cap, power functions, and enabled flag.
     *
     * <p>Server-side the data is rebuilt on server start, datapack reload, and
     * {@code /meridian reload} (listen via {@link MeridianReloadCallback} instead of polling).
     * Client-side it mirrors the server's last sync.
     *
     * @param ench the enchantment holder
     * @return the info record; falls back to the enchantment's vanilla values when no
     *         override is configured (never {@code null})
     */
    public static EnchantmentInfo getEnchantmentInfo(Holder<Enchantment> ench) {
        return EnchantmentInfoRegistry.getInfo(ench);
    }

    /**
     * Read-only view of every known enchantment's {@link EnchantmentInfo}, keyed by registry
     * key. The view is live — a config or datapack reload repopulates it; callers needing a
     * snapshot should copy.
     *
     * @return an unmodifiable, live view of the enchantment info registry
     */
    public static Map<ResourceKey<Enchantment>, EnchantmentInfo> getAllEnchantmentInfo() {
        return EnchantmentInfoRegistry.getAll();
    }

    /**
     * Points stored for {@code enchantment} in the enchantment library at {@code pos} —
     * read-only, for tooltip and automation consumers. Points accumulate as
     * {@code 2^(level - 1)} per deposited book level.
     *
     * @param level       the level containing the block
     * @param pos         the position to query
     * @param enchantment the enchantment to look up
     * @return the stored points ({@code 0} when the library holds none of that enchantment),
     *         or {@link OptionalInt#empty()} as the sentinel when the block at {@code pos} is
     *         not an enchantment library
     */
    public static OptionalInt getStoredPoints(
            Level level, BlockPos pos, ResourceKey<Enchantment> enchantment) {
        if (level.getBlockEntity(pos) instanceof EnchantmentLibraryBlockEntity library) {
            return OptionalInt.of(library.getPoints().getInt(enchantment));
        }
        return OptionalInt.empty();
    }

    /**
     * Rolls a Meridian-consistent enchantment set for {@code stack} at the given effective
     * power — the blessed entry point for sibling/third-party mods that want loot to enchant by
     * the same rules as the enchanting table instead of falling back to vanilla rolls.
     *
     * <p>Honors every Meridian constraint: per-enchantment {@code enabled} flags,
     * {@code getMaxLevel()}/{@code levelCap}, {@code getMaxLootLevel()} (the result is clamped to
     * each entry's loot cap), the active blacklist (config-disabled enchantments), treasure
     * gating, and the configured min/max power functions. Shelf stats (quanta, arcana,
     * rectification) are zero — loot has no table to gather them from — so the roll is a
     * vanilla-weighted draw at {@code power}.
     *
     * <p>Deterministic with respect to {@code rng}: seeding it identically yields an identical
     * list, so per-player loot stays reproducible. Server authority: call this server-side; the
     * result reflects the live config and enchantment registry at call time.
     *
     * @param level           the server level whose {@code registryAccess()} supplies the
     *                        enchantment registry
     * @param rng             pre-seeded random; seed it deterministically for reproducible loot
     * @param stack           item being enchanted; its enchantability gates whether anything
     *                        rolls and its supported-items set filters the candidate pool
     * @param power           effective enchanting power (consumers map e.g. a distance tier here)
     * @param treasureAllowed when {@code false}, treasure-tagged enchantments are excluded
     * @return the chosen enchantments, clamped to each entry's loot cap; an empty list when the
     *         item is unenchantable, nothing rolls, or an unexpected failure is contained at the
     *         API boundary (never {@code null})
     */
    public static List<EnchantmentInstance> rollLootEnchantments(
            ServerLevel level, RandomSource rng, ItemStack stack, int power, boolean treasureAllowed) {
        if (level == null || rng == null || stack == null || stack.isEmpty()) {
            return List.of();
        }
        try {
            List<EnchantmentInstance> rolled = RealEnchantmentHelper.selectEnchantment(
                    rng, stack, power, 0F, 0F, 0F, treasureAllowed,
                    disabledEnchantments(), level.registryAccess());
            return List.copyOf(RealEnchantmentHelper.clampToMaxLootLevel(
                    rolled, ench -> EnchantmentInfoRegistry.getInfo(ench).getMaxLootLevel()));
        } catch (Exception e) {
            Meridian.LOGGER.warn("rollLootEnchantments failed for {}; returning no enchantments",
                    stack, e);
            return List.of();
        }
    }

    /** Config-disabled enchantments, used as the active loot-roll blacklist. */
    private static Set<ResourceKey<Enchantment>> disabledEnchantments() {
        Set<ResourceKey<Enchantment>> disabled = new HashSet<>();
        for (Map.Entry<ResourceKey<Enchantment>, EnchantmentInfo> entry
                : EnchantmentInfoRegistry.getAll().entrySet()) {
            if (!entry.getValue().enabled()) {
                disabled.add(entry.getKey());
            }
        }
        return disabled;
    }
}
