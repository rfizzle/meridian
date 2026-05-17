package com.rfizzle.meridian.compat.jade;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.JadeTooltipFormatter;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.StatCollection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

/**
 * Combined {@link IServerDataProvider} + {@link IBlockComponentProvider} for vanilla enchanting
 * tables. The server side computes the five-axis stats via
 * {@link EnchantingStatRegistry#gatherStats} (which already implements the transmitter LoS + tag
 * fallbacks) and writes them to the Jade data packet; the client side reads the same keys and
 * hands them to {@link JadeTooltipFormatter#enchantingTableLines} for formatting.
 *
 * <p>Going through a server data provider rather than calling {@code gatherStats} on the client
 * is deliberate: the shelf-stat registry is loaded through a
 * {@link net.fabricmc.fabric.api.resource.ResourceManagerHelper#get
 * ResourceManagerHelper.get(PackType.SERVER_DATA)} listener, which only ticks on the integrated
 * server. Remote-multiplayer clients would read an empty registry and report all stats as zero.
 */
final class EnchantingTableJadeProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {

    static final EnchantingTableJadeProvider INSTANCE = new EnchantingTableJadeProvider();

    static final ResourceLocation UID = Meridian.id("enchanting_table_stats");

    static final String KEY_ETERNA = "Eterna";
    static final String KEY_MAX_ETERNA = "MaxEterna";
    static final String KEY_QUANTA = "Quanta";
    static final String KEY_ARCANA = "Arcana";
    static final String KEY_RECTIFICATION = "Rectification";
    static final String KEY_CLUES = "Clues";

    private EnchantingTableJadeProvider() {
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        StatCollection stats = EnchantingStatRegistry.gatherStats(accessor.getLevel(), accessor.getPosition());
        data.putFloat(KEY_ETERNA, stats.eterna());
        data.putFloat(KEY_MAX_ETERNA, stats.maxEterna());
        data.putFloat(KEY_QUANTA, stats.quanta());
        data.putFloat(KEY_ARCANA, stats.arcana());
        data.putFloat(KEY_RECTIFICATION, stats.rectification());
        data.putInt(KEY_CLUES, stats.clues());
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data == null || !data.contains(KEY_ETERNA)) {
            return;
        }
        StatCollection stats = new StatCollection(
                data.getFloat(KEY_ETERNA),
                data.getFloat(KEY_QUANTA),
                data.getFloat(KEY_ARCANA),
                data.getFloat(KEY_RECTIFICATION),
                data.getInt(KEY_CLUES),
                data.getFloat(KEY_MAX_ETERNA),
                java.util.Set.of(),
                false);
        for (String line : JadeTooltipFormatter.enchantingTableLines(stats)) {
            tooltip.add(Component.literal(line));
        }
        tooltip.remove(JadeIds.MC_TOTAL_ENCHANTMENT_POWER);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
