package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SculkShelfBlock extends EnchantingShelfBlock {

    public SculkShelfBlock(BlockBehaviour.Properties properties, ParticleTheme particleTheme) {
        super(properties, particleTheme);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        MeridianConfig config = Meridian.getConfig();
        if (config == null) return;

        if (config.shelves.sculkShelfShriekerChance > 0 && random.nextDouble() < config.shelves.sculkShelfShriekerChance) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(),
                    SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS,
                    2.0F, 0.6F + random.nextFloat() * 0.4F, true);
        }

        if (config.shelves.sculkParticleChance > 0 && random.nextDouble() < config.shelves.sculkParticleChance) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 1.0 + random.nextDouble() * 0.3;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.SCULK_SOUL, x, y, z, 0.0, 0.05, 0.0);
        }
    }
}
