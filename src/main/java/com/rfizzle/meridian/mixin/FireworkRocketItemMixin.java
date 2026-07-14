package com.rfizzle.meridian.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.TraversalEnchantMath;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Thrift (elytra): using a firework rocket to boost a gliding wearer sometimes leaves the rocket
 * unspent. Firework consumption is a plain {@code itemInHand.consume(1, player)} call inside
 * {@code FireworkRocketItem.use}, on the server-only branch that spawns the boost rocket — a
 * different class and moment than {@link FireworkRocketMixin}, which scales the already-spawned
 * rocket's boost. Wrapping only the consume call means Thrift decides whether the rocket is spent
 * and never touches the boost's strength or duration, so it composes cleanly with Tailwind rather
 * than overlapping it.
 *
 * <p>The refund roll runs only where vanilla already consumes — inside the {@code !level.isClientSide}
 * branch — so it is server-authoritative and never desyncs client prediction: the boost rocket is
 * spawned and the glide plays out identically whether or not the rocket is refunded.
 */
@Mixin(FireworkRocketItem.class)
public abstract class FireworkRocketItemMixin {

    @WrapOperation(
            method = "use",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;consume"
                            + "(ILnet/minecraft/world/entity/LivingEntity;)V"))
    private void meridian$thriftRefund(ItemStack stack, int amount, LivingEntity entity,
                                       Operation<Void> original) {
        if (entity instanceof Player player) {
            int level = EnchantmentEffects.getEquippedLevel(player,
                    EnchantmentEffects.THRIFT, EquipmentSlot.CHEST);
            if (level > 0
                    && player.getRandom().nextFloat() < TraversalEnchantMath.thriftRefundChance(level)) {
                // Refund: leave the rocket in the stack, skipping vanilla's consume entirely.
                return;
            }
        }
        original.call(stack, amount, entity);
    }
}
