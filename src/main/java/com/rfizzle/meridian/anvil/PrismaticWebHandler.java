package com.rfizzle.meridian.anvil;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.config.MeridianConfig;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Strips every {@code #minecraft:curse} enchantment from the left stack when a
 * {@link PrismaticWebItem} is placed in slot B. Non-curse enchantments are preserved verbatim,
 * matching Zenith's behaviour (the output carries the surviving enchantments, the curses are gone).
 *
 * <p>The handler declines the pair when any of the following is true — leaving vanilla and the
 * rest of the dispatcher chain free to handle it:
 * <ul>
 *   <li>{@code config.anvil.prismaticWebRemovesCurses} is disabled.</li>
 *   <li>Either slot is empty.</li>
 *   <li>Right slot is not a Prismatic Web.</li>
 *   <li>Left stack carries no curse enchantments (no-op would look like a free repair).</li>
 * </ul>
 *
 * <p>Only one web is consumed per click regardless of stack size. XP cost comes from
 * {@code config.anvil.prismaticWebLevelCost}.
 */
public final class PrismaticWebHandler implements AnvilHandler {

    private final Supplier<MeridianConfig> configSupplier;

    /** Production constructor — reads the live {@link Meridian#getConfig()} at claim time. */
    public PrismaticWebHandler() {
        this(Meridian::getConfig);
    }

    /** Test constructor — lets fixtures inject a specific config without mutating the singleton. */
    PrismaticWebHandler(Supplier<MeridianConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    @Override
    public Optional<AnvilResult> handle(ItemStack left, ItemStack right, Player player) {
        MeridianConfig config = configSupplier.get();
        if (config == null || !config.anvil.prismaticWebRemovesCurses) {
            return Optional.empty();
        }
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        if (!(right.getItem() instanceof PrismaticWebItem)) {
            return Optional.empty();
        }

        ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(left);
        if (existing.isEmpty() || !hasAnyCurse(existing)) {
            return Optional.empty();
        }

        ItemStack output = stripCurses(left);
        return Optional.of(new AnvilResult(output, config.anvil.prismaticWebLevelCost, 1));
    }

    static ItemStack stripCurses(ItemStack input) {
        ItemStack output = input.copy();
        EnchantmentHelper.updateEnchantments(output,
                mutable -> mutable.removeIf(holder -> holder.is(EnchantmentTags.CURSE)));
        return output;
    }

    static boolean hasAnyCurse(ItemEnchantments enchantments) {
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(EnchantmentTags.CURSE)) {
                return true;
            }
        }
        return false;
    }
}
