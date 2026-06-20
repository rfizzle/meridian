package com.rfizzle.meridian.compat.client;

import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the enchanted-book stacks (one per attainable level) that stand in for an enchantment in
 * the browser.
 *
 * <p>Registering these as the recipe <em>output</em> in each viewer is what lets a player look up
 * an enchanted book — "show recipe" in JEI / EMI / REI — and land on its browser entry, where the
 * per-level Eterna gate is shown. Every level is registered because the viewers index enchanted
 * books by their stored level, so a single representative would only match a book of that one
 * level.
 */
public final class EnchantmentBrowserBooks {

    private EnchantmentBrowserBooks() {
    }

    public static List<ItemStack> forRecord(EnchantmentBrowserRecord record) {
        Holder<Enchantment> ench = record.ench();
        int maxLevel = Math.max(record.maxLevel(), 1);
        List<ItemStack> books = new ArrayList<>(maxLevel);
        for (int level = 1; level <= maxLevel; level++) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            stored.set(ench, level);
            book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
            books.add(book);
        }
        return books;
    }
}
