package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.RangedEnchantMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class ProjectileEnchantmentHandler {

    /** Entity types Harpoon must never drag — bosses and similar anchored fights. */
    private static final TagKey<EntityType<?>> HARPOON_IMMUNE =
            TagKey.create(Registries.ENTITY_TYPE, Meridian.id("harpoon_immune"));

    private static final Map<AbstractArrow, Integer> bouncesRemaining = Collections.synchronizedMap(new WeakHashMap<>());

    /** Launch snapshot for Longshot: where the arrow started and its unboosted damage. */
    private record LongshotLaunch(Vec3 origin, double baseDamage) {}

    private static final Map<AbstractArrow, LongshotLaunch> longshotLaunches =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Seeker's fire-time lock. The value is a holder so "computed, nothing under the
     * crosshair" is remembered as a null target and never re-acquired mid-flight.
     * With Multishot every bolt of the volley locks the same crosshair creature and
     * converges on it — intended: the lock is defined by the crosshair, not the bolt.
     */
    private record SeekerLock(LivingEntity target) {}

    private static final Map<AbstractArrow, SeekerLock> seekerLocks =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ProjectileEnchantmentHandler() {}

    public static void handleTick(AbstractArrow arrow) {
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) return;

        // Single pass over the weapon's enchantments — this runs every tick for every
        // arrow in flight, so the levels are extracted together instead of one scan
        // per enchantment.
        int trueFlightLevel = 0;
        int longshotLevel = 0;
        int seekerLevel = 0;
        for (var entry : weapon.getEnchantments().entrySet()) {
            if (entry.getKey().is(EnchantmentEffects.TRUE_FLIGHT)) {
                trueFlightLevel = entry.getIntValue();
            } else if (entry.getKey().is(EnchantmentEffects.LONGSHOT)) {
                longshotLevel = entry.getIntValue();
            } else if (entry.getKey().is(EnchantmentEffects.SEEKER)) {
                seekerLevel = entry.getIntValue();
            }
        }

        if (trueFlightLevel > 0) {
            arrow.setNoGravity(true);
        }
        if (!arrow.level().isClientSide()) {
            handleLongshot(arrow, longshotLevel);
            handleSeeker(arrow, seekerLevel);
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
        handleHarpoon(arrow, weapon, hit);
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

    private static void handleLongshot(AbstractArrow arrow, int level) {
        if (level <= 0) return;

        // The launch snapshot is in-memory only. An arrow that is saved mid-flight and
        // resumes later re-baselines from its current (possibly boosted) damage and
        // position — accepted: arrows land within moments, and the overshoot is bounded
        // by one extra ramp per reload.
        LongshotLaunch launch = longshotLaunches.computeIfAbsent(arrow,
                a -> new LongshotLaunch(a.position(), a.getBaseDamage()));
        double distance = arrow.position().distanceTo(launch.origin());
        arrow.setBaseDamage(launch.baseDamage() * RangedEnchantMath.longshotMultiplier(level, distance));
    }

    private static void handleSeeker(AbstractArrow arrow, int level) {
        if (level <= 0) return;

        SeekerLock lock = seekerLocks.computeIfAbsent(arrow,
                a -> new SeekerLock(acquireSeekerTarget(a)));
        LivingEntity target = lock.target();
        if (target == null || !target.isAlive() || target.isRemoved()) return;

        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        // A stuck or spent bolt has no meaningful velocity left; steering it would
        // just make it wiggle in place.
        if (speed < 0.1) return;

        Vec3 current = velocity.scale(1.0 / speed);
        Vec3 desired = target.getBoundingBox().getCenter().subtract(arrow.position()).normalize();
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, current.dot(desired))));
        if (angle < 1.0e-4) return;

        double maxTurn = RangedEnchantMath.seekerTurnRadians(level);
        Vec3 newDirection = angle <= maxTurn
                ? desired
                : current.lerp(desired, maxTurn / angle);
        if (newDirection.lengthSqr() < 1.0e-7) return;

        arrow.setDeltaMovement(newDirection.normalize().scale(speed));
        arrow.hasImpulse = true;
    }

    /**
     * Resolves the creature under the shooter's crosshair at fire time — the one and
     * only target acquisition Seeker ever performs for a bolt. The sight line is
     * clipped against blocks first so a creature behind a wall can never be locked.
     * (The lock itself is in-memory only; a bolt saved mid-flight forgets it, which is
     * accepted — bolts land within moments.)
     */
    private static LivingEntity acquireSeekerTarget(AbstractArrow arrow) {
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) return null;

        Vec3 start = shooter.getEyePosition();
        Vec3 end = start.add(shooter.getViewVector(1.0f).scale(RangedEnchantMath.SEEKER_LOCK_RANGE));
        BlockHitResult wall = shooter.level().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }

        Vec3 reach = end.subtract(start);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(shooter.level(), shooter,
                start, end,
                shooter.getBoundingBox().expandTowards(reach).inflate(1.0),
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator()
                        && e != arrow && seekerTargetAllowed(e));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /**
     * Whether Seeker may lock onto this entity: mobs always, players only when
     * {@code combat.seekerTargetsPlayers} is enabled.
     */
    public static boolean seekerTargetAllowed(Entity target) {
        if (!(target instanceof Player)) return true;
        MeridianConfig config = Meridian.getConfig();
        return config != null && config.combat.seekerTargetsPlayers;
    }

    /**
     * Whether Harpoon may drag this victim: mobs always, players only when
     * {@code combat.harpoonAffectsPlayers} is enabled.
     */
    public static boolean harpoonVictimAllowed(LivingEntity victim) {
        if (!(victim instanceof Player)) return true;
        MeridianConfig config = Meridian.getConfig();
        return config != null && config.combat.harpoonAffectsPlayers;
    }

    private static void handleHarpoon(AbstractArrow arrow, ItemStack weapon, EntityHitResult hit) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.HARPOON);
        if (level <= 0) return;
        if (!(arrow.getOwner() instanceof LivingEntity owner)) return;
        if (!(hit.getEntity() instanceof LivingEntity victim)) return;
        if (victim == owner || victim.getType().is(HARPOON_IMMUNE)) return;
        if (!harpoonVictimAllowed(victim)) return;

        Vec3 toOwner = owner.position().subtract(victim.position());
        double distance = toOwner.length();
        if (distance < 1.0e-3) return;

        double pullSpeed = RangedEnchantMath.harpoonPullSpeed(level, distance);
        Vec3 pull = toOwner.scale(pullSpeed / distance);
        victim.push(pull.x, pull.y + RangedEnchantMath.HARPOON_LIFT, pull.z);
        if (victim instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.hurtMarked = true;
        }

        arrow.level().playSound(null, BlockPos.containing(victim.position()),
                SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0f, 0.7f);
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
