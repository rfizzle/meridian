package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class ShieldFortifyMixin {

    @ModifyVariable(method = "hurtCurrentlyUsedShield", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float meridian$fortifyReduceDamage(float damage) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack shield = self.getUseItem();
        int level = EnchantmentEffects.getEnchantmentLevel(shield, EnchantmentEffects.FORTIFY);
        if (level <= 0) return damage;

        float reduction = 0.20f * level;
        return damage * (1.0f - reduction);
    }
}
