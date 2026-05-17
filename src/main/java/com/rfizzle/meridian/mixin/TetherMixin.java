package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.event.TetherHandler;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Inventory.class)
public abstract class TetherMixin {

    @Shadow
    @Final
    public Player player;

    @Shadow
    public abstract int getContainerSize();

    @Shadow
    public abstract ItemStack getItem(int slot);

    @Shadow
    public abstract void setItem(int slot, ItemStack stack);

    @Inject(method = "dropAll", at = @At("HEAD"))
    private void meridian$saveTetheredItems(CallbackInfo ci) {
        if (player.level().isClientSide()) return;

        List<ItemStack> saved = new ArrayList<>();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (!stack.isEmpty() && EnchantmentEffects.getEnchantmentLevel(stack, EnchantmentEffects.TETHER) > 0) {
                saved.add(stack.copy());
                setItem(i, ItemStack.EMPTY);
            }
        }

        if (!saved.isEmpty()) {
            TetherHandler.saveTetheredItems(player.getUUID(), saved);
        }
    }
}
