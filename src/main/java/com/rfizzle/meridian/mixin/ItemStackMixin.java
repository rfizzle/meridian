package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.tome.XpTomeItem;
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
        if (meridian$allDisabled(self.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                && meridian$allDisabled(self.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void meridian$xpTomeStackLimit(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof XpTomeItem) {
            if (self.getOrDefault(MeridianRegistry.STORED_XP, 0) > 0) {
                cir.setReturnValue(1);
            }
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean meridian$allDisabled(ItemEnchantments enchantments) {
        if (enchantments.isEmpty()) return true;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (EnchantmentInfoRegistry.getInfo(entry.getKey()).enabled()) return false;
        }
        return true;
    }
}
