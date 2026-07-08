package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.TraversalEnchantMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Grapnel (fishing rod): reeling in a hook that is lodged against a block yanks the
 * player toward it. The pull is gated by a per-level range, a durability cost, and a
 * short item cooldown so it can't be spammed into free flight. Driven from
 * {@link com.rfizzle.meridian.mixin.FishingHookMixin} at the head of
 * {@code FishingHook#retrieve}.
 */
public final class GrapnelHandler {

    private GrapnelHandler() {}

    public static void tryPull(FishingHook hook, ItemStack rod) {
        Level level = hook.level();
        if (level.isClientSide()) return;
        if (!(hook.getPlayerOwner() instanceof ServerPlayer player)) return;

        int enchantLevel = EnchantmentEffects.getEnchantmentLevel(rod, EnchantmentEffects.GRAPNEL);
        if (enchantLevel <= 0) return;

        // Only a hook anchored to terrain grapples: skip fish/entity catches and water bobbers.
        if (hook.getHookedIn() != null) return;
        if (hook.isInWater()) return;
        // A hook still in flight (e.g. grazing a block face mid-cast) hasn't lodged yet.
        if (hook.getDeltaMovement().lengthSqr() > TraversalEnchantMath.GRAPNEL_ANCHOR_MAX_SPEED_SQR) return;
        if (!isAnchoredToBlock(hook)) return;
        if (player.getCooldowns().isOnCooldown(rod.getItem())) return;

        Vec3 toHook = hook.position().subtract(player.position());
        double distance = toHook.length();
        if (distance < 1.0e-3) return;
        if (distance > TraversalEnchantMath.grapnelMaxRange(enchantLevel)) return;

        double pullSpeed = TraversalEnchantMath.grapnelPullSpeed(enchantLevel, distance);
        Vec3 pull = toHook.scale(pullSpeed / distance);
        EffectGuard.run("grapnel", player, () -> applyPull(player, rod, level, pull));
    }

    private static void applyPull(ServerPlayer player, ItemStack rod, Level level, Vec3 pull) {
        player.setDeltaMovement(pull.x, pull.y + TraversalEnchantMath.GRAPNEL_LIFT, pull.z);
        player.hurtMarked = true;
        player.resetFallDistance();

        EquipmentSlot slot = player.getMainHandItem() == rod ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        rod.hurtAndBreak(TraversalEnchantMath.GRAPNEL_DURABILITY_COST, player, slot);
        player.getCooldowns().addCooldown(rod.getItem(), TraversalEnchantMath.GRAPNEL_COOLDOWN_TICKS);

        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    /** True when the hook rests against a solid block — the block it occupies or any face-neighbour. */
    private static boolean isAnchoredToBlock(FishingHook hook) {
        Level level = hook.level();
        BlockPos hookPos = hook.blockPosition();
        if (isSolid(level, hookPos)) return true;
        for (Direction dir : Direction.values()) {
            if (isSolid(level, hookPos.relative(dir))) return true;
        }
        return false;
    }

    private static boolean isSolid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty();
    }
}
