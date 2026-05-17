package com.rfizzle.meridian.enchanting;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;

/**
 * Opt-in hook implemented by shelf {@link net.minecraft.world.level.block.entity.BlockEntity}
 * subclasses whose state blacklists enchantments from table rolls (e.g. the filtering shelf's
 * stored books). Picked up during {@link EnchantingStatRegistry#gatherStats} — each in-range
 * contributor's set is union'd into the aggregated {@link StatCollection#blacklist()}.
 *
 * <p>Declared in the stat package ahead of Epic 3's filtering-shelf BE so the scan side of the
 * integration lands in S-2.2. Pre-Epic-3 builds have no implementors; the hook is a no-op.
 */
public interface BlacklistSource {

    /**
     * Enchantments this shelf excludes from table rolls. Empty and null are both treated as
     * "no contribution" — implementations may return {@link Set#of()} when the shelf is empty.
     */
    Set<ResourceKey<Enchantment>> getEnchantmentBlacklist();
}
