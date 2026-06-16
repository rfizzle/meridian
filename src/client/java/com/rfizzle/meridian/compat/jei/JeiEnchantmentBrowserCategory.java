package com.rfizzle.meridian.compat.jei;

import com.rfizzle.meridian.Meridian;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category that renders one {@link EnchantmentBrowserRecord}.
 */
public final class JeiEnchantmentBrowserCategory extends AbstractRecipeCategory<EnchantmentBrowserRecord> {

    private static final ResourceLocation OVERRIDE_TEXTURE = Meridian.id("textures/gui/enchanting_table.png");

    private final IDrawable overrideIcon;

    public JeiEnchantmentBrowserCategory(mezz.jei.api.recipe.RecipeType<EnchantmentBrowserRecord> recipeType,
                                          Component title,
                                          IDrawable icon,
                                          IGuiHelper guiHelper) {
        super(recipeType, title, icon, 144, 26);
        this.overrideIcon = guiHelper.createDrawable(OVERRIDE_TEXTURE, 224, 0, 16, 16);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EnchantmentBrowserRecord record, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 0, 0)
                .addIngredients(VanillaTypes.ITEM_STACK, record.compatibleItems().stream().map(h -> h.value().getDefaultInstance()).toList());
    }

    @Override
    public void draw(EnchantmentBrowserRecord record, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        Component name = record.ench().value().description();
        MutableComponent styledName = name.copy();
        if (!record.isEnabled()) {
            styledName.withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.RED);
        } else if (record.isTreasure()) {
            styledName.withStyle(ChatFormatting.GOLD);
        }

        guiGraphics.drawString(font, styledName, 2, 2, 0xFFFFFF);

        int x = 2;
        String levels = "I-" + record.maxLevel();
        if (record.maxLevel() == 1) levels = "I";
        Component levelsComp = Component.literal(levels).withStyle(ChatFormatting.GRAY);
        guiGraphics.drawString(font, levelsComp, x, 14, 0xFFFFFF);
        x += font.width(levelsComp) + 6;

        for (String setName : record.exclusiveSetNames()) {
            Component tagText = Component.literal("[" + setName + "]").withStyle(ChatFormatting.AQUA);
            guiGraphics.drawString(font, tagText, x, 14, 0xFFFFFF);
            x += font.width(tagText) + 4;
        }

        if (record.isConfigOverridden()) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(128, 2, 0);
            guiGraphics.pose().scale(0.75f, 0.75f, 1);
            overrideIcon.draw(guiGraphics, 0, 0);
            guiGraphics.pose().popPose();
        }
    }

    @Override
    public List<Component> getTooltipStrings(EnchantmentBrowserRecord record, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= 0 && mouseX <= 144 && mouseY >= 0 && mouseY <= 26) {
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

            if (record.isConfigOverridden()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("gui.meridian.enchant_info.overridden").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GRAY));
            }

            return tooltip;
        }
        return List.of();
    }
}
