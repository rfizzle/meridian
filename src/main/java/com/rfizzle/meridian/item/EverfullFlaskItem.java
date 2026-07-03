package com.rfizzle.meridian.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

/**
 * Infused water vessel that never runs dry. Behaves exactly like a water bucket on use —
 * same ray trace, same placement rules, same waterlogging — but the flask stays in hand
 * instead of reverting to an empty bucket, so one infusion buys a permanent water source.
 *
 * <p>Implemented by delegating to {@link BucketItem#use} and, on a consumed action,
 * substituting a pre-use copy of the flask stack for vanilla's result (which would be
 * an empty bucket on a survival placement). The copy is taken before delegating
 * because the placement path routes the in-hand stack through
 * {@code ItemUtils.createFilledResult}, which shrinks it — returning the original
 * instance would hand back a count-0 stack and the flask would vanish. Scooping is
 * a non-issue: the flask is always "full", so like a full water bucket it simply
 * places into the clicked space.
 *
 * <p>Dispenser and cauldron behaviors are registered in
 * {@link com.rfizzle.meridian.MeridianRegistry#registerEverfullFlaskBehaviors()}: a
 * dispenser places water and keeps the flask; using the flask on a cauldron fills it
 * to the brim, again without emptying.
 */
public class EverfullFlaskItem extends BucketItem {

    public EverfullFlaskItem(Properties properties) {
        super(Fluids.WATER, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack flask = player.getItemInHand(hand).copy();
        InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
        if (result.getResult().consumesAction()) {
            return new InteractionResultHolder<>(result.getResult(), flask);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("info.meridian.everfull_flask").withStyle(ChatFormatting.GRAY));
    }
}
