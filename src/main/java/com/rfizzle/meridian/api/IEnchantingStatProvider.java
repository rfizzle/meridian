package com.rfizzle.meridian.api;

import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Implemented by {@link net.minecraft.world.level.block.Block} subclasses that contribute
 * enchanting stats to a nearby enchantment table. Blocks implementing this interface bypass
 * the datapack {@code enchanting_stats} registry lookup during the shelf scan — Meridian's
 * own shelf blocks implement it, and sibling or third-party mods may implement it on their
 * blocks to participate in the table's stat aggregation without shipping a datapack entry.
 *
 * <p>All methods are defaulted, so a block can implement this interface as a pure marker and
 * inherit registry-backed stats, or override {@link #getStats} for dynamic (state- or
 * position-dependent) stat contributions.
 */
@Stable
public interface IEnchantingStatProvider {

    /**
     * Stats this block contributes to an enchantment table's shelf scan.
     *
     * @param level the level containing the shelf
     * @param pos   the shelf block position
     * @param state the shelf block state
     * @return the per-shelf stat contribution; never {@code null} (return
     *         {@code EnchantingStats.ZERO} for "no contribution")
     */
    default EnchantingStats getStats(Level level, BlockPos pos, BlockState state) {
        return EnchantingStatRegistry.lookup(level, state);
    }

    /**
     * Particle shown drifting from this shelf toward the enchantment table.
     *
     * @param state the shelf block state
     * @return the particle type; defaults to the vanilla enchanting glyphs
     */
    default ParticleOptions getTableParticle(BlockState state) {
        return ParticleTypes.ENCHANT;
    }

    /**
     * Spawns the shelf-to-table ambient particle, replicating the vanilla bookshelf particle
     * behaviour (random 1-in-16 chance per tick, line-of-sight gated through the
     * {@code enchantment_power_transmitter} midpoint block).
     *
     * @param state    the shelf block state
     * @param level    the level (client side — particles are visual only)
     * @param rand     the per-tick random source
     * @param tablePos the enchantment table position
     * @param offset   the shelf's offset relative to the table
     */
    default void spawnTableParticle(BlockState state, Level level, RandomSource rand,
                                    BlockPos tablePos, BlockPos offset) {
        if (rand.nextInt(16) == 0) {
            EnchantingStats stats = EnchantingStatRegistry.lookup(level, state);
            if (stats.eterna() > 0) {
                BlockPos mid = tablePos.offset(offset.getX() / 2, offset.getY(), offset.getZ() / 2);
                if (level.getBlockState(mid).is(BlockTags.ENCHANTMENT_POWER_TRANSMITTER)) {
                    level.addParticle(this.getTableParticle(state),
                            tablePos.getX() + 0.5D, tablePos.getY() + 2.0D, tablePos.getZ() + 0.5D,
                            offset.getX() + rand.nextFloat() - 0.5D,
                            offset.getY() - rand.nextFloat() - 1.0F,
                            offset.getZ() + rand.nextFloat() - 0.5D);
                }
            }
        }
    }
}
