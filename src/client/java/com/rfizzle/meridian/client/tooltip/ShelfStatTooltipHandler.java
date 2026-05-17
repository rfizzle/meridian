package com.rfizzle.meridian.client.tooltip;

import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.shelf.TreasureShelfBlock;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class ShelfStatTooltipHandler {

    private ShelfStatTooltipHandler() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register(ShelfStatTooltipHandler::onTooltip);
    }

    private static void onTooltip(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                  net.minecraft.world.item.TooltipFlag flag, List<Component> lines) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        Block block = blockItem.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        EnchantingStats stats = EnchantingStatRegistry.getInstance().blockEntries().get(blockId);
        boolean hasStats = stats != null && !stats.equals(EnchantingStats.ZERO)
                && (stats.eterna() != 0 || stats.quanta() != 0 || stats.arcana() != 0
                || stats.rectification() != 0 || stats.clues() != 0);
        boolean treasure = block instanceof TreasureShelfBlock;

        if (!hasStats && !treasure) return;

        lines.add(Component.translatable("info.meridian.shelf.ench_stats")
                .withStyle(ChatFormatting.GOLD));

        if (hasStats) {
            if (stats.eterna() != 0) {
                if (stats.eterna() > 0) {
                    lines.add(Component.translatable("info.meridian.shelf.eterna.p",
                            String.format("%.2f", stats.eterna()), String.format("%.2f", stats.maxEterna()))
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    lines.add(Component.translatable("info.meridian.shelf.eterna",
                            String.format("%.2f", stats.eterna())).withStyle(ChatFormatting.GREEN));
                }
            }
            if (stats.quanta() != 0) {
                lines.add(Component.translatable("info.meridian.shelf.quanta" + (stats.quanta() > 0 ? ".p" : ""),
                        String.format("%.2f", stats.quanta())).withStyle(ChatFormatting.RED));
            }
            if (stats.arcana() != 0) {
                lines.add(Component.translatable("info.meridian.shelf.arcana" + (stats.arcana() > 0 ? ".p" : ""),
                        String.format("%.2f", stats.arcana())).withStyle(ChatFormatting.DARK_PURPLE));
            }
            if (stats.rectification() != 0) {
                lines.add(Component.translatable("info.meridian.shelf.rectification" + (stats.rectification() > 0 ? ".p" : ""),
                        String.format("%.2f", stats.rectification())).withStyle(ChatFormatting.YELLOW));
            }
            if (stats.clues() != 0) {
                lines.add(Component.translatable("info.meridian.shelf.clues" + (stats.clues() > 0 ? ".p" : ""),
                        String.format("%d", stats.clues())).withStyle(ChatFormatting.DARK_AQUA));
            }
        }

        if (treasure) {
            lines.add(Component.translatable("info.meridian.shelf.allows_treasure")
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
