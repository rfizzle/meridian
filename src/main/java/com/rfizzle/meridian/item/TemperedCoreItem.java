package com.rfizzle.meridian.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Ignited core consumed at the anvil to make one damageable item permanently unbreakable.
 * The application itself lives in {@code TemperedCoreHandler} — this class only
 * carries the item identity and the usage hint tooltip.
 */
public class TemperedCoreItem extends Item {

    public TemperedCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.meridian.tempered_core").withStyle(ChatFormatting.GRAY));
    }
}
