package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.BrushEnchantMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meticulous: makes brushing a suspicious block faster and biases its archaeology loot toward
 * rarer entries. Both levers key off the brushing player's Meticulous level, captured once at the
 * head of {@code brush} into a per-block-entity field:
 *
 * <ul>
 *   <li><b>Speed</b> — {@link ModifyConstant} lowers the {@code >= 10} completion check so fewer
 *       strokes excavate the block (the cooldown/cadence constants are longs, so only the
 *       completion count is touched).</li>
 *   <li><b>Loot bias</b> — {@link ModifyArg} raises the luck fed into the loot roll inside
 *       {@code unpackLootTable}, which quality-weights the table toward rarer entries. That method
 *       is always reached from within {@code brush}, so the captured level is in place when the
 *       roll happens.</li>
 * </ul>
 */
@Mixin(BrushableBlockEntity.class)
public abstract class BrushableBlockEntityMeticulousMixin {

    @Unique
    private int meridian$meticulousLevel;

    @Inject(method = "brush", at = @At("HEAD"))
    private void meridian$captureMeticulousLevel(long gameTime, Player player, Direction direction,
                                                 CallbackInfoReturnable<Boolean> cir) {
        meridian$meticulousLevel = meridian$meticulousLevel(player);
    }

    @ModifyConstant(method = "brush", constant = @Constant(intValue = BrushEnchantMath.BRUSH_COMPLETION_BASE))
    private int meridian$meticulousCompletion(int required) {
        return BrushEnchantMath.brushCompletionCount(meridian$meticulousLevel);
    }

    @ModifyArg(method = "unpackLootTable",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;"
                             + "withLuck(F)Lnet/minecraft/world/level/storage/loot/LootParams$Builder;"))
    private float meridian$meticulousLuck(float luck) {
        return luck + BrushEnchantMath.meticulousLuckBonus(meridian$meticulousLevel);
    }

    @Unique
    private static int meridian$meticulousLevel(Player player) {
        int main = EnchantmentEffects.getEnchantmentLevel(player.getMainHandItem(), EnchantmentEffects.METICULOUS);
        int off = EnchantmentEffects.getEnchantmentLevel(player.getOffhandItem(), EnchantmentEffects.METICULOUS);
        return Math.max(main, off);
    }
}
