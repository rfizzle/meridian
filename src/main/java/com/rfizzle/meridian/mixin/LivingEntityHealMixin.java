package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealMixin {

    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float meridian$lifeMending(float amount) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || amount <= 0) return amount;

        List<EquipmentSlot> candidates = new ArrayList<>();
        List<Integer> levels = new ArrayList<>();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = self.getItemBySlot(slot);
            if (stack.isDamaged()) {
                int level = EnchantmentEffects.getEnchantmentLevel(stack, EnchantmentEffects.VITAL_MEND);
                if (level > 0) {
                    candidates.add(slot);
                    levels.add(level);
                }
            }
        }

        if (candidates.isEmpty()) return amount;

        int idx = self.getRandom().nextInt(candidates.size());
        EquipmentSlot chosenSlot = candidates.get(idx);
        int level = levels.get(idx);
        ItemStack stack = self.getItemBySlot(chosenSlot);

        float durabilityPerHp = (float) (1 << level);
        float costPerDurability = 1.0f / durabilityPerHp;
        int maxRestore = Mth.floor(amount / costPerDurability);
        maxRestore = Math.min(maxRestore, stack.getDamageValue());

        if (maxRestore > 0) {
            stack.setDamageValue(stack.getDamageValue() - maxRestore);
            return amount - maxRestore * costPerDurability;
        }

        return amount;
    }
}
