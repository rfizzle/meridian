package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Winterward's cold-immunity half: a wearer with the enchant on leggings or boots cannot
 * freeze. Vanilla already exposes {@code canFreeze()} as the per-slot freeze-immunity gate
 * (leather and other {@code FREEZE_IMMUNE_WEARABLES} pieces return false here); Winterward
 * adds an enchantment-level gate on the same method. Forcing it false stops powder-snow
 * {@code ticksFrozen} from ever accumulating in {@code aiStep}, so the periodic
 * {@code DamageTypes.FREEZE} tick never lands — and because the frost-overlay screen effect
 * is driven off the same synced {@code ticksFrozen}, no client-side counterpart is needed.
 *
 * <p>The traversal half (walking on powder snow's surface) is separate — see
 * {@code WinterwardPowderSnowMixin}. This mirrors {@code InexorableMixin}'s shape.
 */
@Mixin(LivingEntity.class)
public abstract class WinterwardFreezeMixin {

    @Inject(method = "canFreeze", at = @At("RETURN"), cancellable = true)
    private void meridian$winterwardFreezeImmunity(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.WINTERWARD,
                EquipmentSlot.LEGS, EquipmentSlot.FEET) <= 0) {
            return;
        }
        cir.setReturnValue(false);
    }
}
