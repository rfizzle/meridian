package com.rfizzle.meridian.mixin;

import java.util.concurrent.atomic.AtomicBoolean;
import org.spongepowered.asm.mixin.Unique;
import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu;
import com.rfizzle.meridian.api.IEnchantingStatProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantingTableBlock.class)
abstract class EnchantmentTableBlockMixin {

    /** One-shot gate: animateTick runs every client tick per table. */
    @Unique
    private static final AtomicBoolean PARTICLE_FAILURE_LOGGED = new AtomicBoolean(false);

    @Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
    private void meridian$menuProvider(
            BlockState state, Level level, BlockPos pos,
            CallbackInfoReturnable<MenuProvider> cir) {
        cir.setReturnValue(new SimpleMenuProvider(
                (id, inv, player) -> new MeridianEnchantmentMenu(
                        id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("container.enchant")));
    }

    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void meridian$animateTick(
            BlockState state, Level level, BlockPos pos, RandomSource random,
            CallbackInfo ci) {
        ci.cancel();
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockState shelfState = level.getBlockState(pos.offset(offset));
            if (shelfState.getBlock() instanceof IEnchantingStatProvider provider) {
                // Client render path with vanilla's animateTick already cancelled: a throwing
                // third-party shelf must not take the table's particles down with it (§3.1).
                try {
                    provider.spawnTableParticle(shelfState, level, random, pos, offset);
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (Throwable t) {
                    if (PARTICLE_FAILURE_LOGGED.compareAndSet(false, true)) {
                        Meridian.LOGGER.warn("IEnchantingStatProvider {} threw in spawnTableParticle; "
                                + "skipping its particles", provider.getClass().getName(), t);
                    }
                }
            } else if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                if (random.nextInt(16) == 0) {
                    level.addParticle(ParticleTypes.ENCHANT,
                            pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D,
                            offset.getX() + random.nextFloat() - 0.5D,
                            offset.getY() - random.nextFloat() - 1.0F,
                            offset.getZ() + random.nextFloat() - 0.5D);
                }
            }
        }
    }
}
