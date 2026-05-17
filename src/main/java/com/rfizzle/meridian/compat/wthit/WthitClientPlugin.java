package com.rfizzle.meridian.compat.wthit;

import com.rfizzle.meridian.library.EnchantmentLibraryBlock;

import net.minecraft.world.level.block.EnchantingTableBlock;

import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IWailaClientPlugin;

public final class WthitClientPlugin implements IWailaClientPlugin {

    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(EnchantingTableWthitProvider.INSTANCE, EnchantingTableBlock.class);
        registrar.body(EnchantmentLibraryWthitProvider.INSTANCE, EnchantmentLibraryBlock.class);
    }
}
