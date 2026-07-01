package com.rfizzle.meridian.compat.client;

import com.rfizzle.meridian.compat.emi.EmiEnchantingPlugin;
import com.rfizzle.meridian.compat.jei.JeiEnchantingPlugin;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Refreshes whichever recipe viewers expose a usable runtime reload after an enchantment-info sync
 * lands on the client, so the enchantment browser reflects the server's values without a manual
 * viewer reload. Each call is guarded by {@link FabricLoader#isModLoaded} so viewer classes are
 * never loaded unless the corresponding mod is installed; this keeps the production jar safe when
 * only a subset of the optional viewers is present.
 *
 * <p>EMI is reloaded via reflection (its only reload entry point is internal); JEI is refreshed
 * through its runtime recipe API. REI has no safe programmatic reload, so it relies on the sync
 * landing before it builds its lists — correct on first join, the common case — and picks up a live
 * {@code /reload} on rejoin or a manual resource reload (F3+T).
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
        if (FabricLoader.getInstance().isModLoaded("jei")) {
            JeiEnchantingPlugin.notifySync();
        }
    }
}
