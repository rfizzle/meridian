package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.mixin.CreeperEntityAccessor;
import com.rfizzle.meridian.mixin.LivingEntityLootInvoker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.WeakHashMap;

public final class EnchantmentEffectHandler {

    private static final WeakHashMap<LivingEntity, Long> rallyCooldowns = new WeakHashMap<>();
    private static final int RALLY_COOLDOWN_TICKS = 6000;

    private static final WeakHashMap<LivingEntity, Long> abyssWardCooldowns = new WeakHashMap<>();
    private static final int ABYSS_WARD_COOLDOWN_TICKS = 12000;

    private static boolean applyingBloodrage = false;
    private static boolean applyingFinalGambit = false;
    private static boolean applyingSoulTax = false;
    private static boolean applyingCleave = false;
    private static boolean applyingPummel = false;

    private EnchantmentEffectHandler() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(EnchantmentEffectHandler::onAllowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(EnchantmentEffectHandler::onAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(EnchantmentEffectHandler::onAfterDeath);
    }

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity.level().isClientSide()) return true;
        if (!source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) return true;

        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.ABYSS_WARD, EquipmentSlot.HEAD);
        if (level <= 0) return true;

        long currentTick = entity.level().getGameTime();
        Long lastUsed = abyssWardCooldowns.get(entity);
        if (lastUsed != null && (currentTick - lastUsed) < ABYSS_WARD_COOLDOWN_TICKS) return true;

        abyssWardCooldowns.put(entity, currentTick);
        entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 2, true, true, true));
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.5, 0));
        if (entity instanceof ServerPlayer sp) {
            sp.hurtMarked = true;
        }
        return false;
    }

    private static void onAfterDamage(LivingEntity entity, DamageSource source,
                                       float baseDamageTaken, float damageTaken, boolean blocked) {
        if (entity.level().isClientSide()) return;

        if (blocked) {
            handleRetribution(entity, source, baseDamageTaken);
        }

        if (damageTaken <= 0 && !blocked) return;

        handleQuell(entity, source);

        if (!applyingFinalGambit) {
            handleFinalGambit(entity, source);
        }

        handleSiphon(entity, source);
        if (!applyingSoulTax) {
            handleSoulTax(entity, source);
        }
        if (!applyingCleave) {
            handleCleave(entity, source);
        }
        if (!applyingPummel) {
            handlePummel(entity, source);
        }
        handleMaceSlam(entity, source);
        handleRepulse(entity, source);
        handleFrostguard(entity, source);
        handleRally(entity);

        if (!applyingBloodrage) {
            handleBloodrage(entity);
        }
    }

    private static void handleQuell(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Creeper creeper)) return;

        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingAttacker.getMainHandItem(), EnchantmentEffects.QUELL);
        if (level <= 0) return;

        ((CreeperEntityAccessor) creeper).meridian$setSwell(0);
        ((CreeperEntityAccessor) creeper).meridian$setOldSwell(0);
        creeper.setSwellDir(-1);
    }

    private static void handleFinalGambit(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;
        if (!livingAttacker.isShiftKeyDown()) return;

        ItemStack weapon = livingAttacker.getMainHandItem();
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.FINAL_GAMBIT);
        if (level <= 0) return;

        float bonusDamage = weapon.getMaxDamage() * 0.15f;
        weapon.setCount(0);

        applyingFinalGambit = true;
        try {
            entity.hurt(livingAttacker.damageSources().mobAttack(livingAttacker), bonusDamage);
        } finally {
            applyingFinalGambit = false;
        }
    }

    private static void handleSiphon(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingAttacker.getMainHandItem(), EnchantmentEffects.SIPHON);
        if (level <= 0) return;

        float chance = 0.20f + 0.10f * level;
        if (livingAttacker.getRandom().nextFloat() >= chance) return;

        float healAmount = 2.0f * level;
        livingAttacker.heal(healAmount);
    }

    private static void handleSoulTax(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(player.getMainHandItem(), EnchantmentEffects.SOUL_TAX);
        if (level <= 0) return;

        int xpCost = 3 + 2 * level;
        if (player.totalExperience < xpCost) return;

        player.giveExperiencePoints(-xpCost);

        float bonusDamage = 2.0f + 1.5f * level;
        applyingSoulTax = true;
        try {
            entity.hurt(player.damageSources().mobAttack(player), bonusDamage);
        } finally {
            applyingSoulTax = false;
        }
    }

    private static void handleCleave(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingAttacker.getMainHandItem(), EnchantmentEffects.CLEAVE);
        if (level <= 0) return;

        double range = 1.5 + 0.5 * level;
        float damage = 2.0f + 1.0f * level;

        AABB area = entity.getBoundingBox().inflate(range);
        List<LivingEntity> nearby = entity.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != entity && e != livingAttacker && e.isAlive());

        applyingCleave = true;
        try {
            for (LivingEntity target : nearby) {
                target.hurt(livingAttacker.damageSources().mobAttack(livingAttacker), damage);
            }
        } finally {
            applyingCleave = false;
        }
    }

    private static void handleRepulse(LivingEntity entity, DamageSource source) {
        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.REPULSE,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS);
        if (level <= 0) return;

        Entity attacker = source.getDirectEntity();
        if (attacker == null) return;

        double distSq = attacker.distanceToSqr(entity);
        if (distSq > 16.0) return;

        Vec3 knockback = attacker.position().subtract(entity.position()).normalize();
        double force = 0.6 + 0.3 * level;
        attacker.push(knockback.x * force, 0.2 + 0.05 * level, knockback.z * force);

        if (attacker instanceof ServerPlayer sp) {
            sp.hurtMarked = true;
        }
    }

    private static void handleFrostguard(LivingEntity entity, DamageSource source) {
        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.FROSTGUARD,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;

        Entity attacker = source.getDirectEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        double distSq = attacker.distanceToSqr(entity);
        if (distSq > 16.0) return;

        int duration = 40 + 20 * level;
        livingAttacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, level - 1));
    }

    private static void handleRally(LivingEntity entity) {
        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.RALLY, EquipmentSlot.CHEST);
        if (level <= 0) return;

        float threshold = entity.getMaxHealth() * 0.2f;
        if (entity.getHealth() > threshold) return;

        long currentTick = entity.level().getGameTime();
        Long lastActivation = rallyCooldowns.get(entity);
        if (lastActivation != null && (currentTick - lastActivation) < RALLY_COOLDOWN_TICKS) return;

        rallyCooldowns.put(entity, currentTick);

        int duration = 60 + 40 * level;
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, true, true, true));
    }

    private static void handleBloodrage(LivingEntity entity) {
        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.BLOODRAGE,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;

        int buffDuration = 80 + 40 * level;
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, buffDuration, 0, true, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, buffDuration, level - 1, true, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, buffDuration, 0, true, false, true));

        float hpCost = 1.5f + 0.5f * level;
        applyingBloodrage = true;
        try {
            entity.hurt(entity.damageSources().magic(), hpCost);
        } finally {
            applyingBloodrage = false;
        }
    }

    private static void handleMaceSlam(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(Items.MACE)) return;
        if (player.fallDistance < 1.5f) return;

        handleTempest(entity, player, weapon);
        handleSeismicSlam(entity, player, weapon);
        handleUpdraft(player, weapon);
    }

    private static void handleTempest(LivingEntity entity, ServerPlayer player, ItemStack weapon) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.TEMPEST);
        if (level <= 0) return;

        ServerLevel serverLevel = player.serverLevel();
        if (!serverLevel.isThundering()) return;

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning == null) return;

        lightning.moveTo(entity.getX(), entity.getY(), entity.getZ());
        lightning.setCause(player);
        serverLevel.addFreshEntity(lightning);

        int immunityTicks = 40 + 20 * level;
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, immunityTicks, 0));
    }

    private static void handleSeismicSlam(LivingEntity entity, ServerPlayer player, ItemStack weapon) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.SEISMIC_SLAM);
        if (level <= 0) return;
        if (!player.isShiftKeyDown()) return;

        Vec3 pos = entity.position();
        double radius = 4.0;
        float damage = 6.0f;
        double force = 1.2;

        AABB area = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius);
        List<LivingEntity> nearby = entity.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e != entity && e.isAlive());

        for (LivingEntity target : nearby) {
            target.hurt(player.damageSources().mobAttack(player), damage);
            Vec3 knockDir = target.position().subtract(pos).normalize();
            target.push(knockDir.x * force, 0.4, knockDir.z * force);
            if (target instanceof ServerPlayer sp) {
                sp.hurtMarked = true;
            }
        }

        entity.level().playSound(null, BlockPos.containing(pos),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.7f);
    }

    private static void handleUpdraft(ServerPlayer player, ItemStack weapon) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.UPDRAFT);
        if (level <= 0) return;

        double launchVelocity = 0.8 + 0.4 * level;
        player.push(0, launchVelocity, 0);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    private static void handleRetribution(LivingEntity entity, DamageSource source,
                                              float blockedAmount) {
        ItemStack useItem = entity.getUseItem();
        int level = EnchantmentEffects.getEnchantmentLevel(useItem, EnchantmentEffects.RETRIBUTION);
        if (level <= 0) return;

        Entity attacker = source.getDirectEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        float procChance = 0.20f + 0.08f * level;
        if (entity.getRandom().nextFloat() >= procChance) return;

        float reflectRatio = 0.10f + 0.10f * level;
        float reflectDamage = blockedAmount * reflectRatio;

        if (reflectDamage > 0) {
            livingAttacker.hurt(entity.damageSources().thorns(entity), reflectDamage);
        }
    }

    private static void handlePummel(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        ItemStack offhand = livingAttacker.getOffhandItem();
        int level = EnchantmentEffects.getEnchantmentLevel(offhand, EnchantmentEffects.PUMMEL);
        if (level <= 0) return;

        float bonusDamage = 1.5f + 1.0f * level;
        offhand.hurtAndBreak(2, livingAttacker, EquipmentSlot.OFFHAND);

        applyingPummel = true;
        try {
            entity.hurt(livingAttacker.damageSources().mobAttack(livingAttacker), bonusDamage);
        } finally {
            applyingPummel = false;
        }
    }

    private static void onAfterDeath(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide()) return;
        handlePlunder(entity, source);
        handleSnare(entity, source);
    }

    private static void handlePlunder(LivingEntity entity, DamageSource source) {
        Entity killer = source.getEntity();
        if (!(killer instanceof LivingEntity livingKiller)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingKiller.getMainHandItem(), EnchantmentEffects.PLUNDER);
        if (level <= 0) return;

        float chance = 0.025f * level;
        if (entity.getRandom().nextFloat() >= chance) return;

        boolean hitByPlayer = entity.getKillCredit() != null;
        ((LivingEntityLootInvoker) entity).meridian$invokeDropFromLootTable(source, hitByPlayer);
    }

    private static void handleSnare(LivingEntity entity, DamageSource source) {
        Entity killer = source.getEntity();
        if (!(killer instanceof LivingEntity livingKiller)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingKiller.getMainHandItem(), EnchantmentEffects.SNARE);
        if (level <= 0) return;

        float chance = 0.05f;
        if (entity.getRandom().nextFloat() >= chance) return;

        SpawnEggItem egg = SpawnEggItem.byId(entity.getType());
        if (egg == null) return;

        entity.spawnAtLocation(new ItemStack(egg));
    }
}
