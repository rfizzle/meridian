package com.rfizzle.meridian.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Sheep.class)
public abstract class SheepMixin {

    @Shadow
    public abstract DyeColor getColor();

    @Shadow
    public abstract void setColor(DyeColor color);

    @Shadow
    public abstract void setSheared(boolean sheared);

    /**
     * Wraps the {@code shear} call inside {@code mobInteract} to fold Prismatic and Renewal into one
     * exception-safe hook. Prismatic temporarily rerolls the sheep's color so the dropped wool is a
     * random color, then restores the real color in a {@code finally} — a two-injector pre/post split
     * cannot guarantee that, so a thrown {@code shear()} used to strand the sheep at the reroll color.
     * Renewal runs only after a successful shear.
     */
    @WrapOperation(method = "mobInteract",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Sheep;shear(Lnet/minecraft/sounds/SoundSource;)V"))
    private void meridian$prismaticShear(Sheep self, SoundSource soundSource, Operation<Void> original,
                                         Player player, InteractionHand hand) {
        ItemStack shears = player.getItemInHand(hand);

        DyeColor originalColor = null;
        if (EnchantmentEffects.getEnchantmentLevel(shears, EnchantmentEffects.PRISMATIC) > 0) {
            originalColor = this.getColor();
            this.setColor(DyeColor.byId(player.getRandom().nextInt(16)));
        }
        try {
            original.call(self, soundSource);
        } finally {
            if (originalColor != null) {
                this.setColor(originalColor);
            }
        }

        if (EnchantmentEffects.getEnchantmentLevel(shears, EnchantmentEffects.RENEWAL) > 0
                && self.getRandom().nextFloat() < 0.5f) {
            this.setSheared(false);
        }
    }
}
