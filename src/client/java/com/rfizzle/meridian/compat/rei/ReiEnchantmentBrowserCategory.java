package com.rfizzle.meridian.compat.rei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.client.EnchantmentBrowserCardRenderer;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserTooltip;
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
 * REI category that renders one {@link ReiEnchantmentBrowserDisplay}. The card face is drawn by
 * the shared {@link EnchantmentBrowserCardRenderer}; this category adds the REI recipe base, the
 * compatible-items slot, and the on-hover detail tooltip, keeping it pixel-identical to the JEI
 * and EMI cards.
 */
public final class ReiEnchantmentBrowserCategory implements DisplayCategory<ReiEnchantmentBrowserDisplay> {

    private static final ResourceLocation ICON_TEXTURE = Meridian.id("icon.png");
    private static final Renderer MOD_ICON = (GuiGraphics graphics, Rectangle bounds, int mouseX, int mouseY, float delta) ->
            graphics.blit(ICON_TEXTURE, bounds.x, bounds.y, 0, 0,
                    bounds.width, bounds.height, bounds.width, bounds.height);

    private final CategoryIdentifier<ReiEnchantmentBrowserDisplay> identifier;
    private final Component title;

    public ReiEnchantmentBrowserCategory(CategoryIdentifier<ReiEnchantmentBrowserDisplay> identifier, Component title) {
        this.identifier = identifier;
        this.title = title;
    }

    @Override
    public Renderer getIcon() {
        return MOD_ICON;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public CategoryIdentifier<? extends ReiEnchantmentBrowserDisplay> getCategoryIdentifier() {
        return identifier;
    }

    @Override
    public int getDisplayHeight() {
        return EnchantmentBrowserCardRenderer.HEIGHT;
    }

    @Override
    public List<Widget> setupDisplay(ReiEnchantmentBrowserDisplay display, Rectangle bounds) {
        EnchantmentBrowserRecord record = display.record();
        List<Widget> widgets = new ArrayList<>();

        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createDrawableWidget((graphics, mouseX, mouseY, delta) ->
                EnchantmentBrowserCardRenderer.draw(graphics, Minecraft.getInstance().font, bounds.x, bounds.y, record)));

        // Draw the enchanted book(s) as the output so "view recipe" on a book navigates here;
        // compatible items stay searchable via the display's input entries but are not drawn.
        if (!display.getOutputEntries().isEmpty()) {
            widgets.add(Widgets.createSlot(new Point(
                            bounds.x + EnchantmentBrowserCardRenderer.SLOT_X,
                            bounds.y + EnchantmentBrowserCardRenderer.SLOT_Y))
                    .entries(display.getOutputEntries().get(0))
                    .markOutput());
        }

        widgets.add(Widgets.createTooltip(bounds, EnchantmentBrowserTooltip.lines(record)));

        return widgets;
    }
}
