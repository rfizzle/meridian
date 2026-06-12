package com.rfizzle.meridian.api;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

/**
 * Fired server-side at the end of {@code /meridian reload}, after the JSON config has been
 * re-read from disk, the per-enchantment info ({@link EnchantmentInfo}) has been rebuilt, and
 * the result has been synced to all connected clients. Listen to this instead of polling
 * {@link MeridianAPI#getEnchantmentInfo} for changes.
 *
 * <p><b>Triggers:</b> exactly one — the {@code /meridian reload} command completing
 * successfully. Vanilla datapack reloads ({@code /reload}) also rebuild the enchantment info
 * but do not fire this event; use Fabric's
 * {@code ServerLifecycleEvents.END_DATA_PACK_RELOAD} for those.
 *
 * <p>Listener exceptions are caught and logged by Meridian (host-side error isolation per the
 * Concord API Standard) — a misbehaving listener cannot break the reload or other listeners.
 */
@ApiStatus.Stable
@FunctionalInterface
public interface MeridianReloadCallback {

    /**
     * The event. Array-backed; listeners are invoked in registration order on the server
     * thread.
     */
    Event<MeridianReloadCallback> EVENT = EventFactory.createArrayBacked(
            MeridianReloadCallback.class,
            listeners -> server -> {
                for (MeridianReloadCallback listener : listeners) {
                    try {
                        listener.onReload(server);
                    } catch (Exception e) {
                        Meridian.LOGGER.error(
                                "MeridianReloadCallback listener {} threw; ignoring",
                                listener.getClass().getName(), e);
                    }
                }
            });

    /**
     * Called after {@code /meridian reload} has re-read the config and rebuilt the
     * enchantment info.
     *
     * @param server the server the reload ran on
     */
    void onReload(MinecraftServer server);
}
