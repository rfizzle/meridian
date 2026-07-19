package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.SpyglassEnchantMath;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracker's Lens: a creature held steady in an enchanted spyglass for
 * {@link SpyglassEnchantMath#TRACKERS_LENS_SIGHTING_TICKS} ticks is marked, glowing through walls
 * for a level-scaled duration — Mark without the arrow. Both acquiring a creature and keeping it
 * held need a clear line of sight; only the glow that follows ignores walls, which is the point of
 * the enchantment.
 *
 * <p>Sighting progress is transient by design: it lives only in {@link #SCOPES}, never touches an
 * attachment or save file, and is forgotten on disconnect and server stop. A player who looks away
 * mid-sighting starts over, which is the intended cost of the mechanic rather than state worth
 * persisting.
 *
 * <p>Acquisition is the expensive half — a block raycast, plus an entity sweep when no target is
 * held — so it runs on a {@link SpyglassEnchantMath#SIGHTING_SAMPLE_INTERVAL_TICKS} interval rather
 * than every tick, and a player already holding a creature takes the cheap path: resolve the target
 * by id, reject on a view-cone test, and clip only as far as the target actually is.
 */
public final class TrackersLensHandler {

    /**
     * Sentinel sample count meaning "this target was already marked and its glow is still running".
     * Without it, holding the lens on a creature would re-mark it every sample and make the glow
     * permanent. Once the glow lapses the sighting restarts, so a player who keeps watching does
     * get a fresh mark — it just costs another full sighting.
     */
    private static final int ALREADY_MARKED = -1;

    /** Each scoping player's current target and how many consecutive samples they have held it. */
    private static final Map<UUID, ScopeState> SCOPES = new ConcurrentHashMap<>();

    /** Drives the sampling interval, so most ticks cost a single increment and a comparison. */
    private static long tickCounter;

    private record ScopeState(UUID targetId, int samples) {}

    private TrackersLensHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TrackersLensHandler::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SCOPES.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> reset());
    }

    private static void onServerTick(MinecraftServer server) {
        // Interval gate first: a server where nobody is scoping pays one increment per tick.
        if (++tickCounter % SpyglassEnchantMath.SIGHTING_SAMPLE_INTERVAL_TICKS != 0) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Cheap bail-outs stay outside the guard, per mc-tick-work.
            ItemStack using = player.isUsingItem() ? player.getUseItem() : ItemStack.EMPTY;
            if (!using.is(Items.SPYGLASS)) {
                if (!SCOPES.isEmpty()) SCOPES.remove(player.getUUID());
                continue;
            }
            EffectGuard.run("trackers_lens", player, () -> sampleScope(player, using));
        }
    }

    /**
     * Advances one player's sighting by a sample and marks the target once it has been held for
     * {@link SpyglassEnchantMath#SIGHTING_SAMPLES} of them. Takes the spyglass explicitly rather
     * than reading {@code player.getUseItem()} so the gametests can drive a sighting without faking
     * item-use state.
     */
    public static void sampleScope(ServerPlayer player, ItemStack spyglass) {
        int level = EnchantmentEffects.getEnchantmentLevel(spyglass, EnchantmentEffects.TRACKERS_LENS);
        if (level <= 0) {
            SCOPES.remove(player.getUUID());
            return;
        }

        ScopeState state = SCOPES.get(player.getUUID());
        LivingEntity target = stillHeld(player, state);
        if (target == null) {
            target = acquireTarget(player);
            state = null; // a different creature (or none): the previous sighting is spent
        }
        if (target == null) {
            SCOPES.remove(player.getUUID());
            return;
        }

        if (state != null && state.samples() == ALREADY_MARKED) {
            // Hold the mark while its glow is still running; once it lapses, start a fresh sighting
            // rather than re-marking for free, so watching longer earns the next mark honestly.
            if (target.hasEffect(MobEffects.GLOWING)) return;
            SCOPES.put(player.getUUID(), new ScopeState(target.getUUID(), 1));
            return;
        }

        int samples = state != null ? state.samples() + 1 : 1;
        if (samples < SpyglassEnchantMath.SIGHTING_SAMPLES) {
            SCOPES.put(player.getUUID(), new ScopeState(target.getUUID(), samples));
            return;
        }

        // ambient=true, no particles, no icon: an unobtrusive tracking glow, not a status debuff.
        // Mirrors Mark, so the two enchantments read identically in-world.
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                SpyglassEnchantMath.trackersLensGlowTicks(level), 0, true, false, false));
        SCOPES.put(player.getUUID(), new ScopeState(target.getUUID(), ALREADY_MARKED));
    }

    /**
     * The cheap continuation path: is {@code state}'s creature still the one being watched? Resolves
     * it by id instead of sweeping entities, rejects on a view-cone test before touching the world,
     * and clips only as far as the target actually is rather than the full acquisition range.
     * Returns {@code null} when the hold is broken, which sends the caller back to a full acquire.
     */
    private static LivingEntity stillHeld(ServerPlayer player, ScopeState state) {
        if (state == null) return null;
        if (!(player.serverLevel().getEntity(state.targetId()) instanceof LivingEntity target)) return null;
        if (!target.isAlive() || target.isSpectator() || target == player) return null;
        if (!trackersLensTargetAllowed(target)) return null;

        Vec3 eye = player.getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        Vec3 delta = targetEye.subtract(eye);
        double distance = delta.length();
        if (distance < 1.0e-4 || distance > SpyglassEnchantMath.TRACKERS_LENS_RANGE) return null;
        if (player.getViewVector(1.0f).dot(delta.scale(1.0 / distance))
                < SpyglassEnchantMath.TRACKING_CONE_COS) {
            return null;
        }

        // Keeping the lock still needs a clear sight line, so ducking behind a wall mid-sighting
        // breaks it — but this clip is only as long as the target is away, not the full range.
        BlockHitResult wall = player.level().clip(new ClipContext(eye, targetEye,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return wall.getType() == HitResult.Type.MISS ? target : null;
    }

    /**
     * Resolves the creature centred in the player's spyglass. The sight line is clipped against
     * blocks first, so a creature behind terrain can never be acquired — mirroring Seeker's
     * lock-on, which is the established shape for "the thing under the crosshair, honestly".
     */
    private static LivingEntity acquireTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = loadedRayEnd(player, start);
        if (end == null) return null;

        BlockHitResult wall = player.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }

        Vec3 reach = end.subtract(start);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player.level(), player,
                start, end,
                player.getBoundingBox().expandTowards(reach).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator()
                        && e != player && trackersLensTargetAllowed(e));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /**
     * The far end of the acquisition ray, pulled back to terrain the server already has in memory.
     * {@code Level#clip} resolves each stepped block through {@code getChunk(.., FULL)}, which
     * loads — and at the edge of a world, generates — synchronously on the server thread. Sampling
     * a full-range ray outward from a player at the frontier would pay that repeatedly for as long
     * as they held the scope, so the ray stops at the last loaded chunk instead. Returns
     * {@code null} if even the near end is unloaded, which cannot normally happen around a player.
     */
    private static Vec3 loadedRayEnd(ServerPlayer player, Vec3 start) {
        Vec3 direction = player.getViewVector(1.0f);
        for (double distance = SpyglassEnchantMath.TRACKERS_LENS_RANGE; distance > 0.0; distance -= 16.0) {
            Vec3 candidate = start.add(direction.scale(distance));
            if (player.level().hasChunkAt(BlockPos.containing(candidate))) return candidate;
        }
        return null;
    }

    /**
     * Whether Tracker's Lens may mark this target: mobs always, players only when
     * {@code combat.trackersLensAffectsPlayers} is enabled. Mirrors Mark's gate.
     */
    public static boolean trackersLensTargetAllowed(Entity target) {
        if (!(target instanceof Player)) return true;
        MeridianConfig config = Meridian.getConfig();
        return config != null && config.combat.trackersLensAffectsPlayers;
    }

    /** Clears all sighting progress. Test seam, and the SERVER_STOPPED reset. */
    public static void reset() {
        SCOPES.clear();
        tickCounter = 0;
    }
}
