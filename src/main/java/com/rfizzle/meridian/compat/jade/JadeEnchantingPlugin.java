package com.rfizzle.meridian.compat.jade;

import com.rfizzle.meridian.library.EnchantmentLibraryBlock;
import com.rfizzle.meridian.library.EnchantmentLibraryBlockEntity;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade probe-tooltip integration (T-7.4.1). Registered via the {@code jade} entrypoint in
 * {@code fabric.mod.json} — Jade scans that list at load time, so the plugin class only links
 * when Jade is actually present in the runtime.
 *
 * <p>The {@link WailaPlugin} annotation is carried for Jade's annotation-based discovery on
 * platforms that prefer it; on Fabric it is redundant with the entrypoint but harmless.
 *
 * <p>Split into two providers:
 * <ul>
 *   <li>{@link EnchantingTableJadeProvider} — pairs an {@code IServerDataProvider} (ships the
 *       five {@code StatCollection} axes over the Jade data packet so dedicated-server clients
 *       see correct stats) with an {@code IBlockComponentProvider} (reads the packet, formats
 *       via {@link com.rfizzle.meridian.compat.common.JadeTooltipFormatter}).</li>
 *   <li>{@link EnchantmentLibraryJadeProvider} — tooltip-only. The library BE is auto-synced to
 *       clients via vanilla chunk update packets, so the component can read {@code points.size()}
 *       directly without a server data round-trip.</li>
 * </ul>
 */
@WailaPlugin("meridian")
public final class JadeEnchantingPlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                EnchantingTableJadeProvider.INSTANCE, EnchantingTableBlock.class);
        registration.registerItemStorage(
                LibraryItemStorageProvider.INSTANCE, EnchantmentLibraryBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                EnchantingTableJadeProvider.INSTANCE, EnchantingTableBlock.class);
        registration.registerBlockComponent(
                EnchantmentLibraryJadeProvider.INSTANCE, EnchantmentLibraryBlock.class);
        registration.registerBlockComponent(
                BlockStatsJadeProvider.INSTANCE, Block.class);
    }
}
