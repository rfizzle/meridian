package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.event.GrapnelHandler;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Grapnel's reel-in hook: {@code FishingHook#retrieve} runs when the player pulls the rod
 * back in. Before vanilla drags any hooked entity, {@link GrapnelHandler} checks whether
 * the hook is lodged in a block and, if so, pulls the player toward it instead.
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

    @Inject(method = "retrieve", at = @At("HEAD"))
    private void meridian$grapnelPull(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        GrapnelHandler.tryPull((FishingHook) (Object) this, stack);
    }
}
