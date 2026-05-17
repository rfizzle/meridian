package com.rfizzle.meridian.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure logic shared by {@code OverLeveledTooltipHandler} — no Fabric-client deps, so tests can
 * exercise the cap lookup and color parser without booting the client or registering events.
 *
 * <p>T-8.2.1 ships the MVP hardcoded cap map (the 16 enchantments Iteration 1 will raise beyond
 * vanilla via BeyondEnchant absorption). Iteration 1 replaces this static map with a config-fed
 * provider so operators can raise or lower individual caps without editing the jar.
 */
public final class TooltipFormatter {

    /**
     * Vanilla level caps for enchantments the pack can exceed. Keys are restricted to the 16
     * BeyondEnchant targets — any enchantment absent from this map never triggers over-leveled
     * coloring (additions ship in Iteration 1 alongside the config-fed override hook).
     */
    public static final Map<ResourceKey<Enchantment>, Integer> VANILLA_CAPS = Map.ofEntries(
            Map.entry(Enchantments.SHARPNESS, 5),
            Map.entry(Enchantments.SMITE, 5),
            Map.entry(Enchantments.BANE_OF_ARTHROPODS, 5),
            Map.entry(Enchantments.EFFICIENCY, 5),
            Map.entry(Enchantments.PROTECTION, 4),
            Map.entry(Enchantments.BLAST_PROTECTION, 4),
            Map.entry(Enchantments.PROJECTILE_PROTECTION, 4),
            Map.entry(Enchantments.FIRE_PROTECTION, 4),
            Map.entry(Enchantments.FEATHER_FALLING, 4),
            Map.entry(Enchantments.UNBREAKING, 3),
            Map.entry(Enchantments.FORTUNE, 3),
            Map.entry(Enchantments.LOOTING, 3),
            Map.entry(Enchantments.MENDING, 1),
            Map.entry(Enchantments.POWER, 5),
            Map.entry(Enchantments.PUNCH, 2),
            Map.entry(Enchantments.RESPIRATION, 3)
    );

    /** Fallback color when {@code config.display.overLeveledColor} fails to parse. */
    public static final TextColor DEFAULT_COLOR = TextColor.fromRgb(0xFF6600);

    private TooltipFormatter() {}

    /**
     * @return the vanilla cap for {@code key}, or {@code null} when the enchantment is outside the
     *         pack's raised-cap roster (i.e. the line should never be recolored).
     */
    public static Integer vanillaCap(ResourceKey<Enchantment> key) {
        return VANILLA_CAPS.get(key);
    }

    /** @return {@code true} iff {@code key} has a hardcoded cap and {@code level} exceeds it. */
    public static boolean isOverLeveled(ResourceKey<Enchantment> key, int level) {
        Integer cap = VANILLA_CAPS.get(key);
        return cap != null && level > cap;
    }

    /**
     * Parses the config-supplied hex string ({@code #RRGGBB}) into a {@link TextColor}. The
     * config validator in T-1.3.3 already falls back to {@code #FF6600} on mismatch, so this
     * method is a second-layer guard: any {@link TextColor#parseColor} failure (e.g. concurrent
     * mutation that slipped past validation) resolves to {@link #DEFAULT_COLOR} rather than
     * painting the line with whatever happens to decode next.
     */
    public static TextColor parseColor(String hex) {
        if (hex == null) return DEFAULT_COLOR;
        return TextColor.parseColor(hex).result().orElse(DEFAULT_COLOR);
    }

    /**
     * Strips every {@code line} in {@code lines} whose rendered string equals one of
     * {@code bookLines} when the operator has disabled {@code config.display.showBookTooltips}.
     * No-op when the flag is true — lines pass through untouched. Mutates {@code lines} in place
     * to match the {@link net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback} contract.
     *
     * <p>Matching is by rendered string, not by identity, because vanilla produces a fresh
     * {@code Component} each tooltip frame and the style attached by earlier listeners (e.g. the
     * over-leveled recolor in {@code OverLeveledTooltipHandler}) may diverge from the component
     * we synthesize for the book-lines list.
     */
    public static void suppressBookLines(List<Component> lines, List<Component> bookLines, boolean showBookTooltips) {
        if (showBookTooltips || bookLines.isEmpty() || lines.isEmpty()) return;
        Set<String> rendered = new HashSet<>(bookLines.size());
        for (Component bookLine : bookLines) {
            rendered.add(bookLine.getString());
        }
        lines.removeIf(line -> rendered.contains(line.getString()));
    }
}
