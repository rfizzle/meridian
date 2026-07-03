package com.rfizzle.meridian.compat.emi;

import com.rfizzle.meridian.client.tooltip.EnchantmentDescriptions;
import com.rfizzle.meridian.compat.client.EnchantmentBrowserBooks;
import com.rfizzle.meridian.compat.client.EnchantmentBrowserCardRenderer;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserTooltip;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * EMI display for one enchantment in the "Enchantments" browser. The card face is drawn by the
 * shared {@link EnchantmentBrowserCardRenderer}; detailed power thresholds and exclusivity are
 * shown on hover. Kept pixel-identical to the JEI and REI cards.
 */
public final class EmiEnchantmentBrowserRecipe extends BasicEmiRecipe {

    private final EnchantmentBrowserRecord record;

    public EmiEnchantmentBrowserRecipe(EmiRecipeCategory category, EnchantmentBrowserRecord record) {
        super(category, record.ench().unwrapKey().orElseThrow().location(),
                EnchantmentBrowserCardRenderer.WIDTH, EnchantmentBrowserCardRenderer.height(record));
        this.record = record;
        // Compatible items as inputs keep "search enchantments by item" working; the enchanted
        // book(s) as outputs let "show recipe" on a book navigate to this entry.
        for (var itemHolder : record.compatibleItems()) {
            this.inputs.add(EmiStack.of(itemHolder.value()));
        }
        for (ItemStack book : EnchantmentBrowserBooks.forRecord(record)) {
            this.outputs.add(EmiStack.of(book));
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addDrawable(0, 0, EnchantmentBrowserCardRenderer.WIDTH, this.height,
                        (graphics, mx, my, delta) ->
                                EnchantmentBrowserCardRenderer.draw(graphics, Minecraft.getInstance().font, 0, 0, record))
                .tooltip((mx, my) -> getTooltip());

        List<ItemStack> books = EnchantmentBrowserBooks.forRecord(record);
        if (!books.isEmpty()) {
            widgets.addSlot(EmiIngredient.of(books.stream().map(EmiStack::of).toList()),
                            EnchantmentBrowserCardRenderer.SLOT_X, EnchantmentBrowserCardRenderer.SLOT_Y)
                    .recipeContext(this);
        }
    }

    private List<ClientTooltipComponent> getTooltip() {
        return EnchantmentBrowserTooltip.lines(record, EnchantmentDescriptions::resolve).stream()
                .map(c -> ClientTooltipComponent.create(c.getVisualOrderText()))
                .toList();
    }
}
