package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.event.ProjectileEnchantmentHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Hooks the tail of the vanilla shoot loop so Volley can loose its extra arrows once the
 * normal shot has been fired. Fires for every projectile weapon, but the handler gates on
 * the Volley enchantment, which its item tag confines to bows.
 */
@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {

    @Inject(method = "shoot", at = @At("TAIL"))
    private void meridian$onShoot(ServerLevel serverLevel, LivingEntity shooter, InteractionHand hand,
                                  ItemStack weapon, List<ItemStack> projectiles, float velocity,
                                  float inaccuracy, boolean crit, LivingEntity target, CallbackInfo ci) {
        ItemStack ammo = projectiles.isEmpty() ? ItemStack.EMPTY : projectiles.get(0);
        ProjectileEnchantmentHandler.handleVolley(
                serverLevel, shooter, weapon, ammo, velocity, inaccuracy, crit, projectiles.size());
    }
}
