package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.api.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMaxLevelMixin {

    @Inject(method = "getMaxLevel", at = @At("RETURN"), cancellable = true)
    private void meridian$overrideMaxLevel(CallbackInfoReturnable<Integer> cir) {
        EnchantmentInfo info = EnchantmentInfoRegistry.getInfoByInstance((Enchantment) (Object) this);
        if (info != null) {
            int configured = info.getMaxLevel();
            if (configured != cir.getReturnValueI()) {
                cir.setReturnValue(configured);
            }
        }
    }
}
