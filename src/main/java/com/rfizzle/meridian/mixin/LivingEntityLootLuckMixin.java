package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.CombatEnchantMath;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Fortuity: raises the luck fed into the victim's death-loot rolls, which drives loot-pool
 * {@code quality} weighting — better entries become likelier without adding rolls. Vanilla
 * already routes the killing player's luck attribute through {@code dropFromLootTable}'s
 * {@code withLuck} call, so the enchantment only tops up that argument. The hook has to be
 * a mixin rather than an {@code AFTER_DEATH} handler because Fabric's death event fires
 * after the drops have already rolled.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLootLuckMixin {

    @Shadow
    protected Player lastHurtByPlayer;

    @ModifyArg(method = "dropFromLootTable",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;withLuck(F)Lnet/minecraft/world/level/storage/loot/LootParams$Builder;"))
    private float meridian$fortuityLuck(float luck) {
        Player killer = this.lastHurtByPlayer;
        if (killer == null) return luck;

        int level = EnchantmentEffects.getEnchantmentLevel(killer.getMainHandItem(), EnchantmentEffects.FORTUITY);
        if (level <= 0) return luck;

        return luck + CombatEnchantMath.fortuityLuckBonus(level);
    }
}
