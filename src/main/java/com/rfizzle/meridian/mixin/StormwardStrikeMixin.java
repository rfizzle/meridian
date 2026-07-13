package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.event.EnchantmentEffectHandler;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stormward's reward half: a lightning strike grants nearby wearers a short Strength surge.
 * Vanilla's {@link LightningBolt#tick()} runs its "strike lands" logic exactly once, on the
 * tick where {@code life == 2} (the bolt is constructed with {@code life = 2} and decrements
 * each tick). Reading that same gate at {@code HEAD} — before the decrement — fires the surge
 * once per bolt, server-side only, skipping cosmetic ({@code visualOnly}) bolts which are not
 * real strikes. The radius scan in {@link EnchantmentEffectHandler#applyStormwardSurge} covers
 * both a wearer struck directly (distance ~0) and one merely standing nearby.
 */
@Mixin(LightningBolt.class)
public abstract class StormwardStrikeMixin {

    @Shadow
    private int life;

    @Shadow
    private boolean visualOnly;

    @Inject(method = "tick", at = @At("HEAD"))
    private void meridian$stormwardStrikeSurge(CallbackInfo ci) {
        LightningBolt self = (LightningBolt) (Object) this;
        if (this.life != 2 || this.visualOnly || self.level().isClientSide()) return;
        EnchantmentEffectHandler.applyStormwardSurge(self);
    }
}
