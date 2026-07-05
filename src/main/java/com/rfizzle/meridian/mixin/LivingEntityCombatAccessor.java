package com.rfizzle.meridian.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Access to the invulnerability-window damage threshold, so a mod-originated bonus hit
 * can bypass the window without lowering the threshold third parties are held to.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityCombatAccessor {
    @Accessor("lastHurt")
    float meridian$getLastHurt();

    @Accessor("lastHurt")
    void meridian$setLastHurt(float value);
}
