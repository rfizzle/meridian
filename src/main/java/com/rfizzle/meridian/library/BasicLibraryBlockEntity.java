package com.rfizzle.meridian.library;

import com.rfizzle.meridian.MeridianRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tier-1 enchantment library — caps stored book levels at {@value #MAX_LEVEL} (so
 * {@link #MAX_POINTS} = {@code 2^(MAX_LEVEL - 1)} = 32 768 per enchantment). The cap is a code
 * constant, not config: {@link #MAX_POINTS} is baked into on-disk NBT pools and changing it
 * mid-save would silently corrupt stored books.
 */
public class BasicLibraryBlockEntity extends EnchantmentLibraryBlockEntity {

    public static final int MAX_LEVEL = 16;
    public static final int MAX_POINTS = points(MAX_LEVEL);

    public BasicLibraryBlockEntity(BlockPos pos, BlockState state) {
        this(MeridianRegistry.BASIC_LIBRARY_BE, pos, state);
    }

    BasicLibraryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, MAX_LEVEL);
    }
}
