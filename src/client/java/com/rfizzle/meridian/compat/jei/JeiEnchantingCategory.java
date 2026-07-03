package com.rfizzle.meridian.compat.jei;

import com.rfizzle.meridian.compat.client.InfusionCardRenderer;
import com.rfizzle.meridian.compat.common.InfusionBars;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * JEI category that renders one {@link TableCraftingDisplay}. The card body — colored
 * stat-requirement bars, XP cost, keep-NBT badge — is drawn by the shared
 * {@link InfusionCardRenderer}, keeping it pixel-identical to the EMI and REI entries; this class
 * only adds JEI's native slots and resolves the per-bar hover tooltip.
 *
 * <p>One class backs both the Shelves and Tomes tabs — the only difference between them is the
 * {@link RecipeType}, title, and icon passed at construction.
 */
public final class JeiEnchantingCategory extends AbstractRecipeCategory<TableCraftingDisplay> {

    public JeiEnchantingCategory(RecipeType<TableCraftingDisplay> recipeType,
                                 Component title,
                                 IDrawable icon) {
        super(recipeType, title, icon, InfusionCardRenderer.WIDTH, InfusionCardRenderer.HEIGHT);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TableCraftingDisplay display, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
                .addIngredients(display.input());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 51, 1)
                .addItemStack(display.result().copy());
    }

    @Override
    public void draw(TableCraftingDisplay display, IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        InfusionCardRenderer.draw(graphics, Minecraft.getInstance().font, 0, 0, display);
    }

    // getTooltipStrings is the tooltip hook on JEI 19.27.x (the targeted JEI line); it carries a
    // forRemoval deprecation against a future JEI major. Suppressed until that bump lands and the
    // replacement API ships.
    @SuppressWarnings("removal")
    @Override
    public List<Component> getTooltipStrings(TableCraftingDisplay display, IRecipeSlotsView slots,
                                             double mouseX, double mouseY) {
        return InfusionCardRenderer.barAt(display, mouseX, mouseY)
                .map(InfusionBars::tooltip)
                .orElse(List.of());
    }
}
