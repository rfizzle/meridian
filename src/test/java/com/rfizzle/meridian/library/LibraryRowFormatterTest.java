package com.rfizzle.meridian.library;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * T-4.4.3 — pins the per-row text the library screen draws. Pure-string formatting; no Minecraft
 * registries, no client classes, so this runs without a {@code Bootstrap.bootStrap()} dance.
 */
class LibraryRowFormatterTest {

    @Test
    void format_buildsFlatRowFromName_maxLevel_andPoints() {
        // The DESIGN-pinned example: {Sharpness, maxLevels=5, points=6144} → flat single-line row.
        assertEquals("Sharpness \u00b7 Lv 5 \u00b7 6144 pts",
                LibraryRowFormatter.format("Sharpness", 5, 6144));
    }

    @Test
    void format_componentOverloadResolvesToString() {
        // The screen will hand a translated Component for the enchantment name; the overload must
        // funnel through the same template as the raw-String path so the row layout is identical.
        Component name = Component.literal("Unbreaking");
        assertEquals(LibraryRowFormatter.format("Unbreaking", 3, 16),
                LibraryRowFormatter.format(name, 3, 16));
    }

    @Test
    void format_zeroPointsAndZeroMaxLevelStillRender() {
        // The screen filters points > 0 before formatting, but the formatter itself must not gate
        // on values — keeping it a pure layout helper means the screen owns the visibility rules.
        assertEquals("Mending \u00b7 Lv 0 \u00b7 0 pts",
                LibraryRowFormatter.format("Mending", 0, 0));
    }
}
