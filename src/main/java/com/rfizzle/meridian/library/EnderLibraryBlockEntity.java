package com.rfizzle.meridian.library;

import com.rfizzle.meridian.MeridianRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tier-2 enchantment library — caps stored book levels at {@value #MAX_LEVEL}, which gives
 * {@link #MAX_POINTS} = {@code 2^(MAX_LEVEL - 1)} = 1 073 741 824 per enchantment (the largest
 * value that still fits in a signed {@code int}). Reached via the
 * {@code meridian:keep_nbt_enchanting} table-crafting upgrade off a
 * {@link BasicLibraryBlockEntity}, so per-enchant pool values stay frozen — saturated entries do
 * not auto-scale to the new ceiling, only fresh deposits push past the old Basic cap.
 *
 * <p>The cap is a code constant, not config: {@link #MAX_POINTS} is baked into on-disk NBT pools
 * and changing it mid-save would silently corrupt stored books.
 */
public class EnderLibraryBlockEntity extends EnchantmentLibraryBlockEntity {

    public static final int MAX_LEVEL = 31;
    public static final int MAX_POINTS = points(MAX_LEVEL);

    public EnderLibraryBlockEntity(BlockPos pos, BlockState state) {
        this(MeridianRegistry.ENDER_LIBRARY_BE, pos, state);
    }

    EnderLibraryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, MAX_LEVEL);
    }
}
