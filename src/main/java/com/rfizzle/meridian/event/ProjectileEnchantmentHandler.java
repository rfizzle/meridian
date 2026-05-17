package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.WeakHashMap;

public final class ProjectileEnchantmentHandler {

    private static final WeakHashMap<AbstractArrow, Integer> bouncesRemaining = new WeakHashMap<>();

    private ProjectileEnchantmentHandler() {}

    public static void handleTick(AbstractArrow arrow) {
        ItemStack weapon = arrow.getWeaponItem();
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.TRUE_FLIGHT);
        if (level > 0) {
            arrow.setNoGravity(true);
        }
    }

    public static void handleEntityImpact(AbstractArrow arrow, EntityHitResult hit) {
        if (arrow.level().isClientSide()) return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;

        Vec3 pos = hit.getEntity().position();
        Entity owner = arrow.getOwner();

        handleGaleShot(arrow, weapon, pos, owner);
        handleResonance(arrow, weapon, pos, owner);
        handlePermafrost(arrow, weapon, pos);
        handleDetonation(arrow, weapon, pos, owner);
        handleStormcall(arrow, weapon, pos);
        handleGlacialLance(arrow, weapon, pos);
    }

    public static boolean handleBlockImpact(AbstractArrow arrow, BlockHitResult hit) {
        if (arrow.level().isClientSide()) return false;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return false;

        if (handleRicochet(arrow, weapon, hit)) {
            return true;
        }

        Vec3 pos = hit.getLocation();
        Entity owner = arrow.getOwner();
        handleGaleShot(arrow, weapon, pos, owner);
        handleResonance(arrow, weapon, pos, owner);
        handlePermafrost(arrow, weapon, pos);
        handleDetonation(arrow, weapon, pos, owner);
        handleStormcall(arrow, weapon, pos);
        handleGlacialLance(arrow, weapon, pos);

        return false;
    }

    private static void handleGaleShot(AbstractArrow arrow, ItemStack weapon, Vec3 pos, Entity owner) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.GALE_SHOT);
        if (level <= 0) return;

        Level world = arrow.level();
        double radius = 3.0 + level;
        double force = 0.8 + 0.4 * level;

        AABB area = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius);
        List<Entity> nearby = world.getEntities(arrow, area, e -> e != owner && e.isAlive());

        for (Entity target : nearby) {
            Vec3 knockDir = target.position().subtract(pos).normalize();
            target.push(knockDir.x * force, 0.3 * force, knockDir.z * force);
            if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
                sp.hurtMarked = true;
            }
        }

        world.playSound(null, BlockPos.containing(pos), SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private static void handleResonance(AbstractArrow arrow, ItemStack weapon, Vec3 pos, Entity owner) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.RESONANCE);
        if (level <= 0) return;

        Level world = arrow.level();
        double radius = 3.0 + level;
        float damage = 4.0f + 2.0f * level;

        AABB area = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != owner && e.isAlive());

        DamageSource source = owner instanceof LivingEntity livingOwner
                ? world.damageSources().sonicBoom(livingOwner)
                : world.damageSources().sonicBoom(arrow);

        for (LivingEntity target : nearby) {
            target.hurt(source, damage);
        }

        world.playSound(null, BlockPos.containing(pos), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 0.6f, 1.2f);
    }

    private static void handlePermafrost(AbstractArrow arrow, ItemStack weapon, Vec3 pos) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.PERMAFROST);
        if (level <= 0) return;

        Level world = arrow.level();
        int radius = 2;
        BlockPos center = BlockPos.containing(pos);

        for (BlockPos bp : BlockPos.betweenClosed(center.offset(-radius, -1, -radius),
                center.offset(radius, 1, radius))) {
            BlockState state = world.getBlockState(bp);
            if (state.is(Blocks.WATER)) {
                world.setBlockAndUpdate(bp, Blocks.FROSTED_ICE.defaultBlockState());
            }
        }

        double slowRadius = 4.0;
        AABB area = new AABB(pos.x - slowRadius, pos.y - slowRadius, pos.z - slowRadius,
                pos.x + slowRadius, pos.y + slowRadius, pos.z + slowRadius);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != arrow.getOwner() && e.isAlive());

        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        world.playSound(null, center, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    private static void handleDetonation(AbstractArrow arrow, ItemStack weapon, Vec3 pos, Entity owner) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.DETONATION);
        if (level <= 0) return;

        float radius = 1.5f + 0.75f * level;

        arrow.level().explode(owner, pos.x, pos.y, pos.z, radius,
                Level.ExplosionInteraction.NONE);
    }

    private static void handleStormcall(AbstractArrow arrow, ItemStack weapon, Vec3 pos) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.STORMCALL);
        if (level <= 0) return;

        if (!(arrow.level() instanceof ServerLevel serverLevel)) return;

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning == null) return;

        lightning.moveTo(pos.x, pos.y, pos.z);
        lightning.setCause(arrow.getOwner() instanceof net.minecraft.server.level.ServerPlayer sp ? sp : null);
        serverLevel.addFreshEntity(lightning);
    }

    private static void handleGlacialLance(AbstractArrow arrow, ItemStack weapon, Vec3 pos) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.GLACIAL_LANCE);
        if (level <= 0) return;

        Level world = arrow.level();
        int radius = 1 + level;
        BlockPos center = BlockPos.containing(pos);

        for (BlockPos bp : BlockPos.betweenClosed(center.offset(-radius, -1, -radius),
                center.offset(radius, 1, radius))) {
            BlockState state = world.getBlockState(bp);
            if (state.is(Blocks.WATER)) {
                world.setBlockAndUpdate(bp, Blocks.FROSTED_ICE.defaultBlockState());
            }
        }

        double slowRadius = 3.0 + level;
        AABB area = new AABB(pos.x - slowRadius, pos.y - slowRadius, pos.z - slowRadius,
                pos.x + slowRadius, pos.y + slowRadius, pos.z + slowRadius);
        List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != arrow.getOwner() && e.isAlive());

        int slowDuration = 40 + 20 * level;
        for (LivingEntity target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, slowDuration, level - 1));
        }

        world.playSound(null, center, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.8f);
    }

    private static boolean handleRicochet(AbstractArrow arrow, ItemStack weapon, BlockHitResult hit) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.RICOCHET);
        if (level <= 0) return false;

        int bounces = bouncesRemaining.computeIfAbsent(arrow, a -> level);
        if (bounces <= 0) return false;

        bouncesRemaining.put(arrow, bounces - 1);

        Vec3 velocity = arrow.getDeltaMovement();
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());

        double dot = velocity.dot(normal);
        Vec3 reflected = velocity.subtract(normal.scale(2.0 * dot));

        double damping = 0.85;
        arrow.setDeltaMovement(reflected.scale(damping));
        arrow.setOnGround(false);
        arrow.hasImpulse = true;

        arrow.level().playSound(null, BlockPos.containing(hit.getLocation()),
                SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 0.5f, 1.4f);

        return true;
    }
}
