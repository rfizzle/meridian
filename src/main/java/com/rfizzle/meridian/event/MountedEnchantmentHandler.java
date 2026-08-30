package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.EnduranceHealMath;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class MountedEnchantmentHandler {

    private static final Map<Entity, Long> trampleCooldowns = Collections.synchronizedMap(new WeakHashMap<>());
    private static final int TRAMPLE_COOLDOWN_TICKS = 10;

    /** Curse of Skittishness: horizontal impulse of the mount's panic bolt (blocks/tick). */
    private static final double SKITTISH_BOLT_SPEED = 0.6;
    /** Curse of Skittishness: chance a spook also throws the rider. */
    private static final float SKITTISH_BUCK_CHANCE = 0.15f;

    private MountedEnchantmentHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MountedEnchantmentHandler::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> trampleCooldowns.clear());
    }

    /**
     * Trample only fires for a horse a player is actively riding, so the scan walks the (small)
     * player list and inspects each player's vehicle rather than every entity in every level —
     * the per-tick cost scales with online players, not world population.
     */
    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getVehicle() instanceof AbstractHorse horse) {
                EffectGuard.run("trample", horse, () -> handleTrample(horse));
            }
        }
    }

    // Package-private so the per-tick trample effect can be driven directly in a gametest.
    public static void handleTrample(AbstractHorse horse) {
        int level = EnchantmentEffects.getEquippedLevel(horse, EnchantmentEffects.TRAMPLE, EquipmentSlot.BODY);
        if (level <= 0) return;

        Vec3 velocity = horse.getDeltaMovement();
        double speed = velocity.horizontalDistance();
        if (speed < 0.1) return;

        long currentTick = horse.level().getGameTime();
        Long lastTrample = trampleCooldowns.get(horse);
        if (lastTrample != null && (currentTick - lastTrample) < TRAMPLE_COOLDOWN_TICKS) return;

        AABB area = horse.getBoundingBox().inflate(0.3);
        Entity rider = horse.getControllingPassenger();
        List<LivingEntity> targets = horse.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != horse && e != rider && e.isAlive() && !horse.hasPassenger(e));

        if (targets.isEmpty()) return;

        trampleCooldowns.put(horse, currentTick);
        float damage = 2.0f + 1.5f * level;

        for (LivingEntity target : targets) {
            if (rider instanceof LivingEntity livingRider) {
                target.hurt(livingRider.damageSources().mobAttack(livingRider), damage);
            } else {
                target.hurt(horse.damageSources().mobAttack(horse), damage);
            }
        }
    }

    /**
     * A single Endurance heal pulse: a mount wearing Endurance on its body slot that is below
     * full health recovers {@link EnduranceHealMath#healPerPulse(int)}. Endurance heals whether
     * or not the mount is ridden, so a wounded horse recovers even while stabled.
     *
     * <p>Public and side-effect-scoped to the heal itself so the pulse can be driven directly in
     * a gametest; {@code EnduranceMixin} throttles it to one call every
     * {@link EnduranceHealMath#PULSE_INTERVAL_TICKS} on the server side.
     */
    public static void handleEndurance(AbstractHorse horse) {
        int level = EnchantmentEffects.getEquippedLevel(horse, EnchantmentEffects.ENDURANCE, EquipmentSlot.BODY);
        if (level <= 0) return;
        if (horse.getHealth() >= horse.getMaxHealth()) return;
        horse.heal(EnduranceHealMath.healPerPulse(level));
    }

    /**
     * Curse of Skittishness: a mount wearing the curse on its body panics when it takes damage —
     * lurching a short distance in a random direction and, on a small chance, bucking its rider.
     * Public and side-effect-scoped to the spook itself so a gametest can drive it directly;
     * {@code EnchantmentEffectHandler}'s {@code AFTER_DAMAGE} dispatch calls it whenever a horse
     * takes a hit.
     */
    public static void handleSkittishness(AbstractHorse horse) {
        int level = EnchantmentEffects.getEquippedLevel(horse, EnchantmentEffects.CURSE_OF_SKITTISHNESS, EquipmentSlot.BODY);
        if (level <= 0) return;

        // Bolt: a horizontal impulse in a random direction, so the mount lurches off from where it
        // stood. hurtMarked forces the velocity change out to the client so the mount visibly bolts.
        float angle = horse.getRandom().nextFloat() * (float) (Math.PI * 2.0);
        horse.setDeltaMovement(horse.getDeltaMovement().add(
                Math.cos(angle) * SKITTISH_BOLT_SPEED, 0.0, Math.sin(angle) * SKITTISH_BOLT_SPEED));
        horse.hurtMarked = true;

        if (horse.getRandom().nextFloat() < SKITTISH_BUCK_CHANCE) {
            horse.ejectPassengers();
        }
    }
}
