package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.event.EnchantmentEffectHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void meridian$steadfast(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        if (!self.onGround()) {
            if (EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.STEADFAST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET) > 0) {
                cir.setReturnValue(cir.getReturnValue() * 5.0F);
            }
        }
    }

    /**
     * Pinpoint hooks the {@code crit()} call inside {@code attack} — vanilla reaches it
     * only when a true critical hit both qualified and landed, so no crit-condition
     * recomputation is needed here.
     */
    @Inject(method = "attack",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/entity/player/Player;crit(Lnet/minecraft/world/entity/Entity;)V"))
    private void meridian$pinpointCrit(Entity target, CallbackInfo ci) {
        EnchantmentEffectHandler.handlePinpointCrit((Player) (Object) this, target);
    }
}
