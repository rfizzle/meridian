package com.rfizzle.meridian.tome;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ScrapTomeItem extends Item {

    public ScrapTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.meridian.scrap_tome").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("info.meridian.scrap_tome2").withStyle(ChatFormatting.GRAY));
    }
}
