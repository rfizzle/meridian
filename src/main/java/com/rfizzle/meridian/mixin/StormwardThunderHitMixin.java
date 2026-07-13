package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stormward's lightning-immunity half: a wearer with the enchant on leggings or boots takes
 * no lightning damage. {@code thunderHit} is declared on {@link Entity} (not overridden on
 * {@code LivingEntity}) and does two things — {@code igniteForSeconds(8)} <em>before</em>
 * {@code hurt(lightningBolt, 5)} — so cancelling the whole method at {@code HEAD} is the only
 * way to stop both the ignite and the damage; a damage-event cancel alone would leave the
 * wearer on fire. Only living wearers can carry equipment, so the instanceof narrows the
 * {@link Entity} target down to the intended case. Mirrors {@code WinterwardFreezeMixin}.
 *
 * <p>Lightning's secondary effects (fire on nearby blocks, mob conversion) live in
 * {@code LightningBolt.tick}, not here, so they are intentionally untouched.
 */
@Mixin(Entity.class)
public abstract class StormwardThunderHitMixin {

    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    private void meridian$stormwardLightningImmunity(CallbackInfo ci) {
        if (!(((Object) this) instanceof LivingEntity self)) return;
        if (EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.STORMWARD,
                EquipmentSlot.LEGS, EquipmentSlot.FEET) <= 0) {
            return;
        }
        ci.cancel();
    }
}
