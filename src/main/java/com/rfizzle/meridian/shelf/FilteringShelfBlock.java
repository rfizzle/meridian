package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.enchanting.IEnchantingStatProvider;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.OptionalInt;

/**
 * Chiseled-bookshelf-style block whose stored enchanted books blacklist matching enchantments
 * from the in-range enchanting table's roll pool. Subclasses vanilla
 * {@link ChiseledBookShelfBlock} for its 6-slot blockstate properties and chiseled-bookshelf
 * model JSON; restricts inserts to single-enchant enchanted books and inherits its stat tuple
 * through {@link IEnchantingStatProvider}'s default registry lookup so the contributing values
 * stay in datapack JSON ({@code data/meridian/enchanting_stats/filtering_shelf.json}).
 *
 * <p>Empty shelves still contribute as a wood-tier base shelf (per DESIGN), so a freshly placed
 * filtering shelf is not strictly worse than a beeshelf when the operator hasn't filled it.
 *
 * <p>Both player interactions are reimplemented locally — vanilla's {@code useItemOn} /
 * {@code useWithoutItem} cast the BE to {@code ChiseledBookShelfBlockEntity}, but our BE
 * extends {@code BlockEntity} directly to avoid the {@code validateBlockState} mismatch that
 * comes from the vanilla type field. {@link #computeHitSlot} provides the same 3×2 grid mapping
 * vanilla uses, kept in sync with the inherited {@link ChiseledBookShelfBlock#SLOT_OCCUPIED_PROPERTIES}.
 */
public class FilteringShelfBlock extends ChiseledBookShelfBlock implements IEnchantingStatProvider {

    // ChiseledBookShelfBlock#codec() is bound to MapCodec<ChiseledBookShelfBlock> with no
    // wildcard, so the override must match exactly. simpleCodec returns MapCodec<FilteringShelfBlock>;
    // the unchecked cast is safe because the codec is only ever invoked on instances of this class.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final MapCodec<ChiseledBookShelfBlock> CODEC =
            (MapCodec) simpleCodec(FilteringShelfBlock::new);


    public FilteringShelfBlock(Properties properties) {
        super(properties);
    }

    static final float ETERNA_PER_BOOK = 0.5F;
    static final float ARCANA_PER_BOOK = 1.0F;

    @Override
    public MapCodec<ChiseledBookShelfBlock> codec() {
        return CODEC;
    }

    @Override
    public EnchantingStats getStats(Level level, BlockPos pos, BlockState state) {
        EnchantingStats base = EnchantingStatRegistry.lookup(level, state);
        if (level.getBlockEntity(pos) instanceof FilteringShelfBlockEntity be) {
            return applyBookScaling(base, be.count());
        }
        return base;
    }

    static EnchantingStats applyBookScaling(EnchantingStats base, int bookCount) {
        if (bookCount <= 0) return base;
        return new EnchantingStats(
                base.maxEterna(),
                base.eterna() + bookCount * ETERNA_PER_BOOK,
                base.quanta(),
                base.arcana() + bookCount * ARCANA_PER_BOOK,
                base.rectification(),
                base.clues());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilteringShelfBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FilteringShelfBlockEntity be)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        OptionalInt slotOpt = computeHitSlot(hit, state);
        if (slotOpt.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int slot = slotOpt.getAsInt();
        if (state.getValue(SLOT_OCCUPIED_PROPERTIES.get(slot))) {
            // Occupied → fall through to useWithoutItem so the player pulls the book regardless
            // of what they're holding (matches vanilla chiseled bookshelf UX).
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!FilteringShelfBlockEntity.canInsert(stack)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            ItemStack inserted = stack.consumeAndReturn(1, player);
            be.setItem(slot, inserted);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FilteringShelfBlockEntity be)) {
            return InteractionResult.PASS;
        }
        OptionalInt slotOpt = computeHitSlot(hit, state);
        if (slotOpt.isEmpty()) {
            return InteractionResult.PASS;
        }
        int slot = slotOpt.getAsInt();
        if (!state.getValue(SLOT_OCCUPIED_PROPERTIES.get(slot))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack book = be.removeItem(slot, 1);
            if (!book.isEmpty() && !player.getInventory().add(book)) {
                player.drop(book, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Maps a {@link BlockHitResult} on the front face of a horizontally-facing chiseled-bookshelf
     * block to a slot index in {@code [0, 5]}. Top row reads left-to-right as slots 0/1/2 when
     * facing the shelf; bottom row is 3/4/5. Matches vanilla's private
     * {@code ChiseledBookShelfBlock#getHitSlot} behavior so visual occupancy (driven by
     * {@code SLOT_OCCUPIED_PROPERTIES}) stays in sync after the entity's {@code setItem}.
     *
     * <p>Returns {@link OptionalInt#empty()} when the hit lies outside the 6-cell grid (edges,
     * margins, or non-front faces).
     */
    public static OptionalInt computeHitSlot(BlockHitResult hit, BlockState state) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (hit.getDirection() != facing) {
            return OptionalInt.empty();
        }
        BlockPos pos = hit.getBlockPos();
        double dx = hit.getLocation().x - pos.getX();
        double dy = hit.getLocation().y - pos.getY();
        double dz = hit.getLocation().z - pos.getZ();
        float horizontal;
        switch (facing) {
            case NORTH -> horizontal = (float) (1.0D - dx);
            case SOUTH -> horizontal = (float) dx;
            case WEST -> horizontal = (float) dz;
            case EAST -> horizontal = (float) (1.0D - dz);
            default -> {
                return OptionalInt.empty();
            }
        }
        return ShelfSlotMapping.computeSlot(horizontal, dy);
    }
}
