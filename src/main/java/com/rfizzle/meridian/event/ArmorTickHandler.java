package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.DefenseEnchantMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.TraversalEnchantMath;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorTickHandler {

    private static long tickCounter = 0;

    private record TrackedBlock(ResourceKey<Level> dimension, BlockPos pos) {}

    private static final Map<TrackedBlock, Long> cinderwalkBlocks = new ConcurrentHashMap<>();
    static final int CINDERWALK_REVERT_TICKS = 80;

    /** Curse of Hunger: extra food exhaustion added per level, every tick, while worn. */
    static final float CURSE_OF_HUNGER_EXHAUSTION_PER_LEVEL = 0.005f;

    /**
     * Curse of Attraction: hostile mobs within this radius of the wearer that have no current
     * target are pulled onto the wearer, so they notice and close in from well beyond their native
     * detection range.
     */
    static final double CURSE_OF_ATTRACTION_RADIUS = 24.0;

    private ArmorTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ArmorTickHandler::onServerTick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            tickCounter = 0;
            cinderwalkBlocks.clear();
        });
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            EffectGuard.run("luminance", player, () -> handleLuminance(player));
            EffectGuard.run("gravitas", player, () -> handleGravitas(player));
            EffectGuard.run("slipstream", player, () -> handleSlipstream(player));
            EffectGuard.run("ballast", player, () -> handleBallast(player));
            EffectGuard.run("cinderwalk", player, () -> handleCinderwalk(player));
            EffectGuard.run("terrasculpt", player, () -> handleTerrasculpt(player));
            EffectGuard.run("thermal", player, () -> handleThermal(player));
            EffectGuard.run("falconstrike", player, () -> handleFalconstrike(player));
            EffectGuard.run("curse_of_hunger", player, () -> handleCurseOfHunger(player));
            EffectGuard.run("curse_of_waterlogging", player, () -> handleCurseOfWaterlogging(player));

            if (tickCounter % DefenseEnchantMath.BULLRUSH_BASH_INTERVAL_TICKS == 0) {
                EffectGuard.run("bullrush", player, () -> handleBullrush(player));
            }

            if (tickCounter % 20 == 0) {
                EffectGuard.run("premonition", player, () -> handlePremonition(player));
                EffectGuard.run("curse_of_attraction", player, () -> handleCurseOfAttraction(player));
            }

            if (tickCounter % TraversalEnchantMath.MOLTING_SHED_INTERVAL_TICKS == 0) {
                EffectGuard.run("curse_of_molting", player, () -> handleCurseOfMolting(player));
            }
        }

        EffectGuard.run("cinderwalk_revert", null, () -> revertCinderwalkBlocks(server));
    }

    private static void handleLuminance(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.LUMINANCE, EquipmentSlot.HEAD);
        if (level <= 0) return;

        MobEffectInstance existing = player.getEffect(MobEffects.NIGHT_VISION);
        if (existing == null || existing.getDuration() < 220) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, true, false, true));
        }
    }

    private static void handleGravitas(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.GRAVITAS,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;

        if (tickCounter % 2 != 0) return;

        double radius = 3.0 + 2.0 * level;
        AABB area = player.getBoundingBox().inflate(radius);
        List<ItemEntity> items = player.serverLevel().getEntitiesOfClass(ItemEntity.class, area,
                e -> e.isAlive() && !e.hasPickUpDelay());

        Vec3 playerPos = player.position().add(0, 0.5, 0);
        double pullStrength = 0.05 + 0.03 * level;

        for (ItemEntity item : items) {
            Vec3 direction = playerPos.subtract(item.position()).normalize();
            item.push(direction.x * pullStrength, direction.y * pullStrength, direction.z * pullStrength);
        }
    }

    private static void handleSlipstream(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.SLIPSTREAM,
                EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;

        if (!player.isInWater()) return;

        MobEffectInstance existing = player.getEffect(MobEffects.DOLPHINS_GRACE);
        if (existing == null || existing.getDuration() < 20) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false, true));
        }
    }

    /**
     * Ballast's controlled vertical mobility in water: crouching drives the wearer down and a held
     * jump drives them up, each toward a per-level terminal speed. Crouch is read straight off the
     * server's player state; the rising intent is the client-reported jump held in
     * {@link BallastHandler}, re-gated here on the enchant and the water check so a spoofed flag only
     * ever swims a wearer up, never flies them. Package-private so ArmorTickHandlerGameTest can drive
     * it directly with a mock player.
     */
    static void handleBallast(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.BALLAST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;
        if (!player.isInWater()) return;

        boolean sink = player.isShiftKeyDown();
        boolean rise = !sink && BallastHandler.isRising(player.getUUID());
        if (!sink && !rise) return;

        double terminal = TraversalEnchantMath.ballastVerticalSpeed(level);
        Vec3 velocity = player.getDeltaMovement();
        double newY = sink
                ? Math.max(-terminal, velocity.y - TraversalEnchantMath.BALLAST_ACCEL_PER_TICK)
                : Math.min(terminal, velocity.y + TraversalEnchantMath.BALLAST_ACCEL_PER_TICK);
        if (newY == velocity.y) return;

        player.setDeltaMovement(velocity.x, newY, velocity.z);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    private static void handleCinderwalk(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.CINDERWALK, EquipmentSlot.FEET);
        if (level <= 0) return;

        if (tickCounter % 3 != 0) return;

        ServerLevel world = player.serverLevel();
        BlockPos center = player.blockPosition().below();
        int radius = 1 + level;
        long currentTick = player.getServer().overworld().getGameTime();

        for (BlockPos bp : BlockPos.betweenClosed(center.offset(-radius, 0, -radius),
                center.offset(radius, 0, radius))) {
            if (bp.distManhattan(center) > radius) continue;

            BlockState state = world.getBlockState(bp);
            if (state.getFluidState().is(Fluids.LAVA) && state.getFluidState().isSource()) {
                BlockPos immutable = bp.immutable();
                world.setBlockAndUpdate(immutable, Blocks.OBSIDIAN.defaultBlockState());
                cinderwalkBlocks.put(new TrackedBlock(world.dimension(), immutable), currentTick);
            }
        }
    }

    static void revertCinderwalkBlocks(MinecraftServer server) {
        if (cinderwalkBlocks.isEmpty()) return;

        long currentTick = server.overworld().getGameTime();
        Iterator<Map.Entry<TrackedBlock, Long>> it = cinderwalkBlocks.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<TrackedBlock, Long> entry = it.next();
            if (currentTick - entry.getValue() < CINDERWALK_REVERT_TICKS) continue;

            TrackedBlock tracked = entry.getKey();
            ServerLevel level = server.getLevel(tracked.dimension());
            if (level != null && level.isLoaded(tracked.pos()) && level.getBlockState(tracked.pos()).is(Blocks.OBSIDIAN)) {
                level.setBlockAndUpdate(tracked.pos(), Blocks.LAVA.defaultBlockState());
            }
            it.remove();
        }
    }

    // Test support: package-private hooks exercised by ArmorTickHandlerGameTest to drive
    // the Cinderwalk revert path with explicit dimension keys and timestamps.
    static void cinderwalkTrackForTest(ResourceKey<Level> dimension, BlockPos pos, long gameTime) {
        cinderwalkBlocks.put(new TrackedBlock(dimension, pos.immutable()), gameTime);
    }

    static void cinderwalkResetForTest() {
        cinderwalkBlocks.clear();
    }

    private static void handleTerrasculpt(ServerPlayer player) {
        if (!(player.getMainHandItem().getItem() instanceof HoeItem)) return;

        int level = EnchantmentEffects.getEnchantmentLevel(player.getMainHandItem(), EnchantmentEffects.TERRASCULPT);
        if (level <= 0) return;

        if (tickCounter % 5 != 0) return;

        BlockPos below = player.blockPosition().below();
        ServerLevel world = player.serverLevel();

        for (BlockPos bp : BlockPos.betweenClosed(below.offset(-1, 0, -1), below.offset(1, 0, 1))) {
            BlockState state = world.getBlockState(bp);
            BlockState converted = getTerrasculptConversion(state);
            if (converted != null) {
                world.setBlockAndUpdate(bp, converted);
            }
        }
    }

    private static BlockState getTerrasculptConversion(BlockState state) {
        if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        if (state.is(Blocks.COBBLESTONE)) return Blocks.STONE.defaultBlockState();
        if (state.is(Blocks.COBBLED_DEEPSLATE)) return Blocks.DEEPSLATE.defaultBlockState();
        if (state.is(Blocks.NETHERRACK)) return Blocks.WARPED_NYLIUM.defaultBlockState();
        if (state.is(Blocks.END_STONE)) return Blocks.GRASS_BLOCK.defaultBlockState();
        return null;
    }

    private static void handleThermal(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.THERMAL, EquipmentSlot.CHEST);
        if (level <= 0) return;
        if (!player.isFallFlying()) return;

        ServerLevel world = player.serverLevel();
        int depth = TraversalEnchantMath.thermalScanDepth(level);
        BlockPos base = player.blockPosition();
        BlockPos.MutableBlockPos cursor = base.mutable();

        boolean heatBelow = false;
        for (int i = 1; i <= depth; i++) {
            cursor.setY(base.getY() - i);
            if (!world.isLoaded(cursor)) break;
            if (isHeatSource(world.getBlockState(cursor))) {
                heatBelow = true;
                break;
            }
        }
        if (!heatBelow) return;

        // Add upward velocity, but never past the terminal climb speed — the updraft can
        // boost a glide over heat, never sustain flight once the heat is left behind.
        Vec3 velocity = player.getDeltaMovement();
        double maxClimb = TraversalEnchantMath.thermalMaxClimb(level);
        if (velocity.y >= maxClimb) return;

        double newY = Math.min(maxClimb, velocity.y + TraversalEnchantMath.thermalLiftPerTick(level));
        player.setDeltaMovement(velocity.x, newY, velocity.z);
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    private static boolean isHeatSource(BlockState state) {
        if (state.getBlock() instanceof BaseFireBlock) return true;
        if (state.is(Blocks.MAGMA_BLOCK)) return true;
        if (state.getFluidState().is(Fluids.LAVA)) return true;
        if (state.getBlock() instanceof CampfireBlock) return state.getValue(CampfireBlock.LIT);
        return false;
    }

    private static void handlePremonition(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.PREMONITION, EquipmentSlot.HEAD);
        if (level <= 0) return;

        double radius = 16.0;
        AABB area = player.getBoundingBox().inflate(radius);
        List<Monster> hostiles = player.serverLevel().getEntitiesOfClass(Monster.class, area,
                LivingEntity::isAlive);

        for (Monster mob : hostiles) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false, false));
        }
    }

    // Package-private so ArmorTickHandlerGameTest can drive the effect directly with a mock player,
    // matching the *ForTest seam convention used elsewhere in this class.
    static void handleCurseOfHunger(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.CURSE_OF_HUNGER,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;

        player.causeFoodExhaustion(CURSE_OF_HUNGER_EXHAUSTION_PER_LEVEL * level);
    }

    /**
     * Curse of Waterlogging: the wearer wades heavy while wet, slowed by a Slowness the water keeps
     * refreshed and that lingers a few seconds after they climb out. Reapplying only once the effect
     * runs low keeps a steady slow without spamming the effect every tick, and never downgrades a
     * stronger Slowness from another source. Package-private so ArmorTickHandlerGameTest can drive it
     * directly with a mock player.
     */
    static void handleCurseOfWaterlogging(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.CURSE_OF_WATERLOGGING,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;
        if (!player.isInWaterOrRain()) return;

        int amplifier = DefenseEnchantMath.waterloggingSlownessAmplifier(level);
        MobEffectInstance existing = player.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (existing == null
                || existing.getDuration() < DefenseEnchantMath.WATERLOGGING_SLOW_REFRESH_BELOW_TICKS
                || existing.getAmplifier() < amplifier) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    DefenseEnchantMath.WATERLOGGING_SLOW_DECAY_TICKS, amplifier, false, false, true));
        }
    }

    /**
     * Curse of Molting (durability): while gliding, the elytra sheds an extra burst of durability on
     * each interval — the call-site gate throttles the interval, so this always sheds when invoked.
     * Package-private so ArmorTickHandlerGameTest can drive it directly with a mock player.
     */
    static void handleCurseOfMolting(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.CURSE_OF_MOLTING, EquipmentSlot.CHEST);
        if (level <= 0) return;
        if (!player.isFallFlying()) return;

        ItemStack elytra = player.getItemBySlot(EquipmentSlot.CHEST);
        elytra.hurtAndBreak(TraversalEnchantMath.MOLTING_SHED_DURABILITY, player, EquipmentSlot.CHEST);
    }

    /**
     * Falconstrike: gliding into a creature transfers the glider's kinetic energy as damage — the
     * offensive complement to Impact Ward's defensive fall-damage soak. Fires each tick while gliding
     * above the drift threshold; vanilla's hurt cooldown throttles repeat hits on the same creature,
     * and a slice of horizontal momentum is bled into the hit so the glide is preserved, not halted.
     * Players are never struck — only Bullrush carries a player-affecting config gate. Package-private
     * so ArmorTickHandlerGameTest can drive it directly with a mock player.
     */
    static void handleFalconstrike(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.FALCONSTRIKE, EquipmentSlot.CHEST);
        if (level <= 0) return;
        if (!player.isFallFlying()) return;

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float damage = TraversalEnchantMath.falconstrikeKineticDamage(level, horizontalSpeed);
        if (damage <= 0.0f) return;

        // Deliberately not modulo-gated (unlike Bullrush): a fast glide-through touches a mob for a
        // single tick, so an interval gate would miss the contact. Three cheap rejects above (level,
        // gliding, above drift speed) already keep the scan off the hot path for everyone else.
        AABB area = player.getBoundingBox().inflate(TraversalEnchantMath.FALCONSTRIKE_REACH);
        List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != player && !(e instanceof Player) && !player.isAlliedTo(e));
        if (targets.isEmpty()) return;

        boolean struck = false;
        for (LivingEntity target : targets) {
            if (target.hurt(player.damageSources().mobAttack(player), damage)) {
                struck = true;
            }
        }
        if (!struck) return;

        double retain = TraversalEnchantMath.FALCONSTRIKE_MOMENTUM_RETENTION;
        player.setDeltaMovement(velocity.x * retain, velocity.y, velocity.z * retain);
        player.hurtMarked = true;
    }

    /**
     * Bullrush: sprinting with an enchanted shield in hand bashes creatures in the charge path aside,
     * knocking them back and dazing them with Slowness at a shield-durability cost per bash. Players
     * are spared unless {@code combat.bullrushAffectsPlayers} is set. Interval-gated by the tick loop
     * so a sustained charge is a rhythmic shove, not a per-tick knockback lock. Package-private so
     * ArmorTickHandlerGameTest can drive it directly with a mock player.
     */
    static void handleBullrush(ServerPlayer player) {
        EquipmentSlot shieldSlot = bullrushShieldSlot(player);
        if (shieldSlot == null) return;
        if (!player.isSprinting()) return;

        // Level and the charged shield both come from the one resolved stack, so a second Bullrush
        // shield in the other hand can never lend its level to a bash the offhand pays the durability
        // for — resolve the slot first, then read everything off it.
        ItemStack shield = player.getItemBySlot(shieldSlot);
        int level = EnchantmentEffects.getEnchantmentLevel(shield, EnchantmentEffects.BULLRUSH);

        AABB area = player.getBoundingBox().inflate(DefenseEnchantMath.BULLRUSH_REACH);
        List<LivingEntity> targets = player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != player && !player.isAlliedTo(e)
                        && EnchantmentEffectHandler.bullrushTargetAllowed(e));
        if (targets.isEmpty()) return;

        int dazeTicks = DefenseEnchantMath.bullrushDazeTicks(level);
        int slowAmplifier = DefenseEnchantMath.bullrushSlownessAmplifier(level);
        double knockback = DefenseEnchantMath.bullrushKnockbackStrength(level);

        for (LivingEntity target : targets) {
            target.knockback(knockback, player.getX() - target.getX(), player.getZ() - target.getZ());
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dazeTicks, slowAmplifier,
                    false, true, true));
            shield.hurtAndBreak(DefenseEnchantMath.BULLRUSH_DURABILITY_COST, player, shieldSlot);
        }
    }

    /**
     * The hand holding a Bullrush-enchanted shield — the stack whose durability a bash spends.
     * Offhand takes precedence (a shield's natural slot); {@code null} if neither hand carries one.
     */
    private static EquipmentSlot bullrushShieldSlot(ServerPlayer player) {
        if (EnchantmentEffects.getEnchantmentLevel(player.getOffhandItem(), EnchantmentEffects.BULLRUSH) > 0) {
            return EquipmentSlot.OFFHAND;
        }
        if (EnchantmentEffects.getEnchantmentLevel(player.getMainHandItem(), EnchantmentEffects.BULLRUSH) > 0) {
            return EquipmentSlot.MAINHAND;
        }
        return null;
    }

    static void handleCurseOfAttraction(ServerPlayer player) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.CURSE_OF_ATTRACTION,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        if (level <= 0) return;

        AABB area = player.getBoundingBox().inflate(CURSE_OF_ATTRACTION_RADIUS);
        List<Monster> hostiles = player.serverLevel().getEntitiesOfClass(Monster.class, area,
                mob -> mob.isAlive() && mob.getTarget() == null && mob.canAttack(player));

        for (Monster mob : hostiles) {
            mob.setTarget(player);
        }
    }
}
