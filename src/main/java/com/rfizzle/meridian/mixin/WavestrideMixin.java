package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.WavestrideMath;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wavestride's traversal: a galloping mount runs across the surface of water instead of swimming,
 * and sinks and swims normally the instant it stops. {@code LivingEntity.canStandOnFluid} is the
 * single gate the collision shape consults to make a fluid's top face solid — it is how a Strider
 * walks on lava, and it also flips {@code travel} out of swim physics — so overriding it for a
 * moving Wavestride mount is the whole feature, with no per-tick scanning.
 *
 * <p>The gate lives on the shared {@code LivingEntity} class (no narrower subclass declares
 * {@code canStandOnFluid}), so the body short-circuits on the cheap {@code AbstractHorse} and
 * water-tag checks before reading the mount's velocity, exactly as {@code SaddleguardMixin} guards
 * its {@code LivingEntity#hurt} inject. One common-side mixin covers both server authority and
 * client movement prediction.
 */
@Mixin(LivingEntity.class)
public abstract class WavestrideMixin {

    @Inject(method = "canStandOnFluid", at = @At("RETURN"), cancellable = true)
    private void meridian$wavestrideWalksOnWater(FluidState fluidState,
                                                 CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        if (!(((Object) this) instanceof AbstractHorse horse)) return;
        if (!fluidState.is(FluidTags.WATER)) return;
        if (EnchantmentEffects.getEquippedLevel(horse, EnchantmentEffects.WAVESTRIDE,
                EquipmentSlot.BODY) <= 0) return;
        if (!WavestrideMath.strides(horse.getDeltaMovement().horizontalDistance())) return;

        cir.setReturnValue(true);
    }
}
