package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "getEnchantmentValue", at = @At("RETURN"), cancellable = true)
    private void meridian$overrideEnchantmentValue(CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() > 0) return;
        MeridianConfig config = Meridian.getConfig();
        if (config != null && config.enchantingTable.globalMinEnchantability > 0) {
            cir.setReturnValue(config.enchantingTable.globalMinEnchantability);
        }
    }
}
