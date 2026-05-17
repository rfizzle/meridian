package com.rfizzle.meridian.shelf;

import java.util.Objects;

import com.rfizzle.meridian.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Identifies which particle family a shelf emits when near an enchanting table. The five families
 * mirror Apotheosis/Zenith's themed shelf tiers — generic enchant swirl, Nether (fire), ocean
 * (water), End, and Deep/sculk. The generic tier uses vanilla {@link ParticleTypes#ENCHANT}; the
 * four themed tiers use custom {@link ModParticles} types backed by the same SGA glyph sprites so
 * they render as enchanting-table-style particles and fly toward the table.
 */
public enum ParticleTheme {
    ENCHANT(ParticleTypes.ENCHANT),
    ENCHANT_FIRE(ModParticles.ENCHANT_FIRE),
    ENCHANT_WATER(ModParticles.ENCHANT_WATER),
    ENCHANT_END(ModParticles.ENCHANT_END),
    ENCHANT_SCULK(ModParticles.ENCHANT_SCULK);

    private final SimpleParticleType particleType;

    ParticleTheme(SimpleParticleType particleType) {
        this.particleType = Objects.requireNonNull(particleType, "particleType");
    }

    public SimpleParticleType getParticleType() {
        return this.particleType;
    }
}
