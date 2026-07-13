package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ironclasp's drop-key half: an Ironclasp item cannot be knocked loose by the drop key.
 * {@link ServerPlayer#drop(boolean)} is the server-side handler for both {@code DROP_ITEM}
 * (Q) and {@code DROP_ALL_ITEMS} (Ctrl+Q), acting on the selected hotbar stack. Cancelling at
 * {@code HEAD} — before {@code removeFromSelected} runs — leaves the server inventory
 * untouched, so there is no partial-drop or slot-desync on the server side.
 *
 * <p>The vanilla client ({@code LocalPlayer.drop}) optimistically removes the stack locally the
 * moment the key is pressed, so a bare cancel would leave the client briefly showing an empty
 * slot. Re-broadcasting the full container state corrects that mispredict immediately.
 *
 * <p>This is deliberately scoped to the drop key only: death drops (Tether's domain) and
 * inventory-screen drags are separate paths and are intentionally left alone.
 */
@Mixin(ServerPlayer.class)
public abstract class IronclaspDropMixin {

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void meridian$ironclaspBlocksDropKey(boolean dropEntireStack, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ItemStack selected = self.getInventory().getSelected();
        if (EnchantmentEffects.getEnchantmentLevel(selected, EnchantmentEffects.IRONCLASP) <= 0) return;

        // Undo the client's optimistic removal; the server never mutated anything.
        self.containerMenu.broadcastFullState();
        cir.setReturnValue(false);
    }
}
