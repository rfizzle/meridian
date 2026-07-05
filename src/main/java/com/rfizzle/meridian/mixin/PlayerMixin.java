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

    /**
     * Steadfast and Grind both adjust the {@code getDestroySpeed} return, and their order is
     * load-bearing: Steadfast's 5x must apply to the base speed <em>before</em> Grind's additive
     * bonus, or the 5x would amplify the bonus. They live in one injector so that order is a plain
     * statement sequence — {@code @Inject} has no per-injector priority, so two separate RETURN
     * injectors would order only by incidental mixin-application order, exactly the fragility this
     * fix removes.
     *
     * <p><b>Steadfast</b> restores full mining speed while airborne (nullifying vanilla's 5x airborne
     * penalty on the base speed).
     *
     * <p><b>Grind</b> adds a break-speed bonus after vanilla (and Efficiency's mining-efficiency
     * attribute) have computed the base speed. Gated on the <em>tool's own</em> speed exceeding 1.0 —
     * the same "tool is effective" gate vanilla uses for the Efficiency bonus — so a pickaxe scraping
     * dirt gains nothing and Haste cannot enable Grind on an ineffective tool. The bonus mirrors the
     * submerged/airborne penalties vanilla already applied to the base speed (Grind must not double as
     * a free Aqua Affinity); Steadfast nullifies the airborne penalty on the base speed, so it exempts
     * the bonus too. Block hardness is a per-state constant, so the position-less lookup through
     * {@link EmptyBlockGetter} reads the same value the mining code sees.
     */
    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    private void meridian$adjustDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir) {
        Player self = (Player) (Object) this;

        boolean airborne = !self.onGround();
        boolean steadfast = airborne && EnchantmentEffects.getEquippedLevel(self,
                EnchantmentEffects.STEADFAST, EquipmentSlot.LEGS, EquipmentSlot.FEET) > 0;
        if (steadfast) {
            cir.setReturnValue(cir.getReturnValue() * 5.0F);
        }

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
        if (airborne && !steadfast) {
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
