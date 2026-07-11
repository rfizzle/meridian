package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.KilnSmelting;
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
     * Kiln + Reclaim: the two drop-altering mining enchants share one drop-routing handler so they
     * compose without double-dropping. Kiln decides <em>what</em> drops (each drop is replaced with
     * its smelted form when a furnace recipe exists); Reclaim decides <em>where</em> they go
     * (straight into the breaking player's inventory instead of ground items). With both present,
     * smelted drops land in the inventory.
     *
     * <p>Targets only the entity-aware {@code dropResources} overload — the player-destroy path — so
     * explosion/plugin drops without a breaker are untouched, and Kiln (a tool enchant) never fires
     * where no tool broke the block. When neither enchant is active on the tool the handler returns
     * immediately, before any drop or recipe work, so the common block-break path is unaffected.
     *
     * <p>Reclaim's inventory routing stays player-only: fake players (quarries, block-breaker
     * machines) never have their phantom inventory filled, so they fall back to ground drops — but
     * Kiln's smelt still applies to those ground drops, since smelting is a property of the tool.
     * {@code spawnAfterBreak} still runs so ore XP is unchanged, and a disabled {@code doTileDrops}
     * gamerule defers to vanilla, which suppresses drops itself.
     */
    @Inject(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/entity/BlockEntity;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private static void meridian$routeDrops(BlockState state, Level level, BlockPos pos,
                                            BlockEntity blockEntity, Entity entity,
                                            ItemStack tool, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        int kiln = EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.KILN);
        Player player = entity instanceof Player p ? p : null;
        boolean routeToInventory = player != null
                && !(player instanceof FakePlayer)
                && EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.RECLAIM) > 0;
        if (kiln <= 0 && !routeToInventory) return;
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) return;

        for (ItemStack drop : Block.getDrops(state, serverLevel, pos, blockEntity, entity, tool)) {
            ItemStack out = kiln > 0 ? KilnSmelting.smelt(serverLevel, drop) : drop;
            if (routeToInventory) {
                player.getInventory().placeItemBackInInventory(out);
            } else {
                // A smelted drop can exceed a stack (a modded high-count smelt of a multi-item
                // drop); pop it in stack-sized pieces so no oversized item entity is spawned.
                while (!out.isEmpty()) {
                    Block.popResource(level, pos, out.split(out.getMaxStackSize()));
                }
            }
        }
        state.spawnAfterBreak(serverLevel, pos, tool, true);
        ci.cancel();
    }
}
