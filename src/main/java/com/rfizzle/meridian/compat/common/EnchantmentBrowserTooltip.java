package com.rfizzle.meridian.compat.common;

import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Builds the hover-tooltip body for one {@link EnchantmentBrowserRecord}, shared by the JEI, EMI,
 * and REI "Enchantments" browser categories so the three stay in sync. Sections run top to
 * bottom: name + plain-language description (resolved through a caller-supplied function,
 * because lang lookups ({@code I18n}) are client-only and this class lives in common code),
 * the Eterna-per-level table, the exclusive set (when present), a "Treasure Enchantment" tag
 * (when applicable), and last the source mod's display name (blue italic, the conventional
 * recipe-viewer footer), each separated by a blank line.
 */
public final class EnchantmentBrowserTooltip {

    private EnchantmentBrowserTooltip() {
    }

    /**
     * @param descResolver resolves an enchantment's plain-language {@code .desc} lore; when empty
     *                     the description line is omitted entirely
     */
    public static List<Component> lines(EnchantmentBrowserRecord record,
                                        Function<Holder<Enchantment>, Optional<Component>> descResolver) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(record.ench().value().description().copy().withStyle(ChatFormatting.WHITE));
        descResolver.apply(record.ench())
                .ifPresent(desc -> tooltip.add(desc.copy().withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));

        if (!record.isEnabled()) {
            tooltip.add(Component.translatable("tooltip.meridian.enchlib.disabled").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("gui.meridian.enchant_info.power_header").withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < record.powerWindows().size(); i++) {
            int level = i + 1;
            int[] window = record.powerWindows().get(i);
            // window holds enchanting-level power bounds; show the Eterna needed to reach them.
            int loEterna = RealEnchantmentHelper.powerToEterna(window[0]);
            int hiEterna = RealEnchantmentHelper.powerToEterna(window[1]);
            tooltip.add(Component.literal("Level " + level + ": Eterna " + loEterna + " - " + hiEterna).withStyle(ChatFormatting.DARK_GREEN));
        }

        if (!record.exclusiveSetNames().isEmpty()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.meridian.enchant_info.exclusive", "").withStyle(ChatFormatting.GRAY));
            for (String setName : record.exclusiveSetNames()) {
                tooltip.add(Component.literal(" - " + setName).withStyle(ChatFormatting.AQUA));
            }
        }

        if (record.isTreasure()) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.meridian.enchant_info.treasure_enchantment").withStyle(ChatFormatting.GOLD));
        }

        tooltip.add(Component.empty());
        tooltip.add(Component.literal(sourceModName(record)).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));

        return tooltip;
    }

    /** Human-readable name of the mod that registered the enchantment, falling back to its namespace. */
    private static String sourceModName(EnchantmentBrowserRecord record) {
        String namespace = record.ench().unwrapKey()
                .map(key -> key.location().getNamespace())
                .orElse("minecraft");
        return FabricLoader.getInstance().getModContainer(namespace)
                .map(container -> container.getMetadata().getName())
                .orElse(namespace);
    }
}
