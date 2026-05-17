package com.rfizzle.meridian.compat.emi;

import com.rfizzle.meridian.compat.common.RecipeInfoFormatter;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * EMI display for one {@code meridian:enchanting} or {@code keep_nbt_enchanting}
 * recipe (T-7.1.3 "each recipe shows input, output, stat requirements, XP cost").
 *
 * <p>Layout:
 * <pre>
 *   [input] → [output]     (y = 0; 18-tall slot row)
 *   Eterna: N               (y = 22+, x = 0; full width)
 *   Quanta: N
 *   Arcana: N
 *   XP cost: L levels
 * </pre>
 *
 * <p>Height is computed from {@link RecipeInfoFormatter#requirementLines} so recipes with fewer
 * gated axes shrink to fit — a recipe with only an Eterna floor leaves no empty Quanta/Arcana
 * rows. {@code keep_nbt_enchanting} recipes get an extra italic "preserves enchantments" badge.
 */
public final class EmiEnchantingRecipe extends BasicEmiRecipe {

    private static final int LINE_HEIGHT = 10;
    private static final int SLOT_ROW_HEIGHT = 22;
    private static final int PADDING = 4;

    private final TableCraftingDisplay display;
    private final List<String> lines;

    public EmiEnchantingRecipe(EmiRecipeCategory category, TableCraftingDisplay display) {
        super(category, display.recipeId(), 144, 0);
        this.display = display;
        this.lines = RecipeInfoFormatter.requirementLines(
                display.requirements(), display.maxRequirements(), display.xpCost());
        this.height = SLOT_ROW_HEIGHT + lines.size() * LINE_HEIGHT
                + (display.keepNbt() ? LINE_HEIGHT : 0) + PADDING;

        this.inputs.add(EmiIngredient.of(display.input()));
        this.outputs.add(EmiStack.of(display.result()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(EmiIngredient.of(display.input()), 0, 0);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 22, 1);
        widgets.addSlot(EmiStack.of(display.result()), 50, 0).recipeContext(this);

        int y = SLOT_ROW_HEIGHT;
        for (String raw : lines) {
            widgets.addText(Component.literal(raw), 0, y, 0x404040, false);
            y += LINE_HEIGHT;
        }
        if (display.keepNbt()) {
            widgets.addText(
                    Component.translatable("emi.meridian.recipe.keep_nbt")
                            .withStyle(ChatFormatting.ITALIC),
                    0, y, 0x555555, false);
        }
    }

    public TableCraftingDisplay display() {
        return display;
    }

    public List<String> lines() {
        return lines;
    }
}
