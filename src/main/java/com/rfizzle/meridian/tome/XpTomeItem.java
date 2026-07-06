package com.rfizzle.meridian.tome;

import com.rfizzle.meridian.MeridianRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class XpTomeItem extends Item {
    private final int capacity;

    public XpTomeItem(Properties properties, int capacity) {
        super(properties);
        this.capacity = capacity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        int stored = stack.getOrDefault(MeridianRegistry.STORED_XP, 0);

        if (player.isSecondaryUseActive()) {
            // Withdraw 1 level
            if (stored > 0) {
                stack.set(MeridianRegistry.STORED_XP, stored - 1);
                player.giveExperienceLevels(1);
                return InteractionResultHolder.consume(stack);
            } else {
                player.displayClientMessage(Component.translatable("message.meridian.xp_tome.empty"), true);
                return InteractionResultHolder.fail(stack);
            }
        } else {
            // Deposit 1 level
            if (stored < capacity) {
                if (player.experienceLevel > 0 || player.getAbilities().instabuild) {
                    stack.set(MeridianRegistry.STORED_XP, stored + 1);
                    if (!player.getAbilities().instabuild) {
                        player.giveExperienceLevels(-1);
                    }
                    return InteractionResultHolder.consume(stack);
                }
            } else {
                player.displayClientMessage(Component.translatable("message.meridian.xp_tome.full"), true);
                return InteractionResultHolder.fail(stack);
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * How many levels {@code stored} can cover toward {@code requested}, never negative and never
     * more than either. Pure arithmetic, split out so the debit clamp is unit-testable without a
     * live inventory.
     */
    public static int clampDebit(int stored, int requested) {
        return Math.max(0, Math.min(stored, requested));
    }

    /**
     * Sum of {@link MeridianRegistry#STORED_XP} across every {@link XpTomeItem} stack in the
     * player's inventory. This is the pool the enchanting table may draw on to cover a level
     * deficit.
     */
    public static int inventoryBalance(Player player) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof XpTomeItem) {
                total += stack.getOrDefault(MeridianRegistry.STORED_XP, 0);
            }
        }
        return total;
    }

    /**
     * Drains {@code levels} of stored XP from the player's tomes in inventory order, skipping empty
     * stacks. Callers are responsible for ensuring {@link #inventoryBalance} covers the request;
     * any shortfall (should not happen after a gate check) simply leaves the request partially
     * satisfied.
     */
    public static void debitInventory(Player player, int levels) {
        if (levels <= 0) {
            return;
        }
        Inventory inventory = player.getInventory();
        int remaining = levels;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!(stack.getItem() instanceof XpTomeItem)) {
                continue;
            }
            int stored = stack.getOrDefault(MeridianRegistry.STORED_XP, 0);
            int take = clampDebit(stored, remaining);
            if (take > 0) {
                stack.set(MeridianRegistry.STORED_XP, stored - take);
                remaining -= take;
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getOrDefault(MeridianRegistry.STORED_XP, 0) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round((float) stack.getOrDefault(MeridianRegistry.STORED_XP, 0) * 13.0F / (float) this.capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(0.4F, 1.0F, 1.0F); // Lime green
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int stored = stack.getOrDefault(MeridianRegistry.STORED_XP, 0);
        tooltip.add(Component.translatable("info.meridian.xp_tome.stored", stored, capacity).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("info.meridian.xp_tome.interact1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("info.meridian.xp_tome.interact2").withStyle(ChatFormatting.GRAY));
    }
}
