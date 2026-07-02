package com.rfizzle.meridian.compat.tribulation;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Soft-dependency probe for Tribulation's soul-inventory, used to decide the single owner of the
 * keep-on-death behavior when both mods are installed.
 *
 * <p>Meridian's {@code TetherHandler} and Tribulation's soul-inventory both implement
 * keep-on-death for enchants in {@code #c:soulbound}. If both captured the same item the player
 * would receive it twice on respawn, so exactly one mod may own a given death: Tribulation wins
 * when its soul-inventory is active, and Meridian stands down.
 *
 * <p>The probe fails open. It reports {@code true} only when Tribulation is loaded, exposes
 * {@code TribulationAPI.isSoulInventoryActive()}, and that accessor returns {@code true}. In every
 * other case — Tribulation absent, an older Tribulation that predates the accessor, the
 * soul-inventory disabled in config, or the reflective call throwing — Meridian handles
 * keep-on-death itself.
 */
public final class TribulationCompat {

    private static final String API_CLASS = "com.rfizzle.tribulation.api.TribulationAPI";
    private static final String API_METHOD = "isSoulInventoryActive";

    private static volatile boolean resolved;
    private static volatile MethodHandle handle;
    private static final AtomicBoolean LOGGED = new AtomicBoolean(false);

    private TribulationCompat() {}

    /**
     * Whether Tribulation's soul-inventory currently owns keep-on-death handling. Config is
     * hot-reloadable on the Tribulation side, so this is re-queried per death rather than cached.
     */
    public static boolean isSoulInventoryActive() {
        if (!FabricLoader.getInstance().isModLoaded("tribulation")) return false;
        MethodHandle h = resolve();
        if (h == null) return false;
        try {
            return (boolean) h.invokeExact();
        } catch (Throwable t) {
            if (LOGGED.compareAndSet(false, true)) {
                Meridian.LOGGER.warn("{}.{} threw; Meridian keeps handling tether", API_CLASS, API_METHOD, t);
            }
            return false;
        }
    }

    private static MethodHandle resolve() {
        if (resolved) return handle;
        synchronized (TribulationCompat.class) {
            if (resolved) return handle;
            try {
                handle = MethodHandles.publicLookup().findStatic(
                        Class.forName(API_CLASS), API_METHOD, MethodType.methodType(boolean.class));
            } catch (Throwable t) {
                if (LOGGED.compareAndSet(false, true)) {
                    Meridian.LOGGER.info(
                            "{}.{} unavailable (Tribulation predates it?); Meridian keeps handling tether",
                            API_CLASS, API_METHOD);
                }
            }
            resolved = true;
            return handle;
        }
    }
}
