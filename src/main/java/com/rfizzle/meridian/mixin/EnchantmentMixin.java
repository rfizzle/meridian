package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.client.tooltip.TooltipFormatter;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantmentInfo;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class EnchantmentMixin {

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

    @Inject(method = "getFullname", at = @At("RETURN"))
    private static void meridian$applyOverLevelColor(
            Holder<Enchantment> enchantment, int level, CallbackInfoReturnable<Component> cir) {
        if (enchantment.is(EnchantmentTags.CURSE)) return;
        int vanillaMax = enchantment.value().definition().maxLevel();
        if (level <= vanillaMax) return;
        Component result = cir.getReturnValue();
        if (result instanceof MutableComponent mc) {
            MeridianConfig config = Meridian.getConfig();
            if (config == null) return;
            TextColor color = TextColor.parseColor(config.display.overLeveledColor)
                    .result().orElse(TooltipFormatter.DEFAULT_COLOR);
            mc.setStyle(mc.getStyle().withColor(color));
        }
    }
}
