package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clearsight — a head-armor enchantment that makes the wearer immune to the two vision-blanking
 * effects, blindness and darkness. Gates {@link LivingEntity#addEffect(MobEffectInstance, Entity)},
 * the single overload every effect application funnels through (the one-arg variant delegates here),
 * and declines the application outright so the effect never lands. Nightfall's darkness and the
 * deep dark's warden pulses both route through this seam, so both are answered.
 *
 * <p>The effect-identity check runs before the equipment scan — the cheapest guard first, so the
 * hot {@code addEffect} path pays for the wardrobe lookup only when a blinding effect is incoming.
 */
@Mixin(LivingEntity.class)
public abstract class ClearsightMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void meridian$blockBlindingEffects(MobEffectInstance instance, @Nullable Entity source,
                                               CallbackInfoReturnable<Boolean> cir) {
        MobEffect effect = instance.getEffect().value();
        if (effect != MobEffects.BLINDNESS.value() && effect != MobEffects.DARKNESS.value()) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.CLEARSIGHT, EquipmentSlot.HEAD) <= 0) {
            return;
        }

        // Decline the application: false is what vanilla returns when an effect is not added.
        cir.setReturnValue(false);
    }
}
