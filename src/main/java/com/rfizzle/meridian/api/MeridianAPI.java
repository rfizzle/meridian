package com.rfizzle.meridian.api;

import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.library.EnchantmentLibraryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.OptionalInt;

/**
 * Stable, read-only entry point to Meridian's enchanting data, per the
 * <a href="https://github.com/rfizzle/concord/blob/master/API-STANDARD.md">Concord API
 * Standard</a>. Nothing here mutates Meridian's state.
 *
 * <p>Consume as a soft dependency: compile against Meridian with {@code modCompileOnly} and
 * guard every call site with
 * {@code FabricLoader.getInstance().isModLoaded("meridian")}.
 */
@ApiStatus.Stable
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
}
