package com.rfizzle.meridian.compat.rei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.client.InfusionCardRenderer;
import com.rfizzle.meridian.compat.common.InfusionBars;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * REI category that renders one {@link ReiEnchantingDisplay}. The card body — colored
 * stat-requirement bars, XP cost, keep-NBT badge — is drawn by the shared
 * {@link InfusionCardRenderer}, keeping it pixel-identical to the EMI and JEI entries; this class
 * only adds REI's recipe base, native slots, and per-bar hover tooltips.
 */
public final class ReiEnchantingCategory implements DisplayCategory<ReiEnchantingDisplay> {

    private static final int PANEL_PADDING = 10;

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
        return InfusionCardRenderer.WIDTH + PANEL_PADDING;
    }

    @Override
    public int getDisplayHeight() {
        return InfusionCardRenderer.HEIGHT + PANEL_PADDING;
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

        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) ->
                InfusionCardRenderer.draw(graphics, Minecraft.getInstance().font,
                        origin.x, origin.y, display.source())));

        List<InfusionBars.Bar> bars = InfusionCardRenderer.bars(display.source());
        for (int i = 0; i < bars.size(); i++) {
            widgets.add(Widgets.createTooltip(
                    new Rectangle(origin.x, origin.y + InfusionCardRenderer.barRowY(i),
                            InfusionCardRenderer.WIDTH, InfusionCardRenderer.BAR_ROW_STEP),
                    InfusionBars.tooltip(bars.get(i))));
        }

        return widgets;
    }
}
