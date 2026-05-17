package com.rfizzle.meridian.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * T-2.5.3 — exposes the three private fields on vanilla {@link EnchantmentMenu} that
 * {@link com.rfizzle.meridian.enchanting.MeridianEnchantmentMenu} needs to drive
 * stat-aware slot recomputation: the input+lapis {@link Container}, the per-menu
 * {@link RandomSource}, and the {@link DataSlot} carrying the enchantment seed.
 *
 * <p>Method names are prefixed with {@code meridian$} per /dev-companion's mixin rules —
 * the explicit {@link Accessor#value()} tells Mixin which field to bind (the prefixed name is
 * otherwise unparseable as a getter).
 */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Accessor("enchantSlots")
    Container meridian$getEnchantSlots();

    @Accessor("random")
    RandomSource meridian$getRandom();

    @Accessor("enchantmentSeed")
    DataSlot meridian$getEnchantmentSeed();
}
