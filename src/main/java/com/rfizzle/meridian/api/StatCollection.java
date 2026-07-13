package com.rfizzle.meridian.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;

/**
 * Aggregated stats across every shelf within an enchantment table's reach.
 *
 * <p>Produced by {@link MeridianAPI#gatherStats}. Values are post-aggregation: eterna uses
 * step-ladder accumulation capped per tier, quanta/arcana/rectification are clamped to
 * {@code [0, 100]}, clues are floored at 0 (no upper cap), and line-of-sight filtering plus
 * filtering/treasure shelf contributions are already applied.
 *
 * @param eterna          aggregated eterna (enchanting power) after step-ladder capping
 * @param quanta          aggregated quanta (randomness), clamped to {@code [0, 100]}
 * @param arcana          aggregated arcana (rarity bias), clamped to {@code [0, 100]}
 * @param rectification   aggregated rectification (downside protection), clamped to {@code [0, 100]}
 * @param clues           number of enchantment clues revealed, floored at 0 (no upper cap)
 * @param maxEterna       the highest per-shelf eterna cap seen during the scan
 * @param blacklist       union of every in-range {@link BlacklistSource} contribution; never {@code null}
 * @param treasureAllowed {@code true} when any in-range shelf is a {@link TreasureFlagSource}
 */
@Stable
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

    /**
     * Returns a copy with every stat clamped to its valid range: eterna and clues floored at 0,
     * and quanta/arcana/rectification clamped to {@code [0, 100]}. {@code maxEterna}, the
     * blacklist, and the treasure flag pass through unchanged. The eterna step-ladder already
     * bounds the upper end, so only the lower floor is applied to eterna here.
     */
    public StatCollection clamped() {
        return new StatCollection(
                Math.max(0F, eterna),
                Math.max(0F, Math.min(quanta, 100F)),
                Math.max(0F, Math.min(arcana, 100F)),
                Math.max(0F, Math.min(rectification, 100F)),
                Math.max(0, clues),
                maxEterna,
                blacklist,
                treasureAllowed);
    }

    /**
     * Returns a copy with {@code eternaDelta} eterna and {@code cluesDelta} clues removed, then
     * clamped once so neither can fall below 0. Quanta, arcana, rectification, {@code maxEterna},
     * the blacklist, and the treasure flag pass through unchanged. Used by Curse of Dissonance to
     * sabotage the wearer's own table session without touching any other axis.
     */
    public StatCollection withDissonanceReduction(float eternaDelta, int cluesDelta) {
        return new StatCollection(
                eterna - eternaDelta,
                quanta,
                arcana,
                rectification,
                clues - cluesDelta,
                maxEterna,
                blacklist,
                treasureAllowed
        ).clamped();
    }

    /**
     * Applies the enchanting table's inherent baseline stats on top of raw shelf contributions,
     * then clamps once. The table provides, independent of surrounding shelves:
     * <ul>
     *   <li>{@code +15} quanta (fixed)</li>
     *   <li>{@code +itemEnchantability / 2} arcana (item-dependent)</li>
     *   <li>{@code +1} clue (fixed)</li>
     * </ul>
     * This mirrors Zenith's {@code TableStats.Builder} — which seeds these baselines <em>before</em>
     * summing shelves and clamps a single time in its record constructor. Folding the baselines
     * into the raw (signed, unclamped) sum before the lone {@link #clamped()} pass is what lets a
     * net-negative shelf contribution (e.g. a Treasure Shelf's −10 quanta) be offset by the +15
     * base rather than being floored to 0 first. Callers must therefore pass a <em>raw</em>
     * collection (see {@link MeridianAPI#gatherStats}'s unclamped sibling), not an already-clamped one.
     */
    public StatCollection applyBaselines(int itemEnchantability) {
        return new StatCollection(
                eterna,
                quanta + 15F,
                arcana + itemEnchantability / 2F,
                rectification,
                clues + 1,
                maxEterna,
                blacklist,
                treasureAllowed
        ).clamped();
    }
}
