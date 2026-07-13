package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Abyssal's pressure plating: armor turns the wearer's depth below sea level into damage reduction,
 * so deep exploration and monument diving become a distinct armor identity. Reducing the incoming
 * amount at {@code hurt}'s HEAD — before armor and absorption — mirrors {@link SaddleguardMixin};
 * depth can't be expressed as a vanilla {@code LevelBasedValue}, so it has to be computed here rather
 * than as a declarative enchantment effect.
 *
 * <p>The reduction is inert at or above sea level and in dimensions with no real sea
 * ({@code DimensionType#natural()} is false for the Nether and End), keeping it an overworld-ocean
 * bonus and not a flat everywhere-plate. The enchant-level check is the cheap early reject that keeps
 * the injection off the hot path for every entity not wearing Abyssal.
 *
 * <p>This shares its target — {@code hurt}'s {@code amount} argument at HEAD — with
 * {@link SaddleguardMixin}. No {@code priority} is set on either because the order is deliberately
 * irrelevant: both apply a {@code amount * (1 - reduction)} scale, and scaling is commutative, so the
 * final damage is identical regardless of which runs first. A future reduction mixin that is <em>not</em>
 * commutative with these (e.g. a subtract-then-clamp) must set an explicit priority.
 */
@Mixin(LivingEntity.class)
public abstract class AbyssalDamageMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float meridian$abyssalDepthReduction(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        int level = EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.ABYSSAL,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return amount;

        Level worldLevel = self.level();
        if (!worldLevel.dimensionType().natural()) return amount;

        double depth = worldLevel.getSeaLevel() - self.getY();
        if (depth <= 0) return amount;

        float reduction = DefenseEnchantMath.abyssalDamageReduction(level, depth);
        return amount * (1.0f - reduction);
    }
}
