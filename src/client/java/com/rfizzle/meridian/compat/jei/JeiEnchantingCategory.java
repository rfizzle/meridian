package com.rfizzle.meridian.compat.jei;

import com.rfizzle.meridian.compat.common.RecipeInfoFormatter;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * JEI category that renders one {@link TableCraftingDisplay}. Shape mirrors the EMI
 * ({@code EmiEnchantingRecipe}) and REI ({@code ReiEnchantingCategory}) displays so a player flipping
 * viewers sees the same panel: input slot, arrow, output slot, then the stat/XP lines produced by
 * {@link RecipeInfoFormatter}. Shared formatter = shared wording across all three adapters.
 *
 * <p>One class backs both the Shelves and Tomes tabs — the only difference between them is the
 * {@link RecipeType}, title, and icon passed at construction.
 */
public final class JeiEnchantingCategory extends AbstractRecipeCategory<TableCraftingDisplay> {

    private static final int WIDTH = 144;
    private static final int LINE_HEIGHT = 10;
    private static final int SLOT_ROW_HEIGHT = 22;
    private static final int PADDING = 4;
    private static final int TEXT_X = 0;
    private static final int MAX_LINES = 5;

    public JeiEnchantingCategory(RecipeType<TableCraftingDisplay> recipeType,
                                 Component title,
                                 IDrawable icon) {
        super(recipeType,
                title,
                icon,
                WIDTH,
                SLOT_ROW_HEIGHT + MAX_LINES * LINE_HEIGHT + PADDING);
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
        Font font = Minecraft.getInstance().font;
        List<String> lines = RecipeInfoFormatter.requirementLines(
                display.requirements(), display.maxRequirements(), display.xpCost());
        int y = SLOT_ROW_HEIGHT;
        for (String raw : lines) {
            graphics.drawString(font, Component.literal(raw), TEXT_X, y, 0x404040, false);
            y += LINE_HEIGHT;
        }
        if (display.keepNbt()) {
            graphics.drawString(font,
                    Component.translatable("jei.meridian.recipe.keep_nbt")
                            .withStyle(ChatFormatting.ITALIC),
                    TEXT_X, y, 0x555555, false);
        }
    }
}
