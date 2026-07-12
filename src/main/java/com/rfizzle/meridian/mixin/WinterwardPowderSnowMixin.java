package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Winterward's traversal half: a wearer with the enchant on leggings or boots walks on the
 * surface of powder snow instead of sinking in, exactly as vanilla lets a leather-booted
 * entity do. {@code PowderSnowBlock.canEntityWalkOnPowderSnow} is the single gate the
 * collision shape consults; it lives on the shared block class, so one common-side mixin
 * covers both server authority and client movement prediction.
 *
 * <p>The cold-immunity half (never taking freeze damage) is separate — see
 * {@code WinterwardFreezeMixin} — because an entity can be freeze-immune yet still sink.
 */
@Mixin(PowderSnowBlock.class)
public abstract class WinterwardPowderSnowMixin {

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"), cancellable = true)
    private static void meridian$winterwardWalksOnPowderSnow(Entity entity,
                                                             CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        if (!(entity instanceof LivingEntity living)) return;

        if (EnchantmentEffects.getEquippedLevel(living, EnchantmentEffects.WINTERWARD,
                EquipmentSlot.LEGS, EquipmentSlot.FEET) > 0) {
            cir.setReturnValue(true);
        }
    }
}
