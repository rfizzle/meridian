package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnduranceHealMath;
import com.rfizzle.meridian.event.MountedEnchantmentHandler;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Endurance's regen: a mount wearing Endurance slowly recovers health with no feeding. Injecting
 * into {@code AbstractHorse.tick} — the narrowest target that fires for every loaded horse,
 * ridden or not — lets a stabled mount heal while scaling with the horse's existing tick cost
 * rather than any world scan.
 *
 * <p>The per-instance {@code tickCount} interval gate is load-bearing: without it the mount would
 * heal every tick instead of once per {@link EnduranceHealMath#PULSE_INTERVAL_TICKS}. The heal
 * runs server-side only; the actual effect body lives in
 * {@link MountedEnchantmentHandler#handleEndurance} so a gametest can drive one pulse directly.
 */
@Mixin(AbstractHorse.class)
public abstract class EnduranceMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void meridian$enduranceRegen(CallbackInfo ci) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (horse.level().isClientSide()) return;
        if (horse.tickCount % EnduranceHealMath.PULSE_INTERVAL_TICKS != 0) return;

        MountedEnchantmentHandler.handleEndurance(horse);
    }
}
