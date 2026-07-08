package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Runtime home for the Decoy enchantment's summoned body. {@link EnchantmentEffectHandler}
 * decides <em>when</em> a decoy deploys (the half-health crossing and cooldown live there); this
 * manager owns the decoy entity itself — spawning it, taunting nearby hostiles onto it each
 * server tick, and discarding it when its lifetime elapses or it is destroyed.
 *
 * <p>The decoy is a plain {@link ArmorStand}: mob melee damages and eventually breaks it (the
 * "low health" body), while the {@link DefenseEnchantMath#DECOY_LIFETIME_TICKS} timer is the hard
 * cap so it never lingers. Taunting is a pure AI nudge — hostiles in range have their target set
 * to the decoy; nothing else about them changes, and they reacquire normally once it is gone.
 *
 * <p>An ArmorStand is a persistent entity, so a hard crash mid-lifetime could otherwise strand one
 * in the world with no in-memory tracker left to remove it. Every decoy carries the
 * {@link #DECOY_ENTITY_TAG} scoreboard tag; when a stranded decoy from a previous session loads,
 * {@link #onEntityLoad} discards it. A decoy spawned this session is registered in
 * {@link #ACTIVE_DECOYS} <em>before</em> it enters the world, so its own load event finds it
 * tracked and leaves it alone.
 */
public final class DecoyManager {

    /** Scoreboard tag marking an entity as a Decoy body, for cross-session orphan cleanup. */
    private static final String DECOY_ENTITY_TAG = "meridian_decoy";

    private record ActiveDecoy(ArmorStand decoy, ServerLevel level, long expiryTick) {}

    private static final List<ActiveDecoy> ACTIVE_DECOYS = Collections.synchronizedList(new ArrayList<>());

    private DecoyManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(DecoyManager::onServerTick);
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> onEntityLoad(entity));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> discardAll());
    }

    /**
     * Spawns a decoy at {@code owner}'s position and begins tracking it. Called on the server
     * thread from the damage handler; the returned decoy is already added to the world.
     */
    public static void deploy(ServerLevel level, LivingEntity owner) {
        ArmorStand decoy = EntityType.ARMOR_STAND.create(level);
        if (decoy == null) return;

        decoy.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0f);
        decoy.setCustomName(Component.translatable("enchantment.meridian.decoy"));
        decoy.setCustomNameVisible(true);
        decoy.addTag(DECOY_ENTITY_TAG);

        // Track before the entity enters the world, so the ENTITY_LOAD fired by addFreshEntity
        // sees this decoy as a live, tracked body rather than a stranded orphan to discard.
        long expiry = level.getGameTime() + DefenseEnchantMath.DECOY_LIFETIME_TICKS;
        ACTIVE_DECOYS.add(new ActiveDecoy(decoy, level, expiry));

        level.addFreshEntity(decoy);
        taunt(decoy, level);
    }

    private static void onServerTick(MinecraftServer server) {
        if (ACTIVE_DECOYS.isEmpty()) return;

        synchronized (ACTIVE_DECOYS) {
            Iterator<ActiveDecoy> it = ACTIVE_DECOYS.iterator();
            while (it.hasNext()) {
                ActiveDecoy active = it.next();
                ArmorStand decoy = active.decoy();

                if (!decoy.isAlive() || decoy.isRemoved() || active.level().getGameTime() >= active.expiryTick()) {
                    decoy.discard();
                    it.remove();
                    continue;
                }

                EffectGuard.run("decoy_taunt", decoy, () -> taunt(decoy, active.level()));
            }
        }
    }

    /** Points every hostile within taunt range at the decoy, leaving already-taunted mobs alone. */
    private static void taunt(ArmorStand decoy, ServerLevel level) {
        AABB area = decoy.getBoundingBox().inflate(DefenseEnchantMath.DECOY_TAUNT_RADIUS);
        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class, area, Monster::isAlive);
        for (Monster mob : hostiles) {
            if (mob.getTarget() != decoy) {
                mob.setTarget(decoy);
            }
        }
    }

    /** Discards a tagged decoy that loaded without a live tracker — an orphan from a crashed session. */
    private static void onEntityLoad(Entity entity) {
        // Cheap filter outside the guard: this hook fires for every entity that loads anywhere on
        // the server, and only a stranded tagged decoy is ours to discard.
        if (!(entity instanceof ArmorStand stand)) return;
        if (!stand.getTags().contains(DECOY_ENTITY_TAG) || isTracked(stand)) return;
        EffectGuard.run("decoy_orphan_cleanup", stand, stand::discard);
    }

    private static boolean isTracked(ArmorStand stand) {
        synchronized (ACTIVE_DECOYS) {
            for (ActiveDecoy active : ACTIVE_DECOYS) {
                if (active.decoy() == stand) return true;
            }
        }
        return false;
    }

    private static void discardAll() {
        synchronized (ACTIVE_DECOYS) {
            for (ActiveDecoy active : ACTIVE_DECOYS) {
                active.decoy().discard();
            }
            ACTIVE_DECOYS.clear();
        }
    }

    // Test support: lets DecoyGameTest assert deployment without reaching into private state.
    public static int activeDecoyCountForTest() {
        return ACTIVE_DECOYS.size();
    }

    public static void clearForTest() {
        discardAll();
    }
}
