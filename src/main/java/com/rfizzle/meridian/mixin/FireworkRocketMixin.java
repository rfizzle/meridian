package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.TraversalEnchantMath;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;
import java.util.UUID;

/**
 * Tailwind (elytra): a firework rocket used to boost a gliding wearer burns longer and pushes
 * harder. The rocket only ever attaches to a glider through the
 * {@code FireworkRocketEntity(Level, ItemStack, LivingEntity)} constructor, so the lifetime
 * extension is applied there once; the per-tick forward push is layered on top of vanilla's own
 * boost in {@code tick}, mirroring vanilla by running on both sides so client prediction matches
 * the server and the glide never rubberbands. Neither hook touches firework crafting or Thermal's
 * updraft — it only scales the boost of an already-spawned attached rocket.
 *
 * <p>Curse of Molting rides the opposite direction on the tick hook: the fizzle verdict is derived
 * from the rocket's UUID, which both sides hold from the spawn packet, so each reaches the same
 * answer without the server having to tell the client. A fizzled rocket is discarded on its first
 * gliding tick before any push lands — the firework is spent for nothing, on both sides alike, so
 * the client never predicts a boost the server discards.
 */
@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketMixin {

    @Shadow
    private LivingEntity attachedToEntity;

    @Shadow
    private int lifetime;

    @Shadow
    @Final
    private static EntityDataAccessor<OptionalInt> DATA_ATTACHED_TO_TARGET;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/entity/LivingEntity;)V", at = @At("TAIL"))
    private void meridian$extendTailwindLifetime(CallbackInfo ci) {
        if (this.attachedToEntity == null) return;
        int level = EnchantmentEffects.getEquippedLevel(this.attachedToEntity,
                EnchantmentEffects.TAILWIND, EquipmentSlot.CHEST);
        if (level <= 0) return;
        this.lifetime += TraversalEnchantMath.tailwindLifetimeBonus(level);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void meridian$moltingFizzle(CallbackInfo ci) {
        LivingEntity glider = meridian$resolveGlider();
        if (glider == null || !glider.isFallFlying()) return;
        int level = EnchantmentEffects.getEquippedLevel(glider,
                EnchantmentEffects.CURSE_OF_MOLTING, EquipmentSlot.CHEST);
        if (level <= 0) return;

        FireworkRocketEntity self = (FireworkRocketEntity) (Object) this;
        UUID id = self.getUUID();
        if (!TraversalEnchantMath.moltingFizzles(id.getMostSignificantBits(), id.getLeastSignificantBits())) {
            return;
        }
        // Spend the rocket without ever applying its boost — the fizzle.
        self.discard();
        ci.cancel();
    }

    /**
     * The glider this rocket is boosting, or null if it is not an attached boost rocket (or its
     * target has not loaded yet).
     *
     * <p>Vanilla assigns {@code attachedToEntity} in the glider constructor, which only ever runs
     * server-side; the client instead resolves it from synced data partway through {@code tick},
     * after this hook has already run. Reading the synced target directly is what lets the client
     * reach the fizzle verdict on the rocket's very first tick, as the server does, rather than
     * applying one tick of boost the server never applies. This only reads — vanilla still owns
     * assigning the field.
     */
    @Unique
    private LivingEntity meridian$resolveGlider() {
        if (this.attachedToEntity != null) return this.attachedToEntity;
        FireworkRocketEntity self = (FireworkRocketEntity) (Object) this;
        OptionalInt target = self.getEntityData().get(DATA_ATTACHED_TO_TARGET);
        if (target.isEmpty()) return null;
        return self.level().getEntity(target.getAsInt()) instanceof LivingEntity living ? living : null;
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
