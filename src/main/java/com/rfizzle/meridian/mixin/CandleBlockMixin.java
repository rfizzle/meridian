package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.api.IEnchantingStatProvider;
import com.rfizzle.meridian.enchanting.EnchantingStats;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes vanilla candles contribute Arcana to a nearby enchanting table, matching Zenith's
 * {@code CandleBlockMixin}: <b>+1.25% Arcana per lit-or-unlit candle</b> in the block, so a full
 * four-candle block grants +5%. Targets {@link CandleBlock}, which is the base class for every
 * dyed and undyed candle, so all 17 colours participate.
 *
 * <p>Implemented by adding {@link IEnchantingStatProvider} to {@code CandleBlock} via mixin and
 * overriding {@link IEnchantingStatProvider#getStats}; the shelf scan already routes any
 * {@code IEnchantingStatProvider} block through {@code getStats}. Eterna stays 0, so the interface's
 * default particle hook (gated on {@code eterna > 0}) never fires — candles add no table particles.
 */
@Mixin(CandleBlock.class)
public abstract class CandleBlockMixin implements IEnchantingStatProvider {

    @Override
    public EnchantingStats getStats(Level level, BlockPos pos, BlockState state) {
        int candles = state.getValue(CandleBlock.CANDLES);
        return new EnchantingStats(0F, 0F, 0F, 1.25F * candles, 0F, 0);
    }
}
