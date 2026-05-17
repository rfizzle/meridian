package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemptGoal.class)
public class TemptGoalMixin {

    @Inject(method = "shouldFollow", at = @At("RETURN"), cancellable = true)
    private void meridian$temptation(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            if (EnchantmentEffects.getEnchantmentLevel(entity.getMainHandItem(), EnchantmentEffects.BECKON) > 0
                    || EnchantmentEffects.getEnchantmentLevel(entity.getOffhandItem(), EnchantmentEffects.BECKON) > 0) {
                cir.setReturnValue(true);
            }
        }
    }
}
