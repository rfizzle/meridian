package com.rfizzle.meridian.compat.jade;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.JadeTooltipFormatter;
import com.rfizzle.meridian.library.EnchantmentLibraryBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Client-side probe tooltip for Basic and Ender libraries. Reads the BE directly — the library
 * BE publishes its full point/level maps via {@link EnchantmentLibraryBlockEntity#getUpdateTag}
 * on every mutation, so the client copy is always current.
 *
 * <p>Only surfaces the aggregate enchantment count, not per-enchant points: DESIGN keeps the
 * detailed breakdown inside the library UI so the world tooltip stays at a single line per
 * library, matching Jade's usual density.
 */
final class EnchantmentLibraryJadeProvider implements IBlockComponentProvider {

    static final EnchantmentLibraryJadeProvider INSTANCE = new EnchantmentLibraryJadeProvider();

    static final ResourceLocation UID = Meridian.id("enchantment_library_summary");

    private EnchantmentLibraryJadeProvider() {
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof EnchantmentLibraryBlockEntity be)) {
            return;
        }
        int storedCount = countStoredEnchants(be);
        tooltip.add(Component.literal(JadeTooltipFormatter.libraryLine(storedCount)));
    }

    /** Visible for package-private tests and the library provider above. */
    static int countStoredEnchants(EnchantmentLibraryBlockEntity be) {
        int stored = 0;
        for (int value : be.getPoints().values()) {
            if (value > 0) {
                stored++;
            }
        }
        return stored;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
