package com.rfizzle.meridian.compat.wthit;

import com.rfizzle.meridian.compat.common.JadeTooltipFormatter;
import com.rfizzle.meridian.enchanting.EnchantingStatRegistry;
import com.rfizzle.meridian.enchanting.StatCollection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IDataProvider;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;

/**
 * Combined server data provider + client tooltip provider for the enchanting table.
 * Mirrors {@code EnchantingTableJadeProvider} — gathers stats server-side, formats via
 * {@link JadeTooltipFormatter} client-side.
 */
final class EnchantingTableWthitProvider
        implements IDataProvider<EnchantingTableBlockEntity>, IBlockComponentProvider {

    static final EnchantingTableWthitProvider INSTANCE = new EnchantingTableWthitProvider();

    static final String KEY_ETERNA = "Eterna";
    static final String KEY_MAX_ETERNA = "MaxEterna";
    static final String KEY_QUANTA = "Quanta";
    static final String KEY_ARCANA = "Arcana";
    static final String KEY_RECTIFICATION = "Rectification";
    static final String KEY_CLUES = "Clues";

    private EnchantingTableWthitProvider() {}

    @Override
    public void appendData(IDataWriter data, IServerAccessor<EnchantingTableBlockEntity> accessor,
                           IPluginConfig config) {
        StatCollection stats = EnchantingStatRegistry.gatherStats(
                accessor.getLevel(), accessor.getTarget().getBlockPos());
        CompoundTag raw = data.raw();
        raw.putFloat(KEY_ETERNA, stats.eterna());
        raw.putFloat(KEY_MAX_ETERNA, stats.maxEterna());
        raw.putFloat(KEY_QUANTA, stats.quanta());
        raw.putFloat(KEY_ARCANA, stats.arcana());
        raw.putFloat(KEY_RECTIFICATION, stats.rectification());
        raw.putInt(KEY_CLUES, stats.clues());
    }

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getData().raw();
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
            tooltip.addLine(Component.literal(line));
        }
    }
}
