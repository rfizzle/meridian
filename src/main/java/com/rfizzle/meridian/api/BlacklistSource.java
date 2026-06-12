package com.rfizzle.meridian.api;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;

/**
 * Opt-in hook implemented by shelf {@link net.minecraft.world.level.block.entity.BlockEntity}
 * subclasses whose state blacklists enchantments from table rolls (e.g. the filtering shelf's
 * stored books). Picked up during the shelf scan ({@link MeridianAPI#gatherStats}) — each
 * in-range contributor's set is union'd into the aggregated {@link StatCollection#blacklist()}.
 */
@ApiStatus.Stable
public interface BlacklistSource {

    /**
     * Enchantments this shelf excludes from table rolls. Empty and null are both treated as
     * "no contribution" — implementations may return {@link Set#of()} when the shelf is empty.
     *
     * @return the enchantment keys this shelf excludes from table rolls
     */
    Set<ResourceKey<Enchantment>> getEnchantmentBlacklist();
}
