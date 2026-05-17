package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.enchanting.IEnchantingStatProvider;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stone-tier shelf whose only stat-scan effect is to flag the table as treasure-eligible —
 * Mending, Frost Walker, Soul Speed, etc. become rollable when one of these is in range.
 * Backs that flag with a {@link TreasureShelfBlockEntity} that carries no state of its own and
 * implements {@link com.rfizzle.meridian.enchanting.TreasureFlagSource} so the existing
 * gather pipeline picks it up via the standard {@code level.getBlockEntity(offset)} lookup.
 *
 * <p>No stat JSON is shipped for {@code treasure_shelf} — DESIGN's "no Eterna contribution of its
 * own" maps directly to {@link com.rfizzle.meridian.enchanting.EnchantingStats#ZERO}
 * which is the registry's default for any block without a matching {@code enchanting_stats}
 * entry. Adding a JSON would be redundant.
 *
 * <p>{@link IEnchantingStatProvider} is implemented for symmetry with the rest of the shelf
 * roster; the default method simply defers to the data-driven registry, so it's a no-op while
 * the JSON is absent and a single-line override if a future iteration wants to give the shelf
 * a non-zero quanta/arcana profile.
 */
public class TreasureShelfBlock extends Block implements EntityBlock, IEnchantingStatProvider {

    public TreasureShelfBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TreasureShelfBlockEntity(pos, state);
    }
}
