package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.event.ProjectileEnchantmentHandler;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@link ThrownTrident} overrides {@code onHitEntity} without calling super, so
 * {@link AbstractArrowMixin}'s entity-impact hook never fires for tridents. This mixin
 * restores the hook for trident enchantments (Harpoon, Glacial Lance).
 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    private void meridian$onHitEntity(EntityHitResult result, CallbackInfo ci) {
        ProjectileEnchantmentHandler.handleEntityImpact((AbstractArrow) (Object) this, result);
    }
}
