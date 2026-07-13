package com.rfizzle.meridian.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.TollExperienceMath;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Curse of Toll: the inverse of Insight/Animus. Those raise the experience a kill or broken block
 * yields; Toll skims a flat fraction off whatever experience the wearer collects from an orb, so
 * the curse's cost lands on enchanting progress itself rather than on durability or combat.
 *
 * <p>Vanilla {@link ExperienceOrb#playerTouch} lets Mending claim its share of the orb first, then
 * hands the remainder to {@code Player#giveExperiencePoints}. Wrapping that one call taxes the kept
 * experience only — never the Mending repair — and leaves every non-orb XP path ({@code /xp}, anvil
 * and table costs, other mods' grants) untouched, matching the issue's scope. {@code @WrapOperation}
 * (not {@code @Redirect}) so the wrap composes with any other mod that also touches this call.
 */
@Mixin(ExperienceOrb.class)
public abstract class TollExperienceMixin {

    @WrapOperation(
            method = "playerTouch",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private void meridian$tollReduceXp(Player player, int amount, Operation<Void> original) {
        int level = EnchantmentEffects.getEquippedLevel(player, EnchantmentEffects.CURSE_OF_TOLL,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        original.call(player, TollExperienceMath.reduce(amount, level));
    }
}
