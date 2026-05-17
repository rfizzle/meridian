package com.rfizzle.meridian.enchanting;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;

/**
 * Aggregated stats across every shelf within an enchantment table's reach.
 *
 * <p>Produced by {@link EnchantingStatRegistry#gatherStats}. Values are raw sums at this
 * stage — clamps, line-of-sight filtering, and filtering/treasure shelf contributions are
 * layered in by later tasks in Story S-2.2.
 */
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

    public static final StatCollection EMPTY = new StatCollection(
            0F, 0F, 0F, 0F, 0, 0F, Set.of(), false);
}
