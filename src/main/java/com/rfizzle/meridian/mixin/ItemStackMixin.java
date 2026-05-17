package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
    private void meridian$suppressDisabledGlint(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (self.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) return;
        if (allDisabled(self.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                && allDisabled(self.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))) {
            cir.setReturnValue(false);
        }
    }

    private static boolean allDisabled(ItemEnchantments enchantments) {
        if (enchantments.isEmpty()) return true;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (EnchantmentInfoRegistry.getInfo(entry.getKey()).enabled()) return false;
        }
        return true;
    }
}
