package com.rfizzle.meridian.api;

import com.rfizzle.meridian.Meridian;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicBoolean;

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
 * <p><b>Isolation posture</b> (Concord API Standard §3.1): each listener is invoked inside its
 * own {@code try}/{@code catch} in the invoker, so a listener that throws — an {@link Exception},
 * or an {@link Error} such as {@code AbstractMethodError} from a stale compile — is skipped and
 * the remaining listeners still run. {@link VirtualMachineError} is rethrown unchanged. The
 * failure is logged once at {@code WARN}, naming the offending listener class.
 */
@Stable
@FunctionalInterface
public interface MeridianReloadCallback {

    /** One-shot gate so a listener that throws on every reload logs its stack trace once. */
    AtomicBoolean LISTENER_FAILURE_LOGGED = new AtomicBoolean(false);

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
                    } catch (VirtualMachineError e) {
                        throw e; // OOME/SOE: the JVM is gone, not the guest
                    } catch (Throwable t) {
                        // Throwable, not Exception: a listener compiled against an older
                        // signature throws Error (AbstractMethodError, NoClassDefFoundError),
                        // which an Exception catch would let escape and kill the reload.
                        if (LISTENER_FAILURE_LOGGED.compareAndSet(false, true)) {
                            Meridian.LOGGER.warn(
                                    "MeridianReloadCallback listener {} threw; skipping",
                                    listener.getClass().getName(), t);
                        }
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
