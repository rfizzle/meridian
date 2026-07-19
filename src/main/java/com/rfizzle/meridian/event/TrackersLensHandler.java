package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.SpyglassEnchantMath;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracker's Lens: a creature held steady in an enchanted spyglass for
 * {@link SpyglassEnchantMath#TRACKERS_LENS_SIGHTING_TICKS} ticks is marked, glowing through walls
 * for a level-scaled duration — Mark without the arrow. The initial sighting needs line of sight;
 * the glow that follows deliberately does not, which is the whole point of the enchantment.
 *
 * <p>Sighting progress is transient by design: it lives only in {@link #SCOPES}, never touches an
 * attachment or save file, and is forgotten on disconnect and server stop. A player who looks away
 * mid-sighting starts over, which is the intended cost of the mechanic rather than state worth
 * persisting.
 */
public final class TrackersLensHandler {

    /**
     * Sentinel tick count meaning "this target was already marked during the current unbroken
     * sighting". Holding the lens on a creature past the mark would otherwise refresh the glow
     * every tick and make it permanent; a second mark costs the player a fresh sighting, which
     * means looking away and back.
     */
    private static final int ALREADY_MARKED = -1;

    /** Each scoping player's current target and how long they have held it. */
    private static final Map<UUID, ScopeState> SCOPES = new ConcurrentHashMap<>();

    private record ScopeState(UUID targetId, int ticks) {}

    private TrackersLensHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TrackersLensHandler::onServerTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SCOPES.remove(handler.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SCOPES.clear());
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Cheap bail-outs stay outside the guard: a server whose players are not scoping a
            // spyglass pays one isUsingItem() check per player per tick and nothing else.
            ItemStack using = player.isUsingItem() ? player.getUseItem() : ItemStack.EMPTY;
            if (!using.is(Items.SPYGLASS)) {
                if (!SCOPES.isEmpty()) SCOPES.remove(player.getUUID());
                continue;
            }
            EffectGuard.run("trackers_lens", player, () -> tickScope(player, using));
        }
    }

    /**
     * Advances one player's sighting by a tick and marks the target once it has been held long
     * enough. Takes the spyglass explicitly rather than reading {@code player.getUseItem()} so the
     * gametests can drive a sighting without faking item-use state.
     */
    public static void tickScope(ServerPlayer player, ItemStack spyglass) {
        int level = EnchantmentEffects.getEnchantmentLevel(spyglass, EnchantmentEffects.TRACKERS_LENS);
        if (level <= 0) {
            SCOPES.remove(player.getUUID());
            return;
        }

        LivingEntity target = acquireTarget(player);
        if (target == null) {
            SCOPES.remove(player.getUUID());
            return;
        }

        ScopeState state = SCOPES.get(player.getUUID());
        boolean sameTarget = state != null && state.targetId().equals(target.getUUID());
        if (sameTarget && state.ticks() == ALREADY_MARKED) return;

        int held = sameTarget ? state.ticks() + 1 : 1;
        if (held < SpyglassEnchantMath.TRACKERS_LENS_SIGHTING_TICKS) {
            SCOPES.put(player.getUUID(), new ScopeState(target.getUUID(), held));
            return;
        }

        // ambient=true, no particles, no icon: an unobtrusive tracking glow, not a status debuff.
        // Mirrors Mark, so the two enchantments read identically in-world.
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                SpyglassEnchantMath.trackersLensGlowTicks(level), 0, true, false, false));
        SCOPES.put(player.getUUID(), new ScopeState(target.getUUID(), ALREADY_MARKED));
    }

    /**
     * Resolves the creature centred in the player's spyglass. The sight line is clipped against
     * blocks first, so a creature behind terrain can never be acquired — mirroring Seeker's
     * lock-on, which is the established shape for "the thing under the crosshair, honestly".
     */
    private static LivingEntity acquireTarget(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getViewVector(1.0f).scale(SpyglassEnchantMath.TRACKERS_LENS_RANGE));
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
    }
}
