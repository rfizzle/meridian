package com.rfizzle.meridian.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;

/**
 * Aggregated stats across every shelf within an enchantment table's reach.
 *
 * <p>Produced by {@link MeridianAPI#gatherStats}. Values are post-aggregation: eterna uses
 * step-ladder accumulation capped per tier, quanta/arcana/rectification are clamped to
 * {@code [0, 100]}, clues to {@code [0, 3]}, and line-of-sight filtering plus
 * filtering/treasure shelf contributions are already applied.
 *
 * @param eterna          aggregated eterna (enchanting power) after step-ladder capping
 * @param quanta          aggregated quanta (randomness), clamped to {@code [0, 100]}
 * @param arcana          aggregated arcana (rarity bias), clamped to {@code [0, 100]}
 * @param rectification   aggregated rectification (downside protection), clamped to {@code [0, 100]}
 * @param clues           number of enchantment clues revealed, clamped to {@code [0, 3]}
 * @param maxEterna       the highest per-shelf eterna cap seen during the scan
 * @param blacklist       union of every in-range {@link BlacklistSource} contribution; never {@code null}
 * @param treasureAllowed {@code true} when any in-range shelf is a {@link TreasureFlagSource}
 */
@ApiStatus.Stable
public record StatCollection(
        float eterna,
        float quanta,
        float arcana,
        float rectification,
        int clues,
        float maxEterna,
        Set<ResourceKey<Enchantment>> blacklist,
        boolean treasureAllowed
) {

    /** The all-zero collection: no shelves in range, no blacklist, treasure locked. */
    public static final StatCollection EMPTY = new StatCollection(
            0F, 0F, 0F, 0F, 0, 0F, Set.of(), false);
}
