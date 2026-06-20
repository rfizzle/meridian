package com.rfizzle.meridian.compat.jei;

import com.rfizzle.meridian.compat.client.EnchantmentBrowserCardRenderer;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
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
        builder.addSlot(RecipeIngredientRole.CATALYST, EnchantmentBrowserCardRenderer.SLOT_X, EnchantmentBrowserCardRenderer.SLOT_Y)
                .addIngredients(VanillaTypes.ITEM_STACK, record.compatibleItems().stream().map(h -> h.value().getDefaultInstance()).toList());
    }

    @Override
    public void draw(EnchantmentBrowserRecord record, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        EnchantmentBrowserCardRenderer.draw(guiGraphics, Minecraft.getInstance().font, 0, 0, record);
    }

    @Override
    public List<Component> getTooltipStrings(EnchantmentBrowserRecord record, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 0 && mouseX <= EnchantmentBrowserCardRenderer.WIDTH
                && mouseY >= 0 && mouseY <= EnchantmentBrowserCardRenderer.HEIGHT) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(record.ench().value().description().copy().withStyle(ChatFormatting.WHITE));

            if (!record.isEnabled()) {
                tooltip.add(Component.translatable("tooltip.meridian.enchlib.disabled").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }

            if (record.isTreasure()) {
                tooltip.add(Component.translatable("info.meridian.shelf.allows_treasure").withStyle(ChatFormatting.GOLD));
            }

            if (!record.exclusiveSetNames().isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("gui.meridian.enchant_info.exclusive", "").withStyle(ChatFormatting.GRAY));
                for (String setName : record.exclusiveSetNames()) {
                    tooltip.add(Component.literal(" - " + setName).withStyle(ChatFormatting.AQUA));
                }
            }

            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.meridian.enchant_info.power_header").withStyle(ChatFormatting.GRAY));
            for (int i = 0; i < record.powerWindows().size(); i++) {
                int level = i + 1;
                int[] window = record.powerWindows().get(i);
                tooltip.add(Component.literal("Level " + level + ": Eterna " + window[0] + " - " + window[1]).withStyle(ChatFormatting.DARK_GREEN));
            }

            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.meridian.enchant_info.stats_header").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("gui.meridian.enchant_info.stats_global").withStyle(ChatFormatting.DARK_AQUA));

            return tooltip;
        }
        return List.of();
    }
}
