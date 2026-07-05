package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Inexorable's stuck-block immunity: cobwebs (and the other {@code makeStuckInBlock}
 * callers — powder snow, berry bushes) don't grip a wearer. Targets {@link Entity}
 * because {@code makeStuckInBlock} is declared there and never overridden on
 * {@code LivingEntity}; the instanceof gate keeps non-living entities on vanilla rules.
 */
@Mixin(Entity.class)
public abstract class InexorableEntityMixin {

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void meridian$ignoreStuckBlocks(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
        if (!((Object) this instanceof LivingEntity living)) return;

        if (EnchantmentEffects.getEquippedLevel(living, EnchantmentEffects.INEXORABLE,
                EquipmentSlot.LEGS, EquipmentSlot.FEET) <= 0) {
            return;
        }
        // Vanilla makeStuckInBlock also resets fall distance — that's what makes a cobweb
        // or powder snow break a long fall. Skipping only the grip must not turn those
        // vanilla-safe landings lethal for a defensive enchant, so keep the fall-break.
        living.resetFallDistance();
        ci.cancel();
    }
}
