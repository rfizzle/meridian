package com.rfizzle.meridian.compat.jade;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.JadeTooltipFormatter;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.EnchantingStats;
import com.rfizzle.meridian.enchanting.IEnchantingStatProvider;
import com.rfizzle.meridian.library.EnchantmentLibraryBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.EnchantingTableBlock;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client-only Jade provider that shows per-block enchanting stat contributions on any block
 * registered in the {@link EnchantingStatRegistry}. Vanilla bookshelves receive the registry's
 * tag-based fallback (eterna: 1, maxEterna: 15); custom shelves resolve from their JSON stat
 * files. Suppresses Jade's built-in "Ench Power" line when Meridian stats are displayed.
 *
 * <p>Enchanting tables and libraries are skipped — they have dedicated providers that show
 * aggregate stats and library summaries respectively.
 */
final class BlockStatsJadeProvider implements IBlockComponentProvider {

    static final BlockStatsJadeProvider INSTANCE = new BlockStatsJadeProvider();

    static final ResourceLocation UID = Meridian.id("block_enchanting_stats");

    private BlockStatsJadeProvider() {}

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (accessor.getBlock() instanceof EnchantingTableBlock
                || accessor.getBlock() instanceof EnchantmentLibraryBlock) {
            return;
        }
        EnchantingStats stats;
        if (accessor.getBlock() instanceof IEnchantingStatProvider provider) {
            stats = provider.getStats(accessor.getLevel(), accessor.getPosition(),
                    accessor.getBlockState());
        } else {
            stats = EnchantingStatRegistry.lookup(accessor.getLevel(), accessor.getBlockState());
        }
        if (stats == null || stats.equals(EnchantingStats.ZERO)) {
            return;
        }
        for (String line : JadeTooltipFormatter.blockStatsLines(stats)) {
            tooltip.add(Component.literal(line));
        }
        tooltip.remove(JadeIds.MC_ENCHANTMENT_POWER);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
