package com.rfizzle.meridian.shelf;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class MeridianShelfItem extends BlockItem {

    public MeridianShelfItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Block block = getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        // Mirrors the per-item appendHoverText pattern: info.meridian.<block> (+ "2", "3", …
        // continuation lines) describe what the block does, ahead of the stat readout.
        if (blockId.getNamespace().equals(Meridian.MOD_ID)) {
            String infoKey = "info." + Meridian.MOD_ID + "." + blockId.getPath();
            for (int i = 0; Language.getInstance().has(i == 0 ? infoKey : infoKey + (i + 1)); i++) {
                tooltip.add(Component.translatable(i == 0 ? infoKey : infoKey + (i + 1))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        EnchantingStats stats = EnchantingStatRegistry.getInstance().blockEntries().get(blockId);
        boolean hasStats = stats != null && !stats.equals(EnchantingStats.ZERO)
                && (stats.eterna() != 0 || stats.quanta() != 0 || stats.arcana() != 0
                || stats.rectification() != 0 || stats.clues() != 0);
        boolean treasure = block instanceof TreasureShelfBlock;

        if (!hasStats && !treasure) return;

        tooltip.add(Component.translatable("info.meridian.shelf.ench_stats")
                .withStyle(ChatFormatting.GOLD));

        if (hasStats) {
            if (stats.eterna() != 0) {
                if (stats.eterna() > 0) {
                    tooltip.add(Component.translatable("info.meridian.shelf.eterna.p",
                            String.format("%.2f", stats.eterna()), String.format("%.2f", stats.maxEterna()))
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    tooltip.add(Component.translatable("info.meridian.shelf.eterna",
                            String.format("%.2f", stats.eterna())).withStyle(ChatFormatting.GREEN));
                }
            }
            if (stats.quanta() != 0) {
                tooltip.add(Component.translatable("info.meridian.shelf.quanta" + (stats.quanta() > 0 ? ".p" : ""),
                        String.format("%.2f", stats.quanta())).withStyle(ChatFormatting.RED));
            }
            if (stats.arcana() != 0) {
                tooltip.add(Component.translatable("info.meridian.shelf.arcana" + (stats.arcana() > 0 ? ".p" : ""),
                        String.format("%.2f", stats.arcana())).withStyle(ChatFormatting.DARK_PURPLE));
            }
            if (stats.rectification() != 0) {
                tooltip.add(Component.translatable("info.meridian.shelf.rectification" + (stats.rectification() > 0 ? ".p" : ""),
                        String.format("%.2f", stats.rectification())).withStyle(ChatFormatting.YELLOW));
            }
            if (stats.clues() != 0) {
                tooltip.add(Component.translatable("info.meridian.shelf.clues" + (stats.clues() > 0 ? ".p" : ""),
                        String.format("%d", stats.clues())).withStyle(ChatFormatting.DARK_AQUA));
            }
        }

        if (treasure) {
            tooltip.add(Component.translatable("info.meridian.shelf.allows_treasure")
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
