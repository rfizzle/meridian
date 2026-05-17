package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheep.class)
public abstract class SheepMixin {

    @Shadow
    public abstract DyeColor getColor();

    @Shadow
    public abstract void setColor(DyeColor color);

    @Shadow
    public abstract void setSheared(boolean sheared);

    @Unique
    private DyeColor meridian$originalColor;

    @Inject(method = "mobInteract",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Sheep;shear(Lnet/minecraft/sounds/SoundSource;)V"))
    private void meridian$preShear(Player player, InteractionHand hand,
                                 CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack shears = player.getItemInHand(hand);
        meridian$originalColor = this.getColor();

        if (EnchantmentEffects.getEnchantmentLevel(shears, EnchantmentEffects.PRISMATIC) > 0) {
            DyeColor randomColor = DyeColor.byId(player.getRandom().nextInt(16));
            this.setColor(randomColor);
        }
    }

    @Inject(method = "mobInteract",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Sheep;shear(Lnet/minecraft/sounds/SoundSource;)V",
                    shift = At.Shift.AFTER))
    private void meridian$postShear(Player player, InteractionHand hand,
                                  CallbackInfoReturnable<InteractionResult> cir) {
        Sheep self = (Sheep) (Object) this;
        ItemStack shears = player.getItemInHand(hand);

        if (meridian$originalColor != null) {
            this.setColor(meridian$originalColor);
            meridian$originalColor = null;
        }

        int growthLevel = EnchantmentEffects.getEnchantmentLevel(shears, EnchantmentEffects.RENEWAL);
        if (growthLevel > 0 && self.getRandom().nextFloat() < 0.5f) {
            this.setSheared(false);
        }
    }
}
