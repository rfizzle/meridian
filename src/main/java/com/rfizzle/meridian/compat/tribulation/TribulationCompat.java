package com.rfizzle.meridian.compat.tribulation;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.tribulation.api.TribulationAPI;
import net.fabricmc.loader.api.FabricLoader;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Soft-dependency bridge to Tribulation's soul-inventory, used to decide the single owner of the
 * keep-on-death behavior when both mods are installed.
 *
 * <p>Meridian's {@code TetherHandler} and Tribulation's soul-inventory both implement
 * keep-on-death for enchants in {@code #c:soulbound}. If both captured the same item the player
 * would receive it twice on respawn, so exactly one mod may own a given death: Tribulation wins
 * when its soul-inventory is active, and Meridian stands down.
 *
 * <p>The bridge fails open. It reports {@code true} only when Tribulation is loaded and
 * {@code TribulationAPI.isSoulInventoryActive()} returns {@code true}. In every other case —
 * Tribulation absent, the soul-inventory disabled in config, or the call throwing — Meridian
 * handles keep-on-death itself.
 *
 * <p>{@code TribulationAPI} is referenced through {@code modCompileOnly} rather than reflection,
 * guarded by {@code isModLoaded}. The {@code catch (Throwable)} still matters: an installed
 * Tribulation older than the {@code tribulation_version} Meridian compiled against predates
 * {@code isSoulInventoryActive()} and surfaces the miss as a {@link LinkageError}, which must
 * degrade to Meridian owning the death — never crash it.
 */
public final class TribulationCompat {

    private static final AtomicBoolean LOGGED = new AtomicBoolean(false);

    private TribulationCompat() {}

    /**
     * Whether Tribulation's soul-inventory currently owns keep-on-death handling. Config is
     * hot-reloadable on the Tribulation side, so this is re-queried per death rather than cached.
     */
    public static boolean isSoulInventoryActive() {
        if (!FabricLoader.getInstance().isModLoaded("tribulation")) return false;
        try {
            return TribulationAPI.isSoulInventoryActive();
        } catch (Throwable t) {
            if (LOGGED.compareAndSet(false, true)) {
                Meridian.LOGGER.warn(
                        "TribulationAPI.isSoulInventoryActive() unavailable (older Tribulation?); "
                                + "Meridian keeps handling tether", t);
            }
            return false;
        }
    }
}
