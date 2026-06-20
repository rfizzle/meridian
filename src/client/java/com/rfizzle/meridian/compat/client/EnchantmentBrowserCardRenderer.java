package com.rfizzle.meridian.compat.client;

import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Shared face renderer for one enchantment card in the JEI / EMI / REI "Enchantments" browser.
 *
 * <p>Centralising the pixel layout here is what keeps the three viewers byte-for-byte identical:
 * each viewer supplies its own native compatible-items slot widget in the right column (at
 * {@link #SLOT_X}, {@link #SLOT_Y}), then defers the name, level pips, and power/flag line to
 * {@link #draw}. The layout, in a {@value #WIDTH}×{@value #HEIGHT} card:
 *
 * <pre>
 *   Abyss Ward                ▪▪▪▪▪  [slot]   ← name (left) · level pips (right of slot)
 *   Eterna 15–60 · Treasure          [slot]   ← min/max Eterna window, then flag/exclusivity chips
 * </pre>
 *
 * <p>There is deliberately no per-card "config override" badge: every Meridian enchantment ships
 * with a tuned power curve, so a "differs from vanilla" flag fired on essentially every row and
 * carried no information. A genuine "you changed this in config" indicator would need a shipped
 * default baseline the client does not hold.
 */
public final class EnchantmentBrowserCardRenderer {

    /** Card footprint shared by all three viewers. */
    public static final int WIDTH = 144;
    public static final int HEIGHT = 26;

    /** Compatible-items slot sits in the right column, vertically centred. */
    public static final int SLOT_X = 126;
    public static final int SLOT_Y = 5;

    private static final int PIP_RIGHT = 122;   // pips right-align just left of the slot
    private static final int PIP_SIZE = 3;
    private static final int PIP_GAP = 2;
    private static final int MAX_PIPS = 10;

    private static final int COLOR_NAME = 0xFFFFFFFF;
    private static final int COLOR_TREASURE = 0xFFFCC44E;
    private static final int COLOR_DISABLED = 0xFFFF5555;
    private static final int COLOR_PIP = 0xFF8AB4F8;
    private static final int COLOR_PIP_DISABLED = 0xFF6E6E6E;
    private static final int COLOR_LINE2 = 0xFFAAAAAA;

    private EnchantmentBrowserCardRenderer() {
    }

    /**
     * Draws the card face with its top-left corner at ({@code x}, {@code y}). The caller is
     * responsible for the compatible-items slot at ({@code x + SLOT_X}, {@code y + SLOT_Y}).
     */
    public static void draw(GuiGraphics graphics, Font font, int x, int y, EnchantmentBrowserRecord record) {
        MutableComponent name = record.ench().value().description().copy();
        int nameColor = COLOR_NAME;
        if (!record.isEnabled()) {
            name.withStyle(ChatFormatting.STRIKETHROUGH);
            nameColor = COLOR_DISABLED;
        } else if (record.isTreasure()) {
            nameColor = COLOR_TREASURE;
        }
        graphics.drawString(font, name, x + 2, y + 2, nameColor);

        drawPips(graphics, x, y, record);

        MutableComponent secondLine = buildSecondLine(record);
        if (secondLine != null) {
            graphics.drawString(font, secondLine, x + 2, y + 14, COLOR_LINE2);
        }
    }

    /** One filled pip per attainable level (capped at {@value #MAX_PIPS}), right-aligned by the slot. */
    private static void drawPips(GuiGraphics graphics, int x, int y, EnchantmentBrowserRecord record) {
        int count = Math.min(Math.max(record.maxLevel(), 1), MAX_PIPS);
        int color = !record.isEnabled() ? COLOR_PIP_DISABLED
                : record.isTreasure() ? COLOR_TREASURE : COLOR_PIP;
        int step = PIP_SIZE + PIP_GAP;
        int startX = x + PIP_RIGHT - (count * step - PIP_GAP);
        int py = y + 5;
        for (int i = 0; i < count; i++) {
            int px = startX + i * step;
            graphics.fill(px, py, px + PIP_SIZE, py + PIP_SIZE, color);
        }
    }

    /** Eterna power window plus flag/exclusivity chips; {@code null} when there is nothing to show. */
    private static MutableComponent buildSecondLine(EnchantmentBrowserRecord record) {
        MutableComponent line = Component.empty();
        boolean any = false;

        if (!record.powerWindows().isEmpty()) {
            int lo = record.powerWindows().get(0)[0];
            int hi = record.powerWindows().get(record.powerWindows().size() - 1)[1];
            line.append(Component.literal("Eterna " + lo + "–" + hi).withStyle(ChatFormatting.GRAY));
            any = true;
        }
        if (record.isTreasure()) {
            any = appendChip(line, any, Component.translatable("gui.meridian.enchant_info.treasure")
                    .withStyle(ChatFormatting.GOLD));
        }
        for (String setName : record.exclusiveSetNames()) {
            any = appendChip(line, any, Component.literal(setName).withStyle(ChatFormatting.AQUA));
        }
        if (!record.isEnabled()) {
            any = appendChip(line, any, Component.translatable("gui.meridian.enchant_info.disabled")
                    .withStyle(ChatFormatting.RED));
        }
        return any ? line : null;
    }

    private static boolean appendChip(MutableComponent line, boolean any, Component chip) {
        if (any) {
            line.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
        }
        line.append(chip);
        return true;
    }
}
