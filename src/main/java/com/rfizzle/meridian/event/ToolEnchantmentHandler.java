package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class ToolEnchantmentHandler {

    private static final Set<UUID> EXCAVATING_PLAYERS = Collections.synchronizedSet(new HashSet<>());
    private static final int PROSPECT_MAX_VEIN = 48;
    private static final int TIMBERFELL_MAX_LOGS = 192;

    private ToolEnchantmentHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(ToolEnchantmentHandler::onBlockBroken);
        UseBlockCallback.EVENT.register(ToolEnchantmentHandler::onUseBlock);
    }

    private static void onBlockBroken(Level world, Player player, BlockPos pos,
                                       BlockState state, BlockEntity blockEntity) {
        if (world.isClientSide()) return;
        if (EXCAVATING_PLAYERS.contains(player.getUUID())) return;
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack tool = player.getMainHandItem();

        ServerLevel serverLevel = (ServerLevel) world;
        EXCAVATING_PLAYERS.add(player.getUUID());
        try {
            EffectGuard.run("excavate", serverPlayer, () -> handleExcavate(serverPlayer, serverLevel, pos, state, tool));
            EffectGuard.run("prospect", serverPlayer, () -> handleProspect(serverPlayer, serverLevel, pos, state, tool));
            EffectGuard.run("timberfell", serverPlayer, () -> handleTimberfell(serverPlayer, serverLevel, pos, state, tool));
            EffectGuard.run("bounty", serverPlayer, () -> handleBounty(serverPlayer, serverLevel, pos, state, tool));
        } finally {
            EXCAVATING_PLAYERS.remove(player.getUUID());
        }
    }

    private static void handleExcavate(ServerPlayer player, ServerLevel world, BlockPos pos,
                                        BlockState state, ItemStack tool) {
        int level = EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.EXCAVATE);
        if (level <= 0) return;

        Direction face = getMiningFace(player);
        List<BlockPos> targets = get3x3(pos, face);

        for (BlockPos target : targets) {
            BlockState targetState = world.getBlockState(target);
            if (targetState.isAir()) continue;
            if (targetState.getDestroySpeed(world, target) < 0) continue;
            if (!player.hasCorrectToolForDrops(targetState) && targetState.requiresCorrectToolForDrops()) continue;

            player.gameMode.destroyBlock(target);
        }
    }

    private static void handleProspect(ServerPlayer player, ServerLevel world, BlockPos pos,
                                        BlockState state, ItemStack tool) {
        int level = EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.PROSPECT);
        if (level <= 0) return;

        if (!state.is(BlockTags.COAL_ORES) && !state.is(BlockTags.IRON_ORES)
                && !state.is(BlockTags.GOLD_ORES) && !state.is(BlockTags.DIAMOND_ORES)
                && !state.is(BlockTags.LAPIS_ORES) && !state.is(BlockTags.REDSTONE_ORES)
                && !state.is(BlockTags.EMERALD_ORES) && !state.is(BlockTags.COPPER_ORES)) {
            return;
        }

        Block oreBlock = state.getBlock();
        List<BlockPos> vein = findVein(world, pos, oreBlock);

        for (BlockPos target : vein) {
            player.gameMode.destroyBlock(target);
        }
    }

    private static void handleTimberfell(ServerPlayer player, ServerLevel world, BlockPos pos,
                                          BlockState state, ItemStack tool) {
        int level = EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.TIMBERFELL);
        if (level <= 0) return;

        if (!state.is(BlockTags.LOGS)) return;

        List<BlockPos> logs = findConnectedLogs(world, pos);
        for (BlockPos target : logs) {
            // Stop before the axe would break: each felled log costs durability, and
            // Timberfell must never be the swing that snaps the tool.
            if (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage() - 1) break;
            player.gameMode.destroyBlock(target);
        }
    }

    private static void handleBounty(ServerPlayer player, ServerLevel world, BlockPos pos,
                                      BlockState state, ItemStack tool) {
        int level = EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.BOUNTY);
        if (level <= 0) return;

        if (!(state.getBlock() instanceof CropBlock crop)) return;
        if (!crop.isMaxAge(state)) return;

        int radius = level + 1;

        for (BlockPos bp : BlockPos.betweenClosed(pos.offset(-radius, 0, -radius),
                pos.offset(radius, 0, radius))) {
            if (bp.equals(pos)) continue;
            BlockState cropState = world.getBlockState(bp);
            if (!(cropState.getBlock() instanceof CropBlock neighborCrop)) continue;
            if (!neighborCrop.isMaxAge(cropState)) continue;

            BlockPos immutable = bp.immutable();
            player.gameMode.destroyBlock(immutable);
            if (world.getBlockState(immutable).isAir()) {
                world.setBlockAndUpdate(immutable, neighborCrop.getStateForAge(0));
            }
        }
    }

    private static InteractionResult onUseBlock(Player player, Level world,
                                                 InteractionHand hand, BlockHitResult hitResult) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof HoeItem)) return InteractionResult.PASS;

        int level = EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.FURROW);
        if (level <= 0) return InteractionResult.PASS;

        BlockPos center = hitResult.getBlockPos();
        BlockState centerState = world.getBlockState(center);
        if (!isTillable(centerState)) return InteractionResult.PASS;

        int radius = level;
        EffectGuard.run("furrow", player, () -> applyFurrow(world, center, radius));

        return InteractionResult.PASS;
    }

    private static void applyFurrow(Level world, BlockPos center, int radius) {
        for (BlockPos bp : BlockPos.betweenClosed(center.offset(-radius, 0, -radius),
                center.offset(radius, 0, radius))) {
            if (bp.equals(center)) continue;
            BlockState state = world.getBlockState(bp);
            if (!isTillable(state)) continue;
            if (!world.getBlockState(bp.above()).isAir()) continue;

            world.setBlockAndUpdate(bp, Blocks.FARMLAND.defaultBlockState());
        }
    }

    private static boolean isTillable(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.DIRT_PATH) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT);
    }

    private static Direction getMiningFace(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        double absX = Math.abs(look.x);
        double absY = Math.abs(look.y);
        double absZ = Math.abs(look.z);

        if (absY >= absX && absY >= absZ) {
            return look.y > 0 ? Direction.UP : Direction.DOWN;
        } else if (absX >= absZ) {
            return look.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return look.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private static List<BlockPos> get3x3(BlockPos center, Direction face) {
        List<BlockPos> result = new ArrayList<>(8);
        Direction.Axis axis = face.getAxis();

        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos offset = switch (axis) {
                    case X -> center.offset(0, a, b);
                    case Y -> center.offset(a, 0, b);
                    case Z -> center.offset(a, b, 0);
                };
                result.add(offset);
            }
        }
        return result;
    }

    private static List<BlockPos> findVein(ServerLevel world, BlockPos start, Block oreBlock) {
        List<BlockPos> vein = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && vein.size() < PROSPECT_MAX_VEIN) {
            BlockPos current = queue.poll();
            if (!current.equals(start)) {
                vein.add(current);
            }

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                if (world.getBlockState(neighbor).is(oreBlock)) {
                    queue.add(neighbor);
                }
            }
        }
        return vein;
    }

    /**
     * Flood-fills the connected logs of a felled tree from {@code start} (the block just
     * broken by vanilla, so it is excluded from the result). Uses 26-neighbour (3×3×3)
     * connectivity rather than the 6-face connectivity Prospect uses, so offset branches
     * and 2×2 giant-tree trunks are caught. Bounded by {@link #TIMBERFELL_MAX_LOGS}.
     */
    static List<BlockPos> findConnectedLogs(ServerLevel world, BlockPos start) {
        List<BlockPos> logs = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && logs.size() < TIMBERFELL_MAX_LOGS) {
            BlockPos current = queue.poll();
            if (!current.equals(start)) {
                logs.add(current);
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (!visited.add(neighbor)) continue;
                        if (world.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return logs;
    }
}
