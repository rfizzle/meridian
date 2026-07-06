package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Everbloom: the mirror of {@link AntidoteMixin}. Where Antidote halves the duration of
 * incoming harmful effects, Everbloom lengthens the duration of incoming beneficial ones by a
 * capped per-level percentage. Both rewrite the {@code MobEffectInstance} argument to
 * {@link LivingEntity#addEffect} at HEAD and gate on disjoint categories, so they coexist on the
 * same target method without interfering.
 */
@Mixin(LivingEntity.class)
public abstract class EverbloomMixin {

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance meridian$extendBeneficialDuration(MobEffectInstance instance) {
        LivingEntity self = (LivingEntity) (Object) this;

        int level = EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.EVERBLOOM, EquipmentSlot.CHEST);
        if (level <= 0) return instance;

        if (instance.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) return instance;

        int extended = DefenseEnchantMath.everbloomExtendedDuration(instance.getDuration(), level);
        if (extended == instance.getDuration()) return instance;

        return new MobEffectInstance(instance.getEffect(), extended, instance.getAmplifier(),
                instance.isAmbient(), instance.isVisible(), instance.showIcon());
    }
}
