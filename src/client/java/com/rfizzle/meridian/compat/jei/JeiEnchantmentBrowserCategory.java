package com.rfizzle.meridian.compat.jei;

import com.rfizzle.meridian.compat.client.EnchantmentBrowserBooks;
import com.rfizzle.meridian.compat.client.EnchantmentBrowserCardRenderer;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserTooltip;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * JEI category that renders one {@link EnchantmentBrowserRecord}. The card face (name, level
 * pips, Eterna window, flag chips) is drawn by the shared
 * {@link EnchantmentBrowserCardRenderer}; this category only adds the compatible-items slot and
 * the on-hover detail tooltip, keeping it pixel-identical to the EMI and REI cards.
 */
public final class JeiEnchantmentBrowserCategory extends AbstractRecipeCategory<EnchantmentBrowserRecord> {

    public JeiEnchantmentBrowserCategory(mezz.jei.api.recipe.RecipeType<EnchantmentBrowserRecord> recipeType,
                                          Component title,
                                          IDrawable icon) {
        super(recipeType, title, icon, EnchantmentBrowserCardRenderer.WIDTH, EnchantmentBrowserCardRenderer.HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EnchantmentBrowserRecord record, IFocusGroup focuses) {
        // The enchanted book(s) are the OUTPUT so "show recipe" on a book navigates here; the
        // drawn slot cycles every level. Compatible items stay searchable but undrawn.
        builder.addSlot(RecipeIngredientRole.OUTPUT, EnchantmentBrowserCardRenderer.SLOT_X, EnchantmentBrowserCardRenderer.SLOT_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, EnchantmentBrowserBooks.forRecord(record));
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT)
                .addIngredients(VanillaTypes.ITEM_STACK, record.compatibleItems().stream().map(h -> h.value().getDefaultInstance()).toList());
    }

    @Override
    public void draw(EnchantmentBrowserRecord record, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        EnchantmentBrowserCardRenderer.draw(guiGraphics, Minecraft.getInstance().font, 0, 0, record);
    }

    // getTooltipStrings is the tooltip hook on JEI 19.27.x (the targeted JEI line); it carries a
    // forRemoval deprecation against a future JEI major. Suppressed until that bump lands and the
    // replacement API ships.
    @SuppressWarnings("removal")
    @Override
    public List<Component> getTooltipStrings(EnchantmentBrowserRecord record, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 0 && mouseX <= EnchantmentBrowserCardRenderer.WIDTH
                && mouseY >= 0 && mouseY <= EnchantmentBrowserCardRenderer.HEIGHT) {
            return EnchantmentBrowserTooltip.lines(record);
        }
        return List.of();
    }
}
