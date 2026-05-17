package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantRandomlyFunction.class)
public class EnchantRandomlyFunctionMixin {

    @Inject(method = "run", at = @At("RETURN"), cancellable = true)
    private void meridian$stripDisabledEnchantments(
            ItemStack stack, LootContext context, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty()) return;
        var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(result);
        if (enchantments.isEmpty()) return;
        boolean hasDisabled = enchantments.entrySet().stream()
                .anyMatch(entry -> {
                    EnchantmentInfo info = EnchantmentInfoRegistry.getInfo(entry.getKey());
                    return !info.enabled();
                });
        if (hasDisabled) {
            var mutable = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(enchantments);
            enchantments.entrySet().forEach(entry -> {
                EnchantmentInfo info = EnchantmentInfoRegistry.getInfo(entry.getKey());
                if (!info.enabled()) {
                    mutable.set(entry.getKey(), 0);
                }
            });
            EnchantmentHelper.setEnchantments(result, mutable.toImmutable());
        }
    }
}
