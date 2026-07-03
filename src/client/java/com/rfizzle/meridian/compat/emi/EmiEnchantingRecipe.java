package com.rfizzle.meridian.compat.emi;

import com.rfizzle.meridian.compat.client.InfusionCardRenderer;
import com.rfizzle.meridian.compat.common.InfusionBars;
import com.rfizzle.meridian.compat.common.TableCraftingDisplay;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import java.util.List;

/**
 * EMI display for one {@code meridian:enchanting} or {@code keep_nbt_enchanting} recipe. The
 * card body — colored stat-requirement bars, XP cost, keep-NBT badge — is drawn by the shared
 * {@link InfusionCardRenderer}, keeping it pixel-identical to the REI and JEI entries; this class
 * only adds EMI's native slots and per-bar hover tooltips. Height is computed per recipe so an
 * entry with one gated axis leaves no empty rows.
 */
public final class EmiEnchantingRecipe extends BasicEmiRecipe {

    private final TableCraftingDisplay display;

    public EmiEnchantingRecipe(EmiRecipeCategory category, TableCraftingDisplay display) {
        super(category, display.recipeId(),
                InfusionCardRenderer.WIDTH, InfusionCardRenderer.height(display));
        this.display = display;

        this.inputs.add(EmiIngredient.of(display.input()));
        this.outputs.add(EmiStack.of(display.result()));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(EmiIngredient.of(display.input()), 0, 0);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 22, 1);
        widgets.addSlot(EmiStack.of(display.result()), 50, 0).recipeContext(this);

        widgets.addDrawable(0, 0, InfusionCardRenderer.WIDTH, this.height,
                (graphics, mouseX, mouseY, delta) ->
                        InfusionCardRenderer.draw(graphics, Minecraft.getInstance().font, 0, 0, display));

        // One empty drawable per bar row carries its hover tooltip — bounds-based, so no
        // coordinate math against EMI's tooltip callback is needed.
        List<InfusionBars.Bar> bars = InfusionCardRenderer.bars(display);
        for (int i = 0; i < bars.size(); i++) {
            InfusionBars.Bar bar = bars.get(i);
            widgets.addDrawable(0, InfusionCardRenderer.barRowY(i),
                            InfusionCardRenderer.WIDTH, InfusionCardRenderer.BAR_ROW_STEP,
                            (graphics, mouseX, mouseY, delta) -> {
                            })
                    .tooltip((mouseX, mouseY) -> InfusionBars.tooltip(bar).stream()
                            .map(c -> ClientTooltipComponent.create(c.getVisualOrderText()))
                            .toList());
        }
    }

    public TableCraftingDisplay display() {
        return display;
    }
}
