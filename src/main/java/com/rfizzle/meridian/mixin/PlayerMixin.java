package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.MiningEnchantMath;
import com.rfizzle.meridian.event.EnchantmentEffectHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void meridian$steadfast(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        if (!self.onGround()) {
            if (EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.STEADFAST,
                    EquipmentSlot.LEGS, EquipmentSlot.FEET) > 0) {
                cir.setReturnValue(cir.getReturnValue() * 5.0F);
            }
        }
    }

    /**
     * Grind's additive break-speed bonus, applied after vanilla (and Efficiency's
     * mining-efficiency attribute) have computed the base speed. Gated on the
     * <em>tool's own</em> speed exceeding 1.0 — the same "tool is effective" gate
     * vanilla uses for the Efficiency bonus — so a pickaxe scraping dirt gains
     * nothing and Haste cannot enable Grind on an ineffective tool. The bonus
     * mirrors the submerged/airborne penalties vanilla already applied to the base
     * speed (Grind must not double as a free Aqua Affinity); Steadfast nullifies
     * the airborne penalty on the base speed, so it exempts the bonus too.
     * Declaration order matters: this must stay below {@code meridian$steadfast}
     * so Steadfast's 5x never amplifies the additive bonus. Block hardness is a
     * per-state constant, so the position-less lookup through
     * {@link EmptyBlockGetter} reads the same value the mining code sees.
     */
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void meridian$grind(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;
        ItemStack tool = self.getMainHandItem();
        int level = EnchantmentEffects.getEnchantmentLevel(tool, EnchantmentEffects.GRIND);
        if (level <= 0) return;
        if (tool.getDestroySpeed(state) <= 1.0F) return;
        float hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        float bonus = MiningEnchantMath.grindBonus(level, hardness);
        if (bonus <= 0.0F) return;

        if (self.isEyeInFluid(FluidTags.WATER)) {
            bonus *= (float) self.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        }
        if (!self.onGround() && EnchantmentEffects.getEquippedLevel(self, EnchantmentEffects.STEADFAST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET) <= 0) {
            bonus /= 5.0F;
        }
        cir.setReturnValue(cir.getReturnValue() + bonus);
    }

    /**
     * Pinpoint hooks the {@code crit()} call inside {@code attack} — vanilla reaches it
     * only when a true critical hit both qualified and landed, so no crit-condition
     * recomputation is needed here.
     */
    @Inject(method = "attack",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/entity/player/Player;crit(Lnet/minecraft/world/entity/Entity;)V"))
    private void meridian$pinpointCrit(Entity target, CallbackInfo ci) {
        EnchantmentEffectHandler.handlePinpointCrit((Player) (Object) this, target);
    }
}
