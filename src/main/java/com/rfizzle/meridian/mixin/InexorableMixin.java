package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inexorable's effect-immunity half: Slowness and Mining Fatigue never land on a wearer
 * with the enchant on leggings or boots. The terrain half (soul sand, cobwebs) is split
 * between {@link #meridian$ignoreTerrainSpeedFactor} here and {@code InexorableEntityMixin}
 * (cobweb-style stuck blocks live on {@code Entity}).
 */
@Mixin(LivingEntity.class)
public abstract class InexorableMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void meridian$blockSlowEffects(MobEffectInstance instance, Entity source,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (!instance.is(MobEffects.MOVEMENT_SLOWDOWN) && !instance.is(MobEffects.DIG_SLOWDOWN)) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.INEXORABLE,
                EquipmentSlot.LEGS, EquipmentSlot.FEET) <= 0) {
            return;
        }
        cir.setReturnValue(false);
    }

    /**
     * Neutralizes slowing block speed factors (soul sand 0.4, honey 0.4). Only lifts
     * factors below 1 — a hypothetical speed-boosting block is left alone.
     */
    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void meridian$ignoreTerrainSpeedFactor(CallbackInfoReturnable<Float> cir) {
        if (cir.getReturnValue() >= 1.0f) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.INEXORABLE,
                EquipmentSlot.LEGS, EquipmentSlot.FEET) <= 0) {
            return;
        }
        cir.setReturnValue(1.0f);
    }
}
