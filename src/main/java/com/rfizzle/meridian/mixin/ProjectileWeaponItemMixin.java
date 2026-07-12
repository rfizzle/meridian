package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.RangedEnchantMath;
import com.rfizzle.meridian.event.ProjectileEnchantmentHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Hooks the vanilla shoot loop for two ranged enchantments. Volley looses its extra arrows once
 * the normal shot has been fired (TAIL); Curse of Wavering widens the firing inaccuracy before the
 * shot is loosed (HEAD). Both fire for every projectile weapon but gate on their own enchantment,
 * confined to the right item types by their tags.
 */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {

    /**
     * Curse of Wavering: adds firing inaccuracy so arrows scatter within a wider cone, growing per
     * level. Captures the target method's arguments to read the {@code weapon} the shot came from;
     * a clean weapon is returned unchanged.
     */
    @ModifyVariable(method = "shoot", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private float meridian$waveringSpread(float inaccuracy, ServerLevel serverLevel, LivingEntity shooter,
                                          InteractionHand hand, ItemStack weapon, List<ItemStack> projectiles,
                                          float velocity, float inaccuracyArg, boolean crit, LivingEntity target) {
        int level = EnchantmentEffects.getEnchantmentLevel(weapon, EnchantmentEffects.CURSE_OF_WAVERING);
        if (level <= 0) return inaccuracy;
        return RangedEnchantMath.waveringInaccuracy(level, inaccuracy);
    }

    @Inject(method = "shoot", at = @At("TAIL"))
    private void meridian$onShoot(ServerLevel serverLevel, LivingEntity shooter, InteractionHand hand,
                                  ItemStack weapon, List<ItemStack> projectiles, float velocity,
                                  float inaccuracy, boolean crit, LivingEntity target, CallbackInfo ci) {
        ItemStack ammo = projectiles.isEmpty() ? ItemStack.EMPTY : projectiles.get(0);
        ProjectileEnchantmentHandler.handleVolley(
                serverLevel, shooter, weapon, ammo, velocity, inaccuracy, crit, projectiles.size());
    }
}
