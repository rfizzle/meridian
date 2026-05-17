package com.rfizzle.meridian.event;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AurifyHandler {

    private AurifyHandler() {}

    public static void register() {
        UseBlockCallback.EVENT.register(AurifyHandler::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand,
                                                 BlockHitResult hitResult) {
        if (world.isClientSide()) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        int level = EnchantmentEffects.getEnchantmentLevel(stack, EnchantmentEffects.AURIFY);
        if (level <= 0) return InteractionResult.PASS;

        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = world.getBlockState(pos);
        BlockState converted = getGoldConversion(state);
        if (converted == null) return InteractionResult.PASS;

        if (player.getRandom().nextFloat() >= 0.30f) {
            EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(3, player, slot);
            return InteractionResult.SUCCESS;
        }

        world.setBlockAndUpdate(pos, converted);
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(5, player, slot);
        world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.8f);

        return InteractionResult.SUCCESS;
    }

    private static BlockState getGoldConversion(BlockState state) {
        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)) {
            return Blocks.GOLD_ORE.defaultBlockState();
        }
        if (state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE)) {
            return Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState();
        }
        if (state.is(Blocks.NETHERRACK)) {
            return Blocks.NETHER_GOLD_ORE.defaultBlockState();
        }
        if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)) {
            return Blocks.GOLD_BLOCK.defaultBlockState();
        }
        return null;
    }
}
