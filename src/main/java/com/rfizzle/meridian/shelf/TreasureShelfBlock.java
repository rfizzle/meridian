package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.api.IEnchantingStatProvider;
import com.rfizzle.meridian.enchanting.EnchantingStats;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Zenith's "Deepshelf of Arcane Treasures" — flags the table as treasure-eligible (Mending, Frost
 * Walker, Soul Speed, etc. become rollable when one is in range) <em>and</em> trades quanta for
 * arcana. Matching Zenith's {@code TreasureShelfBlock}, it contributes <b>+10% Arcana / −10%
 * Quanta</b> and no Eterna. The flag is backed by a {@link TreasureShelfBlockEntity} (carrying no
 * state of its own) that implements {@link com.rfizzle.meridian.api.TreasureFlagSource}, so the
 * gather pipeline picks it up via the standard {@code level.getBlockEntity(offset)} lookup; the
 * stat profile is supplied in code via {@link #getStats} rather than a JSON, since it's fixed.
 *
 * <p>The −10% quanta is intentionally signed: the gather sums shelf contributions unclamped, so a
 * lone Treasure Shelf nets the table's +15% quanta baseline down to 5% (see
 * {@link com.rfizzle.meridian.api.StatCollection#applyBaselines}).
 */
public class TreasureShelfBlock extends Block implements EntityBlock, IEnchantingStatProvider {

    /** Fixed contribution: +10% arcana, −10% quanta, no Eterna — matching Zenith. */
    private static final EnchantingStats STATS = new EnchantingStats(0F, 0F, -10F, 10F, 0F, 0);

    public TreasureShelfBlock(Properties properties) {
        super(properties);
    }

    @Override
    public EnchantingStats getStats(Level level, BlockPos pos, BlockState state) {
        return STATS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TreasureShelfBlockEntity(pos, state);
    }
}
