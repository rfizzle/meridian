package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Loft's safe-fall half: shortens the effective fall distance before vanilla computes
 * fall damage, raising the safe fall height by
 * {@link DefenseEnchantMath#LOFT_SAFE_FALL_PER_LEVEL} blocks per level. The mid-air jump
 * half lives in {@code LoftHandler}.
 */
@Mixin(LivingEntity.class)
public abstract class LoftMixin {

    @ModifyVariable(method = "calculateFallDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float meridian$loftSafeFall(float fallDistance) {
        LivingEntity self = (LivingEntity) (Object) this;
        int level = EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.LOFT, EquipmentSlot.FEET);
        if (level <= 0) return fallDistance;

        return Math.max(0.0f, fallDistance - DefenseEnchantMath.loftSafeFallReduction(level));
    }
}
