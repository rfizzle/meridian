package com.rfizzle.meridian.compat.rei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.RecipeInfoFormatter;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * REI category that renders one {@link ReiEnchantingDisplay}. The layout mirrors the EMI display
 * in {@code EmiEnchantingRecipe} — input slot, arrow, output slot, then the stat-requirement and
 * XP lines produced by {@link RecipeInfoFormatter}. That shared formatter is why shelf and tome
 * tabs look the same to a player flipping between EMI and REI.
 *
 * <p>A single class backs both the Shelves and Tomes tabs — the only difference between them is
 * the category identifier, title, and icon passed at construction.
 */
public final class ReiEnchantingCategory implements DisplayCategory<ReiEnchantingDisplay> {

    private static final int LINE_HEIGHT = 10;
    private static final int SLOT_ROW_HEIGHT = 22;
    private static final int TEXT_X_OFFSET = 0;
    private static final int PADDING = 6;

    private static final ResourceLocation ICON_TEXTURE = Meridian.id("icon.png");
    private static final Renderer MOD_ICON = (GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) ->
            graphics.blit(ICON_TEXTURE, bounds.x, bounds.y, 0, 0,
                    bounds.width, bounds.height, bounds.width, bounds.height);

    private final CategoryIdentifier<ReiEnchantingDisplay> id;
    private final Component title;

    public ReiEnchantingCategory(CategoryIdentifier<ReiEnchantingDisplay> id, Component title) {
        this.id = id;
        this.title = title;
    }

    @Override
    public CategoryIdentifier<? extends ReiEnchantingDisplay> getCategoryIdentifier() {
        return id;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return MOD_ICON;
    }

    @Override
    public int getDisplayWidth(ReiEnchantingDisplay display) {
        return 150;
    }

    @Override
    public int getDisplayHeight() {
        return SLOT_ROW_HEIGHT + 4 * LINE_HEIGHT + PADDING;
    }

    @Override
    public List<Widget> setupDisplay(ReiEnchantingDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        Point origin = new Point(bounds.x + 5, bounds.y + 5);
        widgets.add(Widgets.createSlot(new Point(origin.x, origin.y))
                .entries(display.getInputEntries().get(0))
                .markInput());
        widgets.add(Widgets.createArrow(new Point(origin.x + 22, origin.y)));
        widgets.add(Widgets.createSlot(new Point(origin.x + 50, origin.y))
                .entries(display.getOutputEntries().get(0))
                .markOutput());

        List<String> lines = RecipeInfoFormatter.requirementLines(
                display.source().requirements(),
                display.source().maxRequirements(),
                display.source().xpCost());
        int textX = origin.x + TEXT_X_OFFSET;
        int y = origin.y + SLOT_ROW_HEIGHT;
        for (String raw : lines) {
            widgets.add(Widgets.createLabel(new Point(textX, y), Component.literal(raw))
                    .leftAligned()
                    .color(0x404040, 0xBBBBBB)
                    .noShadow());
            y += LINE_HEIGHT;
        }
        if (display.source().keepNbt()) {
            widgets.add(Widgets.createLabel(new Point(textX, y),
                            Component.translatable("rei.meridian.recipe.keep_nbt")
                                    .withStyle(ChatFormatting.ITALIC))
                    .leftAligned()
                    .color(0x555555, 0xAAAAAA)
                    .noShadow());
        }

        return widgets;
    }
}
