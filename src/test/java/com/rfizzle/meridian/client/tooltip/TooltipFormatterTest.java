package com.rfizzle.meridian.client.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.enchantment.Enchantments;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * T-8.2.1 — pins the pure over-leveled lookup and the config-hex → {@link TextColor} parser that
 * {@code OverLeveledTooltipHandler} delegates to. The listener itself exercises the Fabric client
 * event bus and is covered by in-dev QA rather than junit.
 *
 * <p>T-8.2.2 — also pins the pure book-tooltip suppression helper that the handler delegates to
 * when {@code config.display.showBookTooltips==false}.
 */
class TooltipFormatterTest {

    @Test
    void isOverLeveled_sharpnessSevenExceedsVanillaCap() {
        // Iteration 1's BeyondEnchant absorption raises Sharpness from 5 → 7. At level 7 the
        // tooltip must flip to the overleveled color.
        assertTrue(TooltipFormatter.isOverLeveled(Enchantments.SHARPNESS, 7));
    }

    @Test
    void isOverLeveled_sharpnessFiveStaysVanilla() {
        // Exact vanilla cap must not trigger the recolor — BeyondEnchant-absorbed iterations
        // still leave level 5 at the vanilla tint.
        assertFalse(TooltipFormatter.isOverLeveled(Enchantments.SHARPNESS, 5));
    }

    @Test
    void isOverLeveled_sharpnessBelowCapStaysVanilla() {
        assertFalse(TooltipFormatter.isOverLeveled(Enchantments.SHARPNESS, 1));
    }

    @Test
    void isOverLeveled_mendingTwoExceedsVanillaCap() {
        // Mending ships at vanilla 1 and Iteration 1 raises it to 5; anything above 1 is a
        // soft signal that the foreign-enchantment override or a library pull landed.
        assertTrue(TooltipFormatter.isOverLeveled(Enchantments.MENDING, 2));
    }

    @Test
    void isOverLeveled_unknownEnchantmentNeverColors() {
        // Custom enchantments (meridian:*, yigd:soulbound, …) are absent from the MVP
        // cap map — a level-5 Soulbound must not be treated as over-leveled because there's no
        // vanilla ceiling to exceed.
        assertFalse(TooltipFormatter.isOverLeveled(Enchantments.SOUL_SPEED, 3));
    }

    @Test
    void vanillaCaps_covers16BeyondEnchantTargets() {
        // Exact roster the DESIGN's Iteration 1 section pins; regression guard against a future
        // edit silently dropping or adding entries.
        assertEquals(16, TooltipFormatter.VANILLA_CAPS.size());
        assertEquals(5, TooltipFormatter.VANILLA_CAPS.get(Enchantments.SHARPNESS));
        assertEquals(5, TooltipFormatter.VANILLA_CAPS.get(Enchantments.SMITE));
        assertEquals(5, TooltipFormatter.VANILLA_CAPS.get(Enchantments.BANE_OF_ARTHROPODS));
        assertEquals(5, TooltipFormatter.VANILLA_CAPS.get(Enchantments.EFFICIENCY));
        assertEquals(4, TooltipFormatter.VANILLA_CAPS.get(Enchantments.PROTECTION));
        assertEquals(4, TooltipFormatter.VANILLA_CAPS.get(Enchantments.BLAST_PROTECTION));
        assertEquals(4, TooltipFormatter.VANILLA_CAPS.get(Enchantments.PROJECTILE_PROTECTION));
        assertEquals(4, TooltipFormatter.VANILLA_CAPS.get(Enchantments.FIRE_PROTECTION));
        assertEquals(4, TooltipFormatter.VANILLA_CAPS.get(Enchantments.FEATHER_FALLING));
        assertEquals(3, TooltipFormatter.VANILLA_CAPS.get(Enchantments.UNBREAKING));
        assertEquals(3, TooltipFormatter.VANILLA_CAPS.get(Enchantments.FORTUNE));
        assertEquals(3, TooltipFormatter.VANILLA_CAPS.get(Enchantments.LOOTING));
        assertEquals(1, TooltipFormatter.VANILLA_CAPS.get(Enchantments.MENDING));
        assertEquals(5, TooltipFormatter.VANILLA_CAPS.get(Enchantments.POWER));
        assertEquals(2, TooltipFormatter.VANILLA_CAPS.get(Enchantments.PUNCH));
        assertEquals(3, TooltipFormatter.VANILLA_CAPS.get(Enchantments.RESPIRATION));
    }

    @Test
    void vanillaCap_returnsNullForUntrackedEnchantment() {
        // Null signals "no cap tracked" — callers must distinguish this from a numeric cap.
        assertNull(TooltipFormatter.vanillaCap(Enchantments.SOUL_SPEED));
    }

    @Test
    void parseColor_validHex_returnsMatchingTextColor() {
        TextColor color = TooltipFormatter.parseColor("#FF6600");
        assertNotNull(color);
        assertEquals(0xFF6600, color.getValue());
    }

    @Test
    void parseColor_customHex_returnsMatchingTextColor() {
        TextColor color = TooltipFormatter.parseColor("#1A2B3C");
        assertEquals(0x1A2B3C, color.getValue());
    }

    @Test
    void parseColor_invalidHex_fallsBackToDefault() {
        // Defence in depth: T-1.3.3's config validator already coerces malformed hex to the
        // default, but if a bogus value ever reaches the listener (e.g. concurrent mutation
        // between load and read), paint with the documented fallback instead of a zero color.
        assertSame(TooltipFormatter.DEFAULT_COLOR, TooltipFormatter.parseColor("not-hex"));
    }

    @Test
    void parseColor_nullInput_fallsBackToDefault() {
        assertSame(TooltipFormatter.DEFAULT_COLOR, TooltipFormatter.parseColor(null));
    }

    @Test
    void defaultColor_matchesDesignHex() {
        // Pins the documented #FF6600 fallback — the DESIGN and config validator both depend on
        // this exact integer; changing it here without propagating would visibly shift the
        // fallback color in-game.
        assertEquals(0xFF6600, TooltipFormatter.DEFAULT_COLOR.getValue());
    }

    @Test
    void suppressBookLines_flagTrue_preservesAllLines() {
        // T-8.2.2: when operators leave showBookTooltips at its default (true), a stored-book
        // tooltip must pass through untouched so vanilla's enchant lines stay visible.
        List<Component> lines = new ArrayList<>(List.of(
                Component.literal("Enchanted Book"),
                Component.literal("Sharpness V"),
                Component.literal("Unbreaking III")
        ));
        List<Component> bookLines = List.of(
                Component.literal("Sharpness V"),
                Component.literal("Unbreaking III")
        );

        TooltipFormatter.suppressBookLines(lines, bookLines, true);

        assertEquals(3, lines.size());
        assertEquals("Enchanted Book", lines.get(0).getString());
        assertEquals("Sharpness V", lines.get(1).getString());
        assertEquals("Unbreaking III", lines.get(2).getString());
    }

    @Test
    void suppressBookLines_flagFalse_stripsStoredBookLines() {
        // T-8.2.2: flag=false must drop only the enchantment rows. Non-enchant rows (name, lore)
        // are not passed in as bookLines and must survive — we're suppressing the per-level
        // enchant listing, not nuking the whole tooltip.
        List<Component> lines = new ArrayList<>(List.of(
                Component.literal("Enchanted Book"),
                Component.literal("Sharpness V"),
                Component.literal("Unbreaking III")
        ));
        List<Component> bookLines = List.of(
                Component.literal("Sharpness V"),
                Component.literal("Unbreaking III")
        );

        TooltipFormatter.suppressBookLines(lines, bookLines, false);

        assertEquals(1, lines.size());
        assertEquals("Enchanted Book", lines.get(0).getString());
    }

    @Test
    void suppressBookLines_flagFalse_bookOnlyTooltipEmptiesOut() {
        // Tests the acceptance's "Flag … false → empty list" case directly: when the only lines
        // in the tooltip are the stored-book enchant rows, suppression empties the list.
        List<Component> lines = new ArrayList<>(List.of(
                Component.literal("Sharpness V"),
                Component.literal("Unbreaking III")
        ));
        List<Component> bookLines = List.of(
                Component.literal("Sharpness V"),
                Component.literal("Unbreaking III")
        );

        TooltipFormatter.suppressBookLines(lines, bookLines, false);

        assertTrue(lines.isEmpty());
    }

    @Test
    void suppressBookLines_flagFalse_matchesByRenderedString() {
        // The Fabric callback hands us Components whose Style may have been mutated by earlier
        // listeners (e.g. the over-leveled recolor) — equals() won't match. Matching by
        // getString() (rendered text) is the documented strategy.
        List<Component> lines = new ArrayList<>(List.of(
                Component.literal("Sharpness V").withStyle(net.minecraft.ChatFormatting.GRAY),
                Component.literal("Lore line")
        ));
        List<Component> bookLines = List.of(Component.literal("Sharpness V"));

        TooltipFormatter.suppressBookLines(lines, bookLines, false);

        assertEquals(1, lines.size());
        assertEquals("Lore line", lines.get(0).getString());
    }

    @Test
    void suppressBookLines_flagFalse_emptyBookLinesIsNoOp() {
        // An item with STORED_ENCHANTMENTS=EMPTY still reaches the handler — ensure the helper
        // short-circuits rather than iterating an empty set and accidentally clearing the list.
        List<Component> lines = new ArrayList<>(List.of(
                Component.literal("Enchanted Book")
        ));

        TooltipFormatter.suppressBookLines(lines, List.of(), false);

        assertEquals(1, lines.size());
        assertEquals("Enchanted Book", lines.get(0).getString());
    }

    @Test
    void suppressBookLines_flagFalse_emptyInputIsNoOp() {
        // Defensive: if the tooltip list is already empty (some mods wipe it), don't throw.
        List<Component> lines = new ArrayList<>();
        List<Component> bookLines = List.of(Component.literal("Sharpness V"));

        TooltipFormatter.suppressBookLines(lines, bookLines, false);

        assertTrue(lines.isEmpty());
    }
}
