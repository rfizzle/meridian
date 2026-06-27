package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.api.EnchantmentInfo;
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

        net.minecraft.world.item.enchantment.ItemEnchantments.Mutable mutable = null;
        for (var entry : enchantments.entrySet()) {
            EnchantmentInfo info = EnchantmentInfoRegistry.getInfo(entry.getKey());
            int level = entry.getIntValue();
            int newLevel = level;
            if (!info.enabled()) {
                newLevel = 0;
            } else {
                // Enforce the configured loot cap: -1 passes through, <= 0 strips, > 0 clamps.
                int cap = info.getMaxLootLevel();
                if (cap != -1 && cap <= 0) {
                    newLevel = 0;
                } else if (cap != -1 && level > cap) {
                    newLevel = cap;
                }
            }
            if (newLevel != level) {
                if (mutable == null) {
                    mutable = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(enchantments);
                }
                mutable.set(entry.getKey(), newLevel);
            }
        }

        if (mutable != null) {
            EnchantmentHelper.setEnchantments(result, mutable.toImmutable());
        }
    }
}
