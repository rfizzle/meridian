package com.rfizzle.meridian.enchanting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IEnchantingStatProvider {

    default EnchantingStats getStats(Level level, BlockPos pos, BlockState state) {
        return EnchantingStatRegistry.lookup(level, state);
    }

    default ParticleOptions getTableParticle(BlockState state) {
        return ParticleTypes.ENCHANT;
    }

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
