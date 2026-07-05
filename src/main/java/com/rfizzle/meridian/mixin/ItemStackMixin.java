package com.rfizzle.meridian.mixin;

import com.rfizzle.meridian.MeridianRegistry;
import com.rfizzle.meridian.enchanting.EnchantmentEffects;
import com.rfizzle.meridian.enchanting.EnchantmentInfoRegistry;
import com.rfizzle.meridian.enchanting.MiningEnchantMath;
import com.rfizzle.meridian.tome.XpTomeItem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true)
    private void meridian$suppressDisabledGlint(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (self.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)) return;
        if (meridian$allDisabled(self.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                && meridian$allDisabled(self.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void meridian$xpTomeStackLimit(CallbackInfoReturnable<Integer> cir) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.getItem() instanceof XpTomeItem) {
            if (self.getOrDefault(MeridianRegistry.STORED_XP, 0) > 0) {
                cir.setReturnValue(1);
            }
        }
    }

    /**
     * The vanilla mining-tier ladder Adamant climbs. Gold sits beside wood (same
     * harvest tier), so it maps to index 0 rather than appearing on the ladder.
     */
    @Unique
    private static final List<Tiers> meridian$TIER_LADDER =
            List.of(Tiers.WOOD, Tiers.STONE, Tiers.IRON, Tiers.DIAMOND, Tiers.NETHERITE);

    /**
     * Adamant: lets a pickaxe harvest blocks above its material's tier. Only upgrades a
     * {@code false} verdict — vanilla-correct tools are untouched — and only for
     * pickaxe-mineable blocks, so a command-applied Adamant on other gear stays inert.
     * Tools whose {@link Tier} is not a vanilla {@link Tiers} rung (modded materials)
     * are skipped rather than guessed at.
     */
    @Inject(method = "isCorrectToolForDrops", at = @At("RETURN"), cancellable = true)
    private void meridian$adamantTierBoost(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        ItemStack self = (ItemStack) (Object) this;
        if (!(self.getItem() instanceof PickaxeItem pickaxe)) return;
        if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE)) return;
        int level = EnchantmentEffects.getEnchantmentLevel(self, EnchantmentEffects.ADAMANT);
        if (level <= 0) return;

        Tier tier = pickaxe.getTier();
        int baseIndex = tier == Tiers.GOLD ? 0 : meridian$TIER_LADDER.indexOf(tier);
        if (baseIndex < 0) return;

        int effectiveIndex = MiningEnchantMath.adamantEffectiveTierIndex(
                baseIndex, level, meridian$TIER_LADDER.size() - 1);
        if (!state.is(meridian$TIER_LADDER.get(effectiveIndex).getIncorrectBlocksForDrops())) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean meridian$allDisabled(ItemEnchantments enchantments) {
        if (enchantments.isEmpty()) return true;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (EnchantmentInfoRegistry.getInfo(entry.getKey()).enabled()) return false;
        }
        return true;
    }
}
