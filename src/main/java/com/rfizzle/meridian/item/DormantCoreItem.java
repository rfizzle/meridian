package com.rfizzle.meridian.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Inert precursor to the Tempered Core. Meridian registers the item and its art but ships
 * no recipe for it; the sole survival source is a guaranteed one-per-kill drop from the Ender
 * Dragon, handled by {@link com.rfizzle.meridian.mixin.EnderDragonMixin}. Ignited into a
 * {@link TemperedCoreItem} through the end-tier {@code meridian:enchanting} infusion on a
 * high-Eterna/Arcana table.
 */
public class DormantCoreItem extends Item {

    public DormantCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.meridian.dormant_core").withStyle(ChatFormatting.GRAY));
    }
}
