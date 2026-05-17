package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.WeakHashMap;

public final class MountedEnchantmentHandler {

    private static final WeakHashMap<Entity, Long> trampleCooldowns = new WeakHashMap<>();
    private static final int TRAMPLE_COOLDOWN_TICKS = 10;

    private MountedEnchantmentHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MountedEnchantmentHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof AbstractHorse horse)) continue;
                if (!horse.isVehicle()) continue;

                handleTrample(horse);
            }
        }
    }

    private static void handleTrample(AbstractHorse horse) {
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
}
