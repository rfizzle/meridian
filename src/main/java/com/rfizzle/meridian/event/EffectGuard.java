package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * Runs a single enchantment-effect body in isolation. A thrown {@link Exception} is caught,
 * logged with the effect's name and the entity it was acting on, then swallowed — so one
 * misbehaving effect degrades to "that effect did nothing this tick" instead of escaping into
 * Fabric's event dispatch (or a mixin-injected vanilla method) and breaking the tick or damage
 * path for every other effect and entity on the server.
 *
 * <p>Cheap applicability bail-outs (client-side checks, {@code level <= 0} gates, type tests)
 * belong at the call site, <em>outside</em> the guarded body — the guard is for the effect logic
 * itself, per the {@code mc-tick-work} guardrail. This mirrors the host-side error isolation the
 * Concord API Standard already applies to {@code MeridianReloadCallback} listeners.
 */
public final class EffectGuard {

    /**
     * Last wall-clock time each effect name logged a full failure, so a persistently-throwing
     * effect is reported at most once per {@link #LOG_THROTTLE_MS} instead of flooding the log
     * (and the server thread with stack-trace formatting) at 20 Hz × every entity. Keyed by the
     * fixed, finite set of effect-name literals, so it is inherently bounded and holds no entity
     * or world reference — it deliberately needs no lifecycle reset and self-heals within one
     * throttle window across a restart.
     */
    private static final Map<String, Long> LAST_LOGGED = new ConcurrentHashMap<>();
    private static final long LOG_THROTTLE_MS = 1000L;

    private EffectGuard() {}

    /**
     * Runs a void effect body. A thrown exception is logged against {@code effect}/{@code context}
     * and swallowed so sibling effects in the same tick or event still run.
     *
     * @param effect  short identifier of the effect, for the log line
     * @param context the entity the effect was acting on (may be {@code null} for a server-wide sweep)
     * @param body    the effect logic to run in isolation
     */
    public static void run(String effect, Entity context, Runnable body) {
        try {
            body.run();
        } catch (Exception e) {
            log(effect, context, e);
        }
    }

    /**
     * Runs an effect body whose boolean result the dispatcher acts on (allow-damage, allow-death,
     * arrow-was-consumed, interaction-consumed). On a thrown exception the guard logs and returns
     * {@code fallback}, chosen at each call site so the failure fails <em>open</em> — damage and
     * death proceed as vanilla, the projectile/interaction behaves normally.
     *
     * @param effect   short identifier of the effect, for the log line
     * @param context  the entity the effect was acting on (may be {@code null})
     * @param fallback the fail-open value to return if {@code body} throws
     * @param body     the effect logic to run in isolation
     * @return {@code body}'s result, or {@code fallback} if it threw
     */
    public static boolean run(String effect, Entity context, boolean fallback, BooleanSupplier body) {
        try {
            return body.getAsBoolean();
        } catch (Exception e) {
            log(effect, context, e);
            return fallback;
        }
    }

    private static void log(String effect, Entity context, Exception e) {
        long now = System.currentTimeMillis();
        Long last = LAST_LOGGED.get(effect);
        if (last != null && now - last < LOG_THROTTLE_MS) {
            return; // this effect already logged a full trace within the window — suppress the flood
        }
        LAST_LOGGED.put(effect, now);
        Meridian.LOGGER.error("Enchantment effect '{}' threw for {}; skipping it", effect, context, e);
    }
}
