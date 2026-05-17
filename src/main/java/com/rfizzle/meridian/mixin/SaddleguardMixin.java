package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class SaddleguardMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float meridian$saddleguard(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity vehicle = self.getVehicle();
        if (!(vehicle instanceof AbstractHorse horse)) return amount;

        int level = EnchantmentEffects.getEquippedLevel(horse, EnchantmentEffects.SADDLEGUARD, EquipmentSlot.BODY);
        if (level <= 0) return amount;

        float reduction = 0.05f * level;
        return amount * (1.0f - reduction);
    }
}
