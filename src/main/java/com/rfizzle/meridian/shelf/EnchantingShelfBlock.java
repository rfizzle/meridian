package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.enchanting.IEnchantingStatProvider;

import java.util.Objects;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class EnchantingShelfBlock extends Block implements IEnchantingStatProvider {

    private final ParticleTheme particleTheme;

    public EnchantingShelfBlock(BlockBehaviour.Properties properties, ParticleTheme particleTheme) {
        super(properties);
        this.particleTheme = Objects.requireNonNull(particleTheme, "particleTheme");
    }

    public ParticleTheme getParticleTheme() {
        return this.particleTheme;
    }

    @Override
    public ParticleOptions getTableParticle(BlockState state) {
        return this.particleTheme.getParticleType();
    }
}
