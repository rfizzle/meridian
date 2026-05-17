package com.rfizzle.meridian.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityLootInvoker {
    @Invoker("dropFromLootTable")
    void meridian$invokeDropFromLootTable(DamageSource source, boolean hitByPlayer);
}
