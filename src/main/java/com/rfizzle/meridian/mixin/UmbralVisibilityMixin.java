package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.UmbralStealthMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Umbral: while a wearer sneaks in darkness, a hostile mob's visibility of them is scaled down,
 * shrinking the range at which the mob can newly notice them. {@code getVisibilityPercent} is the
 * factor vanilla Invisibility already dims here; it feeds {@code TargetingConditions#test} as
 * {@code effectiveRange = range * visibility}, so multiplying it down sharply cuts acquisition
 * range without ever expanding it. Because target <em>retention</em> ({@code TargetGoal
 * #canContinueToUse}) reads raw distance and never calls this method, a mob that has already
 * acquired the wearer is unaffected — the "no effect on existing targets" contract holds by
 * construction, with no guard code.
 *
 * <p>Server-gated: mob target acquisition runs only on the logical server, so the light lookup is
 * skipped clientside. Scope is the vanilla {@code Mob} goal path funneled through
 * {@code TargetingConditions}; Brain-driven acquisition (Warden, Piglin) is out of scope.
 */
@Mixin(LivingEntity.class)
public abstract class UmbralVisibilityMixin {

    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void meridian$umbralReducesDetection(Entity looker, CallbackInfoReturnable<Double> cir) {
        if (!(looker instanceof Enemy)) return;

        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.isDiscrete()) return;
        // Target acquisition is server-only; bail before the equipment scan on the client.
        if (self.level().isClientSide()) return;

        int level = EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.UMBRAL, EquipmentSlot.HEAD);
        if (level <= 0) return;

        int lightLevel = self.level().getMaxLocalRawBrightness(self.blockPosition());
        double base = cir.getReturnValueD();
        double reduced = UmbralStealthMath.stealthedVisibility(base, level, self.isDiscrete(), lightLevel);
        if (reduced < base) {
            cir.setReturnValue(reduced);
        }
    }
}
