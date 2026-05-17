package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.event.ProjectileEnchantmentHandler;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void meridian$onTick(CallbackInfo ci) {
        ProjectileEnchantmentHandler.handleTick((AbstractArrow) (Object) this);
    }

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void meridian$onHitEntity(EntityHitResult result, CallbackInfo ci) {
        ProjectileEnchantmentHandler.handleEntityImpact((AbstractArrow) (Object) this, result);
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void meridian$onHitBlock(BlockHitResult result, CallbackInfo ci) {
        if (ProjectileEnchantmentHandler.handleBlockImpact((AbstractArrow) (Object) this, result)) {
            ci.cancel();
        }
    }
}
