package com.rfizzle.meridian.compat.emi;

import com.rfizzle.meridian.compat.client.EnchantmentBrowserBooks;
import com.rfizzle.meridian.compat.client.EnchantmentBrowserCardRenderer;
import com.rfizzle.meridian.compat.common.EnchantmentBrowserRecord;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
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

        return tooltip.stream().map(c -> ClientTooltipComponent.create(c.getVisualOrderText())).toList();
    }
}
