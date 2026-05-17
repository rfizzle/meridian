package com.rfizzle.meridian.enchanting;

/**
 * Marker hook implemented by shelf {@link net.minecraft.world.level.block.entity.BlockEntity}
 * subclasses that unlock treasure-flagged enchantments at the table (e.g. the treasure shelf).
 * Any single in-range contributor flips {@link StatCollection#treasureAllowed()} to {@code true}
 * in {@link EnchantingStatRegistry#gatherStats}.
 *
 * <p>Declared in the stat package ahead of Epic 3's treasure-shelf BE so the scan side of the
 * integration lands in S-2.2. Pre-Epic-3 builds have no implementors; the hook is a no-op.
 */
public interface TreasureFlagSource {
}
