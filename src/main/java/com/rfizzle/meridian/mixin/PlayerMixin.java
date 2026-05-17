package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
