package com.rfizzle.meridian.library;

import net.minecraft.network.chat.Component;

/**
 * Pure-text formatter for the per-enchant row painted by the library screen. Lives in the shared
 * {@code library} package — not in {@code client/} — so the row layout is unit-testable without a
 * Minecraft client classpath; the screen resolves the localized enchantment name through vanilla's
 * translation pipeline before handing it here as a plain {@link String}.
 *
 * <p>Output shape: {@code "<name> · Lv <maxLevel> · <points> pts"} — a single line summary the
 * screen draws inside each scroll row. The Lv badge and points total are the two state values the
 * BE tracks per enchantment (DESIGN § "Enchantment Library — GUI"), surfaced flat so a player can
 * read the whole row without hovering.
 */
public final class LibraryRowFormatter {

    private LibraryRowFormatter() {}

    /**
     * Build the row text from the resolved enchantment name plus the two BE state values.
     * {@code maxLevel} is the per-enchant cap badge; {@code points} is the running pool total.
     */
    public static String format(String enchantName, int maxLevel, int points) {
        return enchantName + " \u00b7 Lv " + maxLevel + " \u00b7 " + points + " pts";
    }

    /** Convenience for callers that already hold a {@link Component} for the enchantment name. */
    public static String format(Component enchantName, int maxLevel, int points) {
        return format(enchantName.getString(), maxLevel, points);
    }
}
