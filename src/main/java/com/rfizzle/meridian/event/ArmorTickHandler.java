package com.rfizzle.meridian.event;

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
import net.minecraft.world.item.HoeItem;
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
            handleLuminance(player);
            handleGravitas(player);
            handleSlipstream(player);
            handleCinderwalk(player);
            handleTerrasculpt(player);
            handleThermal(player);

            if (tickCounter % 20 == 0) {
                handlePremonition(player);
            }
        }

        revertCinderwalkBlocks(server);
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
}
