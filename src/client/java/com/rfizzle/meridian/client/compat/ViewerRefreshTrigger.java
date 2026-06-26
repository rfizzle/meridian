package com.rfizzle.meridian.client.compat;

import com.rfizzle.meridian.compat.emi.EmiEnchantingPlugin;
import com.rfizzle.meridian.compat.jei.JeiEnchantingPlugin;
import com.rfizzle.meridian.compat.rei.ReiEnchantingPlugin;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Dispatches an enchantment-info-sync notification to whichever recipe viewers are present at
 * runtime. Each call is guarded by {@link FabricLoader#isModLoaded} so viewer classes are never
 * loaded unless the corresponding mod is installed; this keeps the production jar safe when only a
 * subset of the optional viewers is present.
 *
 * <p>All calls happen on the render thread (inside a {@code client.execute()} block in
 * {@link com.rfizzle.meridian.client.net.ClientPayloadHandlers}).
 */
public final class ViewerRefreshTrigger {

    private ViewerRefreshTrigger() {
    }

    public static void notifySync() {
        if (FabricLoader.getInstance().isModLoaded("emi")) {
            EmiEnchantingPlugin.notifySync();
        }
        if (FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
            ReiEnchantingPlugin.notifySync();
        }
        if (FabricLoader.getInstance().isModLoaded("jei")) {
            JeiEnchantingPlugin.notifySync();
        }
    }
}
