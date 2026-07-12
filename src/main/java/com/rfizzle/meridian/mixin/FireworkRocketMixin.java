package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.TraversalEnchantMath;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tailwind (elytra): a firework rocket used to boost a gliding wearer burns longer and pushes
 * harder. The rocket only ever attaches to a glider through the
 * {@code FireworkRocketEntity(Level, ItemStack, LivingEntity)} constructor, so the lifetime
 * extension is applied there once; the per-tick forward push is layered on top of vanilla's own
 * boost in {@code tick}, mirroring vanilla by running on both sides so client prediction matches
 * the server and the glide never rubberbands. Neither hook touches firework crafting or Thermal's
 * updraft — it only scales the boost of an already-spawned attached rocket.
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketMixin {

    @Shadow
    private LivingEntity attachedToEntity;

    @Shadow
    private int lifetime;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("TAIL"))
    private void meridian$extendTailwindLifetime(CallbackInfo ci) {
        if (this.attachedToEntity == null) return;
        int level = EnchantmentEffects.getEquippedLevel(this.attachedToEntity,
                EnchantmentEffects.TAILWIND, EquipmentSlot.CHEST);
        if (level <= 0) return;
        this.lifetime += TraversalEnchantMath.tailwindLifetimeBonus(level);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void meridian$tailwindPush(CallbackInfo ci) {
        LivingEntity glider = this.attachedToEntity;
        if (glider == null || !glider.isFallFlying()) return;
        int level = EnchantmentEffects.getEquippedLevel(glider,
                EnchantmentEffects.TAILWIND, EquipmentSlot.CHEST);
        if (level <= 0) return;

        double push = TraversalEnchantMath.tailwindPush(level);
        Vec3 look = glider.getLookAngle();
        glider.setDeltaMovement(glider.getDeltaMovement()
                .add(look.x * push, look.y * push, look.z * push));
    }
}
