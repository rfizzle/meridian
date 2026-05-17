package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class AntidoteMixin {

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance meridian$reduceHarmfulDuration(MobEffectInstance instance) {
        LivingEntity self = (LivingEntity) (Object) this;

        int level = EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.ANTIDOTE,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return instance;

        if (instance.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) return instance;

        int reduced = Math.max(1, instance.getDuration() / 2);
        return new MobEffectInstance(instance.getEffect(), reduced, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon());
    }
}
