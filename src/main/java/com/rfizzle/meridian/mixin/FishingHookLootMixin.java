package com.rfizzle.meridian.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.rfizzle.meridian.event.TwinHookHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Twin Hook's yield hook. {@code FishingHook#retrieve} spawns each caught drop as an
 * {@link net.minecraft.world.entity.item.ItemEntity} via {@code Level#addFreshEntity} (ordinal 0,
 * before the separate experience-orb and {@code FISH_CAUGHT} statements), then loops to the next
 * drop. Wrapping that one call lets {@link TwinHookHandler} spawn one extra copy of the item on a
 * per-level roll without touching the XP or stat awards — so Twin Hook doubles the catch, not the
 * experience. Kept separate from {@link FishingHookMixin} (Grapnel's unrelated reel-in pull check).
 */
@Mixin(FishingHook.class)
public abstract class FishingHookLootMixin {

    @WrapOperation(method = "retrieve",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z",
                    ordinal = 0))
    private boolean meridian$twinHookDuplicate(Level level, Entity spawned, Operation<Boolean> original,
                                               @Local(argsOnly = true) ItemStack rod) {
        boolean result = original.call(level, spawned);
        TwinHookHandler.maybeDuplicate((FishingHook) (Object) this, rod, spawned);
        return result;
    }
}
