package com.rfizzle.meridian.compat.wthit;

import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;

import com.rfizzle.meridian.library.EnchantmentLibraryBlockEntity;

import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IWailaCommonPlugin;

public final class WthitCommonPlugin implements IWailaCommonPlugin {

    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.blockData(EnchantingTableWthitProvider.INSTANCE, EnchantingTableBlockEntity.class);
    }
}
