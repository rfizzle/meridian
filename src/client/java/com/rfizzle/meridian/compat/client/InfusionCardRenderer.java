package com.rfizzle.meridian.compat.client;

import com.rfizzle.meridian.compat.common.InfusionBars;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import com.rfizzle.meridian.enchanting.RealEnchantmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Shared face renderer for one Infusions entry in the JEI / EMI / REI viewers — the Infusion
 * analogue of {@link EnchantmentBrowserCardRenderer}. Each viewer keeps its native input/arrow/
 * output slot row on top, then defers the body to {@link #draw}:
 *
 * <pre>
 *   [input] → [output]                 (slot row, drawn by the viewer)
 *   Eterna   ▮▮▮▮▮▮▮▯▯▯▯▯▯▯▯▯▯▯▯       (colored min fill + translucent max window)
 *   Arcana   ▮▮▮▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯▯
 *   XP cost: 3 levels
 *   Preserves enchantments             (keep-NBT recipes only)
 * </pre>
 *
 * <p>Only gated axes draw a bar ({@link InfusionBars#of}), so recipes shrink to their real
 * requirements. Bars are drawn in code ({@link GuiGraphics#fill}) — a dark track, a colored fill
 * scaled to the axis max, and a translucent ghost segment for a bounded window — keeping the stat
 * colors centralized in {@code MeridianStatColors} with no texture work. Text is light + shadowed
 * to stay legible on both JEI's light panel and the dark EMI / REI panels, matching the browser
 * cards.
 *
 * <p>JEI and REI categories are a fixed height ({@link #HEIGHT}, sized for the fullest entry);
 * EMI sizes each recipe to its own {@link #height(TableCraftingDisplay)}. Per-bar hover tooltips
 * are resolved by the viewers via {@link #barAt} / {@link #barRowY} against
 * {@link InfusionBars#tooltip}.
 */
public final class InfusionCardRenderer {

    /** Card width shared by all three viewers. */
    public static final int WIDTH = 144;

    /** Height of the viewer-drawn slot row above the body. */
    public static final int SLOT_ROW_HEIGHT = 22;

    /** Baseline-to-baseline step of one bar row — also the hover-region height of a bar. */
    public static final int BAR_ROW_STEP = 12;

    private static final int LINE_STEP = 10;
    private static final int PAD_BOTTOM = 4;
    private static final int LABEL_X = 0;
    private static final int BAR_X = 50;
    private static final int BAR_RIGHT_PAD = 2;
    private static final int BAR_HEIGHT = 7;
    private static final int MAX_BARS = 3;

    /** Fixed height for the fixed-size JEI / REI categories — sized for the fullest entry. */
    public static final int HEIGHT = heightFor(MAX_BARS, true);

    private static final int COLOR_TRACK = 0xFF2B2B2B;
    private static final int COLOR_XP = 0xFFE8E8E8;
    private static final int COLOR_KEEP_NBT = 0xFFAAAAAA;
    private static final int GHOST_ALPHA = 0x66;

    private InfusionCardRenderer() {
    }

    /** The bar models for one display, scaled against the live table's Eterna cap. */
    public static List<InfusionBars.Bar> bars(TableCraftingDisplay display) {
        return InfusionBars.of(display.requirements(), display.maxRequirements(),
                RealEnchantmentHelper.resolveMaxEterna());
    }

    /**
     * Draws the bar rows, XP line, and keep-NBT badge with the card's top-left corner at
     * ({@code x}, {@code y}). The caller is responsible for the slot row above
     * {@link #SLOT_ROW_HEIGHT}.
     */
    public static void draw(GuiGraphics graphics, Font font, int x, int y, TableCraftingDisplay display) {
        List<InfusionBars.Bar> bars = bars(display);
        int rowY = y + SLOT_ROW_HEIGHT;
        for (InfusionBars.Bar bar : bars) {
            graphics.drawString(font, Component.translatable(bar.labelKey()),
                    x + LABEL_X, rowY + 1, bar.color());

            int trackLeft = x + BAR_X;
            int trackRight = x + WIDTH - BAR_RIGHT_PAD;
            int trackWidth = trackRight - trackLeft;
            int barTop = rowY + 1;
            int barBottom = barTop + BAR_HEIGHT;
            graphics.fill(trackLeft, barTop, trackRight, barBottom, COLOR_TRACK);

            int minEnd = trackLeft + Math.round(trackWidth * bar.minFraction());
            if (bar.bounded()) {
                int maxEnd = trackLeft + Math.round(trackWidth * bar.maxFraction());
                if (maxEnd > minEnd) {
                    graphics.fill(minEnd, barTop, maxEnd, barBottom,
                            (GHOST_ALPHA << 24) | (bar.color() & 0xFFFFFF));
                }
            }
            if (minEnd > trackLeft) {
                graphics.fill(trackLeft, barTop, minEnd, barBottom, bar.color());
            }
            rowY += BAR_ROW_STEP;
        }

        graphics.drawString(font,
                Component.translatable("gui.meridian.infusion.xp_cost", display.xpCost()),
                x + LABEL_X, rowY, COLOR_XP);
        rowY += LINE_STEP;
        if (display.keepNbt()) {
            graphics.drawString(font,
                    Component.translatable("gui.meridian.infusion.keep_nbt")
                            .withStyle(ChatFormatting.ITALIC),
                    x + LABEL_X, rowY, COLOR_KEEP_NBT);
        }
    }

    /** Dynamic card height for the per-recipe EMI display: only the rows actually shown. */
    public static int height(TableCraftingDisplay display) {
        return heightFor(bars(display).size(), display.keepNbt());
    }

    /** Top edge of bar row {@code index}, relative to the card's top-left corner. */
    public static int barRowY(int index) {
        return SLOT_ROW_HEIGHT + index * BAR_ROW_STEP;
    }

    /** The bar whose row contains ({@code mouseX}, {@code mouseY}), relative to the card origin. */
    public static Optional<InfusionBars.Bar> barAt(TableCraftingDisplay display, double mouseX, double mouseY) {
        if (mouseX < 0 || mouseX >= WIDTH || mouseY < SLOT_ROW_HEIGHT) {
            return Optional.empty();
        }
        List<InfusionBars.Bar> bars = bars(display);
        int index = (int) ((mouseY - SLOT_ROW_HEIGHT) / BAR_ROW_STEP);
        return index < bars.size() ? Optional.of(bars.get(index)) : Optional.empty();
    }

    private static int heightFor(int barCount, boolean keepNbt) {
        return SLOT_ROW_HEIGHT + barCount * BAR_ROW_STEP + LINE_STEP
                + (keepNbt ? LINE_STEP : 0) + PAD_BOTTOM;
    }
}
