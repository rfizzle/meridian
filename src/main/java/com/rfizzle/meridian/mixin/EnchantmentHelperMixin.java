package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @Inject(method = "selectEnchantment", at = @At("RETURN"), cancellable = true)
    private static void meridian$removeDisabledFromLootResult(
            RandomSource random, ItemStack stack, int level,
            Stream<Holder<Enchantment>> enchantments,
            CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        if (EnchantmentEffects.getEnchantmentLevel(stack, EnchantmentEffects.CURSE_OF_SEALING) > 0) {
            cir.setReturnValue(Collections.emptyList());
            return;
        }

        List<EnchantmentInstance> result = cir.getReturnValue();
        if (result == null || result.isEmpty()) return;
        List<EnchantmentInstance> filtered = result.stream()
                .filter(inst -> {
                    EnchantmentInfo info = EnchantmentInfoRegistry.getInfo(inst.enchantment);
                    return info.enabled();
                })
                .toList();
        if (filtered.size() != result.size()) {
            cir.setReturnValue(filtered);
        }
    }
}
