package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.enchanting.TreasureFlagSource;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block entity for {@link TreasureShelfBlock}. Carries no storage — its sole job is to identify
 * the parent block as a {@link TreasureFlagSource} so the in-range enchanting table's stat scan
 * flips {@code treasureAllowed} on. Implementing the marker on the BE rather than the block lets
 * the gather pipeline stay uniform with {@link FilteringShelfBlockEntity}: every "special"
 * shelf hook is reached through {@code level.getBlockEntity(offset)}.
 */
public class TreasureShelfBlockEntity extends BlockEntity implements TreasureFlagSource {

    public TreasureShelfBlockEntity(BlockPos pos, BlockState state) {
        super(MeridianRegistry.TREASURE_SHELF_BE, pos, state);
    }
}
