package com.rfizzle.meridian.enchanting;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure tooltip-line logic for Curse of Obscurity: work out which enchantment name lines to hide and
 * strip them from a finished tooltip. Kept in the common source set (not client) so plain JUnit can
 * exercise the line removal; {@code ObscurityTooltipHandler} is the only runtime caller.
 */
public final class ObscurityTooltipMath {

    private ObscurityTooltipMath() {}

    /**
     * The full-name strings of every enchantment on the stack except Curse of Obscurity itself —
     * the lines the curse hides. The curse's own line is kept so the player can see why the rest
     * are gone. Reads both live and stored enchantments so the hide-set is complete regardless of
     * which component vanilla rendered them from.
     */
    public static Set<String> collectHiddenNames(ItemStack stack) {
        Set<String> names = new HashSet<>();
        collect(stack.get(DataComponents.ENCHANTMENTS), names);
        collect(stack.get(DataComponents.STORED_ENCHANTMENTS), names);
        return names;
    }

    private static void collect(ItemEnchantments enchantments, Set<String> names) {
        if (enchantments == null || enchantments.isEmpty()) return;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (holder.is(EnchantmentEffects.CURSE_OF_OBSCURITY)) continue;
            names.add(Enchantment.getFullname(holder, entry.getIntValue()).getString());
        }
    }

    /**
     * Removes every tooltip line whose plain text matches one of {@code namesToHide}. Matching by
     * rendered string mirrors how the inline-description handler keys its lines, so it lines up with
     * exactly the enchantment lines vanilla added.
     */
    public static void removeMatchingLines(List<Component> lines, Set<String> namesToHide) {
        if (namesToHide.isEmpty()) return;
        lines.removeIf(line -> namesToHide.contains(line.getString()));
    }
}
