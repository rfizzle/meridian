package com.rfizzle.meridian.client.tooltip;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.ObscurityTooltipMath;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Set;

/**
 * Curse of Obscurity: hides the item's other enchantment lines from its tooltip — the anti-Clues
 * curse. Registered before the inline-description handler so a hidden enchantment never sprouts a
 * description line beneath a name that is no longer there.
 */
public final class ObscurityTooltipHandler {

    private ObscurityTooltipHandler() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register(ObscurityTooltipHandler::onTooltip);
    }

    private static void onTooltip(ItemStack stack, Item.TooltipContext context,
                                  TooltipFlag flag, List<Component> lines) {
        if (EnchantmentEffects.getEnchantmentLevel(stack, EnchantmentEffects.CURSE_OF_OBSCURITY) <= 0) return;
        Set<String> hidden = ObscurityTooltipMath.collectHiddenNames(stack);
        ObscurityTooltipMath.removeMatchingLines(lines, hidden);
    }
}
