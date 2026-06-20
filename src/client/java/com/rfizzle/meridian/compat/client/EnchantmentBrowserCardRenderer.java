package com.rfizzle.meridian.compat.client;

import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Shared face renderer for one enchantment card in the JEI / EMI / REI "Enchantments" browser.
 *
 * <p>Centralising the layout here keeps the three viewers consistent: each viewer supplies its own
 * native enchanted-book slot anchored on the left (at {@link #SLOT_X}, {@link #SLOT_Y}) — the
 * click-to-navigate target — then defers the stacked stat lines to {@link #draw}:
 *
 * <pre>
 *   ┌──┐  Vital Mend
 *   │▓▓│  Max Level: III
 *   │▓▓│  Eterna: 25–299
 *   └──┘  Treasure
 *         Exclusive: mending
 * </pre>
 *
 * <p>Lines are emitted top-down and skipped when not applicable, so there are no blank rows between
 * stats. The name colour carries treasure (gold) / disabled (red-strikethrough) state; text is
 * light + shadowed to stay legible on both JEI's light panel and the dark EMI / REI panels.
 *
 * <p>JEI and REI categories are a fixed height ({@link #HEIGHT}, sized for the fullest card); EMI
 * sizes each recipe to its own {@link #height(EnchantmentBrowserRecord)} so its scroll list stays
 * tight. There is deliberately no per-card "config override" badge — every Meridian enchantment
 * ships a tuned power curve, so a "differs from vanilla" flag fired on essentially every row.
 */
public final class EnchantmentBrowserCardRenderer {

    /** Card width shared by all three viewers. */
    public static final int WIDTH = 144;

    /** Enchanted-book slot, anchored top-left as the navigation target. */
    public static final int SLOT_X = 4;
    public static final int SLOT_Y = 4;

    private static final int TEXT_X = 24;       // text column, right of the book slot
    private static final int PAD_TOP = 4;
    private static final int PAD_BOTTOM = 4;
    private static final int PAD_RIGHT = 6;
    private static final int LINE_STEP = 10;    // baseline-to-baseline
    private static final int LINE_HEIGHT = 9;   // glyph height
    private static final int SLOT_SIZE = 16;
    private static final int MAX_LINES = 5;     // name + max level + eterna + treasure + exclusive

    /** Fixed height for the fixed-size JEI / REI categories — sized for the fullest card. */
    public static final int HEIGHT = heightForLines(MAX_LINES);

    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_NAME_TREASURE = 0xFFFFC24B;
    private static final int COLOR_NAME_DISABLED = 0xFFFF6B6B;
    private static final int COLOR_MAX_LEVEL = 0xFFA8C0EE;
    private static final int COLOR_ETERNA = 0xFF7BE0A0;
    private static final int COLOR_TREASURE = 0xFFFFC24B;
    private static final int COLOR_EXCLUSIVE = 0xFF6FE0E0;

    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private EnchantmentBrowserCardRenderer() {
    }

    /**
     * Draws the stat lines with the card's top-left corner at ({@code x}, {@code y}). The caller is
     * responsible for the enchanted-book slot at ({@code x + SLOT_X}, {@code y + SLOT_Y}).
     */
    public static void draw(GuiGraphics graphics, Font font, int x, int y, EnchantmentBrowserRecord record) {
        boolean disabled = !record.isEnabled();
        int nameColor = disabled ? COLOR_NAME_DISABLED
                : record.isTreasure() ? COLOR_NAME_TREASURE : COLOR_NAME;

        int line = 0;
        drawLine(graphics, font, x, y, line++, record.ench().value().description().getString(), nameColor, disabled);
        drawLine(graphics, font, x, y, line++,
                Component.translatable("gui.meridian.enchant_info.max_level", roman(record.maxLevel())).getString(),
                COLOR_MAX_LEVEL, false);

        if (!record.powerWindows().isEmpty()) {
            // Power windows are in enchanting-level units; the card phrases them as the Eterna a
            // player must reach (level = Eterna × LEVELS_PER_ETERNA), so convert back here.
            int lo = RealEnchantmentHelper.powerToEterna(record.powerWindows().get(0)[0]);
            int hi = RealEnchantmentHelper.powerToEterna(
                    record.powerWindows().get(record.powerWindows().size() - 1)[1]);
            drawLine(graphics, font, x, y, line++,
                    Component.translatable("gui.meridian.enchant_info.eterna_gate", lo + "–" + hi).getString(),
                    COLOR_ETERNA, false);
        }
        if (record.isTreasure()) {
            drawLine(graphics, font, x, y, line++,
                    Component.translatable("gui.meridian.enchant_info.treasure").getString(), COLOR_TREASURE, false);
        }
        if (!record.exclusiveSetNames().isEmpty()) {
            drawLine(graphics, font, x, y, line,
                    Component.translatable("gui.meridian.enchant_info.exclusive_short",
                            String.join(", ", record.exclusiveSetNames())).getString(),
                    COLOR_EXCLUSIVE, false);
        }
    }

    /** Dynamic card height for the per-recipe EMI display: only the lines actually shown. */
    public static int height(EnchantmentBrowserRecord record) {
        int lines = 2; // name + max level are always present
        if (!record.powerWindows().isEmpty()) {
            lines++;
        }
        if (record.isTreasure()) {
            lines++;
        }
        if (!record.exclusiveSetNames().isEmpty()) {
            lines++;
        }
        return heightForLines(lines);
    }

    private static int heightForLines(int lines) {
        int textBottom = PAD_TOP + (lines - 1) * LINE_STEP + LINE_HEIGHT;
        return Math.max(textBottom, SLOT_Y + SLOT_SIZE) + PAD_BOTTOM;
    }

    private static void drawLine(GuiGraphics graphics, Font font, int x, int y, int index,
                                 String text, int color, boolean strikethrough) {
        int maxWidth = WIDTH - TEXT_X - PAD_RIGHT;
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, maxWidth - font.width("…")) + "…";
        }
        MutableComponent line = Component.literal(text);
        if (strikethrough) {
            line.withStyle(ChatFormatting.STRIKETHROUGH);
        }
        graphics.drawString(font, line, x + TEXT_X, y + PAD_TOP + index * LINE_STEP, color);
    }

    private static String roman(int level) {
        return (level >= 1 && level < ROMAN.length) ? ROMAN[level] : Integer.toString(level);
    }
}
