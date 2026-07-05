package com.rfizzle.meridian.event;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.attachment.MeridianAttachments;
import com.rfizzle.meridian.config.MeridianConfig;
import com.rfizzle.meridian.enchanting.CombatEnchantMath;
import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.mixin.CreeperEntityAccessor;
import com.rfizzle.meridian.mixin.LivingEntityCombatAccessor;
import com.rfizzle.meridian.mixin.LivingEntityLootInvoker;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class EnchantmentEffectHandler {

    private static final Map<LivingEntity, Long> rallyCooldowns = Collections.synchronizedMap(new WeakHashMap<>());
    private static final int RALLY_COOLDOWN_TICKS = 6000;

    private static final Map<LivingEntity, Long> abyssWardCooldowns = Collections.synchronizedMap(new WeakHashMap<>());
    private static final int ABYSS_WARD_COOLDOWN_TICKS = 12000;

    private static final Set<UUID> BLOODRAGE_PROCESSING = Collections.synchronizedSet(new HashSet<>());

    /**
     * Attackers currently landing a mod-originated bonus hit (Soul Tax, Cleave, Pummel,
     * Final Gambit, Ambush, Pinpoint). While an attacker is in this set the attacker-driven
     * on-hit handlers are skipped entirely, so a bonus hit can never re-proc an on-hit
     * effect — neither its own (recursion) nor a sibling's (double-proc cascades like
     * Pinpoint re-triggering Cleave's splash).
     */
    private static final Set<UUID> BONUS_HIT_PROCESSING = Collections.synchronizedSet(new HashSet<>());

    /**
     * Victim health snapshotted in {@code ALLOW_DAMAGE} for Ambush's opener scaling.
     * {@code AFTER_DAMAGE}'s {@code damageTaken} is pre-armor/-resistance, so reconstructing
     * pre-hit health from it would overestimate against mitigated targets and Ambush would
     * barely fade as an armored victim weakens.
     */
    private static final Map<LivingEntity, Float> AMBUSH_PRE_HIT_HEALTH =
            Collections.synchronizedMap(new WeakHashMap<>());

    // Per-effect balance tuning. Grouped here so a balance pass touches one block instead of
    // hunting inline literals across the handlers. Naming: BASE_* is the level-0 term and
    // *_PER_LEVEL the per-enchantment-level increment unless noted.
    private static final int ABYSS_WARD_LEVITATION_TICKS = 60;
    private static final int ABYSS_WARD_LEVITATION_AMPLIFIER = 2;
    private static final double ABYSS_WARD_UPWARD_BOOST = 0.5;

    private static final float FINAL_GAMBIT_DAMAGE_FRACTION = 0.15f;

    private static final float SIPHON_BASE_CHANCE = 0.20f;
    private static final float SIPHON_CHANCE_PER_LEVEL = 0.10f;
    private static final float SIPHON_HEAL_PER_LEVEL = 2.0f;

    private static final int SOUL_TAX_BASE_XP_COST = 3;
    private static final int SOUL_TAX_XP_COST_PER_LEVEL = 2;
    private static final float SOUL_TAX_BASE_DAMAGE = 2.0f;
    private static final float SOUL_TAX_DAMAGE_PER_LEVEL = 1.5f;

    private static final double CLEAVE_BASE_RANGE = 1.5;
    private static final double CLEAVE_RANGE_PER_LEVEL = 0.5;
    private static final float CLEAVE_BASE_DAMAGE = 2.0f;
    private static final float CLEAVE_DAMAGE_PER_LEVEL = 1.0f;

    private static final double REPULSE_RANGE_SQ = 16.0;
    private static final double REPULSE_BASE_FORCE = 0.6;
    private static final double REPULSE_FORCE_PER_LEVEL = 0.3;
    private static final double REPULSE_BASE_LIFT = 0.2;
    private static final double REPULSE_LIFT_PER_LEVEL = 0.05;

    private static final double FROSTGUARD_RANGE_SQ = 16.0;
    private static final int FROSTGUARD_BASE_SLOW_TICKS = 40;
    private static final int FROSTGUARD_SLOW_TICKS_PER_LEVEL = 20;

    private static final float RALLY_HEALTH_THRESHOLD = 0.2f;
    private static final int RALLY_BASE_REGEN_TICKS = 60;
    private static final int RALLY_REGEN_TICKS_PER_LEVEL = 40;

    private static final int BLOODRAGE_BASE_BUFF_TICKS = 80;
    private static final int BLOODRAGE_BUFF_TICKS_PER_LEVEL = 40;
    private static final float BLOODRAGE_BASE_HP_COST = 1.5f;
    private static final float BLOODRAGE_HP_COST_PER_LEVEL = 0.5f;

    private static final float MACE_SLAM_MIN_FALL_DISTANCE = 1.5f;

    private static final int TEMPEST_BASE_FIRE_RES_TICKS = 40;
    private static final int TEMPEST_FIRE_RES_TICKS_PER_LEVEL = 20;

    private static final double SEISMIC_SLAM_RADIUS = 4.0;
    private static final float SEISMIC_SLAM_DAMAGE = 6.0f;
    private static final double SEISMIC_SLAM_KNOCKBACK = 1.2;
    private static final double SEISMIC_SLAM_LIFT = 0.4;

    private static final double UPDRAFT_BASE_VELOCITY = 0.8;
    private static final double UPDRAFT_VELOCITY_PER_LEVEL = 0.4;

    private static final float RETRIBUTION_BASE_CHANCE = 0.20f;
    private static final float RETRIBUTION_CHANCE_PER_LEVEL = 0.08f;
    private static final float RETRIBUTION_BASE_REFLECT = 0.10f;
    private static final float RETRIBUTION_REFLECT_PER_LEVEL = 0.10f;

    private static final float PUMMEL_BASE_DAMAGE = 1.5f;
    private static final float PUMMEL_DAMAGE_PER_LEVEL = 1.0f;
    private static final int PUMMEL_DURABILITY_COST = 2;

    private static final float PLUNDER_CHANCE_PER_LEVEL = 0.025f;

    private static final float SNARE_CHANCE = 0.05f;

    // Ambush/Pinpoint/Sunder/Trophy/Fortuity tuning lives in CombatEnchantMath, and
    // Blink/Emberward/Reprieve tuning in DefenseEnchantMath, so the formulas stay
    // reachable from plain JUnit (this class drags in Fabric event types).

    /**
     * Mobs whose head Trophy can drop. Vanilla only ships head items for these types
     * (plus player heads, handled separately); Trophy's extension is making them drop
     * from ordinary kills instead of requiring a charged-creeper detonation.
     */
    private static final Map<EntityType<?>, Item> TROPHY_HEADS = Map.of(
            EntityType.ZOMBIE, Items.ZOMBIE_HEAD,
            EntityType.SKELETON, Items.SKELETON_SKULL,
            EntityType.WITHER_SKELETON, Items.WITHER_SKELETON_SKULL,
            EntityType.CREEPER, Items.CREEPER_HEAD,
            EntityType.PIGLIN, Items.PIGLIN_HEAD,
            EntityType.ENDER_DRAGON, Items.DRAGON_HEAD);

    private EnchantmentEffectHandler() {}

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(EnchantmentEffectHandler::onAllowDamage);
        ServerLivingEntityEvents.ALLOW_DEATH.register(EnchantmentEffectHandler::onAllowDeath);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(EnchantmentEffectHandler::onAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(EnchantmentEffectHandler::onAfterDeath);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            rallyCooldowns.clear();
            abyssWardCooldowns.clear();
            BLOODRAGE_PROCESSING.clear();
            BONUS_HIT_PROCESSING.clear();
            AMBUSH_PRE_HIT_HEALTH.clear();
        });
    }

    /**
     * Runs {@code action} with {@code id} held in {@code guard}, removing it afterward even if the
     * action throws. The guard breaks the recursion that arises when an effect's own
     * {@link LivingEntity#hurt} call re-enters the damage event for the same attacker.
     */
    private static void withReentrancyGuard(Set<UUID> guard, UUID id, Runnable action) {
        guard.add(id);
        try {
            action.run();
        } finally {
            guard.remove(id);
        }
    }

    private static boolean onAllowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (entity.level().isClientSide()) return true;
        snapshotAmbushHealth(entity, source);
        if (!source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) return true;

        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.ABYSS_WARD, EquipmentSlot.HEAD);
        if (level <= 0) return true;

        long currentTick = entity.level().getGameTime();
        Long lastUsed = abyssWardCooldowns.get(entity);
        if (lastUsed != null && (currentTick - lastUsed) < ABYSS_WARD_COOLDOWN_TICKS) return true;

        abyssWardCooldowns.put(entity, currentTick);
        entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, ABYSS_WARD_LEVITATION_TICKS, ABYSS_WARD_LEVITATION_AMPLIFIER, true, true, true));
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, ABYSS_WARD_UPWARD_BOOST, 0));
        if (entity instanceof ServerPlayer sp) {
            sp.hurtMarked = true;
        }
        return false;
    }

    /**
     * Blink: a blow that would kill instead teleports the wearer clear, once per
     * {@link DefenseEnchantMath#BLINK_COOLDOWN_GAME_DAYS} game day(s). Hooked at
     * {@code ALLOW_DEATH}, which Fabric fires on fatal damage <em>before</em> vanilla's
     * totem check — so a wearer holding a Totem of Undying is deliberately skipped here,
     * letting the totem consume as vanilla and guaranteeing the two can never fire on the
     * same death event.
     */
    private static boolean onAllowDeath(LivingEntity entity, DamageSource source, float amount) {
        if (entity.level().isClientSide()) return true;
        return !tryBlink(entity, source);
    }

    /** Attempts a Blink rescue; true if it fired and the death must be cancelled. */
    private static boolean tryBlink(LivingEntity entity, DamageSource source) {
        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.BLINK, EquipmentSlot.CHEST);
        if (level <= 0) return false;

        // Mirror the totem's own escape hatch: /kill and void damage go through.
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;

        if (entity.getMainHandItem().is(Items.TOTEM_OF_UNDYING)
                || entity.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            return false;
        }

        long now = entity.level().getGameTime();
        long lastUsed = entity.getAttachedOrElse(MeridianAttachments.BLINK_LAST_USED, DefenseEnchantMath.BLINK_NEVER_USED);
        if (!DefenseEnchantMath.blinkOffCooldown(lastUsed, now)) return false;

        entity.setAttached(MeridianAttachments.BLINK_LAST_USED, now);
        entity.setHealth(DefenseEnchantMath.BLINK_SURVIVAL_HEALTH);
        entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DefenseEnchantMath.BLINK_WEAKNESS_TICKS, 0));
        blinkTeleport(entity);
        return true;
    }

    /**
     * Scatters the wearer chorus-fruit style: random safe landings within
     * {@link DefenseEnchantMath#BLINK_TELEPORT_RANGE} blocks. If no attempt finds a safe
     * spot (sealed chambers), the wearer survives in place — the rescue is the contract,
     * the reposition is best-effort.
     */
    private static void blinkTeleport(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return;

        for (int i = 0; i < DefenseEnchantMath.BLINK_TELEPORT_ATTEMPTS; i++) {
            double x = entity.getX() + (entity.getRandom().nextDouble() - 0.5) * DefenseEnchantMath.BLINK_TELEPORT_RANGE;
            double y = Mth.clamp(entity.getY() + (entity.getRandom().nextInt(16) - 8),
                    level.getMinBuildHeight(), level.getMinBuildHeight() + level.getLogicalHeight() - 1);
            double z = entity.getZ() + (entity.getRandom().nextDouble() - 0.5) * DefenseEnchantMath.BLINK_TELEPORT_RANGE;

            if (entity.isPassenger()) {
                entity.stopRiding();
            }
            if (entity.randomTeleport(x, y, z, true)) {
                level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                        entity.getSoundSource(), 1.0f, 1.0f);
                return;
            }
        }
    }

    /**
     * Records the victim's health before an Ambush-carrying attacker's hit is applied.
     * Skipped while the attacker is landing a bonus hit, so the nested damage event can't
     * overwrite the snapshot with post-hit health.
     */
    private static void snapshotAmbushHealth(LivingEntity entity, DamageSource source) {
        if (source.getEntity() instanceof LivingEntity attacker
                && !BONUS_HIT_PROCESSING.contains(attacker.getUUID())
                && EnchantmentEffects.getEnchantmentLevel(attacker.getMainHandItem(), EnchantmentEffects.AMBUSH) > 0) {
            AMBUSH_PRE_HIT_HEALTH.put(entity, entity.getHealth());
        }
    }

    private static void onAfterDamage(LivingEntity entity, DamageSource source,
                                       float baseDamageTaken, float damageTaken, boolean blocked) {
        if (entity.level().isClientSide()) return;

        if (blocked) {
            handleRetribution(entity, source, baseDamageTaken);
        }

        if (damageTaken <= 0 && !blocked) return;

        // Attacker-driven on-hit effects are suppressed while the attacker is landing a
        // mod-originated bonus hit: the nested damage event must not re-proc them.
        Entity attackerEntity = source.getEntity();
        boolean bonusHit = attackerEntity instanceof LivingEntity livingAttacker
                && BONUS_HIT_PROCESSING.contains(livingAttacker.getUUID());
        if (!bonusHit) {
            handleQuell(entity, source);
            handleFinalGambit(entity, source);
            handleSiphon(entity, source);
            handleSoulTax(entity, source);
            handleAmbush(entity, source, damageTaken);
            handleSunder(entity, source);
            handleCleave(entity, source);
            handlePummel(entity, source);
            handleMaceSlam(entity, source);
        }
        handleRepulse(entity, source);
        handleFrostguard(entity, source);
        handleRally(entity);
        handleBloodrage(entity);
        handleEmberward(entity, source);
        handleReprieve(entity);
    }

    /**
     * Lands a mod-originated bonus hit on a victim that is inside the post-hit
     * invulnerability window (where vanilla would only apply the excess over the
     * triggering hit, typically swallowing the bonus). Zeroes the window so the bonus
     * registers in full, then restores {@code lastHurt} if the triggering hit was larger,
     * so third parties attacking into the window still face the original threshold.
     */
    private static void dealBonusDamage(LivingEntity victim, DamageSource source, float amount) {
        LivingEntityCombatAccessor accessor = (LivingEntityCombatAccessor) victim;
        float previousLastHurt = accessor.meridian$getLastHurt();
        victim.invulnerableTime = 0;
        victim.hurt(source, amount);
        if (accessor.meridian$getLastHurt() < previousLastHurt) {
            accessor.meridian$setLastHurt(previousLastHurt);
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

        float bonusDamage = weapon.getMaxDamage() * FINAL_GAMBIT_DAMAGE_FRACTION;

        // FinalGambit sacrifices the weapon for the bonus hit. Damage it to the break
        // threshold first so the real break effects (sound, particles, break callbacks)
        // fire, then force-consume any survivor: hurtAndBreak honours Unbreaking per
        // damage point and would otherwise spare a lightly-damaged or Unbreaking weapon.
        weapon.setDamageValue(Math.max(0, weapon.getMaxDamage() - 1));
        weapon.hurtAndBreak(weapon.getMaxDamage(), livingAttacker, EquipmentSlot.MAINHAND);
        if (!weapon.isEmpty()) {
            weapon.setCount(0);
        }

        withReentrancyGuard(BONUS_HIT_PROCESSING, livingAttacker.getUUID(),
                () -> entity.hurt(livingAttacker.damageSources().mobAttack(livingAttacker), bonusDamage));
    }

    private static void handleSiphon(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingAttacker.getMainHandItem(), EnchantmentEffects.SIPHON);
        if (level <= 0) return;

        float chance = SIPHON_BASE_CHANCE + SIPHON_CHANCE_PER_LEVEL * level;
        if (livingAttacker.getRandom().nextFloat() >= chance) return;

        float healAmount = SIPHON_HEAL_PER_LEVEL * level;
        livingAttacker.heal(healAmount);
    }

    private static void handleSoulTax(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(player.getMainHandItem(), EnchantmentEffects.SOUL_TAX);
        if (level <= 0) return;

        int xpCost = SOUL_TAX_BASE_XP_COST + SOUL_TAX_XP_COST_PER_LEVEL * level;
        if (player.totalExperience < xpCost) return;

        player.giveExperiencePoints(-xpCost);

        float bonusDamage = SOUL_TAX_BASE_DAMAGE + SOUL_TAX_DAMAGE_PER_LEVEL * level;
        withReentrancyGuard(BONUS_HIT_PROCESSING, player.getUUID(),
                () -> entity.hurt(player.damageSources().mobAttack(player), bonusDamage));
    }

    private static void handleCleave(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingAttacker.getMainHandItem(), EnchantmentEffects.CLEAVE);
        if (level <= 0) return;

        double range = CLEAVE_BASE_RANGE + CLEAVE_RANGE_PER_LEVEL * level;
        float damage = CLEAVE_BASE_DAMAGE + CLEAVE_DAMAGE_PER_LEVEL * level;

        AABB area = entity.getBoundingBox().inflate(range);
        List<LivingEntity> nearby = entity.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != entity && e != livingAttacker && e.isAlive());

        withReentrancyGuard(BONUS_HIT_PROCESSING, livingAttacker.getUUID(), () -> {
            for (LivingEntity target : nearby) {
                target.hurt(livingAttacker.damageSources().mobAttack(livingAttacker), damage);
            }
        });
    }

    private static void handleRepulse(LivingEntity entity, DamageSource source) {
        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.REPULSE,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS);
        if (level <= 0) return;

        Entity attacker = source.getDirectEntity();
        if (attacker == null) return;

        double distSq = attacker.distanceToSqr(entity);
        if (distSq > REPULSE_RANGE_SQ) return;

        Vec3 knockback = attacker.position().subtract(entity.position()).normalize();
        double force = REPULSE_BASE_FORCE + REPULSE_FORCE_PER_LEVEL * level;
        attacker.push(knockback.x * force, REPULSE_BASE_LIFT + REPULSE_LIFT_PER_LEVEL * level, knockback.z * force);

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
        if (distSq > FROSTGUARD_RANGE_SQ) return;

        int duration = FROSTGUARD_BASE_SLOW_TICKS + FROSTGUARD_SLOW_TICKS_PER_LEVEL * level;
        livingAttacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, level - 1));
    }

    private static void handleRally(LivingEntity entity) {
        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.RALLY, EquipmentSlot.CHEST);
        if (level <= 0) return;

        float threshold = entity.getMaxHealth() * RALLY_HEALTH_THRESHOLD;
        if (entity.getHealth() > threshold) return;

        long currentTick = entity.level().getGameTime();
        Long lastActivation = rallyCooldowns.get(entity);
        if (lastActivation != null && (currentTick - lastActivation) < RALLY_COOLDOWN_TICKS) return;

        rallyCooldowns.put(entity, currentTick);

        int duration = RALLY_BASE_REGEN_TICKS + RALLY_REGEN_TICKS_PER_LEVEL * level;
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, true, true, true));
    }

    private static void handleBloodrage(LivingEntity entity) {
        if (BLOODRAGE_PROCESSING.contains(entity.getUUID())) return;

        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.BLOODRAGE,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;

        int buffDuration = BLOODRAGE_BASE_BUFF_TICKS + BLOODRAGE_BUFF_TICKS_PER_LEVEL * level;
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, buffDuration, 0, true, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, buffDuration, level - 1, true, false, true));
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, buffDuration, 0, true, false, true));

        float hpCost = BLOODRAGE_BASE_HP_COST + BLOODRAGE_HP_COST_PER_LEVEL * level;
        withReentrancyGuard(BLOODRAGE_PROCESSING, entity.getUUID(),
                () -> entity.hurt(entity.damageSources().magic(), hpCost));
    }

    /**
     * Emberward: reactive Fire Resistance after fire or lava damage. Not a permanent
     * immunity — the burst expires, and while it holds no fire damage lands, so it only
     * re-arms once the wearer burns again.
     */
    private static void handleEmberward(LivingEntity entity, DamageSource source) {
        if (!source.is(DamageTypeTags.IS_FIRE)) return;

        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.EMBERWARD,
                EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;

        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                DefenseEnchantMath.EMBERWARD_FIRE_RES_TICKS, 0, true, true, true));
    }

    /**
     * Reprieve: stretches the post-hit invulnerability window vanilla just set to 20 in
     * {@code LivingEntity#hurt}. Overwriting here (after the hit fully resolved) also
     * covers mod-originated bonus hits, whose nested hurt re-runs this handler.
     */
    private static void handleReprieve(LivingEntity entity) {
        int level = EnchantmentEffects.getEquippedLevel(entity, EnchantmentEffects.REPRIEVE,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST);
        if (level <= 0) return;

        entity.invulnerableTime = DefenseEnchantMath.reprieveInvulnerabilityTicks(level);
    }

    private static void handleMaceSlam(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        ItemStack weapon = player.getMainHandItem();
        if (!weapon.is(Items.MACE)) return;
        if (player.fallDistance < MACE_SLAM_MIN_FALL_DISTANCE) return;

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

        int immunityTicks = TEMPEST_BASE_FIRE_RES_TICKS + TEMPEST_FIRE_RES_TICKS_PER_LEVEL * level;
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, immunityTicks, 0));
    }

    private static void handleSeismicSlam(LivingEntity entity, ServerPlayer player, ItemStack weapon) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.SEISMIC_SLAM);
        if (level <= 0) return;
        if (!player.isShiftKeyDown()) return;

        Vec3 pos = entity.position();
        double radius = SEISMIC_SLAM_RADIUS;
        float damage = SEISMIC_SLAM_DAMAGE;
        double force = SEISMIC_SLAM_KNOCKBACK;

        AABB area = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius);
        List<LivingEntity> nearby = entity.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e != entity && e.isAlive());

        for (LivingEntity target : nearby) {
            target.hurt(player.damageSources().mobAttack(player), damage);
            Vec3 knockDir = target.position().subtract(pos).normalize();
            target.push(knockDir.x * force, SEISMIC_SLAM_LIFT, knockDir.z * force);
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

        double launchVelocity = UPDRAFT_BASE_VELOCITY + UPDRAFT_VELOCITY_PER_LEVEL * level;
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

        float procChance = RETRIBUTION_BASE_CHANCE + RETRIBUTION_CHANCE_PER_LEVEL * level;
        if (entity.getRandom().nextFloat() >= procChance) return;

        float reflectRatio = RETRIBUTION_BASE_REFLECT + RETRIBUTION_REFLECT_PER_LEVEL * level;
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

        float bonusDamage = PUMMEL_BASE_DAMAGE + PUMMEL_DAMAGE_PER_LEVEL * level;
        offhand.hurtAndBreak(PUMMEL_DURABILITY_COST, livingAttacker, EquipmentSlot.OFFHAND);

        withReentrancyGuard(BONUS_HIT_PROCESSING, livingAttacker.getUUID(),
                () -> entity.hurt(livingAttacker.damageSources().mobAttack(livingAttacker), bonusDamage));
    }

    /**
     * Ambush: bonus damage scaled by how healthy the victim was before the hit — an
     * opener, maximal against an untouched target and fading as it takes damage. An
     * opener does not fire through a successful block, hence the {@code damageTaken}
     * gate.
     */
    private static void handleAmbush(LivingEntity entity, DamageSource source, float damageTaken) {
        if (damageTaken <= 0) return;
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingAttacker.getMainHandItem(), EnchantmentEffects.AMBUSH);
        if (level <= 0) return;

        // Prefer the ALLOW_DAMAGE snapshot (true pre-hit health); fall back to the
        // pre-mitigation reconstruction if some damage path skipped that event.
        Float snapshot = AMBUSH_PRE_HIT_HEALTH.remove(entity);
        float preHitHealth = snapshot != null ? snapshot : entity.getHealth() + damageTaken;
        float fraction = CombatEnchantMath.ambushHealthFraction(preHitHealth, entity.getMaxHealth());
        float bonusDamage = CombatEnchantMath.ambushBonusDamage(level, fraction);
        if (bonusDamage <= 0) return;

        withReentrancyGuard(BONUS_HIT_PROCESSING, livingAttacker.getUUID(),
                () -> dealBonusDamage(entity, livingAttacker.damageSources().mobAttack(livingAttacker), bonusDamage));
    }

    /**
     * Pinpoint's entry point, invoked from {@code PlayerMixin} at the exact point
     * {@code Player#attack} confirms a true critical hit (its {@code crit()} call, which
     * vanilla only reaches when the crit conditions held AND the hit landed). Applies a
     * flat bonus on top of the vanilla 1.5x crit multiplier.
     */
    public static void handlePinpointCrit(Player player, Entity target) {
        if (player.level().isClientSide()) return;
        // Crits against the Ender Dragon hit a part entity; unwrap to the boss itself.
        if (target instanceof EnderDragonPart part) target = part.parentMob;
        if (!(target instanceof LivingEntity livingTarget)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(player.getMainHandItem(), EnchantmentEffects.PINPOINT);
        if (level <= 0) return;

        float bonusDamage = CombatEnchantMath.pinpointBonusDamage(level);
        withReentrancyGuard(BONUS_HIT_PROCESSING, player.getUUID(),
                () -> dealBonusDamage(livingTarget, player.damageSources().playerAttack(player), bonusDamage));
    }

    /** Sunder: chance on hit to knock a random piece of the victim's equipment loose. */
    private static void handleSunder(LivingEntity entity, DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingAttacker.getMainHandItem(), EnchantmentEffects.SUNDER);
        if (level <= 0) return;
        if (!sunderVictimAllowed(entity)) return;
        if (entity.getRandom().nextFloat() >= CombatEnchantMath.sunderChance(level)) return;

        sunderStrip(entity);
    }

    /**
     * Whether Sunder may target this victim: mobs always, players only when
     * {@code combat.sunderAffectsPlayers} is enabled.
     */
    public static boolean sunderVictimAllowed(LivingEntity victim) {
        if (!(victim instanceof Player)) return true;
        MeridianConfig config = Meridian.getConfig();
        return config != null && config.combat.sunderAffectsPlayers;
    }

    /**
     * Removes one random occupied equipment slot from the victim and drops the stack as
     * a recoverable item entity. Returns the stripped slot, or {@code null} if the
     * victim had nothing equipped.
     */
    public static EquipmentSlot sunderStrip(LivingEntity victim) {
        List<EquipmentSlot> occupied = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack candidate = victim.getItemBySlot(slot);
            if (candidate.isEmpty()) continue;
            // Curse of Binding locks gear in place; Sunder respects it.
            if (EnchantmentHelper.has(candidate, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) continue;
            occupied.add(slot);
        }
        if (occupied.isEmpty()) return null;

        EquipmentSlot slot = occupied.get(victim.getRandom().nextInt(occupied.size()));
        ItemStack stack = victim.getItemBySlot(slot).copy();
        victim.setItemSlot(slot, ItemStack.EMPTY);
        victim.spawnAtLocation(stack);
        return slot;
    }

    private static void onAfterDeath(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide()) return;
        handlePlunder(entity, source);
        handleSnare(entity, source);
        handleTrophy(entity, source);
    }

    private static void handlePlunder(LivingEntity entity, DamageSource source) {
        Entity killer = source.getEntity();
        if (!(killer instanceof LivingEntity livingKiller)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingKiller.getMainHandItem(), EnchantmentEffects.PLUNDER);
        if (level <= 0) return;

        float chance = PLUNDER_CHANCE_PER_LEVEL * level;
        if (entity.getRandom().nextFloat() >= chance) return;

        boolean hitByPlayer = entity.getKillCredit() != null;
        ((LivingEntityLootInvoker) entity).meridian$invokeDropFromLootTable(source, hitByPlayer);
    }

    private static void handleSnare(LivingEntity entity, DamageSource source) {
        Entity killer = source.getEntity();
        if (!(killer instanceof LivingEntity livingKiller)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingKiller.getMainHandItem(), EnchantmentEffects.SNARE);
        if (level <= 0) return;

        float chance = SNARE_CHANCE;
        if (entity.getRandom().nextFloat() >= chance) return;

        SpawnEggItem egg = SpawnEggItem.byId(entity.getType());
        if (egg == null) return;

        entity.spawnAtLocation(new ItemStack(egg));
    }

    private static void handleTrophy(LivingEntity entity, DamageSource source) {
        Entity killer = source.getEntity();
        if (!(killer instanceof LivingEntity livingKiller)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(livingKiller.getMainHandItem(), EnchantmentEffects.TROPHY);
        if (level <= 0) return;

        if (entity.getRandom().nextFloat() >= CombatEnchantMath.trophyChance(level)) return;

        ItemStack head = trophyHeadFor(entity);
        if (head.isEmpty()) return;

        entity.spawnAtLocation(head);
    }

    /**
     * The head stack Trophy drops for this victim: the matching {@link #TROPHY_HEADS}
     * item for mobs, an owner-profiled player head for players, or
     * {@link ItemStack#EMPTY} when the victim has no head item.
     */
    public static ItemStack trophyHeadFor(LivingEntity victim) {
        if (victim instanceof Player player) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
            return head;
        }
        Item item = TROPHY_HEADS.get(victim.getType());
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }
}
