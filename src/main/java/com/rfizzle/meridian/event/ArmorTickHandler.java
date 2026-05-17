package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorTickHandler {

    private static int tickCounter = 0;

    private static final Map<BlockPos, Long> cinderwalkBlocks = new ConcurrentHashMap<>();
    private static final int CINDERWALK_REVERT_TICKS = 80;

    private ArmorTickHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ArmorTickHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            handleLuminance(player);
            handleGravitas(player);
            handleSlipstream(player);
            handleCinderwalk(player);
            handleTerrasculpt(player);

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
        long currentTick = world.getGameTime();

        for (BlockPos bp : BlockPos.betweenClosed(center.offset(-radius, 0, -radius),
                center.offset(radius, 0, radius))) {
            if (bp.distManhattan(center) > radius) continue;

            BlockState state = world.getBlockState(bp);
            if (state.getFluidState().is(Fluids.LAVA) && state.getFluidState().isSource()) {
                BlockPos immutable = bp.immutable();
                world.setBlockAndUpdate(immutable, Blocks.OBSIDIAN.defaultBlockState());
                cinderwalkBlocks.put(immutable, currentTick);
            }
        }
    }

    private static void revertCinderwalkBlocks(MinecraftServer server) {
        if (cinderwalkBlocks.isEmpty()) return;

        long currentTick = server.overworld().getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> it = cinderwalkBlocks.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (currentTick - entry.getValue() < CINDERWALK_REVERT_TICKS) continue;

            BlockPos pos = entry.getKey();
            for (ServerLevel level : server.getAllLevels()) {
                if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.OBSIDIAN)) {
                    level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                    break;
                }
            }
            it.remove();
        }
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
