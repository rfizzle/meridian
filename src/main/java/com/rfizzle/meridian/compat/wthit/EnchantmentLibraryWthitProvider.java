package com.rfizzle.meridian.compat.wthit;

import com.rfizzle.meridian.compat.common.JadeTooltipFormatter;
import com.rfizzle.meridian.library.EnchantmentLibraryBlockEntity;

import net.minecraft.network.chat.Component;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;

/**
 * Client-only tooltip provider for enchantment libraries. Reads the block entity directly
 * since libraries sync their data via vanilla chunk update packets.
 */
final class EnchantmentLibraryWthitProvider implements IBlockComponentProvider {

    static final EnchantmentLibraryWthitProvider INSTANCE = new EnchantmentLibraryWthitProvider();

    private EnchantmentLibraryWthitProvider() {}

    @Override
    public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof EnchantmentLibraryBlockEntity be)) {
            return;
        }
        int storedCount = countStoredEnchants(be);
        tooltip.addLine(Component.literal(JadeTooltipFormatter.libraryLine(storedCount)));
    }

    static int countStoredEnchants(EnchantmentLibraryBlockEntity be) {
        int stored = 0;
        for (int value : be.getPoints().values()) {
            if (value > 0) {
                stored++;
            }
        }
        return stored;
    }
}
