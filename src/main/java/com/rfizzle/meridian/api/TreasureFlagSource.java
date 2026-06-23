package com.rfizzle.meridian.api;

/**
 * Marker hook implemented by shelf {@link net.minecraft.world.level.block.entity.BlockEntity}
 * subclasses that unlock treasure-flagged enchantments at the table (e.g. the treasure shelf).
 * Any single in-range contributor flips {@link StatCollection#treasureAllowed()} to {@code true}
 * during the shelf scan ({@link MeridianAPI#gatherStats}).
 */
@Stable
public interface TreasureFlagSource {
}
