package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockDropsMixin {

    /**
     * Reclaim: routes block drops straight into the breaking player's inventory instead
     * of spawning ground items. Targets only the entity-aware {@code dropResources}
     * overload — the one on the player-destroy path — so explosion/plugin drops without
     * a breaker are untouched. Overflow falls back to vanilla behavior via
     * {@code placeItemBackInInventory} (drops at the player's feet), and
     * {@code spawnAfterBreak} still runs so ore XP is unchanged. A disabled
     * {@code doTileDrops} gamerule defers to vanilla, which suppresses drops itself.
     * Fake players (quarries, block-breaker machines wielding a Reclaim tool) also
     * defer to vanilla — their phantom inventory is never emptied, so routing drops
     * into it would destroy items the machine expects to vacuum off the ground.
     */
    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/entity/BlockEntity;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private static void meridian$reclaimDrops(BlockState state, Level level, BlockPos pos,
                                              BlockEntity blockEntity, Entity entity,
                                              ItemStack tool, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(entity instanceof Player player)) return;
        if (player instanceof FakePlayer) return;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) return;
        if (EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.RECLAIM) <= 0) return;

        for (ItemStack drop : Block.getDrops(state, serverLevel, pos, blockEntity, entity, tool)) {
            player.getInventory().placeItemBackInInventory(drop);
        }
        state.spawnAfterBreak(serverLevel, pos, tool, true);
        ci.cancel();
    }
}
