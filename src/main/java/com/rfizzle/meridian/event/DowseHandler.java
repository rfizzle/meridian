package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.MiningEnchantMath;
import com.rfizzle.meridian.net.DowseGlowPayload;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Dowse (pickaxe): sneak-using a Dowse pickaxe scans the nearby world for the closest ore vein and
 * makes it glow through walls for the acting player alone, on a substantial cooldown. The heavy
 * scan is gated behind sneak + the Very-Rare enchant + an {@link net.minecraft.world.item.ItemCooldowns}
 * cooldown that is spent whenever the scan fires (hit or miss), so it can never be spammed. The client
 * render is driven entirely by the {@link DowseGlowPayload} sent here — the server decides, the client
 * only draws. Extends the Mark/Premonition glow family to blocks, which (unlike glowing an entity) needs
 * its own payload and renderer because vanilla has no through-walls outline for a block.
 */
public final class DowseHandler {

    private DowseHandler() {}

    public static void register() {
        UseItemCallback.EVENT.register(DowseHandler::onUseItem);
    }

    private static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Cheap bail-outs first (outside the effect guard): server side, main hand, sneaking, a Dowse
        // pickaxe, off cooldown. Only pickaxes can carry Dowse, but the tag check guards a cheated book.
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);
        if (!stack.is(ItemTags.PICKAXES)) return InteractionResultHolder.pass(stack);
        if (EnchantmentEffects.getEnchantmentLevel(stack, EnchantmentEffects.DOWSE) <= 0) {
            return InteractionResultHolder.pass(stack);
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())) return InteractionResultHolder.pass(stack);

        EffectGuard.run("dowse", serverPlayer, () -> performDowse(serverPlayer, serverPlayer.serverLevel(), stack));
        return InteractionResultHolder.success(stack);
    }

    private static void performDowse(ServerPlayer player, ServerLevel world, ItemStack stack) {
        // Spend the cooldown up front, so even a miss (no ore in range) gates the next expensive scan.
        player.getCooldowns().addCooldown(stack.getItem(), MiningEnchantMath.DOWSE_COOLDOWN_TICKS);

        List<BlockPos> vein = findNearestVein(world, player.blockPosition());
        if (vein.isEmpty()) {
            // A distinct low, dull cue so a miss reads as "nothing nearby" rather than dead input
            // (the cooldown is spent either way).
            world.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT,
                    SoundSource.PLAYERS, 0.5f, 0.6f);
            return;
        }

        if (ServerPlayNetworking.canSend(player, DowseGlowPayload.TYPE)) {
            ServerPlayNetworking.send(player, new DowseGlowPayload(vein));
        }
        world.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    /**
     * Finds the ore block nearest {@code origin} within {@link MiningEnchantMath#DOWSE_SEARCH_RADIUS}
     * and flood-fills its connected vein (6-face, any {@code #c:ores} block, so mixed clusters reveal
     * together), capped at {@link MiningEnchantMath#DOWSE_MAX_VEIN}. Empty when no ore is in range.
     * Exposed for the gametest.
     */
    public static List<BlockPos> findNearestVein(ServerLevel world, BlockPos origin) {
        int r = MiningEnchantMath.DOWSE_SEARCH_RADIUS;
        BlockPos nearest = null;
        long nearestDistSq = Long.MAX_VALUE;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!world.getBlockState(cursor).is(ConventionalBlockTags.ORES)) continue;
                    long distSq = (long) dx * dx + (long) dy * dy + (long) dz * dz;
                    if (distSq < nearestDistSq) {
                        nearestDistSq = distSq;
                        nearest = cursor.immutable();
                    }
                }
            }
        }

        if (nearest == null) return List.of();
        return floodVein(world, nearest);
    }

    private static List<BlockPos> floodVein(ServerLevel world, BlockPos seed) {
        List<BlockPos> vein = new ArrayList<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(seed);
        visited.add(seed);

        while (!queue.isEmpty() && vein.size() < MiningEnchantMath.DOWSE_MAX_VEIN) {
            BlockPos current = queue.poll();
            vein.add(current);

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (!visited.add(neighbor)) continue;
                if (world.getBlockState(neighbor).is(ConventionalBlockTags.ORES)) {
                    queue.add(neighbor);
                }
            }
        }
        return vein;
    }
}
