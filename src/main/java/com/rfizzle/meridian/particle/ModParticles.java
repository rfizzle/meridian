package com.rfizzle.meridian.particle;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModParticles {

    public static final SimpleParticleType ENCHANT_FIRE = FabricParticleTypes.simple();
    public static final SimpleParticleType ENCHANT_WATER = FabricParticleTypes.simple();
    public static final SimpleParticleType ENCHANT_SCULK = FabricParticleTypes.simple();
    public static final SimpleParticleType ENCHANT_END = FabricParticleTypes.simple();

    private static boolean registered = false;

    private ModParticles() {
    }

    public static void register() {
        if (registered) return;
        registered = true;

        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Meridian.id("enchant_fire"), ENCHANT_FIRE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Meridian.id("enchant_water"), ENCHANT_WATER);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Meridian.id("enchant_sculk"), ENCHANT_SCULK);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Meridian.id("enchant_end"), ENCHANT_END);
    }
}
